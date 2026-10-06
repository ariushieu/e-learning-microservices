import type { Metadata } from "next";
import Link from "next/link";
import { CancelEnrollmentButton } from "@/components/student/cancel-enrollment-button";
import { CourseThumb } from "@/components/student/course-card";
import { attempt, getMyEnrollments } from "@/components/student/queries";
import { Alert, Badge, Empty, LinkButton, PageTitle, ProgressBar, formatDate } from "@/components/ui";
import type { Enrollment, EnrollmentStatus } from "@/lib/types";

export const metadata: Metadata = { title: "Khóa học của tôi" };

const order: Record<EnrollmentStatus, number> = { ACTIVE: 0, COMPLETED: 1, CANCELLED: 2 };

export default async function MyCoursesPage() {
  const result = await attempt(getMyEnrollments());
  const enrollments = [...(result.data ?? [])].sort((a, b) => order[a.status] - order[b.status]);

  return (
    <div>
      <PageTitle title="Khóa học của tôi" subtitle="Các khóa bạn đã ghi danh và tiến độ học." />
      {result.error !== null ? (
        <Alert>Không tải được danh sách ghi danh: {result.error}</Alert>
      ) : enrollments.length === 0 ? (
        <Empty>
          <p>Bạn chưa ghi danh khóa học nào.</p>
          <LinkButton href="/" className="mt-4">
            Khám phá khóa học
          </LinkButton>
        </Empty>
      ) : (
        <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
          {enrollments.map((e) => (
            <EnrollmentCard key={e.id} enrollment={e} />
          ))}
        </div>
      )}
    </div>
  );
}

function EnrollmentCard({ enrollment: e }: { enrollment: Enrollment }) {
  const percent = Math.round(e.progressPercent);
  const cancelled = e.status === "CANCELLED";
  return (
    <div className={`flex flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm ${cancelled ? "opacity-75" : ""}`}>
      <Link href={`/courses/${e.courseId}`}>
        <CourseThumb course={{ id: e.courseId, title: e.courseTitle, thumbnailUrl: null }} aspect="aspect-[3/1]" />
      </Link>
      <div className="flex flex-1 flex-col gap-3 p-5">
        <div className="flex items-start justify-between gap-3">
          <Link href={`/courses/${e.courseId}`} className="font-semibold text-slate-900 hover:text-indigo-700">
            {e.courseTitle}
          </Link>
          <Badge value={e.status} className="shrink-0" />
        </div>
        <div className="space-y-1.5">
          <ProgressBar value={percent} />
          <p className="text-xs text-slate-500">{percent}% hoàn thành</p>
        </div>
        <dl className="space-y-0.5 text-xs text-slate-500">
          <div>Ghi danh: {formatDate(e.enrolledAt)}</div>
          {e.lastAccessedAt && <div>Học gần nhất: {formatDate(e.lastAccessedAt)}</div>}
          {e.completedAt && <div>Hoàn thành: {formatDate(e.completedAt)}</div>}
        </dl>
        <div className="mt-auto flex flex-wrap items-center gap-2 pt-2">
          {cancelled ? (
            <LinkButton href={`/courses/${e.courseId}`} variant="secondary">
              Ghi danh lại
            </LinkButton>
          ) : (
            <LinkButton href={`/learn/${e.courseId}`}>Tiếp tục học</LinkButton>
          )}
          {e.status === "COMPLETED" && (
            <LinkButton href={`/certificates/${e.id}`} variant="secondary">
              Chứng chỉ
            </LinkButton>
          )}
          {e.status === "ACTIVE" && <CancelEnrollmentButton enrollmentId={e.id} courseTitle={e.courseTitle} />}
        </div>
      </div>
    </div>
  );
}
