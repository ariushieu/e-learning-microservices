import Link from "next/link";
import { Alert } from "@/components/ui";
import type { Quiz } from "@/lib/types";
import { LessonTypeIcon } from "./icons";

export function QuizList({ quizzes }: { quizzes: Quiz[] }) {
  return (
    <ul className="grid gap-3 sm:grid-cols-2">
      {quizzes.map((q) => (
        <li key={q.id}>
          <Link
            href={`/quizzes/${q.id}`}
            className="flex h-full items-start gap-3 rounded-xl border border-slate-200 bg-white p-4 transition hover:border-indigo-300 hover:shadow-sm"
          >
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-amber-100 text-amber-700">
              <LessonTypeIcon type="QUIZ" className="h-5 w-5" />
            </span>
            <span className="min-w-0 flex-1">
              <span className="block font-medium text-slate-900">{q.title}</span>
              <span className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-xs text-slate-500">
                <span>{q.totalQuestions} câu hỏi</span>
                <span>{q.timeLimitMinutes ? `${q.timeLimitMinutes} phút` : "Không giới hạn thời gian"}</span>
                <span>Đạt từ {Number(q.passScore)}/100 điểm</span>
                {q.maxAttempts > 0 && <span>Tối đa {q.maxAttempts} lượt</span>}
              </span>
            </span>
          </Link>
        </li>
      ))}
    </ul>
  );
}

/** Bài kiểm tra của khóa; học viên chỉ được thấy bài đã xuất bản (API trả cả bản nháp). */
export function CourseQuizzes({ quizzes, error }: { quizzes: Quiz[] | null; error: string | null }) {
  return (
    <section>
      <h2 className="mb-3 text-lg font-semibold text-slate-900">Bài kiểm tra</h2>
      {error !== null ? (
        <Alert>Không tải được bài kiểm tra: {error}</Alert>
      ) : !quizzes || quizzes.length === 0 ? (
        <p className="rounded-xl border border-dashed border-slate-300 bg-white px-4 py-6 text-center text-sm text-slate-500">
          Khóa học chưa có bài kiểm tra nào.
        </p>
      ) : (
        <QuizList quizzes={quizzes} />
      )}
    </section>
  );
}
