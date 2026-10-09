import { ArrowRightIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { ErrorAlert } from "@/components/common/error-alert";
import { Section } from "@/components/common/section";
import { CourseCard } from "@/components/course/course-card";
import { attempt, getMyEnrollments } from "@/components/course/queries";
import type { CourseReview } from "@/components/course/review-types";
import { ContinueLearning } from "@/components/enrollment/continue-learning";
import {
  featuredCourses,
  getCategoryTree,
  getPublishedCourses,
  platformStats,
  summarizeInstructors,
} from "@/components/home/catalog-data";
import { FieldTiles, HomeHero, InstructorCard, StatsBand, Testimonials, VerifyAndTeach, type Testimonial } from "@/components/home/sections";
import { CardGrid } from "@/components/templates/list-page";
import { Button } from "@/components/ui/button";
import { gateway, getSession } from "@/lib/server/gateway";
import type { CourseSummary, Page } from "@/lib/types";

export const metadata: Metadata = { title: { absolute: "HUNRE E-Learning · Khóa học trực tuyến" } };

const CATALOG_PARAMS = ["keyword", "categoryId", "level", "sort", "page"];

export default async function HomePage({ searchParams }: PageProps<"/">) {
  // Danh sách có bộ lọc đã chuyển sang /courses; link cũ dạng /?keyword=… vẫn dùng được.
  const sp = await searchParams;
  if (CATALOG_PARAMS.some((k) => sp[k] !== undefined)) {
    const query = new URLSearchParams();
    for (const k of CATALOG_PARAMS) {
      const v = sp[k];
      if (typeof v === "string" && v) query.set(k, v);
    }
    redirect(`/courses?${query}`);
  }

  const session = await getSession();
  const [courses, categories, mine] = await Promise.all([
    attempt(getPublishedCourses()),
    attempt(getCategoryTree()),
    session ? attempt(getMyEnrollments()) : Promise.resolve(null),
  ]);

  if (courses.error !== null) {
    return <ErrorAlert title="Không tải được danh sách khóa học" message={courses.error} />;
  }

  const all = courses.data;
  const stats = platformStats(all);
  const featured = featuredCourses(all);
  const covers = featured.filter((c) => c.thumbnailUrl).slice(0, 3);
  const instructors = summarizeInstructors(all).slice(0, 3);
  const testimonials = await loadTestimonials(featured);

  return (
    <>
      <HomeHero covers={covers.length > 0 ? covers : featured.slice(0, 3)} stats={stats} />
      <StatsBand stats={stats} />

      {session && mine?.data && <ContinueLearning enrollments={mine.data} fullName={session.fullName} covers={new Map(all.map((c) => [c.id, c.thumbnailUrl]))} />}

      {categories.data && <FieldTiles categories={categories.data} courses={all} />}

      <Section
        className="mb-16"
        title="Khóa học nổi bật"
        description="Được học viên đánh giá cao và ghi danh nhiều nhất."
        actions={
          <Button asChild variant="outline">
            <Link href="/courses">
              Tất cả khóa học <ArrowRightIcon />
            </Link>
          </Button>
        }
      >
        <CardGrid>
          {featured.map((c) => (
            <CourseCard key={c.id} course={c} />
          ))}
        </CardGrid>
      </Section>

      {instructors.length > 0 && (
        <Section
          className="mb-16"
          title="Đội ngũ giảng viên"
          description="Giảng viên và nghiên cứu viên của trường trực tiếp soạn bài và trả lời học viên."
          actions={
            <Button asChild variant="outline">
              <Link href="/instructors">
                Tất cả giảng viên <ArrowRightIcon />
              </Link>
            </Button>
          }
        >
          <div className="grid gap-5 md:grid-cols-3">
            {instructors.map((i) => (
              <InstructorCard key={i.id} instructor={i} />
            ))}
          </div>
        </Section>
      )}

      <Testimonials items={testimonials} />
      <VerifyAndTeach />
    </>
  );
}

/** Đánh giá 4–5 sao có nhận xét từ các khóa nổi bật; lỗi course-service chỉ ẩn phần này. */
async function loadTestimonials(featured: CourseSummary[]): Promise<Testimonial[]> {
  const rated = featured.filter((c) => c.ratingCount > 0).slice(0, 4);
  const pages = await Promise.all(
    rated.map((c) => gateway<Page<CourseReview>>(`/api/courses/${c.id}/reviews?size=5`, { anonymous: true }).catch(() => null)),
  );
  const items: Testimonial[] = [];
  pages.forEach((page, i) => {
    // Đánh giá cao nhất, nhận xét đầy đủ nhất của mỗi khóa.
    const review = page?.content
      .filter((r) => r.rating >= 4 && (r.comment?.length ?? 0) >= 30)
      .sort((a, b) => b.rating - a.rating || (b.comment?.length ?? 0) - (a.comment?.length ?? 0))[0];
    if (review) {
      items.push({ id: review.id, rating: review.rating, comment: review.comment!, authorName: review.authorName, courseId: rated[i].id, courseTitle: rated[i].title });
    }
  });
  return items.slice(0, 3);
}
