"use client";

import { Loader2Icon, Trash2Icon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { toast } from "sonner";
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
import { Button } from "@/components/ui/button";
import { api } from "@/lib/client";
import { formErrorMessage } from "@/lib/forms";

export function RemoveReviewButton({
  courseId,
  reviewId,
  authorName,
}: {
  courseId: number;
  reviewId: number;
  authorName: string;
}) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [pending, setPending] = useState(false);
  const [refreshing, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const busy = pending || refreshing;

  async function remove() {
    if (busy) return;
    setPending(true);
    setError(null);
    try {
      await api(`/api/courses/${courseId}/reviews/${reviewId}`, { method: "DELETE" });
      setOpen(false);
      toast.success("Đã gỡ đánh giá");
      // Quay về trang đầu để không để lại trang rỗng khi gỡ nhận xét cuối trang.
      startTransition(() => {
        router.replace(`/courses/${courseId}#reviews`, { scroll: false });
        router.refresh();
      });
    } catch (cause) {
      setError(formErrorMessage(cause));
    } finally {
      setPending(false);
    }
  }

  return (
    <>
      <Button
        type="button"
        variant="ghost"
        size="sm"
        disabled={busy}
        aria-label={`Gỡ đánh giá của ${authorName}`}
        onClick={() => {
          setError(null);
          setOpen(true);
        }}
      >
        {busy ? <Loader2Icon className="animate-spin" aria-hidden /> : <Trash2Icon aria-hidden />}
        Gỡ
      </Button>
      <AlertDialog
        open={open}
        onOpenChange={(value) => {
          if (!busy) setOpen(value);
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Gỡ đánh giá này?</AlertDialogTitle>
            <AlertDialogDescription className="break-words">
              Đánh giá của {authorName} sẽ bị xóa và điểm trung bình được tính lại. Người viết vẫn có thể đánh giá lại
              nếu đủ điều kiện.
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
                void remove();
              }}
            >
              {busy && <Loader2Icon className="animate-spin" aria-hidden />}
              Xác nhận gỡ
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
