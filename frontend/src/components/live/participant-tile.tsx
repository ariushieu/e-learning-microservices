"use client";

import { HandIcon, Loader2Icon, MicOffIcon, MonitorUpIcon } from "lucide-react";
import { useEffect, useRef, useState, type CSSProperties } from "react";
import { initials } from "@/lib/format";
import { cn } from "@/lib/utils";

// Một AudioContext cho cả trang: trình duyệt giới hạn số AudioContext mở cùng lúc.
let sharedAudio: AudioContext | null = null;

/** Có đang nói không, đo âm lượng track tiếng 5 lần mỗi giây. */
function useSpeaking(stream: MediaStream | null, enabled: boolean) {
  const [speaking, setSpeaking] = useState(false);
  useEffect(() => {
    if (!stream || !enabled || stream.getAudioTracks().length === 0) return;
    sharedAudio ??= new AudioContext();
    const context = sharedAudio;
    void context.resume().catch(() => {});
    const source = context.createMediaStreamSource(new MediaStream(stream.getAudioTracks()));
    const analyser = context.createAnalyser();
    analyser.fftSize = 512;
    source.connect(analyser);
    const samples = new Uint8Array(analyser.fftSize);
    const timer = setInterval(() => {
      analyser.getByteTimeDomainData(samples);
      let sum = 0;
      for (const value of samples) {
        const x = (value - 128) / 128;
        sum += x * x;
      }
      setSpeaking(Math.sqrt(sum / samples.length) > 0.035);
    }, 200);
    return () => {
      clearInterval(timer);
      source.disconnect();
      setSpeaking(false);
    };
  }, [stream, enabled]);
  return speaking;
}

/** Thẻ <video> gắn MediaStream; luồng của chính mình thì tắt tiếng để không vọng lại. */
function StreamVideo({ stream, muted, className }: { stream: MediaStream | null; muted: boolean; className?: string }) {
  const ref = useRef<HTMLVideoElement>(null);
  useEffect(() => {
    const video = ref.current;
    if (video && video.srcObject !== stream) video.srcObject = stream;
  }, [stream]);
  return <video ref={ref} autoPlay playsInline muted={muted} className={className} />;
}

export function ParticipantTile({
  name,
  host,
  isMe,
  stream,
  audioStream,
  mic,
  showVideo,
  screen,
  hand,
  connecting,
  mirrored,
  onMute,
  className,
  style,
}: {
  name: string;
  host: boolean;
  isMe: boolean;
  stream: MediaStream | null;
  /** Luồng để đo tiếng nói; của mình thì là luồng micro, khác luồng hình khi đang chia sẻ màn hình. */
  audioStream: MediaStream | null;
  mic: boolean;
  showVideo: boolean;
  screen: boolean;
  hand: boolean;
  connecting: boolean;
  mirrored: boolean;
  onMute?: () => void;
  className?: string;
  style?: CSSProperties;
}) {
  const speaking = useSpeaking(audioStream, mic);
  const label = `${name}${isMe ? " (bạn)" : ""}`;
  return (
    <figure
      aria-label={label}
      style={style}
      className={cn(
        "relative min-h-0 overflow-hidden rounded-xl bg-zinc-800 ring-2 ring-transparent transition-shadow",
        speaking && "ring-emerald-400",
        className,
      )}
    >
      <StreamVideo
        stream={stream}
        muted={isMe}
        className={cn(
          "size-full",
          screen ? "bg-black object-contain" : "object-cover",
          mirrored && !screen && "-scale-x-100",
          !showVideo && "invisible",
        )}
      />
      {!showVideo && (
        <div className="absolute inset-0 grid place-items-center">
          <span
            className={cn(
              "grid size-16 place-items-center rounded-full bg-primary text-xl font-semibold text-primary-foreground sm:size-20 sm:text-2xl",
              speaking && "ring-4 ring-emerald-400/60",
            )}
            aria-hidden
          >
            {initials(name)}
          </span>
        </div>
      )}
      {hand && (
        <span className="absolute top-2 left-2 grid size-8 place-items-center rounded-full bg-amber-400 text-zinc-900 shadow">
          <HandIcon className="size-4" aria-hidden />
          <span className="sr-only">đang giơ tay</span>
        </span>
      )}
      {connecting && (
        <span className="absolute top-2 right-2 flex items-center gap-1.5 rounded-md bg-black/60 px-2 py-1 text-xs text-zinc-200">
          <Loader2Icon className="size-3.5 animate-spin" aria-hidden /> Đang kết nối
        </span>
      )}
      {!connecting && onMute && mic && (
        <button
          type="button"
          onClick={onMute}
          className="absolute top-2 right-2 flex items-center gap-1.5 rounded-md bg-black/60 px-2 py-1 text-xs text-zinc-100 hover:bg-black/80 focus-visible:outline-2 focus-visible:outline-white"
        >
          <MicOffIcon className="size-3.5" aria-hidden /> Tắt micro
        </button>
      )}
      <figcaption className="absolute bottom-2 left-2 flex max-w-[calc(100%-1rem)] items-center gap-1.5 rounded-md bg-black/60 px-2 py-1 text-xs text-zinc-50">
        {!mic && <MicOffIcon className="size-3.5 shrink-0 text-red-400" aria-label="micro đang tắt" />}
        {screen && <MonitorUpIcon className="size-3.5 shrink-0" aria-label="đang chia sẻ màn hình" />}
        <span className="truncate">{label}</span>
        {host && <span className="shrink-0 rounded bg-primary px-1.5 py-0.5 text-[10px] font-semibold uppercase">Giảng viên</span>}
      </figcaption>
    </figure>
  );
}
