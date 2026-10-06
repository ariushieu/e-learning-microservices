import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { cache } from "react";
import {
  AttemptStatusBadge,
  formatMaxAttempts,
  formatScore,
  formatTimeLimit,
} from "@/components/quiz/labels";
import { Card, Empty, formatDate, LinkButton } from "@/components/ui";
import { ApiError } from "@/lib/errors";
import { gateway } from "@/lib/server/gateway";
import type { QuizAttempt, QuizDetail } from "@/lib/types";
import { QuizRunner } from "./quiz-runner";

/** Đề cho học viên; null kèm lý do khi bài chưa xuất bản (422) hoặc không tồn tại (404). */
const loadQuiz = cache(async (id: string): Promise<{ quiz: QuizDetail | null; reason?: string }> => {
  try {
    return { quiz: await gateway<QuizDetail>(`/api/quizzes/${id}/take`) };
  } catch (e) {
    if (e instanceof ApiError && (e.status === 404 || e.status === 422)) return { quiz: null, reason: e.message };
    throw e;
  }
});

export async function generateMetadata({ params }: PageProps<"/quizzes/[id]">): Promise<Metadata> {
  const { id } = await params;
  if (!/^\d+$/.test(id)) return { title: "Bài kiểm tra" };
  const { quiz } = await loadQuiz(id);
  return { title: quiz?.title ?? "Bài kiểm tra" };
}

export default async function QuizPage({ params }: PageProps<"/quizzes/[id]">) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) notFound();

  const { quiz, reason } = await loadQuiz(id);
  if (!quiz) {
    return (
      <div className="mx-auto max-w-lg py-12 text-center">
        <Card>
          <h1 className="text-xl font-semibold text-slate-900">Bài kiểm tra chưa mở</h1>
          <p className="mt-2 text-slate-500">
            {reason ?? "Bài kiểm tra này chưa được xuất bản hoặc không còn tồn tại."}
          </p>
          <LinkButton href="/my-courses" variant="secondary" className="mt-6">
            Về khóa học của tôi
          </LinkButton>
        </Card>
      </div>
    );
  }

  const attempts = await gateway<QuizAttempt[]>(`/api/quizzes/${id}/attempts`);
  const inProgress = attempts.some((a) => a.status === "IN_PROGRESS");
  // Server đếm cả lượt đang làm và lượt hết giờ vào số lần đã dùng.
  const exhausted = !inProgress && quiz.maxAttempts > 0 && attempts.length >= quiz.maxAttempts;

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <Link href={`/learn/${quiz.courseId}`} className="text-sm text-indigo-600 hover:underline">
          ← Về bài học của khóa
        </Link>
        <h1 className="mt-2 text-2xl font-bold text-slate-900">{quiz.title}</h1>
      </div>

      <QuizRunner quiz={quiz} canStart={!exhausted} resuming={inProgress}>
        <Card className="space-y-5">
          {quiz.description && <p className="whitespace-pre-line text-slate-700">{quiz.description}</p>}
          <dl className="grid grid-cols-2 gap-4 sm:grid-cols-5">
            <Stat label="Số câu hỏi" value={String(quiz.questions.length)} />
            <Stat label="Thời gian" value={formatTimeLimit(quiz.timeLimitMinutes)} />
            <Stat label="Điểm đạt" value={formatScore(quiz.passScore)} />
            <Stat label="Số lần được làm" value={formatMaxAttempts(quiz.maxAttempts)} />
            <Stat
              label="Đã dùng"
              value={quiz.maxAttempts > 0 ? `${attempts.length}/${quiz.maxAttempts}` : `${attempts.length} lần`}
            />
          </dl>
        </Card>

        <section>
          <h2 className="mb-3 text-lg font-semibold text-slate-900">Các lần làm trước</h2>
          {attempts.length === 0 ? (
            <Empty>Bạn chưa làm bài kiểm tra này lần nào.</Empty>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-slate-500">
                  <tr>
                    <th className="px-4 py-3 font-medium">Lần</th>
                    <th className="px-4 py-3 font-medium">Trạng thái</th>
                    <th className="px-4 py-3 font-medium">Điểm</th>
                    <th className="px-4 py-3 font-medium">Kết quả</th>
                    <th className="px-4 py-3 font-medium">Nộp lúc</th>
                    <th className="px-4 py-3" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {attempts.map((a) => (
                    <tr key={a.id}>
                      <td className="px-4 py-3 font-medium text-slate-900">#{a.attemptNo}</td>
                      <td className="px-4 py-3">
                        <AttemptStatusBadge status={a.status} />
                      </td>
                      <td className="px-4 py-3">{a.status === "IN_PROGRESS" ? "—" : formatScore(a.score)}</td>
                      <td className="px-4 py-3">
                        {a.status === "IN_PROGRESS" ? (
                          "—"
                        ) : a.passed ? (
                          <span className="font-medium text-emerald-600">Đạt</span>
                        ) : (
                          <span className="font-medium text-rose-600">Chưa đạt</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-slate-500">{formatDate(a.submittedAt) || "—"}</td>
                      <td className="px-4 py-3 text-right">
                        {a.status !== "IN_PROGRESS" && (
                          <Link href={`/attempts/${a.id}`} className="font-medium text-indigo-600 hover:underline">
                            Xem kết quả
                          </Link>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </QuizRunner>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg bg-slate-50 px-3 py-2.5">
      <dt className="text-xs text-slate-500">{label}</dt>
      <dd className="mt-0.5 font-semibold text-slate-900">{value}</dd>
    </div>
  );
}
