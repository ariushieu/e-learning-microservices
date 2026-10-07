import { ArrowLeftIcon, CheckIcon, ClipboardListIcon, ClockIcon, HistoryIcon, LockIcon, RepeatIcon, TargetIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { cache } from "react";
import { DataTableCard } from "@/components/common/data-table-card";
import { EmptyState } from "@/components/common/empty-state";
import { Fact, FactList } from "@/components/common/fact-list";
import { Section } from "@/components/common/section";
import { StatusBadge } from "@/components/common/status-badge";
import { formatScore, formatTimeLimit } from "@/components/quiz/labels";
import { DetailHero, HeroMeta } from "@/components/templates/detail-page";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ApiError } from "@/lib/errors";
import { formatDate } from "@/lib/format";
import { gateway, gatewayOrNull, getSession } from "@/lib/server/gateway";
import type { Course, CourseProgress, QuizAttempt, QuizDetail } from "@/lib/types";
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
      <div className="mx-auto w-full max-w-lg py-10">
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

  // Tên khóa chỉ để hiện trên đường dẫn; không lấy được thì ghi chung chung, không chặn trang.
  const [attempts, course] = await Promise.all([
    gateway<QuizAttempt[]>(`/api/quizzes/${id}/attempts`),
    gateway<Course>(`/api/courses/${quiz.courseId}`).catch(() => null),
  ]);
  const inProgress = attempts.some((a) => a.status === "IN_PROGRESS");
  // Server đếm cả lượt đang làm và lượt hết giờ vào số lần đã dùng.
  const exhausted = !inProgress && quiz.maxAttempts > 0 && attempts.length >= quiz.maxAttempts;

  const session = await getSession();
  let enrollmentAccess: "allowed" | "required" | "unavailable" = "allowed";
  // Chỉ là hướng dẫn trên web; POST attempts vẫn kiểm quyền và ghi danh ở backend.
  if (!session || (session.userId !== quiz.createdBy && !session.roles.includes("ROLE_ADMIN"))) {
    try {
      const progress = await gatewayOrNull<CourseProgress>(`/api/progress?courseId=${quiz.courseId}`);
      enrollmentAccess = progress && (progress.status === "ACTIVE" || progress.status === "COMPLETED")
        ? "allowed"
        : "required";
    } catch (e) {
      if (!(e instanceof ApiError) || ![502, 503, 504].includes(e.status)) throw e;
      enrollmentAccess = "unavailable";
    }
  }

  const hero = (
    <DetailHero
      crumbs={[
        { href: "/my-courses", label: "Khóa học của tôi" },
        { href: `/learn/${quiz.courseId}`, label: course?.title ?? "Khóa học" },
        { label: quiz.title },
      ]}
      eyebrow="Bài kiểm tra"
      title={quiz.title}
      description={quiz.description ? <span className="whitespace-pre-line">{quiz.description}</span> : undefined}
      meta={
        <>
          <HeroMeta icon={<ClipboardListIcon />}>{quiz.questions.length} câu hỏi</HeroMeta>
          <HeroMeta icon={<ClockIcon />}>{quiz.timeLimitMinutes ? `${quiz.timeLimitMinutes} phút` : "Không giới hạn thời gian"}</HeroMeta>
          <HeroMeta icon={<TargetIcon />}>Điểm đạt {formatScore(quiz.passScore)}</HeroMeta>
          <HeroMeta icon={<RepeatIcon />}>{quiz.maxAttempts > 0 ? `Tối đa ${quiz.maxAttempts} lượt` : "Không giới hạn lượt"}</HeroMeta>
        </>
      }
      actions={
        <Button asChild variant="secondary">
          <Link href={`/learn/${quiz.courseId}`}>
            <ArrowLeftIcon /> Về bài học của khóa
          </Link>
        </Button>
      }
    />
  );

  const rules = [
    quiz.timeLimitMinutes
      ? `Có ${quiz.timeLimitMinutes} phút; hết giờ hệ thống tự nộp bài.`
      : "Không giới hạn thời gian, nhớ bấm nộp bài khi làm xong.",
    "Câu bỏ trống được tính là sai.",
    `Đạt khi điểm từ ${formatScore(quiz.passScore)} trở lên.`,
    quiz.maxAttempts > 0 ? `Được làm tối đa ${quiz.maxAttempts} lần.` : "Được làm lại không giới hạn số lần.",
  ];

  const summary = (
    <>
      <FactList>
        <Fact label="Lượt làm" value={quiz.maxAttempts > 0 ? `${attempts.length}/${quiz.maxAttempts}` : `${attempts.length} / không giới hạn`} />
        <Fact label="Thời gian" value={formatTimeLimit(quiz.timeLimitMinutes)} />
        <Fact label="Điểm đạt" value={formatScore(quiz.passScore)} />
      </FactList>
      <div className="space-y-2.5 border-t pt-4">
        <p className="text-subheading">Quy định</p>
        <ul className="space-y-2 text-sm text-muted-foreground">
          {rules.map((r) => (
            <li key={r} className="flex gap-2">
              <CheckIcon className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden />
              {r}
            </li>
          ))}
        </ul>
      </div>
    </>
  );

  return (
    <QuizRunner key={`${quiz.id}:${enrollmentAccess}`} quiz={quiz} enrollmentAccess={enrollmentAccess} canStart={!exhausted} resuming={inProgress} hero={hero} summary={summary}>
      <Section title="Các lần làm trước" count={attempts.length}>
        <DataTableCard
          isEmpty={attempts.length === 0}
          empty={<EmptyState icon={HistoryIcon} title="Chưa có lần làm nào" description="Kết quả các lần làm bài sẽ hiện ở đây." />}
        >
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Lần</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead>Điểm</TableHead>
                <TableHead>Kết quả</TableHead>
                <TableHead className="hidden sm:table-cell">Nộp lúc</TableHead>
                <TableHead className="text-right">
                  <span className="sr-only">Thao tác</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {attempts.map((a) => (
                <TableRow key={a.id}>
                  <TableCell className="font-medium tabular-nums">#{a.attemptNo}</TableCell>
                  <TableCell>
                    <StatusBadge status={a.status} />
                  </TableCell>
                  <TableCell className="tabular-nums">{a.status === "IN_PROGRESS" ? "—" : formatScore(a.score)}</TableCell>
                  <TableCell>
                    {a.status === "IN_PROGRESS" ? (
                      <span className="text-muted-foreground">—</span>
                    ) : (
                      <StatusBadge status={a.passed ? "PASSED" : "FAILED"} />
                    )}
                  </TableCell>
                  <TableCell className="hidden text-muted-foreground tabular-nums sm:table-cell">{formatDate(a.submittedAt) || "—"}</TableCell>
                  <TableCell className="text-right">
                    {a.status === "SUBMITTED" && (
                      <Button asChild variant="link" size="sm" className="px-0">
                        <Link href={`/attempts/${a.id}`}>Xem kết quả</Link>
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </DataTableCard>
      </Section>
    </QuizRunner>
  );
}
