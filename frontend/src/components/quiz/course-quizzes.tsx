import { ChevronRightIcon, ClipboardListIcon } from "lucide-react";
import Link from "next/link";
import { ErrorAlert } from "@/components/common/error-alert";
import type { Quiz } from "@/lib/types";

function quizMeta(q: Quiz) {
  return [
    `${q.totalQuestions} câu hỏi`,
    q.timeLimitMinutes ? `${q.timeLimitMinutes} phút` : "Không giới hạn",
    `Đạt từ ${Number(q.passScore)}%`,
    q.maxAttempts > 0 ? `Tối đa ${q.maxAttempts} lượt` : "Không giới hạn lượt",
  ];
}

export function QuizList({ quizzes }: { quizzes: Quiz[] }) {
  return (
    <ul className="divide-y overflow-hidden rounded-xl bg-card ring-1 ring-foreground/10">
      {quizzes.map((q) => (
        <li key={q.id}>
          <Link
            href={`/quizzes/${q.id}`}
            className="group flex items-center gap-4 px-4 py-3 transition-colors hover:bg-muted/50"
          >
            <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <ClipboardListIcon className="size-4" />
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm font-medium">{q.title}</span>
              <span className="mt-0.5 block truncate text-xs text-muted-foreground">{quizMeta(q).join(" · ")}</span>
            </span>
            <ChevronRightIcon className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
          </Link>
        </li>
      ))}
    </ul>
  );
}

/** Bài kiểm tra của khóa; học viên chỉ được thấy bài đã xuất bản. */
export function CourseQuizzes({ quizzes, error }: { quizzes: Quiz[] | null; error: string | null }) {
  const published = (quizzes ?? []).filter((q) => q.status === "PUBLISHED");
  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold">Bài kiểm tra</h2>
      {error !== null ? (
        <ErrorAlert title="Không tải được bài kiểm tra" message={error} />
      ) : published.length === 0 ? (
        <p className="rounded-xl border border-dashed px-4 py-6 text-center text-sm text-muted-foreground">
          Khóa học chưa có bài kiểm tra nào.
        </p>
      ) : (
        <QuizList quizzes={published} />
      )}
    </section>
  );
}
