"use client";

import {
  CalendarClockIcon,
  ClipboardCheckIcon,
  Loader2Icon,
  PlayIcon,
  RadioIcon,
  SquareIcon,
  UsersIcon,
  VideoIcon,
  XCircleIcon,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition, type FormEvent, type ReactNode } from "react";
import { toast } from "sonner";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { FormField } from "@/components/common/form-field";
import { IconTile } from "@/components/common/icon-tile";
import { NativeSelect } from "@/components/common/native-select";
import { StatusBadge } from "@/components/common/status-badge";
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
import { Card, CardContent } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { api } from "@/lib/client";
import { formatDate, formatDuration, formatNumber } from "@/lib/format";
import { fieldErrorMap, formErrorMessage } from "@/lib/forms";
import type { LiveAttendance, LiveSession } from "@/lib/types";

const TITLE_MAX = 150;
const DURATIONS = [30, 45, 60, 90, 120, 180];

/** Đang diễn ra trước, rồi sắp tới (gần nhất trước), cuối cùng là đã qua (mới nhất trước). */
export function orderSessions(sessions: LiveSession[]) {
  const rank = { LIVE: 0, SCHEDULED: 1, ENDED: 2, CANCELLED: 2 } as const;
  return [...sessions].sort((a, b) => {
    if (rank[a.status] !== rank[b.status]) return rank[a.status] - rank[b.status];
    const diff = new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime();
    return a.status === "SCHEDULED" ? diff : -diff;
  });
}

/** Giảng viên lên lịch buổi học, hoặc mở lớp ngay để dạy luôn. */
export function LiveSessionScheduler({ courseId, learnerCount }: { courseId: number; learnerCount: number }) {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [when, setWhen] = useState("");
  const [duration, setDuration] = useState("60");
  const [description, setDescription] = useState("");
  const [pending, setPending] = useState<"schedule" | "now" | null>(null);
  const [refreshing, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const busy = pending !== null || refreshing;

  async function create(scheduledAt: Date, fallbackTitle?: string) {
    return api<LiveSession>(`/api/courses/${courseId}/live-sessions`, {
      method: "POST",
      body: {
        title: title.trim() || fallbackTitle,
        description: description.trim() || null,
        scheduledAt: scheduledAt.toISOString(),
        durationMinutes: Number(duration),
      },
    });
  }

  async function schedule(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const errors: Record<string, string> = {};
    if (!title.trim()) errors.title = "Nhập tên buổi học.";
    if (!when) errors.scheduledAt = "Chọn thời gian bắt đầu.";
    else if (new Date(when).getTime() < Date.now() - 60_000) errors.scheduledAt = "Thời gian bắt đầu đã qua.";
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setPending("schedule");
    setError(null);
    try {
      await create(new Date(when));
      toast.success(
        learnerCount > 0
          ? `Đã lên lịch và báo cho ${formatNumber(learnerCount)} học viên`
          : "Đã lên lịch. Khóa học chưa có học viên nên chưa ai nhận được thông báo",
      );
      setTitle("");
      setWhen("");
      setDescription("");
      startTransition(() => router.refresh());
    } catch (cause) {
      setError(formErrorMessage(cause));
      setFieldErrors(fieldErrorMap(cause));
    } finally {
      setPending(null);
    }
  }

  async function startNow() {
    if (busy) return;
    setPending("now");
    setError(null);
    try {
      const created = await create(new Date(), "Lớp học trực tuyến");
      await api(`/api/live-sessions/${created.id}/start`, { method: "POST" });
      router.push(`/live/${created.id}`);
    } catch (cause) {
      setError(formErrorMessage(cause));
      setFieldErrors(fieldErrorMap(cause));
      setPending(null);
    }
  }

  return (
    <Card>
      <CardContent>
        <form onSubmit={schedule} className="space-y-4" aria-label="Lên lịch buổi học trực tuyến">
          <p className="flex items-center gap-2 text-sm text-muted-foreground">
            <UsersIcon className="size-4" aria-hidden />
            {formatNumber(learnerCount)} học viên nhận thông báo và email khi lên lịch, khi lớp bắt đầu và khi hủy.
          </p>
          {error && <ErrorAlert message={error} />}
          <FormField id="live-title" label="Tên buổi học" required error={fieldErrors.title} hint={`${title.length}/${TITLE_MAX}`}>
            <Input
              id="live-title"
              name="title"
              value={title}
              maxLength={TITLE_MAX}
              disabled={busy}
              placeholder="Ví dụ: Ôn tập trước kỳ thi"
              aria-invalid={Boolean(fieldErrors.title)}
              onChange={(e) => setTitle(e.target.value)}
            />
          </FormField>
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField id="live-when" label="Bắt đầu lúc" required error={fieldErrors.scheduledAt}>
              <Input
                id="live-when"
                name="scheduledAt"
                type="datetime-local"
                value={when}
                disabled={busy}
                aria-invalid={Boolean(fieldErrors.scheduledAt)}
                onChange={(e) => setWhen(e.target.value)}
              />
            </FormField>
            <FormField id="live-duration" label="Thời lượng" error={fieldErrors.durationMinutes}>
              <NativeSelect
                id="live-duration"
                name="durationMinutes"
                value={duration}
                disabled={busy}
                onChange={(e) => setDuration(e.target.value)}
              >
                {DURATIONS.map((m) => (
                  <option key={m} value={m}>
                    {formatDuration(m * 60)}
                  </option>
                ))}
              </NativeSelect>
            </FormField>
          </div>
          <FormField id="live-description" label="Nội dung buổi học" error={fieldErrors.description}>
            <Textarea
              id="live-description"
              name="description"
              className="field-sizing-fixed min-h-20 max-h-60"
              rows={3}
              value={description}
              maxLength={2000}
              disabled={busy}
              placeholder="Học viên cần chuẩn bị gì, buổi học nói về phần nào…"
              onChange={(e) => setDescription(e.target.value)}
            />
          </FormField>
          <div className="flex flex-wrap gap-2">
            <Button type="submit" disabled={busy}>
              {pending === "schedule" || refreshing ? <Loader2Icon className="animate-spin" aria-hidden /> : <CalendarClockIcon aria-hidden />}
              Lên lịch
            </Button>
            <Button type="button" variant="outline" disabled={busy} onClick={startNow}>
              {pending === "now" ? <Loader2Icon className="animate-spin" aria-hidden /> : <RadioIcon aria-hidden />}
              Mở lớp ngay
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

/** Danh sách buổi học. Người quản lý khóa thấy thêm nút bắt đầu, kết thúc, hủy và điểm danh. */
export function LiveSessionList({ sessions, canManage }: { sessions: LiveSession[]; canManage: boolean }) {
  if (sessions.length === 0) {
    return (
      <EmptyState
        icon={VideoIcon}
        title="Chưa có buổi học trực tuyến nào"
        description={
          canManage
            ? "Lên lịch một buổi học, học viên sẽ nhận thông báo và email."
            : "Khi giảng viên lên lịch, buổi học sẽ hiện ở đây và bạn nhận được thông báo."
        }
      />
    );
  }
  return (
    <ol className="space-y-3">
      {orderSessions(sessions).map((s) => (
        <li key={s.id}>
          <LiveSessionCard session={s} canManage={canManage} />
        </li>
      ))}
    </ol>
  );
}

function LiveSessionCard({ session: s, canManage }: { session: LiveSession; canManage: boolean }) {
  const live = s.status === "LIVE";
  return (
    <Card className={live ? "ring-2 ring-destructive/40" : undefined}>
      <CardContent className="flex flex-col gap-4 sm:flex-row sm:items-start">
        <div className="flex min-w-0 flex-1 gap-4">
          <IconTile icon={live ? RadioIcon : VideoIcon} tone={live ? "danger" : s.status === "SCHEDULED" ? "info" : "neutral"} />
          <div className="min-w-0 flex-1 space-y-1.5">
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="font-semibold break-words">{s.title}</h3>
              <StatusBadge status={s.status} />
            </div>
            <p className="text-caption text-muted-foreground">
              <time dateTime={s.scheduledAt}>{formatDate(s.scheduledAt)}</time> · {formatDuration(s.durationMinutes * 60)} ·{" "}
              {s.hostName}
              {live && s.participantCount > 0 && ` · ${formatNumber(s.participantCount)} người đang trong lớp`}
              {canManage && s.attendeeCount !== null && s.status !== "SCHEDULED" && s.status !== "CANCELLED" &&
                ` · ${formatNumber(s.attendeeCount)} học viên đã tham gia`}
            </p>
            {s.description && <p className="text-sm leading-relaxed break-words whitespace-pre-wrap">{s.description}</p>}
          </div>
        </div>
        <div className="flex shrink-0 flex-wrap gap-2 sm:justify-end">
          {live && (
            <Button asChild size="sm">
              <Link href={`/live/${s.id}`}>
                <VideoIcon aria-hidden /> Vào lớp
              </Link>
            </Button>
          )}
          {s.status === "SCHEDULED" && !canManage && (
            <Button asChild size="sm" variant="outline">
              <Link href={`/live/${s.id}`}>Phòng chờ</Link>
            </Button>
          )}
          {canManage && s.status === "SCHEDULED" && <StartButton session={s} />}
          {canManage && s.status === "SCHEDULED" && <CancelButton session={s} />}
          {canManage && live && <EndButton session={s} />}
          {canManage && (live || s.status === "ENDED") && <AttendanceDialog session={s} />}
        </div>
      </CardContent>
    </Card>
  );
}

function StartButton({ session }: { session: LiveSession }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);

  async function start() {
    setPending(true);
    try {
      await api(`/api/live-sessions/${session.id}/start`, { method: "POST" });
      router.push(`/live/${session.id}`);
    } catch (cause) {
      toast.error(formErrorMessage(cause));
      setPending(false);
    }
  }

  return (
    <Button size="sm" disabled={pending} onClick={start}>
      {pending ? <Loader2Icon className="animate-spin" aria-hidden /> : <PlayIcon aria-hidden />}
      Bắt đầu
    </Button>
  );
}

/** Nút có hộp xác nhận cho các thao tác không hoàn tác được (kết thúc, hủy buổi học). */
function ConfirmAction({
  session,
  action,
  trigger,
  title,
  description,
  confirm,
  done,
}: {
  session: LiveSession;
  action: "end" | "cancel";
  trigger: ReactNode;
  title: string;
  description: string;
  confirm: string;
  done: string;
}) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [refreshing, startTransition] = useTransition();
  const busy = pending || refreshing;

  async function run() {
    setPending(true);
    try {
      await api(`/api/live-sessions/${session.id}/${action}`, { method: "POST" });
      toast.success(done);
      startTransition(() => router.refresh());
    } catch (cause) {
      toast.error(formErrorMessage(cause));
    } finally {
      setPending(false);
    }
  }

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button size="sm" variant="outline" disabled={busy}>
          {busy ? <Loader2Icon className="animate-spin" aria-hidden /> : null}
          {trigger}
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Quay lại</AlertDialogCancel>
          <AlertDialogAction variant="destructive" onClick={run}>
            {confirm}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

function EndButton({ session }: { session: LiveSession }) {
  return (
    <ConfirmAction
      session={session}
      action="end"
      trigger={
        <>
          <SquareIcon aria-hidden /> Kết thúc
        </>
      }
      title="Kết thúc buổi học cho mọi người?"
      description="Mọi người đang trong phòng sẽ rời lớp. Buổi học đã kết thúc thì không mở lại được."
      confirm="Kết thúc buổi học"
      done="Đã kết thúc buổi học"
    />
  );
}

function CancelButton({ session }: { session: LiveSession }) {
  return (
    <ConfirmAction
      session={session}
      action="cancel"
      trigger={
        <>
          <XCircleIcon aria-hidden /> Hủy
        </>
      }
      title="Hủy buổi học này?"
      description="Học viên sẽ nhận thông báo và email báo buổi học bị hủy."
      confirm="Hủy buổi học"
      done="Đã hủy buổi học và báo cho học viên"
    />
  );
}

/** Ai đã vào lớp, vào lúc nào, có mặt bao lâu. Tải khi mở hộp thoại để luôn là số mới nhất. */
function AttendanceDialog({ session }: { session: LiveSession }) {
  const [rows, setRows] = useState<LiveAttendance[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  function load(open: boolean) {
    if (!open) return;
    setRows(null);
    setError(null);
    api<LiveAttendance[]>(`/api/live-sessions/${session.id}/attendance`)
      .then(setRows)
      .catch((cause) => setError(formErrorMessage(cause)));
  }

  const learners = rows?.filter((r) => !r.host).length ?? 0;
  return (
    <Dialog onOpenChange={load}>
      <DialogTrigger asChild>
        <Button size="sm" variant="outline">
          <ClipboardCheckIcon aria-hidden /> Điểm danh
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Điểm danh: {session.title}</DialogTitle>
          <DialogDescription>
            {rows ? `${formatNumber(learners)} học viên đã vào lớp.` : "Thời gian có mặt cộng dồn mọi lần vào lớp."}
          </DialogDescription>
        </DialogHeader>
        {error && <ErrorAlert message={error} />}
        {!rows && !error && (
          <p role="status" className="flex items-center gap-2 py-6 text-sm text-muted-foreground">
            <Loader2Icon className="size-4 animate-spin" aria-hidden /> Đang tải điểm danh…
          </p>
        )}
        {rows && rows.length === 0 && <p className="py-6 text-sm text-muted-foreground">Chưa có ai vào lớp.</p>}
        {rows && rows.length > 0 && (
          <div className="max-h-[60svh] overflow-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Người tham gia</TableHead>
                  <TableHead>Vào lúc</TableHead>
                  <TableHead className="text-right">Có mặt</TableHead>
                  <TableHead className="hidden text-right sm:table-cell">Số lần vào</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((r) => (
                  <TableRow key={r.userId}>
                    <TableCell className="font-medium">
                      {r.fullName ?? `Người dùng ${r.userId}`}
                      {r.host && <span className="ml-2 text-xs text-muted-foreground">(giảng viên)</span>}
                    </TableCell>
                    <TableCell className="text-muted-foreground">{formatDate(r.firstJoinedAt)}</TableCell>
                    <TableCell className="text-right tabular-nums">
                      {r.lastLeftAt ? formatDuration(r.attendedSeconds) : "Đang trong lớp"}
                    </TableCell>
                    <TableCell className="hidden text-right tabular-nums sm:table-cell">{r.joinCount}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
