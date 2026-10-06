"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { formatPoints, QuestionTypeTag } from "@/components/quiz/labels";
import { QuestionForm } from "@/components/quiz/question-form";
import { Alert, Button, Card, Empty } from "@/components/ui";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { Question, QuizStatus } from "@/lib/types";

export function QuestionManager({
  quizId,
  status,
  questions,
}: {
  quizId: number;
  status: QuizStatus;
  questions: Question[];
}) {
  const router = useRouter();
  // "new" = đang mở form thêm, số = đang sửa câu có id đó.
  const [editing, setEditing] = useState<number | "new" | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  function saved(message: string) {
    setEditing(null);
    setError(null);
    setNotice(message);
    router.refresh();
  }

  async function remove(q: Question, index: number) {
    if (!window.confirm(`Xóa câu ${index + 1}? Thao tác này không hoàn tác được.`)) return;
    setDeletingId(q.id);
    setError(null);
    setNotice(null);
    try {
      await api(`/api/quizzes/${quizId}/questions/${q.id}`, { method: "DELETE" });
      if (editing === q.id) setEditing(null);
      setNotice("Đã xóa câu hỏi.");
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setDeletingId(null);
    }
  }

  const totalScore = questions.reduce((sum, q) => sum + Number(q.score ?? 0), 0);

  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Câu hỏi ({questions.length})</h2>
          <p className="text-sm text-slate-500">Tổng điểm: {formatPoints(totalScore)}</p>
        </div>
        {editing !== "new" && (
          <Button
            onClick={() => {
              setEditing("new");
              setNotice(null);
            }}
          >
            + Thêm câu hỏi
          </Button>
        )}
      </div>

      {status === "PUBLISHED" && (
        <Alert kind="info">Bài kiểm tra đang mở cho học viên: thay đổi câu hỏi áp dụng ngay cho các lượt làm sau.</Alert>
      )}
      {error && <Alert>{error}</Alert>}
      {notice && <Alert kind="success">{notice}</Alert>}

      {editing === "new" && (
        <QuestionForm quizId={quizId} onSaved={() => saved("Đã thêm câu hỏi.")} onCancel={() => setEditing(null)} />
      )}

      {questions.length === 0 && editing !== "new" ? (
        <Empty>Chưa có câu hỏi nào. Thêm ít nhất một câu để có thể xuất bản.</Empty>
      ) : (
        questions.map((q, i) =>
          editing === q.id ? (
            <QuestionForm
              key={q.id}
              quizId={quizId}
              initial={q}
              onSaved={() => saved("Đã lưu câu hỏi.")}
              onCancel={() => setEditing(null)}
            />
          ) : (
            <Card key={q.id}>
              <div className="mb-2 flex flex-wrap items-center gap-2">
                <span className="font-semibold text-slate-900">Câu {i + 1}</span>
                <QuestionTypeTag type={q.type} />
                <span className="text-xs text-slate-500">{formatPoints(q.score)} điểm</span>
                <span className="ml-auto flex gap-1">
                  <Button
                    variant="ghost"
                    className="px-3 py-1"
                    onClick={() => {
                      setEditing(q.id);
                      setNotice(null);
                    }}
                  >
                    Sửa
                  </Button>
                  <Button
                    variant="ghost"
                    className="px-3 py-1 text-rose-600 hover:bg-rose-50"
                    loading={deletingId === q.id}
                    onClick={() => remove(q, i)}
                  >
                    Xóa
                  </Button>
                </span>
              </div>
              <p className="mb-3 whitespace-pre-line text-slate-800">{q.content}</p>
              <ul className="space-y-1.5">
                {q.options.map((o) => (
                  <li
                    key={o.id}
                    className={`flex items-start gap-2 rounded-lg px-3 py-1.5 text-sm ${
                      o.isCorrect ? "bg-emerald-50 text-emerald-800" : "text-slate-700"
                    }`}
                  >
                    <span className="w-4 shrink-0 font-bold">{o.isCorrect ? "✓" : "•"}</span>
                    <span className="whitespace-pre-line">{o.content}</span>
                  </li>
                ))}
              </ul>
              {q.explanation && (
                <p className="mt-3 rounded-lg bg-sky-50 px-3 py-2 text-sm text-sky-800">
                  <span className="font-semibold">Giải thích: </span>
                  {q.explanation}
                </p>
              )}
            </Card>
          ),
        )
      )}
    </section>
  );
}
