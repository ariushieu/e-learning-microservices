"use client";

import { CircleAlertIcon, ClockIcon, InfoIcon, Loader2Icon, PlayIcon, SendIcon } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useEffectEvent, useRef, useState, useTransition, type ReactNode } from "react";
import { Callout } from "@/components/common/callout";
import { FullBleed } from "@/components/common/decor";
import { ErrorAlert } from "@/components/common/error-alert";
import { formatClock, formatPoints, QuestionTypeTag } from "@/components/quiz/labels";
import { DetailPage } from "@/components/templates/detail-page";
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
 * Trang giới thiệu (hero, tóm tắt, lịch sử do server render, truyền vào qua props) và màn hình làm bài.
 * Bấm "Bắt đầu" mới gọi API tạo lượt làm, rồi đổi sang màn làm bài ngay trên trang này.
 */
export function QuizRunner({
  quiz,
  enrollmentAccess,
  canStart,
  resuming,
  hero,
  summary,
  children,
}: {
  quiz: QuizDetail;
  enrollmentAccess: "allowed" | "required" | "unavailable";
  canStart: boolean;
  resuming: boolean;
  /** DetailHero của bài: tiêu đề, mô tả, số liệu. */
  hero: ReactNode;
  /** Số lượt đã dùng, quy định: hiện dưới nút bắt đầu ở cột phụ. */
  summary: ReactNode;
  /** Lịch sử các lần làm: cột nội dung chính. */
  children: ReactNode;
}) {
  const [session, setSession] = useState<{ attempt: QuizAttempt; deadline: number | null } | null>(null);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [enrollmentDenied, setEnrollmentDenied] = useState(false);
  const router = useRouter();
  const [refreshing, refresh] = useTransition();
  const needsEnrollment = enrollmentAccess === "required" || enrollmentDenied;

  async function start() {
    if (starting || needsEnrollment || enrollmentAccess !== "allowed" || !canStart || quiz.questions.length === 0) return;
    setStarting(true);
    setError(null);
    try {
      const attempt = await api<QuizAttempt>(`/api/quizzes/${quiz.id}/attempts`, { method: "POST" });
      // Mốc hết giờ tính theo đồng hồ máy người học, từ số giây còn lại server trả về.
      const deadline = attempt.remainingSeconds != null ? Date.now() + attempt.remainingSeconds * 1000 : null;
      setSession({ attempt, deadline });
      window.scrollTo({ top: 0 });
    } catch (e) {
      if (e instanceof ApiError && e.status === 403) {
        setEnrollmentDenied(true);
      } else {
        setError(e instanceof ApiError && [502, 503, 504].includes(e.status)
          ? "Không kiểm tra được ghi danh, thử lại sau"
          : errorMessage(e));
      }
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

  const aside = (
    <Card>
      <CardContent className="space-y-4">
        {needsEnrollment ? (
          <div aria-live="polite">
            <Callout icon={InfoIcon} tone="info" title="Bạn cần ghi danh khóa học để làm bài kiểm tra">
              <Button asChild size="lg" className="mt-3">
                <Link href={`/courses/${quiz.courseId}`}>Ghi danh</Link>
              </Button>
            </Callout>
          </div>
        ) : enrollmentAccess === "unavailable" ? (
          <>
            <ErrorAlert message="Không kiểm tra được ghi danh, thử lại sau" />
            <Button variant="outline" disabled={refreshing} onClick={() => refresh(() => router.refresh())}>
              {refreshing && <Loader2Icon className="animate-spin" />} Thử lại
            </Button>
          </>
        ) : (
          <>
            <Button size="lg" onClick={start} disabled={starting || !canStart || noQuestions} className="w-full">
              {starting ? <Loader2Icon className="animate-spin" /> : <PlayIcon />}
              {resuming ? "Làm tiếp" : "Bắt đầu làm bài"}
            </Button>
            <p className="flex items-start gap-2 text-sm text-muted-foreground">
              <InfoIcon className="mt-0.5 size-4 shrink-0" aria-hidden />
              {hint}
            </p>
          </>
        )}
        {error && <ErrorAlert message={error} />}
        <div className="space-y-4 border-t pt-4">{summary}</div>
      </CardContent>
    </Card>
  );

  return (
    <DetailPage hero={hero} aside={aside}>
      {children}
    </DetailPage>
  );
}

function QuizTaking({ quiz, attempt, deadline }: { quiz: QuizDetail; attempt: QuizAttempt; deadline: number | null }) {
  const router = useRouter();
  const [answers, setAnswers] = useState<Record<number, number[]>>({});
  const [secondsLeft, setSecondsLeft] = useState<number | null>(attempt.remainingSeconds ?? null);
  const [submitting, setSubmitting] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<{ message: string; closed: boolean } | null>(null);
  const [current, setCurrent] = useState<number | null>(quiz.questions[0]?.id ?? null);
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

  // Câu đang đọc = câu đầu tiên nằm trong dải phía trên màn hình (ngay dưới header và thanh làm bài).
  useEffect(() => {
    const visible = new Map<string, boolean>();
    const observer = new IntersectionObserver(
      (entries) => {
        for (const e of entries) visible.set(e.target.id, e.isIntersecting);
        const first = questions.find((q) => visible.get(`question-${q.id}`));
        if (first) setCurrent(first.id);
      },
      { rootMargin: "-140px 0px -50% 0px" },
    );
    for (const q of questions) {
      const el = document.getElementById(`question-${q.id}`);
      if (el) observer.observe(el);
    }
    return () => observer.disconnect();
  }, [questions]);

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
    <>
      {/* Thanh làm bài trải hết chiều ngang, dính ngay dưới header (h-16); nội dung bên trong thẳng cột nội dung. */}
      <FullBleed
        className="sticky top-16 z-30 -mt-10 border-b bg-card/90 backdrop-blur-md supports-[backdrop-filter]:bg-card/80"
        inner="flex items-center gap-3 py-3 sm:gap-4"
      >
        <div className="min-w-0 flex-1">
          <p className="truncate font-semibold">{quiz.title}</p>
          <p className="truncate text-caption text-muted-foreground">
            Lượt làm thứ {attempt.attemptNo} · Đã trả lời{" "}
            <span className="font-medium text-foreground tabular-nums">
              {answeredCount}/{questions.length}
            </span>{" "}
            câu
          </p>
        </div>
        <div
          role="timer"
          aria-label="Thời gian còn lại"
          title="Thời gian còn lại"
          className={cn(
            "flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1 text-sm font-semibold tabular-nums transition-colors",
            urgent ? "bg-destructive-soft text-destructive-strong" : "bg-muted text-foreground",
          )}
        >
          <ClockIcon className="size-4" aria-hidden />
          {secondsLeft === null ? <span className="font-medium">Không giới hạn</span> : formatClock(secondsLeft)}
        </div>
        {error?.closed ? (
          <Button asChild variant="outline" className="shrink-0">
            <Link href={`/attempts/${attempt.id}`}>Xem kết quả lượt này</Link>
          </Button>
        ) : (
          <Button onClick={requestSubmit} disabled={submitting || (locked && !timeUp)} className="shrink-0">
            {submitting ? <Loader2Icon className="animate-spin" /> : <SendIcon />}
            Nộp bài
          </Button>
        )}
      </FullBleed>

      <div className="mt-8 space-y-6">
        {timeUp && !error && (
          <Alert>
            <Loader2Icon className="animate-spin" />
            <AlertDescription>Đã hết giờ, hệ thống đang tự nộp bài...</AlertDescription>
          </Alert>
        )}
        {error && <ErrorAlert message={error.message} />}

        <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_16rem] lg:gap-8">
          <div className="min-w-0 space-y-4">
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
                <div className="flex items-baseline justify-between gap-2">
                  <p className="text-subheading">Danh sách câu hỏi</p>
                  <span className="text-caption text-muted-foreground tabular-nums">
                    {answeredCount}/{questions.length}
                  </span>
                </div>
                <ol className="grid grid-cols-6 gap-1.5 sm:grid-cols-10 lg:grid-cols-5">
                  {questions.map((q, i) => {
                    const answered = (answers[q.id]?.length ?? 0) > 0;
                    const isCurrent = current === q.id;
                    return (
                      <li key={q.id}>
                        <a
                          href={`#question-${q.id}`}
                          title={answered ? "Đã trả lời" : "Chưa trả lời"}
                          aria-current={isCurrent ? "location" : undefined}
                          onClick={() => setCurrent(q.id)}
                          className={cn(
                            "flex h-9 items-center justify-center rounded-md border text-sm font-medium tabular-nums transition-[color,background-color,box-shadow] outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
                            answered
                              ? "border-primary bg-primary text-primary-foreground hover:bg-primary-strong"
                              : "bg-card text-muted-foreground hover:text-foreground hover:ring-2 hover:ring-primary/30",
                            isCurrent && "ring-2 ring-primary ring-offset-2 ring-offset-card hover:ring-primary",
                          )}
                        >
                          {i + 1}
                        </a>
                      </li>
                    );
                  })}
                </ol>
                <div className="flex flex-wrap gap-x-4 gap-y-1.5 text-caption text-muted-foreground">
                  <span className="flex items-center gap-1.5">
                    <span className="size-3 rounded-sm bg-primary" aria-hidden /> Đã trả lời
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span className="size-3 rounded-sm border bg-card" aria-hidden /> Chưa trả lời
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span className="size-3 rounded-sm ring-2 ring-primary ring-offset-1 ring-offset-card" aria-hidden /> Đang xem
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
    </>
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
    // Cả dòng là <label> nên bấm vào đâu trên dòng cũng chọn được phương án.
    return (
      <label
        key={o.id}
        className={cn(
          "flex min-h-12 cursor-pointer items-start gap-3 rounded-lg border bg-card px-4 py-3 text-sm transition-colors",
          checked ? "border-primary bg-primary-soft" : "hover:border-primary/40 hover:bg-muted/50",
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
        <span id={textId} className={cn("leading-relaxed whitespace-pre-line", checked && "font-medium text-primary-strong")}>
          {o.content}
        </span>
      </label>
    );
  });

  return (
    <Card id={`question-${q.id}`} className="scroll-mt-36">
      <CardHeader className="gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-subheading">Câu {index + 1}</span>
          <QuestionTypeTag type={q.type} />
          <span className="ml-auto text-caption text-muted-foreground tabular-nums">{formatPoints(q.score)} điểm</span>
        </div>
        <p className="text-base leading-relaxed whitespace-pre-line">{q.content}</p>
        {multiple && (
          <p className="flex items-center gap-1.5 text-caption text-muted-foreground">
            <CircleAlertIcon className="size-3.5" aria-hidden /> Chọn tất cả các đáp án đúng.
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
