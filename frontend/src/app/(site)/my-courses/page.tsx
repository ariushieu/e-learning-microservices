import { AwardIcon, BookOpenIcon, CompassIcon, PlayIcon, RotateCcwIcon, XCircleIcon, type LucideIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { CourseCover } from "@/components/common/course-cover";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { Fact, FactList } from "@/components/common/fact-list";
import { ProgressMeter } from "@/components/common/progress-meter";
import { StatusBadge } from "@/components/common/status-badge";
import { attempt, getMyEnrollments } from "@/components/course/queries";
import { CancelEnrollmentButton } from "@/components/enrollment/cancel-enrollment-button";
import { CardGrid, ListPage } from "@/components/templates/list-page";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { formatDay } from "@/lib/format";
import type { Enrollment, EnrollmentStatus } from "@/lib/types";
import { cn } from "@/lib/utils";

export const metadata: Metadata = { title: "Khóa học của tôi" };

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

  return (
    <ListPage
      title="Khóa học của tôi"
      description="Các khóa bạn đã ghi danh và tiến độ học."
      actions={
        <Button asChild variant="outline" size="lg">
          <Link href="/">
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
              <Link href="/">Khám phá khóa học</Link>
            </Button>
          }
        />
      ) : (
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
                      <EnrollmentCard key={e.id} enrollment={e} />
                    ))}
                  </CardGrid>
                )}
              </TabsContent>
            );
          })}
        </Tabs>
      )}
    </ListPage>
  );
}

function EnrollmentCard({ enrollment: e }: { enrollment: Enrollment }) {
  const cancelled = e.status === "CANCELLED";
  const completed = e.status === "COMPLETED";
  return (
    <Card className="h-full gap-0 pt-0 transition-shadow hover:shadow-raised">
      <Link href={`/courses/${e.courseId}`} tabIndex={-1} aria-hidden className={cn("block", cancelled && "opacity-60 grayscale")}>
        <CourseCover title={e.courseTitle} />
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
