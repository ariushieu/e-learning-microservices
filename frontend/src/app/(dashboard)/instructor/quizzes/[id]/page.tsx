import { ArrowLeftIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { cache } from "react";
import { StatusBadge } from "@/components/common/status-badge";
import { formatMaxAttempts, formatScore, formatTimeLimit } from "@/components/quiz/labels";
import { DashboardPage } from "@/components/templates/dashboard-page";
import { Button } from "@/components/ui/button";
import { gateway, gatewayOrNull } from "@/lib/server/gateway";
import type { Course, QuizDetail } from "@/lib/types";
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
  // Tên khóa chỉ để hiện trên đường dẫn; lỗi thì ghi chung chung, không chặn trang.
  const course = await gateway<Course>(`/api/courses/${quiz.courseId}`).catch(() => null);

  return (
    <DashboardPage
      crumbs={[
        { href: "/instructor", label: "Khóa học tôi dạy" },
        { href: `/instructor/courses/${quiz.courseId}`, label: course?.title ?? "Khóa học" },
        { label: quiz.title },
      ]}
      title={quiz.title}
      description={
        <span className="flex flex-col gap-2 text-sm">
          <span className="flex flex-wrap items-center gap-2">
            <StatusBadge status={quiz.status} />
            <span>{STATUS_HINT[quiz.status]}</span>
          </span>
          <span className="tabular-nums">
            {quiz.questions.length} câu hỏi · {formatTimeLimit(quiz.timeLimitMinutes)} · Điểm đạt {formatScore(quiz.passScore)} · Số lần làm:{" "}
            {formatMaxAttempts(quiz.maxAttempts)}
          </span>
        </span>
      }
      actions={
        // PageHeader không cho cột nút co lại; giới hạn theo bề ngang màn hình để nút tự xuống dòng trên điện thoại.
        <div className="flex flex-wrap items-start gap-2">
          <Button asChild variant="ghost">
            <Link href={`/instructor/courses/${quiz.courseId}`}>
              <ArrowLeftIcon /> Về khóa học
            </Link>
          </Button>
          <QuizStatusActions quiz={quiz} />
        </div>
      }
    >
      <div className="grid items-start gap-8 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <QuestionManager quizId={quiz.id} status={quiz.status} questions={quiz.questions} />
        <QuizSettingsForm quiz={quiz} />
      </div>
    </DashboardPage>
  );
}
