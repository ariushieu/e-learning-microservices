"use client";

import { ArchiveIcon, ExternalLinkIcon, Loader2Icon, RotateCcwIcon, SendIcon, Trash2Icon } from "lucide-react";
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
import type { Course, CourseStatus } from "@/lib/types";

type Action = "PUBLISHED" | "ARCHIVED" | "DELETE";

/** Nút đổi trạng thái khóa học ở đầu trang quản lý; thao tác quan trọng đều hỏi lại bằng hộp thoại. */
export function CourseStatusActions({
  course,
}: {
  course: Pick<Course, "id" | "status" | "totalLessons" | "studentCount">;
}) {
  const router = useRouter();
  const [pending, setPending] = useState<Action | null>(null);
  const [confirm, setConfirm] = useState<Action | null>(null);
  const [error, setError] = useState<string | null>(null);

  const isDraft = course.status === "DRAFT" || course.status === "PENDING_REVIEW";
  const noLessons = course.totalLessons === 0;

  async function changeStatus(status: CourseStatus) {
    setPending(status as Action);
    setError(null);
    try {
      await api<Course>(`/api/courses/${course.id}/status`, { method: "PATCH", body: { status } });
      toast.success(
        status === "ARCHIVED" ? "Đã lưu trữ khóa học" : course.status === "ARCHIVED" ? "Đã mở lại khóa học" : "Đã xuất bản khóa học",
      );
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setPending(null);
    }
  }

  async function remove() {
    setPending("DELETE");
    setError(null);
    try {
      await api(`/api/courses/${course.id}`, { method: "DELETE" });
      toast.success("Đã xóa khóa học");
      router.push("/instructor");
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
      setPending(null);
    }
  }

  const dialogs: Record<Action, { title: string; description: string; action: string; destructive?: boolean; run: () => void }> = {
    PUBLISHED: {
      title: "Xuất bản khóa học?",
      description: noLessons
        ? "Khóa học chưa có bài học nào. Học viên ghi danh sẽ không có gì để học. Vẫn xuất bản?"
        : "Học viên sẽ thấy và ghi danh được ngay. Thông tin khóa được đồng bộ sang dịch vụ ghi danh, có thể mất vài giây.",
      action: "Xuất bản",
      run: () => changeStatus("PUBLISHED"),
    },
    ARCHIVED: {
      title: "Lưu trữ khóa học?",
      description:
        "Khóa sẽ bị ẩn khỏi danh sách công khai, học viên mới không ghi danh được và bạn không sửa được thông tin cho tới khi mở lại.",
      action: "Lưu trữ",
      run: () => changeStatus("ARCHIVED"),
    },
    DELETE: {
      title: "Xóa vĩnh viễn khóa học?",
      description: "Toàn bộ chương, bài học và tài liệu của khóa sẽ bị xóa. Không thể hoàn tác.",
      action: "Xóa khóa học",
      destructive: true,
      run: remove,
    },
  };
  const dialog = confirm ? dialogs[confirm] : null;
  const busy = pending !== null;
  const spin = (a: Action) => pending === a && <Loader2Icon className="animate-spin" />;

  return (
    <div className="flex flex-col items-stretch gap-3 sm:items-end">
      <div className="flex flex-wrap gap-2">
        <Button asChild variant="outline">
          <Link href={`/courses/${course.id}`}>
            <ExternalLinkIcon /> Xem trang khóa
          </Link>
        </Button>
        {course.status === "DRAFT" && course.studentCount === 0 && (
          <Button variant="destructive" disabled={busy} onClick={() => setConfirm("DELETE")}>
            {spin("DELETE") || <Trash2Icon />} Xóa
          </Button>
        )}
        {course.status === "PUBLISHED" && (
          <Button variant="outline" disabled={busy} onClick={() => setConfirm("ARCHIVED")}>
            {spin("ARCHIVED") || <ArchiveIcon />} Lưu trữ
          </Button>
        )}
        {course.status === "ARCHIVED" && (
          <Button disabled={busy} onClick={() => changeStatus("PUBLISHED")}>
            {spin("PUBLISHED") || <RotateCcwIcon />} Mở lại
          </Button>
        )}
        {isDraft && (
          <Button disabled={busy} onClick={() => setConfirm("PUBLISHED")}>
            {spin("PUBLISHED") || <SendIcon />} Xuất bản
          </Button>
        )}
      </div>
      {error && <ErrorAlert message={error} />}

      <AlertDialog open={dialog !== null} onOpenChange={(open) => !open && setConfirm(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{dialog?.title}</AlertDialogTitle>
            <AlertDialogDescription>{dialog?.description}</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction variant={dialog?.destructive ? "destructive" : "default"} onClick={() => dialog?.run()}>
              {dialog?.action}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
