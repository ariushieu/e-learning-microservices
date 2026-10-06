import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { cache } from "react";
import { formatMaxAttempts, formatScore, formatTimeLimit } from "@/components/quiz/labels";
import { Badge, LinkButton } from "@/components/ui";
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

export default async function QuizEditorPage({ params }: PageProps<"/instructor/quizzes/[id]">) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) notFound();
  const quiz = await loadQuiz(id);
  if (!quiz) notFound();

  return (
    <div className="space-y-6">
      <div>
        <Link href={`/instructor/courses/${quiz.courseId}`} className="text-sm text-indigo-600 hover:underline">
          ← Về khóa học
        </Link>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-4">
          <div>
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-2xl font-bold text-slate-900">{quiz.title}</h1>
              <Badge value={quiz.status} />
            </div>
            <p className="mt-1 text-sm text-slate-500">
              {quiz.questions.length} câu hỏi · {formatTimeLimit(quiz.timeLimitMinutes)} · Điểm đạt{" "}
              {formatScore(quiz.passScore)} · Số lần làm: {formatMaxAttempts(quiz.maxAttempts)}
            </p>
          </div>
          {quiz.status === "PUBLISHED" && (
            <LinkButton href={`/quizzes/${quiz.id}`} variant="secondary">
              Xem như học viên
            </LinkButton>
          )}
        </div>
      </div>

      <div className="grid items-start gap-6 lg:grid-cols-[1fr_380px]">
        <div className="order-2 lg:order-1">
          <QuestionManager quizId={quiz.id} status={quiz.status} questions={quiz.questions} />
        </div>
        <div className="order-1 space-y-6 lg:order-2">
          <QuizStatusActions quiz={quiz} />
          <QuizSettingsForm quiz={quiz} />
        </div>
      </div>
    </div>
  );
}
