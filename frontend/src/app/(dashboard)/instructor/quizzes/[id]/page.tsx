import { ArrowLeftIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { cache } from "react";
import { StatusBadge } from "@/components/common/status-badge";
import { formatMaxAttempts, formatScore, formatTimeLimit } from "@/components/quiz/labels";
import { Button } from "@/components/ui/button";
import { gatewayOrNull } from "@/lib/server/gateway";
import type { QuizDetail } from "@/lib/types";
import { QuestionManager } from "./question-manager";
import { QuizSettingsForm } from "./quiz-settings-form";
import { QuizStatusActions } from "./quiz-status-actions";

const loadQuiz = cache((id: string) => gatewayOrNull<QuizDetail>(`/api/quizzes/${id}`));

export async function generateMetadata({ params }: PageProps<"/instructor/quizzes/[id]">): Promise<Metadata> {
  const { id } = await params;
  const quiz = /^\d+$/.test(id) ? await loadQuiz(id) : null;
  return { title: quiz ? `Soạn bài kiểm tra: ${quiz.title}` : "Soạn bài kiểm tra" };
}

const STATUS_HINT = {
  PUBLISHED: "Học viên của khóa đang làm được bài này.",
  ARCHIVED: "Đã lưu trữ: học viên không làm được, cài đặt bị khóa. Có thể xuất bản lại.",
  DRAFT: "Bản nháp: chỉ bạn thấy. Cần ít nhất 1 câu hỏi để xuất bản.",
} as const;

export default async function QuizEditorPage({ params }: PageProps<"/instructor/quizzes/[id]">) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) notFound();
  const quiz = await loadQuiz(id);
  if (!quiz) notFound();

  return (
    <div className="space-y-6">
      <div className="space-y-3">
        <Button asChild variant="ghost" size="sm" className="-ml-2.5 text-muted-foreground">
          <Link href={`/instructor/courses/${quiz.courseId}`}>
            <ArrowLeftIcon /> Về khóa học
          </Link>
        </Button>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="min-w-0 space-y-1.5">
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-2xl font-semibold tracking-tight text-balance">{quiz.title}</h1>
              <StatusBadge status={quiz.status} />
            </div>
            <p className="text-sm text-muted-foreground">
              {quiz.questions.length} câu hỏi · {formatTimeLimit(quiz.timeLimitMinutes)} · Điểm đạt{" "}
              {formatScore(quiz.passScore)} · Số lần làm: {formatMaxAttempts(quiz.maxAttempts)}
            </p>
            <p className="text-sm text-muted-foreground">{STATUS_HINT[quiz.status]}</p>
          </div>
          <QuizStatusActions quiz={quiz} />
        </div>
      </div>

      <div className="grid items-start gap-6 lg:grid-cols-[1fr_22rem]">
        <QuestionManager quizId={quiz.id} status={quiz.status} questions={quiz.questions} />
        <QuizSettingsForm quiz={quiz} />
      </div>
    </div>
  );
}
