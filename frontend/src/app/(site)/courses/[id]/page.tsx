import {
  AwardIcon,
  CalendarIcon,
  CircleAlertIcon,
  ClockIcon,
  GlobeIcon,
  InfoIcon,
  ListVideoIcon,
  PlayIcon,
  SettingsIcon,
  SignalIcon,
  StarIcon,
  UserIcon,
  UsersIcon,
  type LucideIcon,
} from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import type { ReactNode } from "react";
import { ErrorAlert } from "@/components/common/error-alert";
import { StatusBadge } from "@/components/common/status-badge";
import { CourseThumb, PriceTag } from "@/components/course/course-card";
import { CurriculumList } from "@/components/course/curriculum-list";
import { attempt, getCourse, getMyEnrollments, isId } from "@/components/course/queries";
import { EnrollButton } from "@/components/enrollment/enroll-button";
import { QuizList } from "@/components/quiz/course-quizzes";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { hasRole, type Session } from "@/lib/auth-shared";
import { formatDay, formatDuration, formatNumber, label } from "@/lib/format";
import { gateway, getSession } from "@/lib/server/gateway";
import type { Course, Enrollment, Quiz, Section } from "@/lib/types";

export async function generateMetadata({ params }: PageProps<"/courses/[id]">): Promise<Metadata> {
  const { id } = await params;
  const course = isId(id) ? await getCourse(id).catch(() => null) : null;
  return course ? { title: course.title, description: course.summary ?? undefined } : { title: "Khóa học" };
}

function languageName(code: string) {
  return code === "vi" ? "Tiếng Việt" : code === "en" ? "Tiếng Anh" : code;
}

export default async function CourseDetailPage({ params }: PageProps<"/courses/[id]">) {
  const { id } = await params;
  if (!isId(id)) notFound();
  const [course, session] = await Promise.all([getCourse(id), getSession()]);
  if (!course) notFound();

  const [curriculum, enrollments, quizzes] = await Promise.all([
    attempt(gateway<Section[]>(`/api/courses/${id}/curriculum`)),
    session ? attempt(getMyEnrollments()) : Promise.resolve(null),
    session ? attempt(gateway<Quiz[]>(`/api/quizzes?courseId=${id}`)) : Promise.resolve(null),
  ]);
  const enrollment = enrollments?.data?.find((e) => e.courseId === course.id) ?? null;
  const activeEnrollment = enrollment && enrollment.status !== "CANCELLED" ? enrollment : null;
  const isManager = !!session && (course.instructorId === session.userId || hasRole(session, "ROLE_ADMIN"));
  // Học viên chỉ thấy bài kiểm tra đã xuất bản, và chỉ khi đã ghi danh (hoặc là người quản lý khóa).
  const publishedQuizzes = quizzes?.data?.filter((q) => q.status === "PUBLISHED") ?? [];
  const showQuizzes = (activeEnrollment !== null || isManager) && publishedQuizzes.length > 0;

  return (
    <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_340px] lg:gap-10">
      <CourseHeader course={course} />

      <aside className="lg:col-start-2 lg:row-span-2 lg:row-start-1">
        <div className="lg:sticky lg:top-20">
          <Card className="gap-0 pt-0">
            <CourseThumb course={course} />
            <CardContent className="space-y-4 pt-5">
              <PriceTag price={course.price} className="text-2xl" />
              <EnrollAction
                course={course}
                session={session}
                enrollment={enrollment}
                enrollmentError={enrollments?.error ?? null}
                isManager={isManager}
              />
            </CardContent>
          </Card>
        </div>
      </aside>

      <div className="min-w-0 space-y-10">
        <section className="space-y-4">
          <div className="flex flex-wrap items-baseline justify-between gap-2">
            <h2 className="text-lg font-semibold">Nội dung khóa học</h2>
            {curriculum.data && (
              <p className="text-sm text-muted-foreground tabular-nums">
                {curriculum.data.length} chương · {course.totalLessons} bài học · {formatDuration(course.totalDurationSeconds)}
              </p>
            )}
          </div>
          {curriculum.error !== null ? (
            <ErrorAlert title="Không tải được đề cương" message={curriculum.error} />
          ) : (
            <CurriculumList sections={curriculum.data} courseId={course.id} canLearn={activeEnrollment !== null} />
          )}
        </section>

        {course.description && (
          <Card>
            <CardHeader>
              <CardTitle className="text-lg font-semibold">Giới thiệu khóa học</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="lesson-content text-foreground/90">{course.description}</div>
            </CardContent>
          </Card>
        )}

        {showQuizzes && (
          <section className="space-y-4">
            <h2 className="text-lg font-semibold">Bài kiểm tra</h2>
            <QuizList quizzes={publishedQuizzes} />
          </section>
        )}
      </div>
    </div>
  );
}

function CourseHeader({ course }: { course: Course }) {
  return (
    <section className="min-w-0 space-y-5">
      <Breadcrumb>
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href="/">Khám phá</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href={`/?categoryId=${course.categoryId}`}>{course.categoryName}</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator className="hidden sm:block" />
          <BreadcrumbItem className="hidden sm:inline-flex">
            <BreadcrumbPage className="line-clamp-1">{course.title}</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      <div className="space-y-3">
        {course.status !== "PUBLISHED" && <StatusBadge status={course.status} />}
        <h1 className="text-3xl font-semibold tracking-tight text-balance">{course.title}</h1>
        {course.summary && <p className="text-lg text-muted-foreground">{course.summary}</p>}
      </div>

      <ul className="flex flex-wrap gap-x-5 gap-y-2 text-sm text-muted-foreground">
        <Meta icon={UserIcon}>
          <span className="font-medium text-foreground">{course.instructorName || "Giảng viên HUNRE"}</span>
        </Meta>
        <Meta icon={SignalIcon}>{label(course.level)}</Meta>
        <Meta icon={ListVideoIcon}>{course.totalLessons} bài học</Meta>
        <Meta icon={ClockIcon}>{formatDuration(course.totalDurationSeconds)}</Meta>
        <Meta icon={UsersIcon}>{formatNumber(course.studentCount)} học viên</Meta>
        <Meta icon={GlobeIcon}>{languageName(course.language)}</Meta>
        {course.ratingCount > 0 && (
          <Meta icon={StarIcon}>
            {formatNumber(course.ratingAvg, 1)}/5 ({formatNumber(course.ratingCount)})
          </Meta>
        )}
        {course.updatedAt && <Meta icon={CalendarIcon}>Cập nhật {formatDay(course.updatedAt)}</Meta>}
      </ul>

      {course.status === "ARCHIVED" && (
        <Alert>
          <InfoIcon />
          <AlertTitle>Khóa học đã lưu trữ</AlertTitle>
          <AlertDescription>Khóa không nhận ghi danh mới. Học viên đã ghi danh vẫn học tiếp được.</AlertDescription>
        </Alert>
      )}
      {(course.status === "DRAFT" || course.status === "PENDING_REVIEW") && (
        <Alert>
          <InfoIcon />
          <AlertTitle>Khóa học chưa xuất bản</AlertTitle>
          <AlertDescription>Chỉ giảng viên của khóa và quản trị viên thấy trang này.</AlertDescription>
        </Alert>
      )}
    </section>
  );
}

function Meta({ icon: Icon, children }: { icon: LucideIcon; children: ReactNode }) {
  return (
    <li className="inline-flex items-center gap-1.5">
      <Icon className="size-4" />
      {children}
    </li>
  );
}

function EnrollAction({
  course,
  session,
  enrollment,
  enrollmentError,
  isManager,
}: {
  course: Course;
  session: Session | null;
  enrollment: Enrollment | null;
  enrollmentError: string | null;
  isManager: boolean;
}) {
  const manage = isManager && (
    <Button asChild variant="outline" size="lg" className="w-full">
      <Link href={`/instructor/courses/${course.id}`}>
        <SettingsIcon /> Quản lý khóa học
      </Link>
    </Button>
  );

  if (!session) {
    return (
      <Button asChild size="lg" className="w-full">
        <Link href={`/login?next=${encodeURIComponent(`/courses/${course.id}`)}`}>Đăng nhập để ghi danh</Link>
      </Button>
    );
  }

  if (enrollment && enrollment.status !== "CANCELLED") {
    const percent = Math.round(enrollment.progressPercent);
    return (
      <div className="space-y-3">
        <div className="space-y-2">
          <div className="flex items-center justify-between text-sm">
            <StatusBadge status={enrollment.status} />
            <span className="text-muted-foreground tabular-nums">{percent}% hoàn thành</span>
          </div>
          <Progress value={percent} aria-label="Tiến độ học" />
        </div>
        <Button asChild size="lg" className="w-full">
          <Link href={`/learn/${course.id}`}>
            <PlayIcon /> Vào học
          </Link>
        </Button>
        {enrollment.status === "COMPLETED" && (
          <Button asChild variant="outline" size="lg" className="w-full">
            <Link href={`/certificates/${enrollment.id}`}>
              <AwardIcon /> Xem chứng chỉ
            </Link>
          </Button>
        )}
        {manage}
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {enrollmentError && (
        <Alert>
          <CircleAlertIcon />
          <AlertDescription>Không kiểm tra được trạng thái ghi danh: {enrollmentError}</AlertDescription>
        </Alert>
      )}
      {course.status === "PUBLISHED" ? (
        <EnrollButton courseId={course.id} label={enrollment ? "Ghi danh lại" : "Ghi danh ngay"} />
      ) : (
        <p className="rounded-lg bg-muted px-3 py-2 text-sm text-muted-foreground">
          {course.status === "ARCHIVED"
            ? "Khóa học đã lưu trữ nên không nhận ghi danh mới."
            : "Khóa học chưa xuất bản nên chưa thể ghi danh."}
        </p>
      )}
      {manage}
    </div>
  );
}
