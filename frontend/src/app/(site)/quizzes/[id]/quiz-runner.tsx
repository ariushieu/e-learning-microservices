"use client";

import { CircleAlertIcon, ClockIcon, InfoIcon, Loader2Icon, PlayIcon, SendIcon } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useEffectEvent, useRef, useState, type ReactNode } from "react";
import { ErrorAlert } from "@/components/common/error-alert";
import { formatClock, formatPoints, QuestionTypeTag } from "@/components/quiz/labels";
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
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { api } from "@/lib/client";
import { ApiError, errorMessage } from "@/lib/errors";
import type { Question, QuizAttempt, QuizDetail, QuizResult } from "@/lib/types";
import { cn } from "@/lib/utils";

/**
 * Trang giới thiệu (do server render, truyền vào qua `intro` và `children`) và màn hình làm bài.
 * Bấm "Bắt đầu" mới gọi API tạo lượt làm, rồi đổi sang màn làm bài ngay trên trang này.
 */
export function QuizRunner({
  quiz,
  canStart,
  resuming,
  intro,
  children,
}: {
  quiz: QuizDetail;
  canStart: boolean;
  resuming: boolean;
  /** Tiêu đề, mô tả, số liệu của bài: hiện phía trên nút bắt đầu. */
  intro: ReactNode;
  /** Lịch sử các lần làm: hiện phía dưới nút bắt đầu. */
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
  const hint = noQuestions
    ? "Bài kiểm tra chưa có câu hỏi nào."
    : !canStart
      ? "Bạn đã dùng hết số lần làm bài cho phép."
      : resuming
        ? "Bạn có một lượt làm bài chưa nộp, có thể làm tiếp."
        : quiz.timeLimitMinutes
          ? `Đồng hồ bắt đầu đếm ngay khi bạn bấm bắt đầu (${quiz.timeLimitMinutes} phút).`
          : "Bài không giới hạn thời gian, nhớ bấm nộp bài khi làm xong.";

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      {intro}
      <div className="flex flex-col gap-3 rounded-xl border bg-muted/30 p-4 sm:flex-row sm:items-center sm:justify-between">
        <p className="flex items-start gap-2 text-sm text-muted-foreground">
          <InfoIcon className="mt-0.5 size-4 shrink-0" />
          {hint}
        </p>
        <Button size="lg" onClick={start} disabled={starting || !canStart || noQuestions} className="shrink-0">
          {starting ? <Loader2Icon className="animate-spin" /> : <PlayIcon />}
          {resuming ? "Làm tiếp" : "Bắt đầu làm bài"}
        </Button>
      </div>
      {error && <ErrorAlert message={error} />}
      {children}
    </div>
  );
}

function QuizTaking({ quiz, attempt, deadline }: { quiz: QuizDetail; attempt: QuizAttempt; deadline: number | null }) {
  const router = useRouter();
  const [answers, setAnswers] = useState<Record<number, number[]>>({});
  const [secondsLeft, setSecondsLeft] = useState<number | null>(attempt.remainingSeconds ?? null);
  const [submitting, setSubmitting] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<{ message: string; closed: boolean } | null>(null);
  const submittingRef = useRef(false);

  const questions = quiz.questions;
  const answeredCount = questions.filter((q) => (answers[q.id]?.length ?? 0) > 0).length;
  const missing = questions.length - answeredCount;
  const timeUp = secondsLeft !== null && secondsLeft <= 0;

  async function submit() {
    if (submittingRef.current) return;
    submittingRef.current = true;
    setConfirming(false);
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

  /** Nộp bằng tay: còn câu bỏ trống thì hỏi lại trước. */
  function requestSubmit() {
    if (missing > 0) setConfirming(true);
    else void submit();
  }

  const onTimeUp = useEffectEvent(() => {
    void submit();
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
  const urgent = secondsLeft !== null && secondsLeft < 60;

  return (
    <div className="space-y-6">
      {/* Thanh trạng thái dính ngay dưới header của trang (header cao h-14). */}
      <div className="sticky top-14 z-30 -mx-4 border-b bg-background/90 px-4 py-3 backdrop-blur sm:-mx-6 sm:px-6">
        <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
          <div className="min-w-0 flex-1">
            <p className="truncate font-semibold">{quiz.title}</p>
            <p className="text-xs text-muted-foreground">
              Lượt làm thứ {attempt.attemptNo} · Đã trả lời{" "}
              <span className="font-medium text-foreground tabular-nums">
                {answeredCount}/{questions.length}
              </span>{" "}
              câu
            </p>
          </div>
          <div
            className={cn(
              "flex items-center gap-1.5 rounded-lg px-2.5 py-1 font-mono text-lg font-semibold tabular-nums",
              urgent ? "bg-red-50 text-red-600 dark:bg-red-950/40" : "bg-muted",
            )}
            aria-live="polite"
            title="Thời gian còn lại"
          >
            <ClockIcon className="size-4" />
            {secondsLeft === null ? <span className="font-sans text-sm font-medium">Không giới hạn</span> : formatClock(secondsLeft)}
          </div>
          {error?.closed ? (
            <Button asChild variant="outline">
              <Link href={`/attempts/${attempt.id}`}>Xem kết quả lượt này</Link>
            </Button>
          ) : (
            <Button onClick={requestSubmit} disabled={submitting || (locked && !timeUp)}>
              {submitting ? <Loader2Icon className="animate-spin" /> : <SendIcon />}
              Nộp bài
            </Button>
          )}
        </div>
      </div>

      {timeUp && !error && (
        <Alert>
          <Loader2Icon className="animate-spin" />
          <AlertDescription>Đã hết giờ, hệ thống đang tự nộp bài...</AlertDescription>
        </Alert>
      )}
      {error && <ErrorAlert message={error.message} />}

      <div className="grid items-start gap-6 lg:grid-cols-[1fr_15rem]">
        <div className="space-y-4">
          {questions.map((q, i) => (
            <QuestionCard
              key={q.id}
              question={q}
              index={i}
              selected={answers[q.id] ?? []}
              locked={locked}
              onChoose={(optionId, checked) => choose(q, optionId, checked)}
            />
          ))}
        </div>

        <aside className="lg:sticky lg:top-36">
          <Card size="sm">
            <CardContent className="space-y-3">
              <p className="text-sm font-medium">Danh sách câu hỏi</p>
              <div className="grid grid-cols-6 gap-1.5 lg:grid-cols-5">
                {questions.map((q, i) => {
                  const answered = (answers[q.id]?.length ?? 0) > 0;
                  return (
                    <a
                      key={q.id}
                      href={`#question-${q.id}`}
                      title={answered ? "Đã trả lời" : "Chưa trả lời"}
                      className={cn(
                        "flex h-9 items-center justify-center rounded-md border text-sm font-medium tabular-nums transition-colors",
                        answered
                          ? "border-primary bg-primary text-primary-foreground"
                          : "bg-background text-muted-foreground hover:bg-muted",
                      )}
                    >
                      {i + 1}
                    </a>
                  );
                })}
              </div>
              <div className="flex gap-4 text-xs text-muted-foreground">
                <span className="flex items-center gap-1.5">
                  <span className="size-3 rounded-sm bg-primary" /> Đã trả lời
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="size-3 rounded-sm border" /> Chưa trả lời
                </span>
              </div>
            </CardContent>
          </Card>
        </aside>
      </div>

      <AlertDialog open={confirming} onOpenChange={setConfirming}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Nộp bài khi còn câu chưa trả lời?</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn còn {missing} câu chưa trả lời. Câu bỏ trống được tính là sai và không làm lại được trong lượt này.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Làm tiếp</AlertDialogCancel>
            <AlertDialogAction onClick={() => void submit()}>Vẫn nộp bài</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

function QuestionCard({
  question: q,
  index,
  selected,
  locked,
  onChoose,
}: {
  question: Question;
  index: number;
  selected: number[];
  locked: boolean;
  onChoose: (optionId: number, checked: boolean) => void;
}) {
  const multiple = q.type === "MULTIPLE_CHOICE";

  const rows = q.options.map((o) => {
    const checked = selected.includes(o.id);
    const textId = `option-${q.id}-${o.id}`;
    return (
      <label
        key={o.id}
        className={cn(
          "flex cursor-pointer items-start gap-3 rounded-lg border px-4 py-3 text-sm transition-colors",
          checked ? "border-primary bg-primary/5" : "hover:bg-muted/50",
          locked && "cursor-not-allowed opacity-70",
        )}
      >
        {multiple ? (
          <Checkbox
            checked={checked}
            disabled={locked}
            onCheckedChange={(c) => onChoose(o.id, c === true)}
            aria-labelledby={textId}
            className="mt-0.5"
          />
        ) : (
          <RadioGroupItem value={String(o.id)} disabled={locked} aria-labelledby={textId} className="mt-0.5" />
        )}
        <span id={textId} className="whitespace-pre-line">
          {o.content}
        </span>
      </label>
    );
  });

  return (
    <Card id={`question-${q.id}`} className="scroll-mt-36">
      <CardHeader className="gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-sm font-semibold">Câu {index + 1}</span>
          <QuestionTypeTag type={q.type} />
          <span className="ml-auto text-xs text-muted-foreground tabular-nums">{formatPoints(q.score)} điểm</span>
        </div>
        <p className="text-base whitespace-pre-line">{q.content}</p>
        {multiple && (
          <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <CircleAlertIcon className="size-3.5" /> Chọn tất cả các đáp án đúng.
          </p>
        )}
      </CardHeader>
      <CardContent>
        {multiple ? (
          <div className="grid gap-2">{rows}</div>
        ) : (
          <RadioGroup
            value={selected[0] != null ? String(selected[0]) : ""}
            onValueChange={(v) => onChoose(Number(v), true)}
            disabled={locked}
            aria-label={`Câu ${index + 1}`}
          >
            {rows}
          </RadioGroup>
        )}
      </CardContent>
    </Card>
  );
}
