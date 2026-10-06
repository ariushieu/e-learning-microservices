import { AwardIcon, BookOpenIcon, CompassIcon, PlayIcon, RotateCcwIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { PageHeader } from "@/components/common/page-header";
import { StatusBadge } from "@/components/common/status-badge";
import { attempt, getMyEnrollments } from "@/components/course/queries";
import { CancelEnrollmentButton } from "@/components/enrollment/cancel-enrollment-button";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
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

const EMPTY: Record<string, string> = {
  active: "Không có khóa nào đang học.",
  completed: "Bạn chưa hoàn thành khóa học nào.",
  cancelled: "Không có khóa nào đã hủy.",
};

export default async function MyCoursesPage() {
  const result = await attempt(getMyEnrollments());
  const enrollments = [...(result.data ?? [])].sort((a, b) => order[a.status] - order[b.status]);

  return (
    <div>
      <PageHeader
        title="Khóa học của tôi"
        description="Các khóa bạn đã ghi danh và tiến độ học."
        actions={
          <Button asChild variant="outline" size="lg">
            <Link href="/">
              <CompassIcon /> Khám phá khóa học
            </Link>
          </Button>
        }
      />
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
          <TabsList className="max-w-full overflow-x-auto">
            {TABS.map((t) => (
              <TabsTrigger key={t.value} value={t.value} className="px-3">
                {t.label}
                <span className="text-xs text-muted-foreground tabular-nums">
                  {t.status ? enrollments.filter((e) => e.status === t.status).length : enrollments.length}
                </span>
              </TabsTrigger>
            ))}
          </TabsList>
          {TABS.map((t) => {
            const items = t.status ? enrollments.filter((e) => e.status === t.status) : enrollments;
            return (
              <TabsContent key={t.value} value={t.value}>
                {items.length === 0 ? (
                  <EmptyState icon={BookOpenIcon} title={EMPTY[t.value] ?? "Không có khóa học."} />
                ) : (
                  <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
                    {items.map((e) => (
                      <EnrollmentCard key={e.id} enrollment={e} />
                    ))}
                  </div>
                )}
              </TabsContent>
            );
          })}
        </Tabs>
      )}
    </div>
  );
}

function EnrollmentCard({ enrollment: e }: { enrollment: Enrollment }) {
  const percent = Math.round(e.progressPercent);
  const cancelled = e.status === "CANCELLED";
  return (
    <Card className="h-full">
      <CardContent className="flex flex-1 flex-col gap-4">
        <div className="flex items-start gap-3">
          <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
            <BookOpenIcon className="size-4.5" />
          </div>
          <div className="min-w-0 flex-1 space-y-1.5">
            <Link
              href={`/courses/${e.courseId}`}
              className="line-clamp-2 leading-snug font-medium underline-offset-4 hover:underline"
            >
              {e.courseTitle}
            </Link>
            <StatusBadge status={e.status} />
          </div>
        </div>

        <div className={cn("space-y-2", cancelled && "opacity-60")}>
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>Tiến độ</span>
            <span className="font-medium text-foreground tabular-nums">{percent}%</span>
          </div>
          <Progress value={percent} />
        </div>

        <dl className="mt-auto space-y-1 text-xs text-muted-foreground">
          <div className="flex justify-between gap-2">
            <dt>Ghi danh</dt>
            <dd className="tabular-nums">{formatDay(e.enrolledAt)}</dd>
          </div>
          {e.lastAccessedAt && (
            <div className="flex justify-between gap-2">
              <dt>Học gần nhất</dt>
              <dd className="tabular-nums">{formatDay(e.lastAccessedAt)}</dd>
            </div>
          )}
          {e.completedAt && (
            <div className="flex justify-between gap-2">
              <dt>Hoàn thành</dt>
              <dd className="tabular-nums">{formatDay(e.completedAt)}</dd>
            </div>
          )}
        </dl>
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
        {e.status === "COMPLETED" && (
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
