import { ArrowRightIcon, AwardIcon, BookOpenIcon, ClipboardCheckIcon, CompassIcon, PlayIcon, RotateCcwIcon, TrophyIcon, XCircleIcon, type LucideIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { CourseCover } from "@/components/common/course-cover";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { Fact, FactList } from "@/components/common/fact-list";
import { ProgressMeter } from "@/components/common/progress-meter";
import { Section } from "@/components/common/section";
import { Stat, StatGrid } from "@/components/common/stat";
import { StatusBadge } from "@/components/common/status-badge";
import { attempt, getMyEnrollments } from "@/components/course/queries";
import { CancelEnrollmentButton } from "@/components/enrollment/cancel-enrollment-button";
import { CertificateTile } from "@/components/enrollment/certificate-tile";
import { getMyCertificates, getMyQuizResults, type QuizResult } from "@/components/enrollment/learning-data";
import { getPublishedCourses } from "@/components/home/catalog-data";
import { CardGrid, ListPage } from "@/components/templates/list-page";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { formatDay, formatNumber } from "@/lib/format";
import type { Enrollment, EnrollmentStatus } from "@/lib/types";
import { cn } from "@/lib/utils";

export const metadata: Metadata = { title: "Học tập của tôi" };

const order: Record<EnrollmentStatus, number> = { ACTIVE: 0, COMPLETED: 1, CANCELLED: 2 };

const TABS: { value: string; label: string; status?: EnrollmentStatus }[] = [
  { value: "all", label: "Tất cả" },
  { value: "active", label: "Đang học", status: "ACTIVE" },
  { value: "completed", label: "Hoàn thành", status: "COMPLETED" },
  { value: "cancelled", label: "Đã hủy", status: "CANCELLED" },
];

const EMPTY: Record<string, { icon: LucideIcon; title: string; description: string }> = {
  active: { icon: PlayIcon, title: "Không có khóa nào đang học.", description: "Ghi danh một khóa mới để tiếp tục hành trình học tập." },
  completed: { icon: AwardIcon, title: "Bạn chưa hoàn thành khóa học nào.", description: "Học hết các bài của một khóa để nhận chứng chỉ." },
  cancelled: { icon: XCircleIcon, title: "Không có khóa nào đã hủy.", description: "Các khóa bạn hủy ghi danh sẽ hiện ở đây." },
};

export default async function MyCoursesPage() {
  const result = await attempt(getMyEnrollments());
  const enrollments = [...(result.data ?? [])].sort((a, b) => order[a.status] - order[b.status]);
  // Số liệu phụ: service nào lỗi thì chỉ phần đó trống, danh sách khóa vẫn hiện.
  const [certificates, quizResults, catalog] = await Promise.all([
    result.data ? getMyCertificates(result.data).catch(() => null) : null,
    result.data ? getMyQuizResults(result.data).catch(() => null) : null,
    getPublishedCourses().catch(() => []),
  ]);
  const covers = new Map(catalog.map((c) => [c.id, c.thumbnailUrl]));
  const active = enrollments
    .filter((e) => e.status === "ACTIVE")
    .sort((a, b) => (b.lastAccessedAt ?? b.enrolledAt).localeCompare(a.lastAccessedAt ?? a.enrolledAt));
  const scores = (quizResults ?? []).map((r) => Number(r.attempt.score ?? 0));
  const passedCount = (quizResults ?? []).filter((r) => r.attempt.passed).length;

  return (
    <ListPage
      eyebrow="Bảng điều khiển"
      title="Học tập của tôi"
      description="Tiến độ, kết quả bài kiểm tra và chứng chỉ của bạn ở một nơi."
      actions={
        <Button asChild variant="outline" size="lg">
          <Link href="/courses">
            <CompassIcon /> Khám phá khóa học
          </Link>
        </Button>
      }
    >
      {result.error !== null ? (
        <ErrorAlert title="Không tải được danh sách ghi danh" message={result.error} />
      ) : enrollments.length === 0 ? (
        <EmptyState
          icon={BookOpenIcon}
          title="Bạn chưa ghi danh khóa học nào"
          description="Chọn một khóa học trong danh mục để bắt đầu học."
          action={
            <Button asChild size="lg">
              <Link href="/courses">Khám phá khóa học</Link>
            </Button>
          }
        />
      ) : (
        <div className="space-y-12">
          <StatGrid>
            <Stat label="Đang học" value={active.length} icon={PlayIcon} tone="info" />
            <Stat label="Đã hoàn thành" value={enrollments.filter((e) => e.status === "COMPLETED").length} icon={TrophyIcon} tone="achievement" />
            <Stat label="Chứng chỉ" value={certificates?.length ?? "—"} icon={AwardIcon} tone="achievement" />
            <Stat
              label="Điểm kiểm tra trung bình"
              value={scores.length ? formatNumber(scores.reduce((a, b) => a + b, 0) / scores.length, 1) : "—"}
              icon={ClipboardCheckIcon}
              hint={scores.length ? `${passedCount}/${scores.length} lượt đạt` : "Chưa làm bài kiểm tra nào"}
            />
          </StatGrid>

          <div className="grid gap-8 lg:grid-cols-[1.4fr_1fr]">
            <Section className="min-w-0" title="Tiếp tục học" count={active.length}>
              {active.length === 0 ? (
                <EmptyState icon={PlayIcon} title="Không có khóa nào đang học." description="Ghi danh một khóa mới để tiếp tục hành trình học tập." />
              ) : (
                <ul className="divide-y overflow-hidden rounded-xl bg-card ring-1 ring-border">
                  {active.map((e) => (
                    <li key={e.id} className="flex items-center gap-4 p-4">
                      <div className="min-w-0 flex-1 space-y-2">
                        <Link href={`/learn/${e.courseId}`} className="line-clamp-1 font-medium underline-offset-4 hover:text-primary hover:underline">
                          {e.courseTitle}
                        </Link>
                        <ProgressMeter value={e.progressPercent} label={e.lastAccessedAt ? `Học gần nhất ${formatDay(e.lastAccessedAt)}` : "Chưa bắt đầu"} size="sm" />
                      </div>
                      <Button asChild size="sm">
                        <Link href={`/learn/${e.courseId}`}>
                          <PlayIcon /> {Number(e.progressPercent) > 0 ? "Học tiếp" : "Bắt đầu"}
                        </Link>
                      </Button>
                    </li>
                  ))}
                </ul>
              )}
            </Section>

            <Section className="min-w-0" title="Kết quả kiểm tra gần đây">
              <QuizResults results={quizResults} />
            </Section>
          </div>

          {certificates && certificates.length > 0 && (
            <Section
              title="Chứng chỉ mới nhất"
              count={certificates.length}
              actions={
                <Button asChild variant="outline">
                  <Link href="/certificates">
                    Tất cả chứng chỉ <ArrowRightIcon />
                  </Link>
                </Button>
              }
            >
              <CardGrid>
                {certificates.slice(0, 3).map((c) => (
                  <CertificateTile key={c.id} certificate={c} />
                ))}
              </CardGrid>
            </Section>
          )}

          <Section title="Tất cả khóa học đã ghi danh">
        <Tabs defaultValue="all" className="gap-6">
          <div className="max-w-full overflow-x-auto">
            <TabsList>
              {TABS.map((t) => (
                <TabsTrigger key={t.value} value={t.value} className="px-3">
                  {t.label}
                  <span className="rounded-full bg-muted px-1.5 text-xs text-muted-foreground tabular-nums">
                    {t.status ? enrollments.filter((e) => e.status === t.status).length : enrollments.length}
                  </span>
                </TabsTrigger>
              ))}
            </TabsList>
          </div>
          {TABS.map((t) => {
            const items = t.status ? enrollments.filter((e) => e.status === t.status) : enrollments;
            const empty = EMPTY[t.value];
            return (
              <TabsContent key={t.value} value={t.value}>
                {items.length === 0 ? (
                  <EmptyState icon={empty?.icon ?? BookOpenIcon} title={empty?.title ?? "Không có khóa học."} description={empty?.description} />
                ) : (
                  <CardGrid>
                    {items.map((e) => (
                      <EnrollmentCard key={e.id} enrollment={e} thumbnailUrl={covers.get(e.courseId) ?? null} />
                    ))}
                  </CardGrid>
                )}
              </TabsContent>
            );
          })}
        </Tabs>
          </Section>
        </div>
      )}
    </ListPage>
  );
}

function EnrollmentCard({ enrollment: e, thumbnailUrl }: { enrollment: Enrollment; thumbnailUrl: string | null }) {
  const cancelled = e.status === "CANCELLED";
  const completed = e.status === "COMPLETED";
  return (
    <Card className="h-full gap-0 pt-0 transition-shadow hover:shadow-raised">
      <Link href={`/courses/${e.courseId}`} tabIndex={-1} aria-hidden className={cn("block", cancelled && "opacity-60 grayscale")}>
        <CourseCover title={e.courseTitle} thumbnailUrl={thumbnailUrl} />
      </Link>
      <CardContent className="flex flex-1 flex-col gap-4 pt-4 pb-4">
        <div className="space-y-2">
          <StatusBadge status={e.status} />
          <Link
            href={`/courses/${e.courseId}`}
            className="line-clamp-2 text-subheading underline-offset-4 hover:text-primary hover:underline"
          >
            {e.courseTitle}
          </Link>
        </div>

        <ProgressMeter value={e.progressPercent} className={cn(cancelled && "opacity-60")} />

        <FactList className="mt-auto text-xs">
          <Fact label="Ghi danh" value={formatDay(e.enrolledAt)} />
          {e.lastAccessedAt && <Fact label="Học gần nhất" value={formatDay(e.lastAccessedAt)} />}
          {e.completedAt && <Fact label="Hoàn thành" value={formatDay(e.completedAt)} />}
        </FactList>
      </CardContent>
      <CardFooter className="flex-wrap gap-2">
        {cancelled ? (
          <Button asChild variant="outline">
            <Link href={`/courses/${e.courseId}`}>
              <RotateCcwIcon /> Ghi danh lại
            </Link>
          </Button>
        ) : (
          <Button asChild>
            <Link href={`/learn/${e.courseId}`}>
              <PlayIcon /> Tiếp tục học
            </Link>
          </Button>
        )}
        {completed && (
          <Button asChild variant="outline">
            <Link href={`/certificates/${e.id}`}>
              <AwardIcon /> Chứng chỉ
            </Link>
          </Button>
        )}
        {e.status === "ACTIVE" && (
          <div className="ml-auto">
            <CancelEnrollmentButton enrollmentId={e.id} courseTitle={e.courseTitle} />
          </div>
        )}
      </CardFooter>
    </Card>
  );
}

function QuizResults({ results }: { results: QuizResult[] | null }) {
  if (results === null) {
    return <p className="text-sm text-muted-foreground">Không tải được kết quả bài kiểm tra.</p>;
  }
  if (results.length === 0) {
    return <EmptyState icon={ClipboardCheckIcon} title="Chưa có lượt làm bài nào" description="Bài kiểm tra nằm trong trang học của từng khóa." />;
  }
  return (
    <ul className="divide-y overflow-hidden rounded-xl bg-card ring-1 ring-border">
      {results.slice(0, 5).map(({ attempt: a, courseTitle }) => (
        <li key={a.id}>
          <Link href={`/attempts/${a.id}`} className="flex items-center gap-3 p-4 hover:bg-muted/50">
            <span
              className={cn(
                "flex size-12 shrink-0 flex-col items-center justify-center rounded-xl text-sm font-semibold tabular-nums",
                a.passed ? "bg-achievement-soft text-achievement-strong" : "bg-muted text-muted-foreground",
              )}
            >
              {formatNumber(Number(a.score ?? 0))}
              <span className="text-[10px] font-normal">điểm</span>
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate font-medium">{a.quizTitle}</span>
              <span className="block truncate text-sm text-muted-foreground">{courseTitle}</span>
            </span>
            <span className={cn("shrink-0 text-xs font-medium", a.passed ? "text-achievement-strong" : "text-muted-foreground")}>
              {a.passed ? "Đạt" : "Chưa đạt"}
            </span>
          </Link>
        </li>
      ))}
    </ul>
  );
}
