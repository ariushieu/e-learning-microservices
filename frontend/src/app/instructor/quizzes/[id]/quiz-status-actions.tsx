"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert, Badge, Button, Card } from "@/components/ui";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { QuizDetail } from "@/lib/types";

export function QuizStatusActions({ quiz }: { quiz: QuizDetail }) {
  const router = useRouter();
  const [busy, setBusy] = useState<"PUBLISHED" | "ARCHIVED" | "DELETE" | null>(null);
  const [error, setError] = useState<string | null>(null);
  const noQuestions = quiz.questions.length === 0;

  async function changeStatus(status: "PUBLISHED" | "ARCHIVED") {
    if (status === "ARCHIVED" && !window.confirm("Lưu trữ bài kiểm tra? Học viên sẽ không làm bài được nữa.")) return;
    setBusy(status);
    setError(null);
    try {
      await api(`/api/quizzes/${quiz.id}/status`, { method: "PATCH", body: { status } });
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(null);
    }
  }

  async function remove() {
    if (!window.confirm(`Xóa bài kiểm tra "${quiz.title}" cùng toàn bộ câu hỏi? Thao tác này không hoàn tác được.`)) {
      return;
    }
    setBusy("DELETE");
    setError(null);
    try {
      await api(`/api/quizzes/${quiz.id}`, { method: "DELETE" });
      router.push(`/instructor/courses/${quiz.courseId}`);
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
      setBusy(null);
    }
  }

  return (
    <Card className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="font-semibold text-slate-900">Trạng thái</h2>
        <Badge value={quiz.status} />
      </div>
      <p className="text-sm text-slate-500">
        {quiz.status === "PUBLISHED"
          ? "Học viên của khóa đang làm được bài này."
          : quiz.status === "ARCHIVED"
            ? "Đã lưu trữ: học viên không làm được, cài đặt bị khóa. Có thể xuất bản lại."
            : "Bản nháp: chỉ bạn thấy. Cần ít nhất 1 câu hỏi để xuất bản."}
      </p>
      {error && <Alert>{error}</Alert>}
      <div className="flex flex-wrap gap-2">
        {quiz.status !== "PUBLISHED" && (
          <Button
            onClick={() => changeStatus("PUBLISHED")}
            loading={busy === "PUBLISHED"}
            disabled={noQuestions || busy !== null}
            title={noQuestions ? "Thêm ít nhất 1 câu hỏi trước khi xuất bản" : undefined}
          >
            Xuất bản
          </Button>
        )}
        {quiz.status !== "ARCHIVED" && (
          <Button
            variant="secondary"
            onClick={() => changeStatus("ARCHIVED")}
            loading={busy === "ARCHIVED"}
            disabled={busy !== null}
          >
            Lưu trữ
          </Button>
        )}
        <Button variant="danger" onClick={remove} loading={busy === "DELETE"} disabled={busy !== null}>
          Xóa
        </Button>
      </div>
    </Card>
  );
}
