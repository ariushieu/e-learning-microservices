import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { CourseThumb } from "@/components/student/course-card";
import { CurriculumList } from "@/components/student/curriculum-list";
import { EnrollButton } from "@/components/student/enroll-button";
import { attempt, getCourse, getMyEnrollments, isId } from "@/components/student/queries";
import { Alert, Badge, Card, LinkButton, ProgressBar, formatDate, formatDuration, formatPrice } from "@/components/ui";
import { hasRole, type Session } from "@/lib/auth-shared";
import { gateway, getSession } from "@/lib/server/gateway";
import type { Course, Enrollment, Section } from "@/lib/types";

export async function generateMetadata({ params }: PageProps<"/courses/[id]">): Promise<Metadata> {
  const { id } = await params;
  const course = isId(id) ? await getCourse(id).catch(() => null) : null;
  return course
    ? { title: course.title, description: course.summary ?? undefined }
    : { title: "Khóa học" };
}

export default async function CourseDetailPage({ params }: PageProps<"/courses/[id]">) {
  const { id } = await params;
  if (!isId(id)) notFound();
  const [course, session] = await Promise.all([getCourse(id), getSession()]);
  if (!course) notFound();

  const [curriculum, enrollments] = await Promise.all([
    attempt(gateway<Section[]>(`/api/courses/${id}/curriculum`)),
    session ? attempt(getMyEnrollments()) : Promise.resolve(null),
  ]);
  const enrollment = enrollments?.data?.find((e) => e.courseId === course.id) ?? null;
  const activeEnrollment = enrollment && enrollment.status !== "CANCELLED" ? enrollment : null;

  return (
    <div className="space-y-8">
      <CourseHero course={course} />

      <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_340px]">
        <div className="order-2 space-y-8 lg:order-1">
          {course.description && (
            <section>
              <h2 className="mb-3 text-lg font-semibold text-slate-900">Giới thiệu khóa học</h2>
              <Card>
                <div className="lesson-content text-slate-700">{course.description}</div>
              </Card>
            </section>
          )}
          <section>
            <h2 className="mb-3 text-lg font-semibold text-slate-900">Nội dung khóa học</h2>
            {curriculum.error !== null ? (
              <Alert>Không tải được đề cương: {curriculum.error}</Alert>
            ) : (
              <CurriculumList sections={curriculum.data} courseId={course.id} canLearn={activeEnrollment !== null} />
            )}
          </section>
        </div>

        <aside className="order-1 lg:order-2">
          <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm lg:sticky lg:top-24">
            <CourseThumb course={course} />
            <div className="space-y-5 p-5">
              <div className="text-3xl font-bold text-slate-900">{formatPrice(course.price)}</div>
              <EnrollAction
                course={course}
                session={session}
                enrollment={enrollment}
                enrollmentError={enrollments?.error ?? null}
              />
              <CourseStats course={course} />
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}

function CourseHero({ course }: { course: Course }) {
  return (
    <section className="rounded-2xl bg-linear-to-br from-slate-900 via-indigo-950 to-indigo-800 px-6 py-8 text-white sm:px-10">
      <div className="flex flex-wrap items-center gap-2 text-sm text-indigo-200">
        <span>{course.categoryName}</span>
        <span>·</span>
        <Badge value={course.level} />
        {course.status !== "PUBLISHED" && <Badge value={course.status} />}
      </div>
      <h1 className="mt-3 max-w-3xl text-3xl font-bold">{course.title}</h1>
      {course.summary && <p className="mt-3 max-w-3xl text-indigo-100">{course.summary}</p>}
      <p className="mt-4 text-sm text-indigo-200">
        Giảng viên: <span className="font-medium text-white">{course.instructorName || "Giảng viên HUNRE"}</span>
        {course.updatedAt && <> · Cập nhật {formatDate(course.updatedAt)}</>}
      </p>
    </section>
  );
}

function EnrollAction({
  course,
  session,
  enrollment,
  enrollmentError,
}: {
  course: Course;
  session: Session | null;
  enrollment: Enrollment | null;
  enrollmentError: string | null;
}) {
  const isManager = !!session && (course.instructorId === session.userId || hasRole(session, "ROLE_ADMIN"));
  const manage = isManager && (
    <LinkButton href={`/instructor/courses/${course.id}`} variant="secondary" className="w-full">
      Quản lý khóa học
    </LinkButton>
  );

  if (!session) {
    return (
      <LinkButton href={`/login?next=${encodeURIComponent(`/courses/${course.id}`)}`} className="w-full py-3 text-base">
        Đăng nhập để ghi danh
      </LinkButton>
    );
  }

  if (enrollment && enrollment.status !== "CANCELLED") {
    const percent = Math.round(enrollment.progressPercent);
    return (
      <div className="space-y-3">
        <div className="space-y-1.5">
          <div className="flex justify-between text-sm">
            <Badge value={enrollment.status} />
            <span className="font-medium text-slate-700">{percent}% hoàn thành</span>
          </div>
          <ProgressBar value={percent} />
        </div>
        <LinkButton href={`/learn/${course.id}`} className="w-full py-3 text-base">
          Vào học
        </LinkButton>
        {enrollment.status === "COMPLETED" && (
          <LinkButton href={`/certificates/${enrollment.id}`} variant="secondary" className="w-full">
            Xem chứng chỉ
          </LinkButton>
        )}
        {manage}
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {enrollmentError && <Alert kind="info">Không kiểm tra được trạng thái ghi danh: {enrollmentError}</Alert>}
      {course.status === "PUBLISHED" ? (
        <EnrollButton courseId={course.id} label={enrollment ? "Ghi danh lại" : "Ghi danh ngay"} />
      ) : (
        <Alert kind="info">Khóa học chưa xuất bản nên chưa thể ghi danh.</Alert>
      )}
      {manage}
    </div>
  );
}

function CourseStats({ course }: { course: Course }) {
  const rows: [string, string][] = [
    ["Bài học", `${course.totalLessons} bài`],
    ["Thời lượng", formatDuration(course.totalDurationSeconds)],
    ["Học viên", `${course.studentCount}`],
    ["Ngôn ngữ", course.language === "vi" ? "Tiếng Việt" : course.language === "en" ? "Tiếng Anh" : course.language],
  ];
  if (course.ratingCount > 0) rows.push(["Đánh giá", `${course.ratingAvg.toFixed(1)}/5 (${course.ratingCount})`]);

  return (
    <dl className="divide-y divide-slate-100 border-t border-slate-100 text-sm">
      {rows.map(([label, value]) => (
        <div key={label} className="flex justify-between py-2">
          <dt className="text-slate-500">{label}</dt>
          <dd className="font-medium text-slate-800">{value}</dd>
        </div>
      ))}
    </dl>
  );
}
