"use client";

import { Loader2Icon, MessageSquareReplyIcon, PencilIcon, Trash2Icon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, useTransition, type FormEvent } from "react";
import { toast } from "sonner";
import { ErrorAlert } from "@/components/common/error-alert";
import { FormField } from "@/components/common/form-field";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { api } from "@/lib/client";
import { formatDay } from "@/lib/format";
import { fieldErrorMap, formErrorMessage } from "@/lib/forms";
import type { CourseReview } from "./review-types";

export function ReviewReply({
  courseId,
  review,
  canReply,
}: {
  courseId: number;
  review: CourseReview;
  canReply: boolean;
}) {
  const router = useRouter();
  const [editing, setEditing] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [content, setContent] = useState(review.reply ?? "");
  const [pending, setPending] = useState(false);
  const [refreshing, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const [fieldError, setFieldError] = useState<string | undefined>();
  const busy = pending || refreshing;
  const inputId = `review-reply-${review.id}`;

  async function mutate(method: "PUT" | "DELETE") {
    if (busy) return;
    setPending(true);
    setError(null);
    setFieldError(undefined);
    try {
      await api(`/api/courses/${courseId}/reviews/${review.id}/reply`, {
        method,
        ...(method === "PUT" ? { body: { content } } : {}),
      });
      toast.success(method === "PUT" ? "Đã lưu phản hồi" : "Đã xóa phản hồi");
      setEditing(false);
      setConfirming(false);
      startTransition(() => router.refresh());
    } catch (cause) {
      setError(formErrorMessage(cause));
      setFieldError(fieldErrorMap(cause).content);
    } finally {
      setPending(false);
    }
  }

  function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!content.trim() || content.length > 1000) {
      setFieldError("Nhập phản hồi từ 1 đến 1000 ký tự, không chỉ gồm khoảng trắng.");
      return;
    }
    void mutate("PUT");
  }

  return (
    <div className="mt-3 space-y-3">
      {review.reply !== null && (
        <div className="space-y-2 rounded-lg border-l-2 border-primary bg-primary-soft p-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h4 className="text-sm font-semibold text-primary-strong">Phản hồi của giảng viên</h4>
            {review.repliedAt && (
              <time dateTime={review.repliedAt} className="text-caption text-muted-foreground">
                {formatDay(review.repliedAt)}
              </time>
            )}
          </div>
          <p className="whitespace-pre-wrap break-words text-sm leading-relaxed">{review.reply}</p>
        </div>
      )}
      {canReply && (
        <>
          {editing ? (
            <form
              onSubmit={save}
              className="space-y-3 rounded-lg border p-4"
              aria-label={`Phản hồi đánh giá của ${review.authorName}`}
            >
              {error && <ErrorAlert message={error} />}
              <FormField
                id={inputId}
                label="Nội dung phản hồi"
                required
                error={fieldError}
                hint={`${content.length}/1000 ký tự`}
              >
                <Textarea
                  id={inputId}
                  className="field-sizing-fixed min-h-28 max-h-64"
                  value={content}
                  maxLength={1000}
                  rows={4}
                  disabled={busy}
                  aria-invalid={Boolean(fieldError)}
                  onChange={(event) => setContent(event.target.value)}
                />
              </FormField>
              <div className="flex flex-wrap gap-2">
                <Button type="submit" disabled={busy}>
                  {busy && <Loader2Icon className="animate-spin" aria-hidden />}
                  Lưu phản hồi
                </Button>
                <Button type="button" variant="ghost" disabled={busy} onClick={() => setEditing(false)}>
                  Hủy
                </Button>
              </div>
            </form>
          ) : (
            <div className="flex flex-wrap gap-2">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                disabled={busy}
                onClick={() => {
                  setContent(review.reply ?? "");
                  setError(null);
                  setFieldError(undefined);
                  setEditing(true);
                }}
              >
                {review.reply ? <PencilIcon aria-hidden /> : <MessageSquareReplyIcon aria-hidden />}
                {review.reply ? "Sửa phản hồi" : "Trả lời"}
              </Button>
              {review.reply && (
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  disabled={busy}
                  onClick={() => {
                    setError(null);
                    setConfirming(true);
                  }}
                >
                  <Trash2Icon aria-hidden />
                  Xóa phản hồi
                </Button>
              )}
            </div>
          )}
          <AlertDialog
            open={confirming}
            onOpenChange={(value) => {
              if (!busy) setConfirming(value);
            }}
          >
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Xóa phản hồi này?</AlertDialogTitle>
                <AlertDialogDescription>
                  Chỉ phản hồi bị xóa. Số sao và nhận xét của học viên vẫn được giữ lại.
                </AlertDialogDescription>
              </AlertDialogHeader>
              {error && <ErrorAlert message={error} />}
              <AlertDialogFooter>
                <AlertDialogCancel disabled={busy}>Giữ lại</AlertDialogCancel>
                <AlertDialogAction
                  variant="destructive"
                  disabled={busy}
                  onClick={(event) => {
                    event.preventDefault();
                    void mutate("DELETE");
                  }}
                >
                  {busy && <Loader2Icon className="animate-spin" aria-hidden />}
                  Xác nhận xóa phản hồi
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </>
      )}
    </div>
  );
}
