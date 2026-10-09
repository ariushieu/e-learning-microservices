import { AwardIcon, BookOpenIcon, LayersIcon, SearchXIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { ContourPattern, FullBleed } from "@/components/common/decor";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { Pagination } from "@/components/common/pagination";
import { Toolbar } from "@/components/common/toolbar";
import { CatalogFilters, CatalogSearch, CategoryChips, catalogSorts, levels } from "@/components/course/catalog-filters";
import { CourseCard } from "@/components/course/course-card";
import { attempt } from "@/components/course/queries";
import { CardGrid } from "@/components/templates/list-page";
import { HeroMeta } from "@/components/templates/detail-page";
import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import { gateway } from "@/lib/server/gateway";
import type { Category, CourseSummary, Page } from "@/lib/types";

export const metadata: Metadata = { title: "Khóa học" };

const PAGE_SIZE = 12;

function one(value: string | string[] | undefined): string {
  return (Array.isArray(value) ? value[0] : value)?.trim() ?? "";
}

export default async function CoursesPage({ searchParams }: PageProps<"/courses">) {
  const sp = await searchParams;
  const keyword = one(sp.keyword);
  const categoryId = /^\d+$/.test(one(sp.categoryId)) ? one(sp.categoryId) : "";
  const level = levels.some((l) => l.value === one(sp.level)) ? one(sp.level) : "";
  const sort = catalogSorts.find((s) => s.value === one(sp.sort))?.value ?? "createdAt,desc";
  // Trên URL trang đếm từ 1 cho dễ đọc, backend đếm từ 0.
  const pageParam = Number(one(sp.page));
  const page = Number.isInteger(pageParam) && pageParam > 1 ? pageParam - 1 : 0;

  const filters = { keyword, categoryId, level, sort };
  const filtered = Boolean(keyword || categoryId || level);
  const query = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
  for (const [k, v] of Object.entries(filters)) if (v) query.set(k, v);
  // Khóa cùng giá/số học viên vẫn có thứ tự ổn định khi chuyển trang.
  query.append("sort", "id,desc");

  const [categories, courses] = await Promise.all([
    attempt(gateway<Category[]>("/api/categories/tree")),
    attempt(gateway<Page<CourseSummary>>(`/api/courses?${query}`)),
  ]);

  const hrefFor = (p: number) => {
    const q = new URLSearchParams();
    for (const [k, v] of Object.entries(filters)) if (v) q.set(k, v);
    if (p > 0) q.set("page", String(p + 1));
    const s = q.toString();
    return s ? `/courses?${s}` : "/courses";
  };

  const categoryCount = (categories.data ?? []).reduce((n, c) => n + 1 + (c.subCategories?.length ?? 0), 0);

  return (
    <>
      <FullBleed className="relative isolate -mt-10 mb-10 overflow-hidden bg-sidebar text-white" inner="relative py-12 lg:py-16">
        <ContourPattern className="-z-10 text-white/[0.07]" />
        <div className="max-w-2xl space-y-5">
          <p className="text-eyebrow text-sidebar-primary">Khám phá</p>
          <h1 className="text-display text-white">Tất cả khóa học</h1>
          <p className="text-lg leading-relaxed text-white/80">
            Tìm theo tên, lọc theo lĩnh vực và trình độ. Mọi khóa đều có bài kiểm tra và chứng chỉ khi hoàn thành.
          </p>
          <CatalogSearch keyword={keyword} />
          <div className="flex flex-wrap items-center gap-x-5 gap-y-2 pt-1 text-sm text-white/80 [&_svg]:size-4 [&_svg]:text-white/60">
            {/* Tổng số khóa chỉ đúng khi chưa lọc; đang lọc thì số kết quả đã có ở thanh công cụ. */}
            {!filtered && courses.data && (
              <HeroMeta icon={<BookOpenIcon />}>
                <span className="tabular-nums">{formatNumber(courses.data.totalElements)}</span> khóa học
              </HeroMeta>
            )}
            {categoryCount > 0 && (
              <HeroMeta icon={<LayersIcon />}>
                <span className="tabular-nums">{formatNumber(categoryCount)}</span> danh mục
              </HeroMeta>
            )}
            <HeroMeta icon={<AwardIcon />}>Chứng chỉ khi hoàn thành</HeroMeta>
          </div>
        </div>
      </FullBleed>

      <section aria-label="Danh sách khóa học" className="min-w-0">
        {categories.error !== null ? (
          <ErrorAlert title="Không tải được danh mục" message={categories.error} />
        ) : (
          <CategoryChips categories={categories.data} {...filters} />
        )}
        <Toolbar
          start={<CatalogFilters {...filters} />}
          end={
            courses.data &&
            courses.data.totalElements > 0 && (
              <p>
                <span className="font-medium text-foreground tabular-nums">{formatNumber(courses.data.totalElements)}</span>{" "}
                khóa học
              </p>
            )
          }
        />

        {courses.error !== null ? (
          <ErrorAlert title="Không tải được danh sách khóa học" message={courses.error} />
        ) : courses.data.content.length === 0 ? (
          <EmptyState
            icon={SearchXIcon}
            title="Không tìm thấy khóa học nào phù hợp"
            description={filtered ? "Thử từ khóa khác hoặc bỏ bớt bộ lọc." : "Chưa có khóa học nào được xuất bản."}
            action={
              filtered && (
                <Button asChild variant="outline">
                  <Link href="/courses">Xem tất cả khóa học</Link>
                </Button>
              )
            }
          />
        ) : (
          <>
            <CardGrid>
              {courses.data.content.map((c) => (
                <CourseCard key={c.id} course={c} />
              ))}
            </CardGrid>
            <Pagination page={courses.data.page} totalPages={courses.data.totalPages} hrefFor={hrefFor} />
          </>
        )}
      </section>
    </>
  );
}
