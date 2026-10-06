"use client";

import { useRouter } from "next/navigation";
import { useEffect, useEffectEvent, useRef, useState, type ReactNode } from "react";
import { formatClock, formatPoints, QuestionTypeTag } from "@/components/quiz/labels";
import { Alert, Button, Card, LinkButton } from "@/components/ui";
import { api } from "@/lib/client";
import { ApiError, errorMessage } from "@/lib/errors";
import type { Question, QuizAttempt, QuizDetail, QuizResult } from "@/lib/types";

/**
 * Trang giới thiệu (do server render, truyền vào qua children) và màn hình làm bài.
 * Bấm "Bắt đầu" mới gọi API tạo lượt làm, rồi đổi sang màn làm bài ngay trên trang này.
 */
export function QuizRunner({
  quiz,
  canStart,
  resuming,
  children,
}: {
  quiz: QuizDetail;
  canStart: boolean;
  resuming: boolean;
  children: ReactNode;
}) {
  const [session, setSession] = useState<{ attempt: QuizAttempt; deadline: number | null } | null>(null);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function start() {
    setStarting(true);
    setError(null);
    try {
      const attempt = await api<QuizAttempt>(`/api/quizzes/${quiz.id}/attempts`, { method: "POST" });
      // Mốc hết giờ tính theo đồng hồ máy người học, từ số giây còn lại server trả về.
      const deadline = attempt.remainingSeconds != null ? Date.now() + attempt.remainingSeconds * 1000 : null;
      setSession({ attempt, deadline });
      window.scrollTo({ top: 0 });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setStarting(false);
    }
  }

  if (session) {
    return <QuizTaking quiz={quiz} attempt={session.attempt} deadline={session.deadline} />;
  }

  const noQuestions = quiz.questions.length === 0;
  return (
    <div className="space-y-6">
      {children}
      <Card className="flex flex-wrap items-center justify-between gap-4">
        <div className="text-sm text-slate-600">
          {noQuestions
            ? "Bài kiểm tra chưa có câu hỏi nào."
            : !canStart
              ? "Bạn đã dùng hết số lần làm bài cho phép."
              : resuming
                ? "Bạn có một lượt làm bài chưa nộp, có thể làm tiếp."
                : quiz.timeLimitMinutes
                  ? `Đồng hồ bắt đầu đếm ngay khi bạn bấm bắt đầu (${quiz.timeLimitMinutes} phút).`
                  : "Bài không giới hạn thời gian, nhớ bấm nộp bài khi làm xong."}
        </div>
        <Button onClick={start} loading={starting} disabled={!canStart || noQuestions}>
          {resuming ? "Làm tiếp" : "Bắt đầu làm bài"}
        </Button>
      </Card>
      {error && <Alert>{error}</Alert>}
    </div>
  );
}

function QuizTaking({ quiz, attempt, deadline }: { quiz: QuizDetail; attempt: QuizAttempt; deadline: number | null }) {
  const router = useRouter();
  const [answers, setAnswers] = useState<Record<number, number[]>>({});
  const [secondsLeft, setSecondsLeft] = useState<number | null>(attempt.remainingSeconds ?? null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<{ message: string; closed: boolean } | null>(null);
  const submittingRef = useRef(false);

  const questions = quiz.questions;
  const answeredCount = questions.filter((q) => (answers[q.id]?.length ?? 0) > 0).length;
  const timeUp = secondsLeft !== null && secondsLeft <= 0;

  async function submit(auto: boolean) {
    if (submittingRef.current) return;
    if (!auto) {
      const missing = questions.length - answeredCount;
      if (missing > 0 && !window.confirm(`Bạn còn ${missing} câu chưa trả lời. Vẫn nộp bài?`)) return;
    }
    submittingRef.current = true;
    setSubmitting(true);
    setError(null);
    try {
      await api<QuizResult>(`/api/attempts/${attempt.id}/submit`, {
        method: "POST",
        body: {
          // Gửi đủ mọi câu, câu bỏ trống là mảng rỗng.
          answers: questions.map((q) => ({ questionId: q.id, selectedOptionIds: answers[q.id] ?? [] })),
        },
      });
      router.push(`/attempts/${attempt.id}`);
    } catch (e) {
      // 422: lượt này đã nộp hoặc đã quá giờ ở phía server, không nộp lại được nữa.
      const closed = e instanceof ApiError && e.status === 422;
      setError({ message: errorMessage(e), closed });
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  const onTimeUp = useEffectEvent(() => {
    void submit(true);
  });

  useEffect(() => {
    if (deadline === null) return;
    const timer = setInterval(() => {
      const left = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
      setSecondsLeft(left);
      if (left <= 0) {
        clearInterval(timer);
        onTimeUp();
      }
    }, 1000);
    return () => clearInterval(timer);
  }, [deadline]);

  function choose(q: Question, optionId: number, checked: boolean) {
    setAnswers((prev) => {
      const current = prev[q.id] ?? [];
      const next =
        q.type === "MULTIPLE_CHOICE"
          ? checked
            ? [...current, optionId]
            : current.filter((id) => id !== optionId)
          : [optionId];
      return { ...prev, [q.id]: next };
    });
  }

  const locked = submitting || timeUp || Boolean(error?.closed);

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_280px]">
      <div className="order-2 space-y-4 lg:order-1">
        {questions.map((q, i) => {
          const selected = answers[q.id] ?? [];
          const multiple = q.type === "MULTIPLE_CHOICE";
          return (
            <Card key={q.id}>
              <div id={`question-${q.id}`} className="scroll-mt-24" />
              <div className="mb-3 flex flex-wrap items-center gap-2">
                <span className="font-semibold text-slate-900">Câu {i + 1}</span>
                <QuestionTypeTag type={q.type} />
                <span className="text-xs text-slate-500">{formatPoints(q.score)} điểm</span>
              </div>
              <p className="mb-4 whitespace-pre-line text-slate-800">{q.content}</p>
              {multiple && <p className="mb-2 text-xs text-slate-500">Chọn tất cả các đáp án đúng.</p>}
              <div className="space-y-2">
                {q.options.map((o) => {
                  const checked = selected.includes(o.id);
                  return (
                    <label
                      key={o.id}
                      className={`flex cursor-pointer items-start gap-3 rounded-lg border px-3 py-2.5 text-sm transition ${
                        checked ? "border-indigo-400 bg-indigo-50" : "border-slate-200 hover:bg-slate-50"
                      } ${locked ? "cursor-not-allowed opacity-70" : ""}`}
                    >
                      <input
                        type={multiple ? "checkbox" : "radio"}
                        name={`question-${q.id}`}
                        className="mt-0.5 h-4 w-4 accent-indigo-600"
                        checked={checked}
                        disabled={locked}
                        onChange={(e) => choose(q, o.id, e.target.checked)}
                      />
                      <span className="whitespace-pre-line text-slate-800">{o.content}</span>
                    </label>
                  );
                })}
              </div>
            </Card>
          );
        })}
      </div>

      <aside className="order-1 lg:order-2">
        <Card className="space-y-4 lg:sticky lg:top-6">
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-500">Lượt làm thứ {attempt.attemptNo}</p>
            {secondsLeft === null ? (
              <p className="mt-1 text-sm text-slate-600">Không giới hạn thời gian</p>
            ) : (
              <p
                className={`mt-1 font-mono text-3xl font-bold ${secondsLeft <= 60 ? "text-rose-600" : "text-slate-900"}`}
                aria-live="polite"
              >
                {formatClock(secondsLeft)}
              </p>
            )}
          </div>

          <div>
            <p className="mb-2 text-sm text-slate-600">
              Đã trả lời <span className="font-semibold text-slate-900">{answeredCount}</span>/{questions.length} câu
            </p>
            <div className="grid grid-cols-6 gap-1.5">
              {questions.map((q, i) => {
                const answered = (answers[q.id]?.length ?? 0) > 0;
                return (
                  <a
                    key={q.id}
                    href={`#question-${q.id}`}
                    title={answered ? "Đã trả lời" : "Chưa trả lời"}
                    className={`flex h-9 items-center justify-center rounded-md border text-sm font-medium ${
                      answered
                        ? "border-indigo-600 bg-indigo-600 text-white"
                        : "border-slate-300 bg-white text-slate-600 hover:bg-slate-50"
                    }`}
                  >
                    {i + 1}
                  </a>
                );
              })}
            </div>
            <div className="mt-2 flex gap-4 text-xs text-slate-500">
              <span className="flex items-center gap-1">
                <span className="h-3 w-3 rounded bg-indigo-600" /> Đã trả lời
              </span>
              <span className="flex items-center gap-1">
                <span className="h-3 w-3 rounded border border-slate-300" /> Chưa trả lời
              </span>
            </div>
          </div>

          {timeUp && !error && <Alert kind="info">Đã hết giờ, hệ thống đang tự nộp bài...</Alert>}
          {error && (
            <Alert>{error.message}</Alert>
          )}

          {error?.closed ? (
            <LinkButton href={`/attempts/${attempt.id}`} variant="secondary" className="w-full">
              Xem kết quả lượt này
            </LinkButton>
          ) : (
            <Button className="w-full" onClick={() => submit(false)} loading={submitting} disabled={locked && !timeUp}>
              Nộp bài
            </Button>
          )}
        </Card>
      </aside>
    </div>
  );
}
