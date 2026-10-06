"use client";

import { ArchiveIcon, EyeIcon, Loader2Icon, SendIcon, Trash2Icon } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
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
import { errorMessage } from "@/lib/errors";
import type { QuizDetail } from "@/lib/types";

type Busy = "PUBLISHED" | "ARCHIVED" | "DELETE" | null;

/** Nút đổi trạng thái, xóa bài và xem như học viên, đặt ở đầu trang soạn bài. */
export function QuizStatusActions({ quiz }: { quiz: QuizDetail }) {
  const router = useRouter();
  const [busy, setBusy] = useState<Busy>(null);
  const [confirm, setConfirm] = useState<"ARCHIVED" | "DELETE" | null>(null);
  const [error, setError] = useState<string | null>(null);
  const noQuestions = quiz.questions.length === 0;

  async function changeStatus(status: "PUBLISHED" | "ARCHIVED") {
    setBusy(status);
    setError(null);
    try {
      await api(`/api/quizzes/${quiz.id}/status`, { method: "PATCH", body: { status } });
      toast.success(status === "PUBLISHED" ? "Đã xuất bản bài kiểm tra" : "Đã lưu trữ bài kiểm tra");
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(null);
    }
  }

  async function remove() {
    setBusy("DELETE");
    setError(null);
    try {
      await api(`/api/quizzes/${quiz.id}`, { method: "DELETE" });
      toast.success("Đã xóa bài kiểm tra");
      router.push(`/instructor/courses/${quiz.courseId}`);
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
      setBusy(null);
    }
  }

  function onConfirm() {
    const action = confirm;
    setConfirm(null);
    if (action === "DELETE") void remove();
    else if (action === "ARCHIVED") void changeStatus("ARCHIVED");
  }

  const spinner = (key: Busy) => (busy === key ? <Loader2Icon className="animate-spin" /> : null);

  return (
    <div className="flex flex-col items-end gap-3">
      <div className="flex flex-wrap justify-end gap-2">
        {quiz.status === "PUBLISHED" && (
          <Button asChild variant="ghost">
            <Link href={`/quizzes/${quiz.id}`}>
              <EyeIcon /> Xem như học viên
            </Link>
          </Button>
        )}
        <Button
          variant="destructive"
          onClick={() => setConfirm("DELETE")}
          disabled={busy !== null}
        >
          {spinner("DELETE") ?? <Trash2Icon />} Xóa
        </Button>
        {quiz.status !== "ARCHIVED" && (
          <Button variant="outline" onClick={() => setConfirm("ARCHIVED")} disabled={busy !== null}>
            {spinner("ARCHIVED") ?? <ArchiveIcon />} Lưu trữ
          </Button>
        )}
        {quiz.status !== "PUBLISHED" && (
          <Button
            onClick={() => changeStatus("PUBLISHED")}
            disabled={noQuestions || busy !== null}
            title={noQuestions ? "Thêm ít nhất 1 câu hỏi trước khi xuất bản" : undefined}
          >
            {spinner("PUBLISHED") ?? <SendIcon />} Xuất bản
          </Button>
        )}
      </div>
      {error && (
        <div className="w-full max-w-md">
          <ErrorAlert message={error} />
        </div>
      )}

      <AlertDialog open={confirm !== null} onOpenChange={(open) => !open && setConfirm(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {confirm === "DELETE" ? `Xóa bài kiểm tra "${quiz.title}"?` : "Lưu trữ bài kiểm tra?"}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {confirm === "DELETE"
                ? "Bài kiểm tra cùng toàn bộ câu hỏi sẽ bị xóa. Thao tác này không hoàn tác được."
                : "Học viên sẽ không làm bài được nữa và cài đặt bị khóa. Bạn có thể xuất bản lại sau."}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction variant={confirm === "DELETE" ? "destructive" : "default"} onClick={onConfirm}>
              {confirm === "DELETE" ? "Xóa bài kiểm tra" : "Lưu trữ"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
