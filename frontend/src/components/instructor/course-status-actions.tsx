"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { Course, CourseStatus } from "@/lib/types";
import { Alert, Button, LinkButton } from "@/components/ui";

export function CourseStatusActions({
  course,
}: {
  course: Pick<Course, "id" | "status" | "totalLessons" | "studentCount">;
}) {
  const router = useRouter();
  const [pending, setPending] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function changeStatus(status: CourseStatus, confirmText?: string) {
    if (confirmText && !window.confirm(confirmText)) return;
    setPending(status);
    setError(null);
    try {
      await api<Course>(`/api/courses/${course.id}/status`, { method: "PATCH", body: { status } });
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setPending(null);
    }
  }

  async function remove() {
    if (!window.confirm("Xóa vĩnh viễn khóa học này cùng toàn bộ chương, bài học? Không thể hoàn tác.")) return;
    setPending("DELETE");
    setError(null);
    try {
      await api(`/api/courses/${course.id}`, { method: "DELETE" });
      router.push("/instructor");
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
      setPending(null);
    }
  }

  const isDraft = course.status === "DRAFT" || course.status === "PENDING_REVIEW";
  const noLessons = course.totalLessons === 0;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-2">
        {isDraft && (
          <Button
            loading={pending === "PUBLISHED"}
            disabled={pending !== null}
            onClick={() =>
              changeStatus(
                "PUBLISHED",
                noLessons
                  ? "Khóa học chưa có bài học nào. Học viên ghi danh sẽ không có gì để học. Vẫn xuất bản?"
                  : "Xuất bản khóa học? Học viên sẽ thấy và ghi danh được ngay.",
              )
            }
          >
            Xuất bản
          </Button>
        )}
        {course.status === "PUBLISHED" && (
          <Button
            variant="secondary"
            loading={pending === "ARCHIVED"}
            disabled={pending !== null}
            onClick={() =>
              changeStatus(
                "ARCHIVED",
                "Lưu trữ khóa học? Khóa sẽ bị ẩn khỏi danh sách công khai, học viên mới không ghi danh được và bạn không sửa được thông tin cho tới khi mở lại.",
              )
            }
          >
            Lưu trữ
          </Button>
        )}
        {course.status === "ARCHIVED" && (
          <Button loading={pending === "PUBLISHED"} disabled={pending !== null} onClick={() => changeStatus("PUBLISHED")}>
            Mở lại
          </Button>
        )}
        <LinkButton href={`/courses/${course.id}`} variant="secondary">
          Xem trang khóa
        </LinkButton>
        {course.status === "DRAFT" && course.studentCount === 0 && (
          <Button variant="danger" loading={pending === "DELETE"} disabled={pending !== null} onClick={remove}>
            Xóa khóa học
          </Button>
        )}
      </div>
      {isDraft && noLessons && (
        <p className="text-sm text-amber-700">Khóa học chưa có bài học nào, nên thêm nội dung trước khi xuất bản.</p>
      )}
      <p className="text-xs text-slate-500">
        Khi xuất bản (hoặc sửa khóa đang công khai), thông tin khóa được đồng bộ sang dịch vụ ghi danh để học viên đăng
        ký được. Có thể mất vài giây trước khi học viên ghi danh được.
      </p>
      {error && <Alert>{error}</Alert>}
    </div>
  );
}
