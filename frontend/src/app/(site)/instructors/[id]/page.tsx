import { BookOpenIcon, GraduationCapIcon, StarIcon, UsersIcon } from "lucide-react";
import type { Metadata } from "next";
import { notFound, redirect } from "next/navigation";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { Pagination } from "@/components/common/pagination";
import { Section } from "@/components/common/section";
import { Stat } from "@/components/common/stat";
import { CourseCard } from "@/components/course/course-card";
import { getInstructorProfile } from "@/components/course/instructor-profile-query";
import { attempt, isId } from "@/components/course/queries";
import { RatingStars } from "@/components/course/rating-stars";
import { DetailHero, DetailPage, HeroMeta } from "@/components/templates/detail-page";
import { CardGrid, ListPage } from "@/components/templates/list-page";
import { formatNumber } from "@/lib/format";
import { gateway } from "@/lib/server/gateway";
import type { CourseSummary, Page } from "@/lib/types";

export async function generateMetadata({ params }: PageProps<"/instructors/[id]">): Promise<Metadata> {
  const { id } = await params;
  const profile = isId(id) ? await getInstructorProfile(id).catch(() => null) : null;
  return { title: profile ? `Giảng viên ${profile.name}` : "Giảng viên" };
}

export default async function InstructorProfilePage({ params, searchParams }: PageProps<"/instructors/[id]">) {
  const { id } = await params;
  if (!isId(id)) notFound();
  const result = await attempt(getInstructorProfile(id));
  if (result.error !== null) {
    return (
      <ListPage title="Hồ sơ giảng viên">
        <ErrorAlert title="Không tải được hồ sơ" message={result.error} />
      </ListPage>
    );
  }
  const profile = result.data;
  if (!profile) notFound();
  const query = await searchParams;
  const rawPage = Array.isArray(query.page) ? query.page[0] : query.page;
  const page = rawPage && /^\d{1,6}$/.test(rawPage) ? Math.max(0, Number(rawPage) - 1) : 0;
  const hrefFor = (index: number) => `/instructors/${id}${index > 0 ? `?page=${index + 1}` : ""}`;
  // Trang công khai luôn dùng quyền khách: chủ khóa/admin cũng không thấy khóa nháp ở đây.
  const courses = await attempt(
    gateway<Page<CourseSummary>>(
      `/api/courses?instructorId=${id}&page=${page}&size=12&sort=createdAt,desc&sort=id,desc`,
      { anonymous: true },
    ),
  );
  if (courses.data && page > 0 && page >= courses.data.totalPages) {
    redirect(hrefFor(Math.max(0, courses.data.totalPages - 1)));
  }
  const initials = profile.name
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((word) => Array.from(word)[0])
    .join("")
    .toUpperCase();

  return (
    <DetailPage
      hero={
        <DetailHero
          crumbs={[{ href: "/", label: "Khám phá" }, { label: "Giảng viên" }]}
          eyebrow="Đồng hành cùng người học"
          title={
            <span className="flex min-w-0 flex-wrap items-center gap-5">
              <span
                aria-hidden
                className="flex size-20 shrink-0 items-center justify-center rounded-2xl bg-sidebar-accent text-title text-white ring-1 ring-white/20"
              >
                {initials}
              </span>
              <span className="min-w-0 break-words">{profile.name}</span>
            </span>
          }
          description="Khám phá các khóa học và đánh giá từ cộng đồng học viên."
          meta={
            <>
              <HeroMeta icon={<GraduationCapIcon />}>Giảng viên HUNRE E-Learning</HeroMeta>
            </>
          }
          actions={profile.ratingCount > 0 ? <RatingStars rating={profile.ratingAvg} /> : undefined}
        />
      }
    >
      <div className="grid gap-4 sm:grid-cols-3" aria-label="Thống kê giảng viên">
        <div data-testid="instructor-course-count">
          <Stat
            label="Khóa học"
            value={formatNumber(profile.publishedCourses)}
            hint="Đã xuất bản"
            icon={BookOpenIcon}
          />
        </div>
        <div data-testid="instructor-student-count">
          <Stat
            label="Học viên"
            value={formatNumber(profile.totalStudents)}
            hint="Cộng theo từng khóa học"
            icon={UsersIcon}
            tone="info"
          />
        </div>
        <div data-testid="instructor-rating">
          <Stat
            label="Đánh giá"
            value={profile.ratingCount ? `${formatNumber(profile.ratingAvg, 1)}/5` : "Chưa có"}
            hint={`${formatNumber(profile.ratingCount)} lượt đánh giá`}
            icon={StarIcon}
          />
        </div>
      </div>
      <Section title="Khóa học đang giảng dạy" count={courses.data?.totalElements}>
        {courses.error !== null ? (
          <ErrorAlert title="Không tải được khóa học" message={courses.error} />
        ) : courses.data.content.length === 0 ? (
          <EmptyState
            icon={BookOpenIcon}
            title="Chưa có khóa học để hiển thị"
            description="Các khóa học đang được cập nhật. Bạn vui lòng quay lại sau."
          />
        ) : (
          <>
            <CardGrid>
              {courses.data.content.map((course) => (
                <CourseCard key={course.id} course={course} />
              ))}
            </CardGrid>
            <Pagination page={courses.data.page} totalPages={courses.data.totalPages} hrefFor={hrefFor} />
          </>
        )}
      </Section>
    </DetailPage>
  );
}
