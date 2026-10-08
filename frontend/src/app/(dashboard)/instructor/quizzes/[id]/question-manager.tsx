"use client";

import { CheckIcon, FileQuestionIcon, InfoIcon, LightbulbIcon, Loader2Icon, PencilIcon, PlusIcon, Trash2Icon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { Section } from "@/components/common/section";
import { formatPoints, QuestionTypeTag } from "@/components/quiz/labels";
import { QuestionForm } from "@/components/quiz/question-form";
import { ImportQuestionsDialog } from "@/components/quiz/import-questions-dialog";
import { Alert, AlertDescription } from "@/components/ui/alert";
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
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { Question, QuizStatus } from "@/lib/types";
import { cn } from "@/lib/utils";

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
  const [confirmDelete, setConfirmDelete] = useState<{ question: Question; index: number } | null>(null);
  const [error, setError] = useState<string | null>(null);

  function saved(message: string) {
    setEditing(null);
    setError(null);
    toast.success(message);
    router.refresh();
  }

  async function remove(q: Question) {
    setDeletingId(q.id);
    setError(null);
    try {
      await api(`/api/quizzes/${quizId}/questions/${q.id}`, { method: "DELETE" });
      if (editing === q.id) setEditing(null);
      toast.success("Đã xóa câu hỏi");
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setDeletingId(null);
    }
  }

  const totalScore = questions.reduce((sum, q) => sum + Number(q.score ?? 0), 0);

  return (
    <Section
      title="Câu hỏi"
      count={questions.length}
      description={<span className="tabular-nums">Tổng điểm: {formatPoints(totalScore)}</span>}
      actions={
        <div className="flex flex-wrap gap-2">
          <ImportQuestionsDialog quizId={quizId} onImported={() => { setEditing(null); setError(null); }} />
          {editing !== "new" && (
            <Button variant="outline" onClick={() => setEditing("new")}>
              <PlusIcon /> Thêm câu hỏi
            </Button>
          )}
        </div>
      }
      className="min-w-0"
    >

      {status === "PUBLISHED" && (
        <Alert>
          <InfoIcon />
          <AlertDescription>
            Bài kiểm tra đang mở cho học viên: thay đổi câu hỏi áp dụng ngay cho các lượt làm sau.
          </AlertDescription>
        </Alert>
      )}
      {error && <ErrorAlert message={error} />}

      {editing === "new" && (
        <QuestionForm quizId={quizId} onSaved={() => saved("Đã thêm câu hỏi")} onCancel={() => setEditing(null)} />
      )}

      {questions.length === 0 && editing !== "new" ? (
        <EmptyState
          icon={FileQuestionIcon}
          title="Chưa có câu hỏi nào"
          description="Thêm ít nhất một câu để có thể xuất bản bài kiểm tra."
        />
      ) : (
        questions.map((q, i) =>
          editing === q.id ? (
            <QuestionForm
              key={q.id}
              quizId={quizId}
              initial={q}
              index={i}
              onSaved={() => saved("Đã lưu câu hỏi")}
              onCancel={() => setEditing(null)}
            />
          ) : (
            <Card key={q.id}>
              <CardHeader className="gap-3">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-subheading">Câu {i + 1}</span>
                  <QuestionTypeTag type={q.type} />
                  <span className="text-caption text-muted-foreground tabular-nums">{formatPoints(q.score)} điểm</span>
                  <span className="ml-auto flex gap-1">
                    <Button variant="ghost" size="sm" onClick={() => setEditing(q.id)}>
                      <PencilIcon /> Sửa
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-destructive hover:bg-destructive-soft hover:text-destructive-strong"
                      disabled={deletingId === q.id}
                      onClick={() => setConfirmDelete({ question: q, index: i })}
                    >
                      {deletingId === q.id ? <Loader2Icon className="animate-spin" /> : <Trash2Icon />} Xóa
                    </Button>
                  </span>
                </div>
                <p className="leading-relaxed whitespace-pre-line">{q.content}</p>
              </CardHeader>
              <CardContent className="space-y-3">
                <ul className="space-y-1.5">
                  {q.options.map((o) => (
                    <li
                      key={o.id}
                      className={cn(
                        "flex items-start gap-2.5 rounded-lg px-3 py-2 text-sm",
                        o.isCorrect ? "bg-success-soft text-success-strong" : "bg-muted/50",
                      )}
                    >
                      <span
                        className={cn(
                          "mt-0.5 flex size-4 shrink-0 items-center justify-center rounded-full",
                          o.isCorrect ? "bg-success text-white" : "border bg-card",
                        )}
                        aria-hidden
                      >
                        {o.isCorrect && <CheckIcon className="size-3" />}
                      </span>
                      <span className="flex-1 whitespace-pre-line">{o.content}</span>
                      {o.isCorrect && <span className="sr-only">(đáp án đúng)</span>}
                    </li>
                  ))}
                </ul>
                {q.explanation && (
                  <div className="flex gap-2.5 rounded-lg bg-muted px-3 py-2 text-sm">
                    <LightbulbIcon className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
                    <p>
                      <span className="font-medium">Giải thích: </span>
                      <span className="whitespace-pre-line text-muted-foreground">{q.explanation}</span>
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          ),
        )
      )}

      <AlertDialog open={confirmDelete !== null} onOpenChange={(open) => !open && setConfirmDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa câu {confirmDelete ? confirmDelete.index + 1 : ""}?</AlertDialogTitle>
            <AlertDialogDescription>Câu hỏi và các phương án sẽ bị xóa. Thao tác này không hoàn tác được.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              onClick={() => {
                if (confirmDelete) void remove(confirmDelete.question);
                setConfirmDelete(null);
              }}
            >
              Xóa câu hỏi
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Section>
  );
}
