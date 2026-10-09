import { ArrowLeftIcon, ChartColumnIcon, DownloadIcon, InfoIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Callout } from "@/components/common/callout";
import { DataTableCard } from "@/components/common/data-table-card";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { Pagination } from "@/components/common/pagination";
import { Section } from "@/components/common/section";
import { Stat } from "@/components/common/stat";
import { StatusBadge } from "@/components/common/status-badge";
import { QuestionBreakdown } from "@/components/quiz/question-breakdown";
import type { QuizResults } from "@/components/quiz/results-types";
import { DashboardPage } from "@/components/templates/dashboard-page";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ApiError } from "@/lib/errors";
import { formatDate, formatNumber } from "@/lib/format";
import { gateway } from "@/lib/server/gateway";

export const metadata: Metadata = { title: "Kết quả bài kiểm tra" };

export default async function ResultsPage({ params, searchParams }: PageProps<"/instructor/quizzes/[id]/results">) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) notFound();
  const query = await searchParams;
  const rawPage = typeof query.page === "string" ? Number(query.page) : 0;
  const page = Number.isSafeInteger(rawPage) && rawPage >= 0 && rawPage <= 100000 ? rawPage : 0;
  let data: QuizResults;
  try {
    data = await gateway<QuizResults>(`/api/quizzes/${id}/results?page=${page}&size=20`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) notFound();
    if (!(error instanceof ApiError)) throw error;
    return (
      <DashboardPage title="Kết quả bài kiểm tra">
        <ErrorAlert message={error.message} />
        <Button asChild variant="outline">
          <Link href={`/instructor/quizzes/${id}/results`}>Thử lại</Link>
        </Button>
      </DashboardPage>
    );
  }
  const { summary, learners } = data;
  return (
    <DashboardPage
      title="Kết quả bài kiểm tra"
      description={data.quizTitle}
      crumbs={[{ label: "Soạn đề", href: `/instructor/quizzes/${id}` }, { label: "Kết quả" }]}
      actions={
        <>
          <Button asChild>
            <a href={`/api/quizzes/${id}/results/export`}>
              <DownloadIcon /> Tải CSV
            </a>
          </Button>
          <Button asChild variant="outline">
            <Link href={`/instructor/quizzes/${id}`}>
              <ArrowLeftIcon /> Về soạn đề
            </Link>
          </Button>
        </>
      }
      stats={<>
        <Stat label="Học viên đã nộp" value={formatNumber(summary.submittedLearners)} hint={`${formatNumber(summary.submittedAttempts)} lượt đã nộp`} />
        <Stat label="Điểm trung bình" value={formatNumber(summary.averageBestScore, 2)} hint="Điểm cao nhất của mỗi học viên · thang 100" />
        <Stat label="Tỉ lệ đạt" value={`${formatNumber(summary.passRate, 2)}%`} hint="Học viên có ít nhất một lượt đạt" tone="achievement" />
        <Stat label="Lượt hết giờ" value={formatNumber(summary.expiredAttempts)} hint="Không tính vào điểm trung bình" tone="warning" />
      </>}
    >
      {summary.unclassifiedAttempts > 0 && (
        <Callout icon={InfoIcon} tone="warning" title="Một phần dữ liệu cũ chưa được tính">
          {formatNumber(summary.unclassifiedAttempts)} lượt cũ chưa có thông tin phân biệt làm thật và làm thử. Các lượt này được loại khỏi thống kê.
        </Callout>
      )}
      <Section
        title="Kết quả học viên"
        count={learners.totalElements}
        description="Chỉ tính bài đã nộp; bỏ lượt đang làm và lượt làm thử của tác giả hoặc admin. Mới nộp gần đây nhất ở trên.">
        <DataTableCard
          className="[contain:inline-size]"
          isEmpty={learners.content.length === 0}
          empty={
            <EmptyState
              icon={ChartColumnIcon}
              title={learners.totalElements === 0 ? "Chưa có học viên nộp bài" : "Trang này không có học viên"}
              description="Kết quả sẽ xuất hiện sau khi học viên nộp bài."
              action={page > 0 ? (
                <Button asChild variant="outline">
                  <Link href={`/instructor/quizzes/${id}/results`}>Về trang đầu</Link>
                </Button>
              ) : undefined}
            />
          }
        >
          <Table className="min-w-[42rem]">
            <TableHeader>
              <TableRow>
                <TableHead>Học viên</TableHead>
                <TableHead>Lượt nộp</TableHead>
                <TableHead>Điểm cao nhất</TableHead>
                <TableHead>Kết quả</TableHead>
                <TableHead>Nộp gần nhất</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {learners.content.map((learner) => (
                <TableRow key={learner.learnerId}>
                  <TableCell className="min-w-48 max-w-64 whitespace-normal break-words font-medium">
                    {learner.learnerName}
                  </TableCell>
                  <TableCell className="tabular-nums">{learner.submittedAttempts}</TableCell>
                  <TableCell className="tabular-nums">{formatNumber(learner.bestScore, 2)}</TableCell>
                  <TableCell>
                    <StatusBadge status={learner.passed ? "PASSED" : "FAILED"} />
                  </TableCell>
                  <TableCell>{formatDate(learner.lastSubmittedAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </DataTableCard>
        <Pagination page={learners.page} totalPages={learners.totalPages} hrefFor={(p) => `/instructor/quizzes/${id}/results?page=${p}`} />
      </Section>
      <Section
        title="Tỉ lệ đúng từng câu"
        count={summary.questions.length}
        description="Tính trên tất cả lượt đã chấm câu đó, kể cả các lần làm lại. Thanh dưới mỗi phương án là tỉ lệ lượt chọn; câu dưới 50% và phương án sai được chọn nhiều hơn đáp án đúng được đánh dấu để xem lại."
      >
        {summary.questions.length === 0 ? (
          <EmptyState title="Chưa có câu hỏi" />
        ) : (
          <div className="grid gap-4 xl:grid-cols-2">
            {summary.questions.map((question, i) => (
              <QuestionBreakdown key={question.questionId} question={question} index={i} />
            ))}
          </div>
        )}
      </Section>
    </DashboardPage>
  );
}
