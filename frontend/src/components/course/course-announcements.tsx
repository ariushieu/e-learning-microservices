"use client";

import { Loader2Icon, MegaphoneIcon, SendIcon, Trash2Icon, UsersIcon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, useTransition, type FormEvent } from "react";
import { toast } from "sonner";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { FormField } from "@/components/common/form-field";
import { IconTile } from "@/components/common/icon-tile";
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
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { api } from "@/lib/client";
import { formatDate, formatNumber } from "@/lib/format";
import { fieldErrorMap, formErrorMessage } from "@/lib/forms";
import type { CourseAnnouncement } from "@/lib/types";

const TITLE_MAX = 150;
const CONTENT_MAX = 2000;

/** Giảng viên soạn thông báo; gửi xong học viên nhận ngay trong chuông thông báo. */
export function AnnouncementComposer({ courseId, learnerCount }: { courseId: number; learnerCount: number }) {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [pending, setPending] = useState(false);
  const [refreshing, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const busy = pending || refreshing;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const errors: Record<string, string> = {};
    if (!title.trim()) errors.title = "Nhập tiêu đề thông báo.";
    if (!content.trim()) errors.content = "Nhập nội dung thông báo.";
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setPending(true);
    setError(null);
    try {
      const saved = await api<CourseAnnouncement>(`/api/courses/${courseId}/announcements`, {
        method: "POST",
        body: { title, content },
      });
      toast.success(
        saved.recipientCount
          ? `Đã gửi thông báo tới ${formatNumber(saved.recipientCount)} học viên`
          : "Đã đăng thông báo. Khóa học chưa có học viên nên chưa ai nhận được",
      );
      setTitle("");
      setContent("");
      startTransition(() => router.refresh());
    } catch (cause) {
      setError(formErrorMessage(cause));
      setFieldErrors(fieldErrorMap(cause));
    } finally {
      setPending(false);
    }
  }

  return (
    <Card>
      <CardContent>
        <form onSubmit={submit} className="space-y-4" aria-label="Soạn thông báo cho học viên">
          <p className="flex items-center gap-2 text-sm text-muted-foreground">
            <UsersIcon className="size-4" aria-hidden />
            Gửi tới {formatNumber(learnerCount)} học viên đã ghi danh, hiện ngay trong chuông thông báo của họ.
          </p>
          {error && <ErrorAlert message={error} />}
          <FormField id="announcement-title" label="Tiêu đề" required error={fieldErrors.title} hint={`${title.length}/${TITLE_MAX}`}>
            <Input
              id="announcement-title"
              name="title"
              value={title}
              maxLength={TITLE_MAX}
              disabled={busy}
              placeholder="Ví dụ: Lịch thi cuối kỳ"
              aria-invalid={Boolean(fieldErrors.title)}
              onChange={(e) => setTitle(e.target.value)}
            />
          </FormField>
          <FormField id="announcement-content" label="Nội dung" required error={fieldErrors.content} hint={`${content.length}/${CONTENT_MAX}`}>
            <Textarea
              id="announcement-content"
              name="content"
              className="field-sizing-fixed min-h-32 max-h-80"
              rows={5}
              value={content}
              maxLength={CONTENT_MAX}
              disabled={busy}
              aria-invalid={Boolean(fieldErrors.content)}
              onChange={(e) => setContent(e.target.value)}
            />
          </FormField>
          <Button type="submit" disabled={busy}>
            {busy ? <Loader2Icon className="animate-spin" aria-hidden /> : <SendIcon aria-hidden />}
            Gửi thông báo
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}

/** Danh sách thông báo, mới nhất trước. Người quản lý khóa thấy thêm số người nhận và nút gỡ. */
export function AnnouncementList({
  courseId,
  announcements,
  canManage,
}: {
  courseId: number;
  announcements: CourseAnnouncement[];
  canManage: boolean;
}) {
  if (announcements.length === 0) {
    return (
      <EmptyState
        icon={MegaphoneIcon}
        title="Chưa có thông báo nào"
        description={canManage ? "Thông báo bạn gửi sẽ hiện ở đây." : "Giảng viên chưa gửi thông báo nào cho khóa học này."}
      />
    );
  }
  return (
    <ol className="space-y-3">
      {announcements.map((a) => (
        <li key={a.id}>
          <Card>
            <CardContent className="flex gap-4">
              <IconTile icon={MegaphoneIcon} tone="info" />
              <div className="min-w-0 flex-1 space-y-1.5">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <h3 className="font-semibold break-words">{a.title}</h3>
                  {canManage && <DeleteAnnouncement courseId={courseId} announcement={a} />}
                </div>
                <p className="text-caption text-muted-foreground">
                  {a.authorName} · <time dateTime={a.createdAt}>{formatDate(a.createdAt)}</time>
                  {canManage && a.recipientCount !== null && ` · ${formatNumber(a.recipientCount)} người nhận`}
                </p>
                <p className="text-sm leading-relaxed break-words whitespace-pre-wrap">{a.content}</p>
              </div>
            </CardContent>
          </Card>
        </li>
      ))}
    </ol>
  );
}

function DeleteAnnouncement({ courseId, announcement }: { courseId: number; announcement: CourseAnnouncement }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [refreshing, startTransition] = useTransition();
  const busy = pending || refreshing;

  async function remove() {
    setPending(true);
    try {
      await api(`/api/courses/${courseId}/announcements/${announcement.id}`, { method: "DELETE" });
      toast.success("Đã gỡ thông báo khỏi trang khóa học");
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
        <Button variant="ghost" size="sm" disabled={busy} aria-label={`Gỡ thông báo ${announcement.title}`}>
          {busy ? <Loader2Icon className="animate-spin" aria-hidden /> : <Trash2Icon aria-hidden />}
          Gỡ
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Gỡ thông báo này?</AlertDialogTitle>
          <AlertDialogDescription>
            Thông báo không còn trên trang khóa học. Học viên đã nhận trong hộp thư thì vẫn giữ.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Hủy</AlertDialogCancel>
          <AlertDialogAction variant="destructive" onClick={remove}>
            Gỡ thông báo
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
