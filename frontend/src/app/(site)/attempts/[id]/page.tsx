import { ArrowLeftIcon, CalendarIcon, CheckIcon, ClockIcon, FileQuestionIcon, InfoIcon, LightbulbIcon, RotateCcwIcon, TargetIcon, XIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { cache } from "react";
import { Callout } from "@/components/common/callout";
import { EmptyState } from "@/components/common/empty-state";
import { Fact, FactList } from "@/components/common/fact-list";
import { Section } from "@/components/common/section";
import { StatusBadge, TONE_CLASSES, type Tone } from "@/components/common/status-badge";
import { formatPoints, formatScore, QuestionTypeTag } from "@/components/quiz/labels";
import { DetailHero, DetailPage, HeroMeta } from "@/components/templates/detail-page";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
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
  const duration = seconds === null ? "—" : formatDuration(seconds);

  const actions = (
    <div className="flex flex-col gap-2">
      <Button asChild size="lg" className="w-full">
        <Link href={`/quizzes/${result.quizId}`}>
          <RotateCcwIcon /> {inProgress ? "Làm tiếp" : "Làm lại"}
        </Link>
      </Button>
      {quiz && (
        <Button asChild size="lg" variant="outline" className="w-full">
          <Link href={`/learn/${quiz.courseId}`}>
            <ArrowLeftIcon /> Về khóa học
          </Link>
        </Button>
      )}
    </div>
  );

  const hero = (
    <DetailHero
      crumbs={[
        { href: "/my-courses", label: "Khóa học của tôi" },
        { href: `/quizzes/${result.quizId}`, label: result.quizTitle },
        { label: `Kết quả lần ${result.attemptNo}` },
      ]}
      eyebrow={`Kết quả · Lần ${result.attemptNo}`}
      title={result.quizTitle}
      meta={
        <>
          {!inProgress && <HeroMeta icon={<CalendarIcon />}>Nộp lúc {formatDate(result.submittedAt) || "—"}</HeroMeta>}
          {!inProgress && <HeroMeta icon={<ClockIcon />}>Làm trong {duration}</HeroMeta>}
          <HeroMeta icon={<TargetIcon />}>Điểm đạt {formatScore(result.passScore)}</HeroMeta>
        </>
      }
    />
  );

  if (inProgress) {
    return (
      <DetailPage hero={hero}>
        <Callout icon={InfoIcon} tone="info" title="Lượt làm bài này chưa được nộp nên chưa có kết quả." action={actions} />
      </DetailPage>
    );
  }

  const scoreCard = (
    <Card>
      <CardContent className="space-y-5">
        <div className="space-y-2 text-center">
          <p className="text-eyebrow text-muted-foreground">Điểm của bạn</p>
          {/* Không qua cn(): tailwind-merge coi text-display là màu chữ và bỏ mất nó. */}
          <p className={`text-display tabular-nums ${result.passed ? "text-achievement-strong" : "text-destructive-strong"}`}>
            {formatScore(result.score)}
          </p>
          <StatusBadge status={result.passed ? "PASSED" : "FAILED"} />
          <p className="text-sm text-muted-foreground">
            {result.passed ? "Chúc mừng, bạn đã vượt qua bài kiểm tra." : "Ôn lại kiến thức và thử lại nhé."}
          </p>
          {status === "EXPIRED" && (
            <p className="rounded-lg bg-destructive-soft px-3 py-2 text-sm text-destructive-strong">
              Lượt làm đã hết giờ trước khi nộp nên không được chấm.
            </p>
          )}
        </div>
        <FactList layout="grid" className="border-t pt-5">
          <Fact label="Điểm" value={`${formatPoints(earned)}/${formatPoints(total)}`} hint={`Đạt từ ${formatScore(result.passScore)}`} />
          <Fact label="Đúng" value={`${correctCount}/${questions.length} câu`} />
          <Fact label="Thời gian" value={duration} />
          <Fact label="Lượt" value={`Lần ${result.attemptNo}`} hint={formatDate(result.submittedAt) || undefined} />
        </FactList>
        <div className="border-t pt-5">{actions}</div>
      </CardContent>
    </Card>
  );

  const wrongCount = questions.filter((q) => !isQuestionCorrect(q) && (q.selectedOptionIds?.length ?? 0) > 0).length;
  const skippedCount = questions.length - correctCount - wrongCount;

  return (
    <DetailPage hero={hero} aside={scoreCard}>
      <Section
        title="Chi tiết từng câu"
        count={questions.length}
        description={`${correctCount} đúng · ${wrongCount} sai · ${skippedCount} bỏ trống`}
      >
        {questions.length === 0 ? (
          <EmptyState icon={FileQuestionIcon} title="Không có câu hỏi để xem lại" />
        ) : (
          <div className="space-y-4">
            {questions.map((q, i) => (
              <QuestionResultCard key={q.questionId} q={q} index={i} />
            ))}
          </div>
        )}
      </Section>
    </DetailPage>
  );
}

function QuestionResultCard({ q, index }: { q: QuestionResult; index: number }) {
  const correct = isQuestionCorrect(q);
  const selected = new Set((q.selectedOptionIds ?? []).map(Number));
  const right = new Set((q.correctOptionIds ?? []).map(Number));
  const skipped = selected.size === 0;
  const tone: Tone = correct ? "success" : skipped ? "neutral" : "danger";

  return (
    <Card>
      <CardHeader className="gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-subheading">Câu {index + 1}</span>
          <QuestionTypeTag type={q.type} />
          <Badge variant="outline" className={cn("border-0", TONE_CLASSES[tone].soft)}>
            {correct ? <CheckIcon /> : <XIcon />}
            {correct ? "Đúng" : skipped ? "Bỏ trống" : "Sai"}
          </Badge>
          <span className="ml-auto text-caption text-muted-foreground tabular-nums">
            {formatPoints(q.earnedScore)}/{formatPoints(q.questionScore)} điểm
          </span>
        </div>
        <p className="text-base leading-relaxed whitespace-pre-line">{q.content}</p>
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
                    ? "border-success/30 bg-success-soft text-success-strong"
                    : isChosen
                      ? "border-destructive/30 bg-destructive-soft text-destructive-strong"
                      : "bg-card",
                )}
              >
                <span
                  className={cn(
                    "mt-0.5 flex size-4 shrink-0 items-center justify-center rounded-full",
                    isRight ? "bg-success text-white" : isChosen ? "bg-destructive text-white" : "border",
                  )}
                  aria-hidden
                >
                  {isRight ? <CheckIcon className="size-3" /> : isChosen ? <XIcon className="size-3" /> : null}
                </span>
                <span className="flex-1 leading-relaxed whitespace-pre-line">{o.content}</span>
                <span className="flex shrink-0 flex-wrap justify-end gap-1">
                  {isChosen && (
                    <Badge variant="outline" className="border-current/20 bg-card text-inherit">
                      Bạn chọn
                    </Badge>
                  )}
                  {isRight && <Badge className="bg-success text-white">Đáp án đúng</Badge>}
                </span>
              </li>
            );
          })}
        </ul>

        {q.explanation && (
          <div className="flex gap-2.5 rounded-lg bg-muted px-4 py-3 text-sm">
            <LightbulbIcon className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden />
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
