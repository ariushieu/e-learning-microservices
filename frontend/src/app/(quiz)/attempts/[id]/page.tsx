import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { cache } from "react";
import { formatPoints, formatScore, QuestionTypeTag } from "@/components/quiz/labels";
import { Alert, Card, formatDate, formatDuration, LinkButton } from "@/components/ui";
import { gateway, gatewayOrNull } from "@/lib/server/gateway";
import { isQuestionCorrect, type QuestionResult, type QuizAttempt, type QuizDetail, type QuizResult } from "@/lib/types";

const loadResult = cache((id: string) => gatewayOrNull<QuizResult>(`/api/attempts/${id}`));

export async function generateMetadata({ params }: PageProps<"/attempts/[id]">): Promise<Metadata> {
  const { id } = await params;
  const result = /^\d+$/.test(id) ? await loadResult(id) : null;
  return { title: result ? `Kết quả: ${result.quizTitle}` : "Kết quả bài kiểm tra" };
}

export default async function AttemptResultPage({ params }: PageProps<"/attempts/[id]">) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) notFound();
  const result = await loadResult(id);
  if (!result) notFound();

  // QuizResult không có courseId và trạng thái lượt làm; lấy thêm, lỗi thì chỉ ẩn phần liên quan.
  const [quiz, attempts] = await Promise.all([
    gateway<QuizDetail>(`/api/quizzes/${result.quizId}/take`).catch(() => null),
    gateway<QuizAttempt[]>(`/api/quizzes/${result.quizId}/attempts`).catch(() => null),
  ]);
  const status = attempts?.find((a) => a.id === result.attemptId)?.status;
  const inProgress = status === "IN_PROGRESS" || !result.submittedAt;

  const questions = result.questionResults;
  const correctCount = questions.filter(isQuestionCorrect).length;
  const earned = questions.reduce((sum, q) => sum + Number(q.earnedScore ?? 0), 0);
  const total = questions.reduce((sum, q) => sum + Number(q.questionScore ?? 0), 0);
  const seconds =
    result.submittedAt && result.startedAt
      ? Math.max(0, Math.round((new Date(result.submittedAt).getTime() - new Date(result.startedAt).getTime()) / 1000))
      : null;

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <p className="text-sm text-slate-500">Kết quả bài kiểm tra</p>
        <h1 className="mt-1 text-2xl font-bold text-slate-900">{result.quizTitle}</h1>
      </div>

      {inProgress ? (
        <Alert kind="info">Lượt làm bài này chưa được nộp nên chưa có kết quả.</Alert>
      ) : (
        <Card
          className={`border-2 ${result.passed ? "border-emerald-200 bg-emerald-50/40" : "border-rose-200 bg-rose-50/40"}`}
        >
          <div className="flex flex-wrap items-center justify-between gap-6">
            <div>
              <p className={`text-5xl font-bold ${result.passed ? "text-emerald-600" : "text-rose-600"}`}>
                {formatScore(result.score)}
              </p>
              <p className={`mt-2 text-lg font-semibold ${result.passed ? "text-emerald-700" : "text-rose-700"}`}>
                {result.passed ? "Đạt — chúc mừng bạn!" : "Chưa đạt"}
              </p>
              {status === "EXPIRED" && (
                <p className="mt-1 text-sm text-rose-600">Lượt làm đã hết giờ trước khi nộp nên không được chấm.</p>
              )}
            </div>
            <dl className="grid grid-cols-2 gap-x-8 gap-y-3 text-sm">
              <Info label="Điểm đạt" value={formatScore(result.passScore)} />
              <Info label="Lần làm" value={`#${result.attemptNo}`} />
              <Info label="Câu đúng" value={`${correctCount}/${questions.length}`} />
              <Info label="Điểm câu hỏi" value={`${formatPoints(earned)}/${formatPoints(total)}`} />
              <Info label="Thời gian làm" value={seconds === null ? "—" : formatDuration(seconds)} />
              <Info label="Nộp lúc" value={formatDate(result.submittedAt) || "—"} />
            </dl>
          </div>
        </Card>
      )}

      <div className="flex flex-wrap gap-3">
        <LinkButton href={`/quizzes/${result.quizId}`}>{inProgress ? "Làm tiếp" : "Làm lại"}</LinkButton>
        {quiz && (
          <LinkButton href={`/learn/${quiz.courseId}`} variant="secondary">
            Về khóa học
          </LinkButton>
        )}
      </div>

      {!inProgress && (
        <section className="space-y-4">
          <h2 className="text-lg font-semibold text-slate-900">Chi tiết từng câu</h2>
          {questions.map((q, i) => (
            <QuestionResultCard key={q.questionId} q={q} index={i} />
          ))}
        </section>
      )}
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-slate-500">{label}</dt>
      <dd className="font-semibold text-slate-900">{value}</dd>
    </div>
  );
}

function QuestionResultCard({ q, index }: { q: QuestionResult; index: number }) {
  const correct = isQuestionCorrect(q);
  const selected = new Set((q.selectedOptionIds ?? []).map(Number));
  const right = new Set((q.correctOptionIds ?? []).map(Number));
  const skipped = selected.size === 0;

  return (
    <Card className={`border-l-4 ${correct ? "border-l-emerald-500" : "border-l-rose-500"}`}>
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <span className="font-semibold text-slate-900">Câu {index + 1}</span>
        <QuestionTypeTag type={q.type} />
        <span
          className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${
            correct ? "bg-emerald-100 text-emerald-700" : "bg-rose-100 text-rose-700"
          }`}
        >
          {correct ? "Đúng" : skipped ? "Bỏ trống" : "Sai"}
        </span>
        <span className="ml-auto text-sm text-slate-500">
          {formatPoints(q.earnedScore)}/{formatPoints(q.questionScore)} điểm
        </span>
      </div>
      <p className="mb-4 whitespace-pre-line text-slate-800">{q.content}</p>

      <ul className="space-y-2">
        {q.options.map((o) => {
          const isChosen = selected.has(Number(o.id));
          const isRight = right.has(Number(o.id));
          const tone = isRight
            ? "border-emerald-300 bg-emerald-50"
            : isChosen
              ? "border-rose-300 bg-rose-50"
              : "border-slate-200";
          return (
            <li key={o.id} className={`flex items-start gap-3 rounded-lg border px-3 py-2.5 text-sm ${tone}`}>
              <span
                className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                  isRight ? "bg-emerald-500 text-white" : isChosen ? "bg-rose-500 text-white" : "bg-slate-200 text-slate-500"
                }`}
                aria-hidden
              >
                {isRight ? "✓" : isChosen ? "✗" : ""}
              </span>
              <span className="flex-1 whitespace-pre-line text-slate-800">{o.content}</span>
              <span className="flex shrink-0 flex-wrap gap-1">
                {isChosen && (
                  <span className="rounded bg-slate-800 px-1.5 py-0.5 text-xs text-white">Bạn chọn</span>
                )}
                {isRight && (
                  <span className="rounded bg-emerald-600 px-1.5 py-0.5 text-xs text-white">Đáp án đúng</span>
                )}
              </span>
            </li>
          );
        })}
      </ul>

      {q.explanation && (
        <div className="mt-4 rounded-lg bg-sky-50 px-4 py-3 text-sm text-sky-800">
          <span className="font-semibold">Giải thích: </span>
          <span className="whitespace-pre-line">{q.explanation}</span>
        </div>
      )}
    </Card>
  );
}
