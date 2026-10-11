"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "@/lib/client";
import type { LiveSession } from "@/lib/types";

/**
 * Kết nối vào phòng học trực tuyến.
 *
 * Hình và tiếng đi thẳng giữa các trình duyệt (WebRTC, mỗi cặp người một kết nối). Máy chủ chỉ
 * chuyển tin báo hiệu qua luồng SSE `/api/live-sessions/{id}/room` và các lệnh POST bên cạnh.
 *
 * Quy ước để hai bên không cùng gửi offer: người mới vào gửi offer cho từng người có sẵn trong
 * phòng (danh sách nằm trong sự kiện `welcome`), người có sẵn chỉ trả lời.
 *
 * Mỗi kết nối luôn có đúng hai transceiver (tiếng, hình) ở chế độ gửi-nhận. Bật/tắt camera,
 * chia sẻ màn hình chỉ là thay track trên transceiver đó (`replaceTrack`), không phải thương
 * lượng lại.
 */

// STUN công khai giúp hai máy khác mạng tìm đường tới nhau. Cùng máy hoặc cùng mạng LAN thì
// không cần, và mất mạng ngoài cũng không làm hỏng kết nối nội bộ.
const ICE_SERVERS: RTCIceServer[] = [{ urls: "stun:stun.l.google.com:19302" }];
// Gom tin báo hiệu (nhất là ICE candidate) thành một request, khỏi vượt hạn mức 20 request/giây của gateway.
const SIGNAL_BATCH_MS = 60;
const MAX_SIGNALS_PER_REQUEST = 50;
const RECONNECT_MS = 2000;

export interface Participant {
  peerId: string;
  userId: number;
  name: string;
  host: boolean;
  mic: boolean;
  cam: boolean;
  hand: boolean;
  screen: boolean;
}

export interface RemotePeer extends Participant {
  stream: MediaStream;
  connection: RTCPeerConnectionState;
}

export interface ChatMessage {
  id: number;
  peerId: string;
  userId: number;
  name: string;
  host: boolean;
  text: string;
  sentAt: string;
}

export interface LocalState {
  mic: boolean;
  cam: boolean;
  hand: boolean;
  screen: boolean;
}

export type RoomPhase = "connecting" | "joined" | "reconnecting" | "ended" | "left";

type SignalType = "offer" | "answer" | "candidate";
interface OutgoingSignal {
  to: string;
  type: SignalType;
  data: unknown;
}
interface Connection {
  pc: RTCPeerConnection;
  stream: MediaStream;
  pending: RTCIceCandidateInit[];
  // Xử lý tin của cùng một người theo đúng thứ tự: candidate đến lúc offer chưa xong thì phải chờ.
  chain: Promise<void>;
}

export function useLiveRoom({
  session,
  localStream,
  initial,
  onMuteRequest,
}: {
  session: LiveSession;
  localStream: MediaStream | null;
  initial: { mic: boolean; cam: boolean };
  onMuteRequest?: (by: string) => void;
}) {
  const base = `/api/live-sessions/${session.id}/room`;
  const [phase, setPhase] = useState<RoomPhase>("connecting");
  const [peers, setPeers] = useState<Record<string, RemotePeer>>({});
  const [chat, setChat] = useState<ChatMessage[]>([]);
  const [local, setLocal] = useState<LocalState>({ ...initial, hand: false, screen: false });
  const [screenStream, setScreenStream] = useState<MediaStream | null>(null);
  const [myPeerId, setMyPeerId] = useState<string | null>(null);

  const peerIdRef = useRef<string | null>(null);
  const connections = useRef(new Map<string, Connection>());
  const outbox = useRef<OutgoingSignal[]>([]);
  const flushTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const localRef = useRef<LocalState>({ ...initial, hand: false, screen: false });
  const videoTrackRef = useRef<MediaStreamTrack | null>(null);
  const muteRequestRef = useRef(onMuteRequest);
  // Ngắt luồng SSE từ ngoài effect (bấm "Rời lớp").
  const stopRef = useRef<() => void>(() => {});

  useEffect(() => {
    muteRequestRef.current = onMuteRequest;
  }, [onMuteRequest]);

  const audioTrack = useCallback(() => localStream?.getAudioTracks()[0] ?? null, [localStream]);
  const cameraTrack = useCallback(() => localStream?.getVideoTracks()[0] ?? null, [localStream]);

  const updatePeer = useCallback((peerId: string, patch: Partial<RemotePeer>) => {
    setPeers((current) => (current[peerId] ? { ...current, [peerId]: { ...current[peerId], ...patch } } : current));
  }, []);

  const flush = useCallback(() => {
    flushTimer.current = null;
    const from = peerIdRef.current;
    if (!from || outbox.current.length === 0) return;
    while (outbox.current.length > 0) {
      const messages = outbox.current.splice(0, MAX_SIGNALS_PER_REQUEST);
      api(`${base}/signal`, { method: "POST", body: { from, messages } }).catch(() => {
        // Mất một gói báo hiệu thì kết nối đó rơi vào "failed" và tự thử lại (xem onconnectionstatechange).
      });
    }
  }, [base]);

  const queueSignal = useCallback(
    (to: string, type: SignalType, data: unknown) => {
      outbox.current.push({ to, type, data });
      if (!flushTimer.current) flushTimer.current = setTimeout(flush, SIGNAL_BATCH_MS);
    },
    [flush],
  );

  const sendState = useCallback(() => {
    const from = peerIdRef.current;
    if (!from) return;
    api(`${base}/state`, { method: "POST", body: { from, ...localRef.current } }).catch(() => {});
  }, [base]);

  const changeLocal = useCallback(
    (patch: Partial<LocalState>) => {
      localRef.current = { ...localRef.current, ...patch };
      setLocal(localRef.current);
      sendState();
    },
    [sendState],
  );

  /** Gắn track đang phát của mình vào hai transceiver của một kết nối. */
  const attachLocalTracks = useCallback(
    async (pc: RTCPeerConnection) => {
      for (const transceiver of pc.getTransceivers()) {
        const kind = transceiver.receiver.track.kind;
        transceiver.direction = "sendrecv";
        await transceiver.sender.replaceTrack(kind === "audio" ? audioTrack() : videoTrackRef.current);
      }
    },
    [audioTrack],
  );

  const closeConnection = useCallback((peerId: string) => {
    const connection = connections.current.get(peerId);
    if (!connection) return;
    connection.pc.close();
    connections.current.delete(peerId);
  }, []);

  const offer = useCallback(
    async (peerId: string, connection: Connection, iceRestart = false) => {
      const description = await connection.pc.createOffer({ iceRestart });
      await connection.pc.setLocalDescription(description);
      queueSignal(peerId, "offer", connection.pc.localDescription?.toJSON());
    },
    [queueSignal],
  );

  const createConnection = useCallback(
    (peerId: string, initiator: boolean): Connection => {
      closeConnection(peerId);
      const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS });
      const connection: Connection = { pc, stream: new MediaStream(), pending: [], chain: Promise.resolve() };
      connections.current.set(peerId, connection);

      pc.onicecandidate = (event) => {
        if (event.candidate) queueSignal(peerId, "candidate", event.candidate.toJSON());
      };
      pc.ontrack = (event) => {
        // Luồng mới mỗi lần có track: thẻ <video> nhận srcObject mới nên chắc chắn phát track vừa thêm.
        if (!connection.stream.getTracks().includes(event.track)) {
          connection.stream = new MediaStream([...connection.stream.getTracks(), event.track]);
        }
        updatePeer(peerId, { stream: connection.stream });
      };
      pc.onconnectionstatechange = () => {
        updatePeer(peerId, { connection: pc.connectionState });
        // Mạng đổi giữa chừng: người gửi offer ban đầu thử lại đường đi mới.
        if (pc.connectionState === "failed" && initiator && connections.current.get(peerId) === connection) {
          connection.chain = connection.chain.then(() => offer(peerId, connection, true)).catch(() => {});
        }
      };
      updatePeer(peerId, { stream: connection.stream, connection: "new" });

      if (initiator) {
        pc.addTransceiver("audio", { direction: "sendrecv" });
        pc.addTransceiver("video", { direction: "sendrecv" });
        connection.chain = attachLocalTracks(pc)
          .then(() => offer(peerId, connection))
          .catch(() => {});
      }
      return connection;
    },
    [attachLocalTracks, closeConnection, offer, queueSignal, updatePeer],
  );

  const handleSignal = useCallback(
    (from: string, type: SignalType, data: unknown) => {
      const connection =
        connections.current.get(from) ?? (type === "offer" ? createConnection(from, false) : undefined);
      if (!connection) return;
      const { pc } = connection;
      connection.chain = connection.chain
        .then(async () => {
          if (type === "offer") {
            await pc.setRemoteDescription(data as RTCSessionDescriptionInit);
            await attachLocalTracks(pc);
            await pc.setLocalDescription(await pc.createAnswer());
            queueSignal(from, "answer", pc.localDescription?.toJSON());
          } else if (type === "answer") {
            if (pc.signalingState !== "have-local-offer") return;
            await pc.setRemoteDescription(data as RTCSessionDescriptionInit);
          } else {
            if (!pc.remoteDescription) {
              connection.pending.push(data as RTCIceCandidateInit);
              return;
            }
            await pc.addIceCandidate(data as RTCIceCandidateInit);
            return;
          }
          for (const candidate of connection.pending.splice(0)) {
            await pc.addIceCandidate(candidate).catch(() => {});
          }
        })
        .catch(() => {});
    },
    [attachLocalTracks, createConnection, queueSignal],
  );

  const teardown = useCallback(() => {
    for (const peerId of [...connections.current.keys()]) closeConnection(peerId);
    outbox.current = [];
    if (flushTimer.current) clearTimeout(flushTimer.current);
    flushTimer.current = null;
  }, [closeConnection]);

  const sendLeave = useCallback(() => {
    const from = peerIdRef.current;
    if (!from) return;
    peerIdRef.current = null;
    // keepalive: request vẫn đi khi trang đang đóng.
    fetch(`${base}/leave`, {
      method: "POST",
      keepalive: true,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ from }),
    }).catch(() => {});
  }, [base]);

  useEffect(() => {
    videoTrackRef.current = cameraTrack();
    let stopped = false;
    let source: EventSource | null = null;
    let retry: ReturnType<typeof setTimeout> | null = null;

    const on = <T,>(name: string, handler: (data: T) => void) =>
      source?.addEventListener(name, (event) => {
        if (!stopped) handler(JSON.parse((event as MessageEvent<string>).data) as T);
      });

    const open = () => {
      const query = new URLSearchParams({ mic: String(localRef.current.mic), cam: String(localRef.current.cam) });
      source = new EventSource(`${base}?${query}`);

      on<{ peerId: string; participants: Participant[]; chat: ChatMessage[] }>("welcome", (data) => {
        teardown();
        peerIdRef.current = data.peerId;
        setMyPeerId(data.peerId);
        setPeers(
          Object.fromEntries(
            data.participants.map((p) => [p.peerId, { ...p, stream: new MediaStream(), connection: "new" }]),
          ),
        );
        setChat(data.chat);
        setPhase("joined");
        for (const participant of data.participants) createConnection(participant.peerId, true);
        // Vào lại sau khi rớt mạng: báo lại tay đang giơ, màn hình đang chia sẻ.
        if (localRef.current.hand || localRef.current.screen) sendState();
      });
      on<Participant>("peer-joined", (p) => {
        setPeers((current) => ({ ...current, [p.peerId]: { ...p, stream: new MediaStream(), connection: "new" } }));
      });
      on<Participant>("peer-updated", (p) => {
        if (p.peerId !== peerIdRef.current) updatePeer(p.peerId, p);
      });
      on<{ peerId: string }>("peer-left", ({ peerId }) => {
        closeConnection(peerId);
        setPeers((current) => {
          const next = { ...current };
          delete next[peerId];
          return next;
        });
      });
      on<{ from: string; type: SignalType; data: unknown }>("signal", ({ from, type, data }) =>
        handleSignal(from, type, data),
      );
      on<ChatMessage>("chat", (message) => setChat((current) => [...current.slice(-199), message]));
      on<{ by: string }>("mute-request", ({ by }) => {
        const track = audioTrack();
        if (track) track.enabled = false;
        if (localRef.current.mic) changeLocal({ mic: false });
        muteRequestRef.current?.(by);
      });
      on("ended", () => {
        stopped = true;
        peerIdRef.current = null;
        source?.close();
        teardown();
        setPhase("ended");
      });

      source.onerror = () => {
        if (stopped) return;
        source?.close();
        teardown();
        peerIdRef.current = null;
        setPeers({});
        setPhase("reconnecting");
        // Luồng đứt: hỏi xem buổi học còn diễn ra không rồi mới nối lại.
        api<LiveSession>(`/api/live-sessions/${session.id}`)
          .then((latest) => {
            if (stopped) return;
            if (latest.status !== "LIVE") {
              stopped = true;
              setPhase("ended");
            } else {
              retry = setTimeout(open, RECONNECT_MS);
            }
          })
          .catch(() => {
            if (!stopped) retry = setTimeout(open, RECONNECT_MS);
          });
      };
    };

    open();
    stopRef.current = () => {
      stopped = true;
      if (retry) clearTimeout(retry);
      source?.close();
    };
    window.addEventListener("pagehide", sendLeave);
    return () => {
      stopRef.current();
      window.removeEventListener("pagehide", sendLeave);
      sendLeave();
      teardown();
    };
  }, [
    audioTrack,
    base,
    cameraTrack,
    changeLocal,
    closeConnection,
    createConnection,
    handleSignal,
    sendLeave,
    sendState,
    session.id,
    teardown,
    updatePeer,
  ]);

  /** Đổi track hình đang gửi cho mọi người (camera ↔ màn hình). */
  const replaceVideo = useCallback(async (track: MediaStreamTrack | null) => {
    videoTrackRef.current = track;
    await Promise.all(
      [...connections.current.values()].map(({ pc }) => {
        const transceiver = pc.getTransceivers().find((t) => t.receiver.track.kind === "video");
        return transceiver?.sender.replaceTrack(track).catch(() => {});
      }),
    );
  }, []);

  const toggleMic = useCallback(() => {
    const track = audioTrack();
    if (!track) return;
    track.enabled = !localRef.current.mic;
    changeLocal({ mic: track.enabled });
  }, [audioTrack, changeLocal]);

  const toggleCam = useCallback(() => {
    const track = cameraTrack();
    if (!track) return;
    track.enabled = !localRef.current.cam;
    changeLocal({ cam: track.enabled });
  }, [cameraTrack, changeLocal]);

  const toggleHand = useCallback(() => changeLocal({ hand: !localRef.current.hand }), [changeLocal]);

  const stopScreen = useCallback(async () => {
    setScreenStream((current) => {
      current?.getTracks().forEach((t) => t.stop());
      return null;
    });
    await replaceVideo(cameraTrack());
    changeLocal({ screen: false });
  }, [cameraTrack, changeLocal, replaceVideo]);

  /** Bắt đầu chia sẻ màn hình. Ném lỗi nếu người dùng bấm hủy ở hộp chọn màn hình của trình duyệt. */
  const startScreen = useCallback(async () => {
    const display = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: false });
    const track = display.getVideoTracks()[0];
    // Bấm "Dừng chia sẻ" trên thanh của trình duyệt cũng phải quay về camera.
    track.addEventListener("ended", () => void stopScreen());
    setScreenStream(display);
    await replaceVideo(track);
    changeLocal({ screen: true });
  }, [changeLocal, replaceVideo, stopScreen]);

  const sendChat = useCallback(
    async (text: string) => {
      const from = peerIdRef.current;
      if (!from) throw new Error("Chưa vào phòng");
      await api(`${base}/chat`, { method: "POST", body: { from, text } });
    },
    [base],
  );

  const muteOther = useCallback(
    async (target: string) => {
      const from = peerIdRef.current;
      if (!from) return;
      await api(`${base}/mute`, { method: "POST", body: { from, target } });
    },
    [base],
  );

  const leave = useCallback(() => {
    stopRef.current();
    sendLeave();
    teardown();
    setPhase("left");
  }, [sendLeave, teardown]);

  return {
    phase,
    myPeerId,
    peers: Object.values(peers),
    chat,
    local,
    screenStream,
    toggleMic,
    toggleCam,
    toggleHand,
    startScreen,
    stopScreen,
    sendChat,
    muteOther,
    leave,
  };
}
