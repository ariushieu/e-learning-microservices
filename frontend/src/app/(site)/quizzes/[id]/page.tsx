import { ArrowLeftIcon, ClipboardListIcon, ClockIcon, HistoryIcon, LockIcon, RepeatIcon, TargetIcon } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { cache } from "react";
import { EmptyState } from "@/components/common/empty-state";
import { StatusBadge } from "@/components/common/status-badge";
import { formatScore, formatTimeLimit } from "@/components/quiz/labels";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ApiError } from "@/lib/errors";
import { formatDate } from "@/lib/format";
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
      <div className="mx-auto max-w-lg py-10">
        <EmptyState
          icon={LockIcon}
          title="Bài kiểm tra chưa mở"
          description={reason ?? "Bài kiểm tra này chưa được xuất bản hoặc không còn tồn tại."}
          action={
            <Button asChild variant="outline">
              <Link href="/my-courses">Về khóa học của tôi</Link>
            </Button>
          }
        />
      </div>
    );
  }

  const attempts = await gateway<QuizAttempt[]>(`/api/quizzes/${id}/attempts`);
  const inProgress = attempts.some((a) => a.status === "IN_PROGRESS");
  // Server đếm cả lượt đang làm và lượt hết giờ vào số lần đã dùng.
  const exhausted = !inProgress && quiz.maxAttempts > 0 && attempts.length >= quiz.maxAttempts;

  const intro = (
    <>
      <div className="space-y-3">
        <Button asChild variant="ghost" size="sm" className="-ml-2.5 text-muted-foreground">
          <Link href={`/learn/${quiz.courseId}`}>
            <ArrowLeftIcon /> Về bài học của khóa
          </Link>
        </Button>
        <div className="space-y-2">
          <p className="text-sm font-medium text-primary">Bài kiểm tra</p>
          <h1 className="text-2xl font-semibold tracking-tight text-balance">{quiz.title}</h1>
          {quiz.description && (
            <p className="whitespace-pre-line text-muted-foreground">{quiz.description}</p>
          )}
        </div>
      </div>

      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatTile icon={ClipboardListIcon} label="Câu hỏi" value={String(quiz.questions.length)} />
        <StatTile icon={ClockIcon} label="Thời gian" value={formatTimeLimit(quiz.timeLimitMinutes)} />
        <StatTile icon={TargetIcon} label="Điểm đạt" value={formatScore(quiz.passScore)} />
        <StatTile
          icon={RepeatIcon}
          label="Lượt làm"
          value={quiz.maxAttempts > 0 ? `${attempts.length}/${quiz.maxAttempts}` : `${attempts.length} / không giới hạn`}
        />
      </dl>
    </>
  );

  return (
    <QuizRunner quiz={quiz} canStart={!exhausted} resuming={inProgress} intro={intro}>
      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold">Các lần làm trước</CardTitle>
        </CardHeader>
        <CardContent>
          {attempts.length === 0 ? (
            <EmptyState icon={HistoryIcon} title="Chưa có lần làm nào" description="Kết quả các lần làm bài sẽ hiện ở đây." />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Lần</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead>Điểm</TableHead>
                  <TableHead>Kết quả</TableHead>
                  <TableHead>Nộp lúc</TableHead>
                  <TableHead className="text-right" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {attempts.map((a) => (
                  <TableRow key={a.id}>
                    <TableCell className="font-medium tabular-nums">#{a.attemptNo}</TableCell>
                    <TableCell>
                      <StatusBadge status={a.status} />
                    </TableCell>
                    <TableCell className="tabular-nums">
                      {a.status === "IN_PROGRESS" ? "—" : formatScore(a.score)}
                    </TableCell>
                    <TableCell>
                      {a.status === "IN_PROGRESS" ? (
                        <span className="text-muted-foreground">—</span>
                      ) : a.passed ? (
                        <span className="font-medium text-emerald-600">Đạt</span>
                      ) : (
                        <span className="font-medium text-red-600">Chưa đạt</span>
                      )}
                    </TableCell>
                    <TableCell className="text-muted-foreground tabular-nums">
                      {formatDate(a.submittedAt) || "—"}
                    </TableCell>
                    <TableCell className="text-right">
                      {a.status !== "IN_PROGRESS" && (
                        <Button asChild variant="link" size="sm" className="px-0">
                          <Link href={`/attempts/${a.id}`}>Xem kết quả</Link>
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </QuizRunner>
  );
}

function StatTile({ icon: Icon, label, value }: { icon: LucideIcon; label: string; value: string }) {
  return (
    <div className="rounded-xl border bg-card px-4 py-3">
      <dt className="flex items-center gap-1.5 text-xs text-muted-foreground">
        <Icon className="size-3.5" />
        {label}
      </dt>
      <dd className="mt-1 font-semibold tabular-nums">{value}</dd>
    </div>
  );
}
