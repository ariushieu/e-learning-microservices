import { SearchXIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { CatalogFilters, levels } from "@/components/course/catalog-filters";
import { CourseCard } from "@/components/course/course-card";
import { Pagination } from "@/components/course/pagination";
import { attempt } from "@/components/course/queries";
import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import { gateway } from "@/lib/server/gateway";
import type { Category, CourseSummary, Page } from "@/lib/types";

export const metadata: Metadata = { title: { absolute: "HUNRE E-Learning · Khóa học trực tuyến" } };

const PAGE_SIZE = 12;

function one(value: string | string[] | undefined): string {
  return (Array.isArray(value) ? value[0] : value)?.trim() ?? "";
}

export default async function HomePage({ searchParams }: PageProps<"/">) {
  const sp = await searchParams;
  const keyword = one(sp.keyword);
  const categoryId = /^\d+$/.test(one(sp.categoryId)) ? one(sp.categoryId) : "";
  const level = levels.some((l) => l.value === one(sp.level)) ? one(sp.level) : "";
  // Trên URL trang đếm từ 1 cho dễ đọc, backend đếm từ 0.
  const pageParam = Number(one(sp.page));
  const page = Number.isInteger(pageParam) && pageParam > 1 ? pageParam - 1 : 0;

  const filters = { keyword, categoryId, level };
  const filtered = Boolean(keyword || categoryId || level);
  const query = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
  for (const [k, v] of Object.entries(filters)) if (v) query.set(k, v);

  const [categories, courses] = await Promise.all([
    attempt(gateway<Category[]>("/api/categories/tree")),
    attempt(gateway<Page<CourseSummary>>(`/api/courses?${query}`)),
  ]);

  const hrefFor = (p: number) => {
    const q = new URLSearchParams();
    for (const [k, v] of Object.entries(filters)) if (v) q.set(k, v);
    if (p > 0) q.set("page", String(p + 1));
    const s = q.toString();
    return s ? `/?${s}` : "/";
  };

  return (
    <div className="space-y-10">
      <section className="space-y-6">
        <div className="max-w-2xl space-y-2">
          <h1 className="text-3xl font-semibold tracking-tight text-balance sm:text-4xl">Học mọi lúc, mọi nơi</h1>
          <p className="text-lg text-muted-foreground">
            Khóa học từ giảng viên Trường Đại học Tài nguyên và Môi trường Hà Nội, học theo lộ trình và nhận chứng chỉ.
          </p>
        </div>
        <CatalogFilters categories={categories.data ?? []} {...filters} />
      </section>

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
                <Link href="/">Xem tất cả khóa học</Link>
              </Button>
            )
          }
        />
      ) : (
        <section className="space-y-6">
          <p className="text-sm text-muted-foreground">
            <span className="font-medium text-foreground tabular-nums">{formatNumber(courses.data.totalElements)}</span> khóa học
          </p>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {courses.data.content.map((c) => (
              <CourseCard key={c.id} course={c} />
            ))}
          </div>
          <Pagination page={courses.data.page} totalPages={courses.data.totalPages} hrefFor={hrefFor} />
        </section>
      )}
    </div>
  );
}
