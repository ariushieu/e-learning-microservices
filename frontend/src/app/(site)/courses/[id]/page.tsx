import {
  ArchiveIcon,
  AwardIcon,
  CalendarIcon,
  ClipboardListIcon,
  ClockIcon,
  GlobeIcon,
  InfoIcon,
  LayersIcon,
  ListVideoIcon,
  PlayIcon,
  SettingsIcon,
  SignalIcon,
  StarIcon,
  UserIcon,
  UsersIcon,
} from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Callout } from "@/components/common/callout";
import { CourseCover } from "@/components/common/course-cover";
import { ErrorAlert } from "@/components/common/error-alert";
import { Fact, FactList } from "@/components/common/fact-list";
import { ProgressMeter } from "@/components/common/progress-meter";
import { Section } from "@/components/common/section";
import { StatusBadge } from "@/components/common/status-badge";
import { PriceTag } from "@/components/course/course-card";
import { CurriculumList } from "@/components/course/curriculum-list";
import { CourseReviews } from "@/components/course/course-reviews";
import { Suspense } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { attempt, getCourse, getMyEnrollments, isId } from "@/components/course/queries";
import { EnrollButton } from "@/components/enrollment/enroll-button";
import { QuizList } from "@/components/quiz/course-quizzes";
import { DetailHero, DetailPage, HeroMeta } from "@/components/templates/detail-page";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { hasRole, type Session } from "@/lib/auth-shared";
import { formatDay, formatDuration, formatNumber, label } from "@/lib/format";
import { gateway, getSession } from "@/lib/server/gateway";
import type { Course, Enrollment, Quiz, Section as CourseSection } from "@/lib/types";

export async function generateMetadata({ params }: PageProps<"/courses/[id]">): Promise<Metadata> {
  const { id } = await params;
  const course = isId(id) ? await getCourse(id).catch(() => null) : null;
  return course ? { title: course.title, description: course.summary ?? undefined } : { title: "Khóa học" };
}

function languageName(code: string) {
  return code === "vi" ? "Tiếng Việt" : code === "en" ? "Tiếng Anh" : code;
}

export default async function CourseDetailPage({ params, searchParams }: PageProps<"/courses/[id]">) {
  const { id } = await params;
  const query = await searchParams;
  const rawReviewPage = Array.isArray(query.reviewPage) ? query.reviewPage[0] : query.reviewPage;
  const reviewPage = rawReviewPage && /^\d{1,6}$/.test(rawReviewPage) ? Math.max(0, Number(rawReviewPage) - 1) : 0;
  if (!isId(id)) notFound();
  const [course, session] = await Promise.all([getCourse(id), getSession()]);
  if (!course) notFound();

  const [curriculum, enrollments, quizzes] = await Promise.all([
    attempt(gateway<CourseSection[]>(`/api/courses/${id}/curriculum`)),
    session ? attempt(getMyEnrollments()) : Promise.resolve(null),
    session ? attempt(gateway<Quiz[]>(`/api/quizzes?courseId=${id}`)) : Promise.resolve(null),
  ]);
  const enrollment = enrollments?.data?.find((e) => e.courseId === course.id) ?? null;
  const activeEnrollment = enrollment && enrollment.status !== "CANCELLED" ? enrollment : null;
  const isManager = !!session && (course.instructorId === session.userId || hasRole(session, "ROLE_ADMIN"));
  // Học viên chỉ thấy bài kiểm tra đã xuất bản, và chỉ khi đã ghi danh (hoặc là người quản lý khóa).
  const publishedQuizzes = quizzes?.data?.filter((q) => q.status === "PUBLISHED") ?? [];
  const showQuizzes = (activeEnrollment !== null || isManager) && publishedQuizzes.length > 0;
  const cover = <CourseCover title={course.title} category={course.categoryName} thumbnailUrl={course.thumbnailUrl} />;

  return (
    <DetailPage
      hero={
        <DetailHero
          crumbs={[
            { href: "/", label: "Khám phá" },
            { href: `/?categoryId=${course.categoryId}`, label: course.categoryName },
            { label: course.title },
          ]}
          eyebrow={course.categoryName}
          title={course.title}
          description={course.summary}
          meta={
            <>
              {course.status !== "PUBLISHED" && <StatusBadge status={course.status} />}
              <HeroMeta icon={<UserIcon />}>
                <Link
                  href={`/instructors/${course.instructorId}`}
                  className="rounded-sm font-medium text-white underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-white"
                >
                  {course.instructorName || "Giảng viên HUNRE"}
                </Link>
              </HeroMeta>
              <HeroMeta icon={<SignalIcon />}>{label(course.level)}</HeroMeta>
              <HeroMeta icon={<ListVideoIcon />}>{course.totalLessons} bài học</HeroMeta>
              <HeroMeta icon={<ClockIcon />}>{formatDuration(course.totalDurationSeconds)}</HeroMeta>
              <HeroMeta icon={<UsersIcon />}>{formatNumber(course.studentCount)} học viên</HeroMeta>
              <HeroMeta icon={<GlobeIcon />}>{languageName(course.language)}</HeroMeta>
              {course.ratingCount > 0 && (
                <HeroMeta icon={<StarIcon />}>
                  {formatNumber(course.ratingAvg, 1)}/5 ({formatNumber(course.ratingCount)})
                </HeroMeta>
              )}
              {course.updatedAt && <HeroMeta icon={<CalendarIcon />}>Cập nhật {formatDay(course.updatedAt)}</HeroMeta>}
            </>
          }
          media={cover}
        />
      }
      aside={
        <Card className="gap-0 py-0">
          {/* Trên màn lớn ảnh bìa đã nằm trong dải hero; điện thoại không có nên hiện ở đây. */}
          <div className="lg:hidden">{cover}</div>
          <div className="space-y-5 p-5">
            <PriceTag price={course.price} className="block text-title" />
            <EnrollAction
              course={course}
              session={session}
              enrollment={enrollment}
              enrollmentError={enrollments?.error ?? null}
              isManager={isManager}
            />
          </div>
          <div className="space-y-3 border-t p-5">
            <h2 className="text-subheading">Khóa học gồm</h2>
            <FactList>
              {curriculum.data && <Fact icon={LayersIcon} label="Chương" value={curriculum.data.length} />}
              <Fact icon={ListVideoIcon} label="Bài học" value={course.totalLessons} />
              <Fact icon={ClockIcon} label="Thời lượng" value={formatDuration(course.totalDurationSeconds)} />
              {showQuizzes && <Fact icon={ClipboardListIcon} label="Bài kiểm tra" value={publishedQuizzes.length} />}
              <Fact icon={GlobeIcon} label="Ngôn ngữ" value={languageName(course.language)} />
              <Fact icon={AwardIcon} label="Chứng chỉ" value="Khi hoàn thành" />
            </FactList>
          </div>
        </Card>
      }
    >
      {course.status === "ARCHIVED" && (
        <Callout icon={ArchiveIcon} tone="neutral" title="Khóa học đã lưu trữ">
          Khóa không nhận ghi danh mới. Học viên đã ghi danh vẫn học tiếp được.
        </Callout>
      )}
      {(course.status === "DRAFT" || course.status === "PENDING_REVIEW") && (
        <Callout icon={InfoIcon} tone="warning" title="Khóa học chưa xuất bản">
          Chỉ giảng viên của khóa và quản trị viên thấy trang này.
        </Callout>
      )}

      <Section
        title="Nội dung khóa học"
        actions={
          curriculum.data && (
            <p className="text-sm text-muted-foreground tabular-nums">
              {curriculum.data.length} chương · {course.totalLessons} bài học ·{" "}
              {formatDuration(course.totalDurationSeconds)}
            </p>
          )
        }
      >
        {curriculum.error !== null ? (
          <ErrorAlert title="Không tải được đề cương" message={curriculum.error} />
        ) : (
          <CurriculumList sections={curriculum.data} courseId={course.id} canLearn={activeEnrollment !== null} />
        )}
      </Section>

      {course.description && (
        <Section title="Giới thiệu khóa học">
          <Card>
            <CardContent>
              <div className="lesson-content text-foreground/90">{course.description}</div>
            </CardContent>
          </Card>
        </Section>
      )}

      <Suspense
        fallback={
          <div role="status" aria-label="Đang tải đánh giá">
            <Skeleton className="h-64 rounded-xl" />
          </div>
        }
      >
        <CourseReviews
          course={course}
          loggedIn={session !== null}
          page={reviewPage}
          isAdmin={hasRole(session, "ROLE_ADMIN")}
          canReply={
            hasRole(session, "ROLE_ADMIN") ||
            (hasRole(session, "ROLE_INSTRUCTOR") && course.instructorId === session?.userId)
          }
        />
      </Suspense>

      {showQuizzes && (
        <Section title="Bài kiểm tra" count={publishedQuizzes.length}>
          <QuizList quizzes={publishedQuizzes} />
        </Section>
      )}
    </DetailPage>
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
      <Button asChild size="lg" className="h-10 w-full text-base">
        <Link href={`/login?next=${encodeURIComponent(`/courses/${course.id}`)}`}>Đăng nhập để ghi danh</Link>
      </Button>
    );
  }

  if (enrollment && enrollment.status !== "CANCELLED") {
    return (
      <div className="space-y-4">
        <div className="space-y-2.5">
          <StatusBadge status={enrollment.status} />
          <ProgressMeter
            value={enrollment.progressPercent}
            label="Tiến độ học"
            detail={`${Math.round(enrollment.progressPercent)}% hoàn thành`}
          />
        </div>
        <div className="space-y-2">
          <Button asChild size="lg" className="h-10 w-full text-base">
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
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {enrollmentError && <ErrorAlert message={`Không kiểm tra được trạng thái ghi danh: ${enrollmentError}`} />}
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
