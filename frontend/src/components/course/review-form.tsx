"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Loader2Icon, StarIcon, Trash2Icon } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { FormField } from "@/components/common/form-field";
import { ErrorAlert } from "@/components/common/error-alert";
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
import { api } from "@/lib/client";
import { formErrorMessage, fieldErrorMap } from "@/lib/forms";
import { cn } from "@/lib/utils";
import type { CourseReview } from "./review-types";

const ratingLabels = ["Chưa hài lòng", "Cần cải thiện", "Khá tốt", "Hài lòng", "Rất hài lòng"];

export function ReviewForm({ courseId, review }: { courseId: number; review: CourseReview | null }) {
  const router = useRouter();
  const [rating, setRating] = useState(review?.rating ?? 0);
  const [comment, setComment] = useState(review?.comment ?? "");
  const [busy, setBusy] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!rating) {
      setErrors({ rating: "Hãy chọn số sao trước khi gửi." });
      return;
    }
    await mutate("PUT");
  }
  async function mutate(method: "PUT" | "DELETE") {
    setBusy(true);
    setError(null);
    setErrors({});
    try {
      await api(`/api/courses/${courseId}/reviews/me`, {
        method,
        ...(method === "PUT" ? { body: { rating, comment } } : {}),
      });
      toast.success(method === "PUT" ? "Đã lưu đánh giá của bạn" : "Đã xóa đánh giá");
      setConfirming(false);
      if (method === "DELETE")
        router.replace(`/courses/${courseId}#reviews`, {
          scroll: false,
        });
      router.refresh();
    } catch (cause) {
      setErrors(fieldErrorMap(cause));
      setError(formErrorMessage(cause));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="rounded-xl border bg-card p-5 shadow-card sm:p-6">
      <div className="mb-5">
        <h3 className="text-subheading">{review ? "Đánh giá của bạn" : "Chia sẻ trải nghiệm học tập"}</h3>
        <p className="mt-1 text-sm text-muted-foreground">
          Nhận xét của bạn giúp người học tiếp theo lựa chọn khóa học phù hợp.
        </p>
      </div>
      <form onSubmit={save} className="space-y-5">
        {error && <ErrorAlert message={error} />}
        <fieldset disabled={busy} className="space-y-2">
          <legend className="mb-2 text-sm font-medium">Mức độ hài lòng</legend>
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex gap-1">
              {ratingLabels.map((text, index) => (
                <label
                  key={text}
                  className="relative cursor-pointer rounded-lg p-2 has-focus-visible:ring-2 has-focus-visible:ring-ring"
                >
                  <input
                    className="sr-only"
                    type="radio"
                    name="rating"
                    value={index + 1}
                    aria-label={`${index + 1} sao — ${text}`}
                    checked={rating === index + 1}
                    onChange={() => setRating(index + 1)}
                  />
                  <StarIcon
                    aria-hidden
                    className={cn("size-7", index < rating ? "fill-primary text-primary" : "text-muted-foreground")}
                  />
                </label>
              ))}
            </div>
            <span className="text-sm font-medium text-primary" aria-live="polite">
              {rating ? ratingLabels[rating - 1] : "Chọn số sao"}
            </span>
          </div>
          {errors.rating && (
            <p className="text-sm text-destructive" role="alert">
              {errors.rating}
            </p>
          )}
        </fieldset>
        <FormField
          id="review-comment"
          label="Nhận xét (không bắt buộc)"
          error={errors.comment}
          hint={`${comment.length}/2000 ký tự`}
        >
          <Textarea
            id="review-comment"
            maxLength={2000}
            rows={4}
            value={comment}
            disabled={busy}
            aria-invalid={Boolean(errors.comment)}
            onChange={(e) => setComment(e.target.value)}
            placeholder="Bạn học được điều gì? Phần nào của khóa học hữu ích với bạn?"
          />
        </FormField>
        <div className="flex flex-wrap justify-between gap-3">
          <Button type="submit" disabled={busy}>
            {busy && <Loader2Icon className="animate-spin" aria-hidden />}
            {review ? "Lưu thay đổi" : "Gửi đánh giá"}
          </Button>
          {review && (
            <Button type="button" variant="ghost" disabled={busy} onClick={() => setConfirming(true)}>
              <Trash2Icon aria-hidden />
              Xóa đánh giá
            </Button>
          )}
        </div>
      </form>
      <AlertDialog open={confirming} onOpenChange={setConfirming}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa đánh giá của bạn?</AlertDialogTitle>
            <AlertDialogDescription>
              Số sao và nhận xét sẽ được gỡ khỏi khóa học. Bạn có thể viết lại sau.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={busy}>Giữ lại</AlertDialogCancel>
            <AlertDialogAction
              disabled={busy}
              onClick={(event) => {
                event.preventDefault();
                void mutate("DELETE");
              }}
            >
              Xác nhận xóa
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
