"use client";

import {
  ArrowLeftIcon,
  CalendarClockIcon,
  CalendarXIcon,
  CameraOffIcon,
  ClockIcon,
  Loader2Icon,
  MicIcon,
  MicOffIcon,
  PlayIcon,
  UserIcon,
  UsersIcon,
  VideoIcon,
  VideoOffIcon,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { toast } from "sonner";
import { Callout } from "@/components/common/callout";
import { StatusBadge } from "@/components/common/status-badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { api } from "@/lib/client";
import { formatDate, formatDuration, formatNumber, initials } from "@/lib/format";
import { formErrorMessage } from "@/lib/forms";
import type { LiveSession } from "@/lib/types";
import { cn } from "@/lib/utils";
import { LiveRoom } from "./live-room";

// Phòng chờ hỏi lại máy chủ định kỳ để tự vào được khi giảng viên mở lớp.
const WAITING_REFRESH_MS = 10_000;

type Media = { stream: MediaStream | null; error: string | null; ready: boolean };

/**
 * Trang /live/[id]: phòng chờ khi lớp chưa mở, màn hình thử camera/micro trước khi vào,
 * rồi tới phòng học. Lớp đã kết thúc hoặc bị hủy thì chỉ báo và dẫn về trang khóa học.
 */
export function LiveLobby({ session, me }: { session: LiveSession; me: { userId: number; fullName: string } }) {
  if (session.status === "SCHEDULED") return <WaitingRoom session={session} />;
  if (session.status !== "LIVE") return <Closed session={session} />;
  return <JoinScreen session={session} me={me} />;
}

function backHref(session: LiveSession) {
  return session.canHost ? `/instructor/courses/${session.courseId}?tab=truc-tuyen` : `/courses/${session.courseId}#truc-tuyen`;
}

function Shell({ session, children }: { session: LiveSession; children: ReactNode }) {
  return (
    <div className="mx-auto flex min-h-svh max-w-5xl flex-col gap-6 px-4 py-6 sm:px-6">
      <Link
        href={backHref(session)}
        className="inline-flex w-fit items-center gap-1.5 rounded-sm text-sm text-muted-foreground hover:text-foreground focus-visible:outline-2 focus-visible:outline-ring"
      >
        <ArrowLeftIcon className="size-4" aria-hidden /> {session.courseTitle}
      </Link>
      {children}
    </div>
  );
}

function SessionFacts({ session }: { session: LiveSession }) {
  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <StatusBadge status={session.status} />
        <span className="text-sm text-muted-foreground">Lớp học trực tuyến</span>
      </div>
      <h1 className="text-title break-words">{session.title}</h1>
      <ul className="space-y-1.5 text-sm text-muted-foreground">
        <li className="flex items-center gap-2">
          <CalendarClockIcon className="size-4 shrink-0" aria-hidden />
          <time dateTime={session.scheduledAt}>{formatDate(session.scheduledAt)}</time> ·{" "}
          {formatDuration(session.durationMinutes * 60)}
        </li>
        <li className="flex items-center gap-2">
          <UserIcon className="size-4 shrink-0" aria-hidden /> {session.hostName}
        </li>
        {session.status === "LIVE" && (
          <li className="flex items-center gap-2">
            <UsersIcon className="size-4 shrink-0" aria-hidden />
            {session.participantCount > 0 ? `${formatNumber(session.participantCount)} người đang trong lớp` : "Chưa có ai trong lớp"}
          </li>
        )}
      </ul>
      {session.description && <p className="text-sm leading-relaxed break-words whitespace-pre-wrap">{session.description}</p>}
    </div>
  );
}

function WaitingRoom({ session }: { session: LiveSession }) {
  const router = useRouter();
  const [starting, setStarting] = useState(false);

  useEffect(() => {
    if (session.canHost) return;
    const timer = setInterval(() => router.refresh(), WAITING_REFRESH_MS);
    return () => clearInterval(timer);
  }, [router, session.canHost]);

  async function start() {
    setStarting(true);
    try {
      await api(`/api/live-sessions/${session.id}/start`, { method: "POST" });
      router.refresh();
    } catch (cause) {
      toast.error(formErrorMessage(cause));
      setStarting(false);
    }
  }

  return (
    <Shell session={session}>
      <Card>
        <CardContent className="space-y-6">
          <SessionFacts session={session} />
          {session.canHost ? (
            <Button size="lg" onClick={start} disabled={starting}>
              {starting ? <Loader2Icon className="animate-spin" aria-hidden /> : <PlayIcon aria-hidden />}
              Bắt đầu buổi học
            </Button>
          ) : (
            <Callout icon={ClockIcon} tone="info" title="Lớp chưa bắt đầu">
              Giữ trang này mở: khi giảng viên mở lớp, trang tự chuyển sang màn hình vào lớp. Bạn cũng nhận được thông báo.
            </Callout>
          )}
        </CardContent>
      </Card>
    </Shell>
  );
}

function Closed({ session }: { session: LiveSession }) {
  const cancelled = session.status === "CANCELLED";
  return (
    <Shell session={session}>
      <Card>
        <CardContent className="space-y-6">
          <SessionFacts session={session} />
          <Callout icon={cancelled ? CalendarXIcon : ClockIcon} tone="neutral" title={cancelled ? "Buổi học đã bị hủy" : "Buổi học đã kết thúc"}>
            {cancelled ? "Xem lịch các buổi học khác ở trang khóa học." : "Phòng học đã đóng. Xem lịch các buổi học khác ở trang khóa học."}
          </Callout>
          <Button asChild>
            <Link href={backHref(session)}>{session.canHost ? "Về trang quản lý lớp" : "Về trang khóa học"}</Link>
          </Button>
        </CardContent>
      </Card>
    </Shell>
  );
}

function mediaError(error: unknown) {
  const name = error instanceof DOMException ? error.name : "";
  if (name === "NotAllowedError") {
    return "Trình duyệt đang chặn camera và micro. Bấm biểu tượng bên trái thanh địa chỉ để cho phép rồi tải lại trang.";
  }
  if (name === "NotFoundError") return "Không tìm thấy camera hoặc micro trên máy này.";
  if (name === "NotReadableError") return "Camera hoặc micro đang được ứng dụng khác dùng.";
  return "Không mở được camera và micro.";
}

/** Xin camera + micro; không có camera thì thử riêng micro; không có gì vẫn vào lớp để xem và nghe. */
async function openMedia(): Promise<Media> {
  if (!navigator.mediaDevices?.getUserMedia) {
    return { stream: null, ready: true, error: "Trình duyệt không cho dùng camera ở địa chỉ này (cần HTTPS hoặc localhost)." };
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: true, noiseSuppression: true },
      video: { width: { ideal: 1280 }, height: { ideal: 720 } },
    });
    return { stream, ready: true, error: null };
  } catch (first) {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true } });
      return { stream, ready: true, error: `${mediaError(first)} Bạn vẫn nói và nghe được.` };
    } catch {
      return { stream: null, ready: true, error: `${mediaError(first)} Bạn vẫn vào lớp để xem và nghe.` };
    }
  }
}

function JoinScreen({ session, me }: { session: LiveSession; me: { userId: number; fullName: string } }) {
  const [media, setMedia] = useState<Media>({ stream: null, error: null, ready: false });
  const [mic, setMic] = useState(true);
  const [cam, setCam] = useState(true);
  const [joined, setJoined] = useState(0);
  const preview = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    let cancelled = false;
    let opened: MediaStream | null = null;
    openMedia().then((result) => {
      opened = result.stream;
      if (cancelled) result.stream?.getTracks().forEach((t) => t.stop());
      else setMedia(result);
    });
    return () => {
      cancelled = true;
      // Rời trang thì tắt đèn camera.
      opened?.getTracks().forEach((t) => t.stop());
    };
  }, []);

  useEffect(() => {
    if (preview.current) preview.current.srcObject = media.stream;
  }, [media.stream, joined]);

  const hasMic = (media.stream?.getAudioTracks().length ?? 0) > 0;
  const hasCam = (media.stream?.getVideoTracks().length ?? 0) > 0;

  function toggle(kind: "audio" | "video") {
    const track = kind === "audio" ? media.stream?.getAudioTracks()[0] : media.stream?.getVideoTracks()[0];
    if (!track) return;
    track.enabled = !track.enabled;
    if (kind === "audio") setMic(track.enabled);
    else setCam(track.enabled);
  }

  if (joined > 0) {
    return (
      <LiveRoom
        key={joined}
        session={session}
        me={me}
        localStream={media.stream}
        initial={{ mic: mic && hasMic, cam: cam && hasCam }}
        onRejoin={() => setJoined((n) => n + 1)}
      />
    );
  }

  return (
    <Shell session={session}>
      <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="relative aspect-video overflow-hidden rounded-2xl bg-zinc-900 text-zinc-50">
          <video ref={preview} autoPlay playsInline muted className={cn("size-full -scale-x-100 object-cover", (!cam || !hasCam) && "invisible")} />
          {(!cam || !hasCam) && (
            <div className="absolute inset-0 grid place-items-center">
              {media.ready ? (
                <span className="grid size-20 place-items-center rounded-full bg-primary text-2xl font-semibold" aria-hidden>
                  {initials(me.fullName)}
                </span>
              ) : (
                <span role="status" className="flex items-center gap-2 text-sm text-zinc-300">
                  <Loader2Icon className="size-4 animate-spin" aria-hidden /> Đang mở camera và micro…
                </span>
              )}
            </div>
          )}
          <div className="absolute inset-x-0 bottom-4 flex justify-center gap-3">
            <PreviewToggle
              on={!media.ready || (mic && hasMic)}
              disabled={!hasMic}
              onIcon={MicIcon}
              offIcon={MicOffIcon}
              label={!hasMic ? "Không có micro" : mic ? "Tắt micro" : "Bật micro"}
              onClick={() => toggle("audio")}
            />
            <PreviewToggle
              on={!media.ready || (cam && hasCam)}
              disabled={!hasCam}
              onIcon={VideoIcon}
              offIcon={hasCam ? VideoOffIcon : CameraOffIcon}
              label={!hasCam ? "Không có camera" : cam ? "Tắt camera" : "Bật camera"}
              onClick={() => toggle("video")}
            />
          </div>
        </div>

        <Card>
          <CardContent className="space-y-6">
            <SessionFacts session={session} />
            {media.error && (
              <Callout icon={CameraOffIcon} tone="warning" title="Camera, micro">
                {media.error}
              </Callout>
            )}
            <Button size="lg" className="h-11 w-full text-base" disabled={!media.ready} onClick={() => setJoined(1)}>
              {media.ready ? <VideoIcon aria-hidden /> : <Loader2Icon className="animate-spin" aria-hidden />}
              Vào lớp
            </Button>
          </CardContent>
        </Card>
      </div>
    </Shell>
  );
}

function PreviewToggle({
  on,
  disabled,
  onIcon: OnIcon,
  offIcon: OffIcon,
  label,
  onClick,
}: {
  on: boolean;
  disabled: boolean;
  onIcon: typeof MicIcon;
  offIcon: typeof MicIcon;
  label: string;
  onClick: () => void;
}) {
  const Icon = on ? OnIcon : OffIcon;
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      title={label}
      className={cn(
        "grid size-12 place-items-center rounded-full text-white transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white disabled:cursor-not-allowed disabled:opacity-60",
        on ? "bg-zinc-700/80 hover:bg-zinc-600" : "bg-red-600 hover:bg-red-500",
      )}
    >
      <Icon className="size-5" aria-hidden />
    </button>
  );
}
