"use client";

import {
  HandIcon,
  Loader2Icon,
  LogOutIcon,
  MessageSquareTextIcon,
  MicIcon,
  MicOffIcon,
  ScreenShareIcon,
  ScreenShareOffIcon,
  SendIcon,
  SquareIcon,
  UsersIcon,
  VideoIcon,
  VideoOffIcon,
  XIcon,
  type LucideIcon,
} from "lucide-react";
import Link from "next/link";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { toast } from "sonner";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/client";
import { formatNumber, initials, TIME_ZONE } from "@/lib/format";
import { formErrorMessage } from "@/lib/forms";
import type { LiveSession } from "@/lib/types";
import { cn } from "@/lib/utils";
import { ParticipantTile } from "./participant-tile";
import { useLiveRoom, type ChatMessage } from "./use-live-room";

type Panel = "chat" | "people" | null;

/** Phòng học toàn màn hình kiểu Google Meet: lưới video, thanh điều khiển, chat và danh sách người. */
export function LiveRoom({
  session,
  me,
  localStream,
  initial,
  onRejoin,
}: {
  session: LiveSession;
  me: { userId: number; fullName: string };
  localStream: MediaStream | null;
  initial: { mic: boolean; cam: boolean };
  onRejoin: () => void;
}) {
  const room = useLiveRoom({
    session,
    localStream,
    initial,
    onMuteRequest: (by) => toast.info(`${by} đã tắt micro của bạn. Bấm nút micro để nói tiếp.`),
  });
  const [panel, setPanel] = useState<Panel>(null);
  const [seenChat, setSeenChat] = useState(0);
  const [canShareScreen] = useState(() => typeof navigator !== "undefined" && !!navigator.mediaDevices?.getDisplayMedia);
  const hasMic = (localStream?.getAudioTracks().length ?? 0) > 0;
  const hasCam = (localStream?.getVideoTracks().length ?? 0) > 0;

  function togglePanel(next: Exclude<Panel, null>) {
    // Đóng hoặc rời khung chat thì mọi tin hiện có tính là đã xem.
    if (panel === "chat" || next === "chat") setSeenChat(room.chat.length);
    setPanel((current) => (current === next ? null : next));
  }

  async function toggleScreen() {
    try {
      if (room.local.screen) await room.stopScreen();
      else await room.startScreen();
    } catch {
      // Bấm "Hủy" ở hộp chọn màn hình của trình duyệt: không phải lỗi.
    }
  }

  if (room.phase === "ended" || room.phase === "left") {
    return (
      <RoomExit
        session={session}
        ended={room.phase === "ended"}
        onRejoin={onRejoin}
      />
    );
  }

  const meTile = {
    key: "me",
    name: me.fullName || "Bạn",
    host: session.canHost,
    isMe: true,
    stream: room.local.screen ? room.screenStream : localStream,
    audioStream: localStream,
    mic: room.local.mic && hasMic,
    showVideo: room.local.screen || (room.local.cam && hasCam),
    screen: room.local.screen,
    hand: room.local.hand,
    connecting: false,
    mirrored: !room.local.screen,
  };
  const peerTiles = room.peers.map((p) => ({
    key: p.peerId,
    name: p.name,
    host: p.host,
    isMe: false,
    stream: p.stream,
    audioStream: p.stream,
    mic: p.mic,
    showVideo: p.cam || p.screen,
    screen: p.screen,
    hand: p.hand,
    connecting: p.connection === "new" || p.connection === "connecting",
    mirrored: false,
    onMute: session.canHost ? () => muteOther(p.peerId, p.name) : undefined,
  }));
  const tiles = [meTile, ...peerTiles];
  const spotlight = tiles.find((t) => t.screen) ?? null;
  const others = spotlight ? tiles.filter((t) => t !== spotlight) : tiles;
  const unread = panel === "chat" ? 0 : Math.max(room.chat.length - seenChat, 0);
  const hands = room.peers.filter((p) => p.hand).length + (room.local.hand ? 1 : 0);

  async function muteOther(peerId: string, name: string) {
    try {
      await room.muteOther(peerId);
      toast.success(`Đã tắt micro của ${name}`);
    } catch (cause) {
      toast.error(formErrorMessage(cause));
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-zinc-950 text-zinc-50">
      <header className="flex h-14 shrink-0 items-center gap-3 border-b border-white/10 px-3 sm:px-4">
        <span className="flex shrink-0 items-center gap-1.5 rounded-md bg-red-600 px-2 py-1 text-xs font-semibold tracking-wide uppercase">
          <span className="size-2 rounded-full bg-white motion-safe:animate-pulse" aria-hidden />
          Trực tiếp
        </span>
        <div className="min-w-0 flex-1">
          <h1 className="truncate text-sm font-semibold sm:text-base">{session.title}</h1>
          <p className="truncate text-xs text-zinc-400">
            {session.courseTitle}
            {session.startedAt && (
              <>
                {" · "}
                <Elapsed since={session.startedAt} />
              </>
            )}
          </p>
        </div>
        {room.phase !== "joined" && (
          <span role="status" className="flex shrink-0 items-center gap-1.5 text-xs text-amber-300">
            <Loader2Icon className="size-3.5 animate-spin" aria-hidden />
            {room.phase === "connecting" ? "Đang vào lớp…" : "Mất kết nối, đang nối lại…"}
          </span>
        )}
        <span className="flex shrink-0 items-center gap-1.5 text-sm text-zinc-300" aria-label={`${tiles.length} người trong lớp`}>
          <UsersIcon className="size-4" aria-hidden />
          {formatNumber(tiles.length)}
        </span>
      </header>

      <div className="relative flex min-h-0 flex-1">
        <main className="min-h-0 flex-1 p-2 sm:p-3">
          {spotlight ? (
            <div className="grid h-full min-h-0 gap-2 sm:gap-3 lg:grid-cols-[minmax(0,1fr)_15rem]">
              <ParticipantTile {...spotlight} className="h-full" />
              <div className="hidden min-h-0 flex-col gap-2 overflow-y-auto lg:flex">
                {others.map((t) => (
                  <ParticipantTile {...t} key={t.key} className="aspect-video shrink-0" />
                ))}
              </div>
            </div>
          ) : (
            <div
              className={cn(
                "flex h-full min-h-0 flex-wrap content-center items-center justify-center gap-2 [container-type:size] sm:gap-3",
                gridLayout(tiles.length),
              )}
            >
              {tiles.map((t) => (
                <ParticipantTile {...t} key={t.key} className="aspect-video" style={{ width: TILE_WIDTH }} />
              ))}
            </div>
          )}
        </main>

        {panel && (
          <aside
            className="absolute inset-0 z-10 flex flex-col bg-zinc-900 sm:static sm:w-80 sm:shrink-0 sm:border-l sm:border-white/10"
            aria-label={panel === "chat" ? "Trò chuyện trong lớp" : "Người tham gia"}
          >
            <div className="flex h-12 shrink-0 items-center justify-between border-b border-white/10 px-4">
              <h2 className="text-sm font-semibold">
                {panel === "chat" ? "Trò chuyện" : `Người tham gia (${tiles.length})`}
              </h2>
              <button
                type="button"
                onClick={() => togglePanel(panel)}
                className="grid size-8 place-items-center rounded-md text-zinc-400 hover:bg-white/10 hover:text-white focus-visible:outline-2 focus-visible:outline-white"
                aria-label="Đóng"
              >
                <XIcon className="size-4" aria-hidden />
              </button>
            </div>
            {panel === "chat" ? (
              <ChatPanel messages={room.chat} myPeerId={room.myPeerId} onSend={room.sendChat} disabled={room.phase !== "joined"} />
            ) : (
              <PeoplePanel
                tiles={tiles}
                canMute={session.canHost}
              />
            )}
          </aside>
        )}
      </div>

      <footer className="flex shrink-0 flex-wrap items-center justify-center gap-2 border-t border-white/10 px-3 py-3 sm:gap-3">
        <ControlButton
          icon={room.local.mic && hasMic ? MicIcon : MicOffIcon}
          label={!hasMic ? "Không có micro" : room.local.mic ? "Tắt micro" : "Bật micro"}
          off={!room.local.mic || !hasMic}
          disabled={!hasMic}
          onClick={room.toggleMic}
        />
        <ControlButton
          icon={room.local.cam && hasCam ? VideoIcon : VideoOffIcon}
          label={!hasCam ? "Không có camera" : room.local.cam ? "Tắt camera" : "Bật camera"}
          off={!room.local.cam || !hasCam}
          disabled={!hasCam}
          onClick={room.toggleCam}
        />
        {canShareScreen && (
          <ControlButton
            icon={room.local.screen ? ScreenShareOffIcon : ScreenShareIcon}
            label={room.local.screen ? "Dừng chia sẻ" : "Chia sẻ màn hình"}
            active={room.local.screen}
            onClick={toggleScreen}
          />
        )}
        <ControlButton
          icon={HandIcon}
          label={room.local.hand ? "Hạ tay" : "Giơ tay"}
          active={room.local.hand}
          onClick={room.toggleHand}
        />
        <ControlButton
          icon={MessageSquareTextIcon}
          label="Trò chuyện"
          active={panel === "chat"}
          badge={unread}
          onClick={() => togglePanel("chat")}
        />
        <ControlButton
          icon={UsersIcon}
          label="Người tham gia"
          active={panel === "people"}
          badge={session.canHost ? hands : 0}
          onClick={() => togglePanel("people")}
        />
        <Button
          type="button"
          className="h-12 rounded-full bg-red-600 px-5 text-white hover:bg-red-500 focus-visible:ring-red-400/50"
          onClick={room.leave}
        >
          <LogOutIcon aria-hidden /> Rời lớp
        </Button>
        {session.canHost && <EndForEveryone sessionId={session.id} />}
      </footer>
    </div>
  );
}

/**
 * Ô 16:9 lớn nhất mà vẫn xếp đủ --cols cột × --rows hàng trong khung (đơn vị cqw/cqh của khung
 * lưới): màn rộng thì ô cao bằng khung chia hàng, màn hẹp thì ô rộng bằng khung chia cột.
 */
const TILE_WIDTH =
  "min(calc((100cqw - (var(--cols) - 1) * 0.75rem) / var(--cols)), calc((100cqh - (var(--rows) - 1) * 0.75rem) / var(--rows) * 16 / 9))";

/** Số cột × hàng theo số người: điện thoại xếp dọc, màn rộng xếp ngang. */
function gridLayout(count: number) {
  if (count <= 1) return "[--cols:1] [--rows:1]";
  if (count === 2) return "[--cols:1] [--rows:2] sm:[--cols:2] sm:[--rows:1]";
  if (count === 3) return "[--cols:1] [--rows:3] sm:[--cols:2] sm:[--rows:2]";
  if (count === 4) return "[--cols:2] [--rows:2]";
  if (count <= 6) return "[--cols:2] [--rows:3] sm:[--cols:3] sm:[--rows:2]";
  if (count <= 8) return "[--cols:2] [--rows:4] sm:[--cols:3] sm:[--rows:3]";
  if (count === 9) return "[--cols:2] [--rows:5] sm:[--cols:3] sm:[--rows:3]";
  return "[--cols:2] [--rows:6] sm:[--cols:4] sm:[--rows:3]";
}

function ControlButton({
  icon: Icon,
  label,
  onClick,
  off = false,
  active = false,
  disabled = false,
  badge = 0,
}: {
  icon: LucideIcon;
  label: string;
  onClick: () => void;
  off?: boolean;
  active?: boolean;
  disabled?: boolean;
  badge?: number;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      title={label}
      aria-label={label}
      aria-pressed={active || undefined}
      className={cn(
        "relative grid size-12 place-items-center rounded-full transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white disabled:cursor-not-allowed disabled:opacity-50",
        off ? "bg-red-600 text-white hover:bg-red-500" : active ? "bg-emerald-300 text-zinc-900 hover:bg-emerald-200" : "bg-zinc-700 text-white hover:bg-zinc-600",
      )}
    >
      <Icon className="size-5" aria-hidden />
      {badge > 0 && (
        <span className="absolute -top-1 -right-1 grid min-w-5 place-items-center rounded-full bg-amber-400 px-1 text-[11px] font-bold text-zinc-900 tabular-nums">
          {badge > 99 ? "99+" : badge}
        </span>
      )}
    </button>
  );
}

function EndForEveryone({ sessionId }: { sessionId: number }) {
  const [pending, setPending] = useState(false);

  async function end() {
    setPending(true);
    try {
      // Máy chủ báo "ended" cho cả phòng, kể cả người bấm; màn hình kết thúc hiện ra từ sự kiện đó.
      await api(`/api/live-sessions/${sessionId}/end`, { method: "POST" });
    } catch (cause) {
      toast.error(formErrorMessage(cause));
      setPending(false);
    }
  }

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button
          type="button"
          variant="outline"
          disabled={pending}
          className="h-12 rounded-full border-red-500/60 bg-transparent px-5 text-red-300 hover:bg-red-500/10 hover:text-red-200"
        >
          {pending ? <Loader2Icon className="animate-spin" aria-hidden /> : <SquareIcon aria-hidden />}
          Kết thúc lớp
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Kết thúc buổi học cho mọi người?</AlertDialogTitle>
          <AlertDialogDescription>
            Mọi người đang trong phòng sẽ rời lớp và thời gian có mặt được chốt vào bảng điểm danh.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Quay lại</AlertDialogCancel>
          <AlertDialogAction variant="destructive" onClick={end}>
            Kết thúc buổi học
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

function ChatPanel({
  messages,
  myPeerId,
  onSend,
  disabled,
}: {
  messages: ChatMessage[];
  myPeerId: string | null;
  onSend: (text: string) => Promise<void>;
  disabled: boolean;
}) {
  const [text, setText] = useState("");
  const [sending, setSending] = useState(false);
  const listRef = useRef<HTMLOListElement>(null);

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight });
  }, [messages.length]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const value = text.trim();
    if (!value || sending) return;
    setSending(true);
    try {
      await onSend(value);
      setText("");
    } catch (cause) {
      toast.error(formErrorMessage(cause));
    } finally {
      setSending(false);
    }
  }

  return (
    <>
      <ol ref={listRef} className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4" aria-live="polite">
        {messages.length === 0 && (
          <li className="text-center text-sm text-zinc-400">Chưa có tin nhắn. Tin nhắn chỉ người trong lớp thấy.</li>
        )}
        {messages.map((m) => (
          <li key={m.id} className="space-y-0.5">
            <p className="flex items-baseline gap-2 text-xs text-zinc-400">
              <span className={cn("font-semibold", m.host ? "text-emerald-300" : "text-zinc-200")}>
                {m.peerId === myPeerId ? "Bạn" : m.name}
              </span>
              <time dateTime={m.sentAt}>
                {new Date(m.sentAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit", timeZone: TIME_ZONE })}
              </time>
            </p>
            <p className="text-sm break-words whitespace-pre-wrap">{m.text}</p>
          </li>
        ))}
      </ol>
      <form onSubmit={submit} className="flex shrink-0 gap-2 border-t border-white/10 p-3">
        <label htmlFor="live-chat" className="sr-only">
          Tin nhắn
        </label>
        <input
          id="live-chat"
          value={text}
          maxLength={1000}
          disabled={disabled}
          autoComplete="off"
          placeholder="Gửi tin nhắn cho cả lớp"
          onChange={(e) => setText(e.target.value)}
          className="h-10 min-w-0 flex-1 rounded-lg border border-white/15 bg-zinc-800 px-3 text-sm text-white placeholder:text-zinc-500 focus-visible:border-emerald-300 focus-visible:outline-none"
        />
        <Button type="submit" size="icon" className="size-10" disabled={disabled || sending || !text.trim()} aria-label="Gửi">
          {sending ? <Loader2Icon className="animate-spin" aria-hidden /> : <SendIcon aria-hidden />}
        </Button>
      </form>
    </>
  );
}

function PeoplePanel({
  tiles,
  canMute,
}: {
  tiles: { key: string; name: string; host: boolean; isMe: boolean; mic: boolean; showVideo: boolean; hand: boolean; onMute?: () => void }[];
  canMute: boolean;
}) {
  // Người giơ tay lên đầu để giảng viên thấy ngay.
  const sorted = [...tiles].sort((a, b) => Number(b.hand) - Number(a.hand) || Number(b.host) - Number(a.host));
  return (
    <ul className="min-h-0 flex-1 space-y-1 overflow-y-auto p-2">
      {sorted.map((t) => (
        <li key={t.key} className="flex items-center gap-3 rounded-lg px-2 py-2 hover:bg-white/5">
          <span className="grid size-9 shrink-0 place-items-center rounded-full bg-primary text-sm font-semibold" aria-hidden>
            {initials(t.name)}
          </span>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm">
              {t.name}
              {t.isMe && " (bạn)"}
            </p>
            {t.host && <p className="text-xs text-emerald-300">Giảng viên</p>}
          </div>
          {t.hand && <HandIcon className="size-4 shrink-0 text-amber-300" aria-label="đang giơ tay" />}
          {t.mic ? (
            <MicIcon className="size-4 shrink-0 text-zinc-400" aria-label="micro đang bật" />
          ) : (
            <MicOffIcon className="size-4 shrink-0 text-red-400" aria-label="micro đang tắt" />
          )}
          {canMute && !t.isMe && t.mic && t.onMute && (
            <button
              type="button"
              onClick={t.onMute}
              className="shrink-0 rounded-md px-2 py-1 text-xs text-zinc-300 hover:bg-white/10 hover:text-white focus-visible:outline-2 focus-visible:outline-white"
            >
              Tắt micro
            </button>
          )}
        </li>
      ))}
    </ul>
  );
}

/** Thời gian lớp đã diễn ra, cập nhật mỗi giây. */
function Elapsed({ since }: { since: string }) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);
  const seconds = Math.max(0, Math.floor((now - new Date(since).getTime()) / 1000));
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  const pad = (n: number) => String(n).padStart(2, "0");
  return <span className="tabular-nums">{h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`}</span>;
}

function RoomExit({ session, ended, onRejoin }: { session: LiveSession; ended: boolean; onRejoin: () => void }) {
  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-zinc-950 px-6 text-center text-zinc-50">
      <div className="max-w-md space-y-4">
        <h1 className="text-title">{ended ? "Buổi học đã kết thúc" : "Bạn đã rời lớp"}</h1>
        <p className="text-zinc-400">
          {ended
            ? "Cảm ơn bạn đã tham gia. Thời gian có mặt của bạn đã được ghi vào bảng điểm danh."
            : "Lớp vẫn đang diễn ra, bạn có thể vào lại bất cứ lúc nào."}
        </p>
        <div className="flex flex-wrap justify-center gap-2">
          {!ended && (
            <Button size="lg" onClick={onRejoin}>
              <VideoIcon aria-hidden /> Vào lại
            </Button>
          )}
          <Button asChild size="lg" variant={ended ? "default" : "outline"} className={ended ? undefined : "bg-transparent text-zinc-50"}>
            <Link href={session.canHost ? `/instructor/courses/${session.courseId}?tab=truc-tuyen` : `/courses/${session.courseId}#truc-tuyen`}>
              {session.canHost ? "Về trang quản lý lớp" : "Về trang khóa học"}
            </Link>
          </Button>
        </div>
      </div>
    </div>
  );
}
