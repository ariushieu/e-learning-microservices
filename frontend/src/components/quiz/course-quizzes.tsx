import { ChevronRightIcon, ClipboardListIcon } from "lucide-react";
import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { IconTile } from "@/components/common/icon-tile";
import { Section } from "@/components/common/section";
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
    <ul className="divide-y overflow-hidden rounded-xl bg-card shadow-card ring-1 ring-border">
      {quizzes.map((q) => (
        <li key={q.id}>
          <Link
            href={`/quizzes/${q.id}`}
            className="group flex items-center gap-3 px-4 py-3 transition-colors outline-none hover:bg-muted/50 focus-visible:bg-muted/50"
          >
            <IconTile icon={ClipboardListIcon} size="sm" />
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm font-medium group-hover:text-primary">{q.title}</span>
              <span className="mt-0.5 block truncate text-caption text-muted-foreground">{quizMeta(q).join(" · ")}</span>
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
    <Section title="Bài kiểm tra" count={error === null ? published.length : undefined}>
      {error !== null ? (
        <ErrorAlert title="Không tải được bài kiểm tra" message={error} />
      ) : published.length === 0 ? (
        <EmptyState icon={ClipboardListIcon} title="Khóa học chưa có bài kiểm tra nào." className="py-8" />
      ) : (
        <QuizList quizzes={published} />
      )}
    </Section>
  );
}
