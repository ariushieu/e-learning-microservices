import { ArrowLeftIcon, CheckIcon, InfoIcon, LightbulbIcon, RotateCcwIcon, XIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { cache } from "react";
import { formatPoints, formatScore, QuestionTypeTag } from "@/components/quiz/labels";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { formatDate, formatDuration } from "@/lib/format";
import { gateway, gatewayOrNull } from "@/lib/server/gateway";
import { isQuestionCorrect, type QuestionResult, type QuizAttempt, type QuizDetail, type QuizResult } from "@/lib/types";
import { cn } from "@/lib/utils";

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

  const actions = (
    <div className="flex flex-wrap gap-2">
      <Button asChild size="lg">
        <Link href={`/quizzes/${result.quizId}`}>
          <RotateCcwIcon /> {inProgress ? "Làm tiếp" : "Làm lại"}
        </Link>
      </Button>
      {quiz && (
        <Button asChild size="lg" variant="outline">
          <Link href={`/learn/${quiz.courseId}`}>
            <ArrowLeftIcon /> Về khóa học
          </Link>
        </Button>
      )}
    </div>
  );

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="space-y-1">
        <p className="text-sm font-medium text-primary">Kết quả bài kiểm tra · Lần {result.attemptNo}</p>
        <h1 className="text-2xl font-semibold tracking-tight text-balance">{result.quizTitle}</h1>
      </div>

      {inProgress ? (
        <>
          <Alert>
            <InfoIcon />
            <AlertDescription>Lượt làm bài này chưa được nộp nên chưa có kết quả.</AlertDescription>
          </Alert>
          {actions}
        </>
      ) : (
        <Card>
          <CardContent className="space-y-6">
            <div className="flex flex-wrap items-center gap-x-8 gap-y-4">
              <div>
                <p
                  className={cn(
                    "text-5xl font-semibold tracking-tight tabular-nums",
                    result.passed ? "text-emerald-600" : "text-red-600",
                  )}
                >
                  {formatScore(result.score)}
                </p>
                <div className="mt-2 flex items-center gap-2">
                  <Badge
                    variant="outline"
                    className={cn(
                      "border-0 ring-1 ring-inset",
                      result.passed
                        ? "bg-emerald-50 text-emerald-700 ring-emerald-600/20"
                        : "bg-red-50 text-red-700 ring-red-600/20",
                    )}
                  >
                    {result.passed ? <CheckIcon /> : <XIcon />}
                    {result.passed ? "Đạt" : "Chưa đạt"}
                  </Badge>
                  <span className="text-sm text-muted-foreground">
                    {result.passed ? "Chúc mừng, bạn đã vượt qua bài kiểm tra." : "Ôn lại kiến thức và thử lại nhé."}
                  </span>
                </div>
                {status === "EXPIRED" && (
                  <p className="mt-2 text-sm text-red-600">Lượt làm đã hết giờ trước khi nộp nên không được chấm.</p>
                )}
              </div>
            </div>

            <Separator />

            <dl className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-4">
              <Info label="Điểm đạt" value={formatScore(result.passScore)} />
              <Info
                label="Câu đúng"
                value={`${correctCount}/${questions.length}`}
                hint={`${formatPoints(earned)}/${formatPoints(total)} điểm`}
              />
              <Info label="Thời gian làm" value={seconds === null ? "—" : formatDuration(seconds)} />
              <Info label="Nộp lúc" value={formatDate(result.submittedAt) || "—"} />
            </dl>

            {actions}
          </CardContent>
        </Card>
      )}

      {!inProgress && (
        <section className="space-y-4">
          <h2 className="text-lg font-semibold">Chi tiết từng câu</h2>
          {questions.map((q, i) => (
            <QuestionResultCard key={q.questionId} q={q} index={i} />
          ))}
        </section>
      )}
    </div>
  );
}

function Info({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div>
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="mt-0.5 font-semibold tabular-nums">{value}</dd>
      {hint && <dd className="text-xs text-muted-foreground tabular-nums">{hint}</dd>}
    </div>
  );
}

function QuestionResultCard({ q, index }: { q: QuestionResult; index: number }) {
  const correct = isQuestionCorrect(q);
  const selected = new Set((q.selectedOptionIds ?? []).map(Number));
  const right = new Set((q.correctOptionIds ?? []).map(Number));
  const skipped = selected.size === 0;

  return (
    <Card>
      <CardHeader className="gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-sm font-semibold">Câu {index + 1}</span>
          <QuestionTypeTag type={q.type} />
          <Badge
            variant="outline"
            className={cn(
              "border-0 ring-1 ring-inset",
              correct
                ? "bg-emerald-50 text-emerald-700 ring-emerald-600/20"
                : skipped
                  ? "bg-muted text-muted-foreground ring-border"
                  : "bg-red-50 text-red-700 ring-red-600/20",
            )}
          >
            {correct ? <CheckIcon /> : <XIcon />}
            {correct ? "Đúng" : skipped ? "Bỏ trống" : "Sai"}
          </Badge>
          <span className="ml-auto text-xs text-muted-foreground tabular-nums">
            {formatPoints(q.earnedScore)}/{formatPoints(q.questionScore)} điểm
          </span>
        </div>
        <p className="text-base whitespace-pre-line">{q.content}</p>
      </CardHeader>
      <CardContent className="space-y-4">
        <ul className="space-y-2">
          {q.options.map((o) => {
            const isChosen = selected.has(Number(o.id));
            const isRight = right.has(Number(o.id));
            return (
              <li
                key={o.id}
                className={cn(
                  "flex items-start gap-3 rounded-lg border px-4 py-3 text-sm",
                  isRight
                    ? "border-emerald-200 bg-emerald-50/70 dark:border-emerald-900 dark:bg-emerald-950/30"
                    : isChosen
                      ? "border-red-200 bg-red-50/70 dark:border-red-900 dark:bg-red-950/30"
                      : "",
                )}
              >
                <span
                  className={cn(
                    "mt-0.5 flex size-4 shrink-0 items-center justify-center rounded-full",
                    isRight
                      ? "bg-emerald-600 text-white"
                      : isChosen
                        ? "bg-red-600 text-white"
                        : "border",
                  )}
                  aria-hidden
                >
                  {isRight ? <CheckIcon className="size-3" /> : isChosen ? <XIcon className="size-3" /> : null}
                </span>
                <span className="flex-1 whitespace-pre-line">{o.content}</span>
                <span className="flex shrink-0 flex-wrap justify-end gap-1">
                  {isChosen && <Badge variant="secondary">Bạn chọn</Badge>}
                  {isRight && (
                    <Badge variant="outline" className="border-0 bg-emerald-600 text-white">
                      Đáp án đúng
                    </Badge>
                  )}
                </span>
              </li>
            );
          })}
        </ul>

        {q.explanation && (
          <div className="flex gap-2.5 rounded-lg bg-muted/60 px-4 py-3 text-sm">
            <LightbulbIcon className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
            <p>
              <span className="font-medium">Giải thích: </span>
              <span className="whitespace-pre-line text-muted-foreground">{q.explanation}</span>
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
