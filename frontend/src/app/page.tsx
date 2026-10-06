import type { Metadata } from "next";
import { CatalogFilters, levels } from "@/components/student/catalog-filters";
import { CourseCard } from "@/components/student/course-card";
import { Pagination } from "@/components/student/pagination";
import { attempt } from "@/components/student/queries";
import { Alert, Empty, LinkButton } from "@/components/ui";
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
    <div className="space-y-8">
      <section className="relative overflow-hidden rounded-2xl bg-linear-to-br from-indigo-700 via-indigo-600 to-violet-600 px-6 py-10 text-white sm:px-10">
        <div className="max-w-2xl">
          <p className="text-sm font-medium uppercase tracking-wider text-indigo-200">HUNRE E-Learning</p>
          <h1 className="mt-2 text-3xl font-bold sm:text-4xl">Học mọi lúc, mọi nơi</h1>
          <p className="mt-3 text-indigo-100">
            Khóa học từ giảng viên Trường Đại học Tài nguyên và Môi trường Hà Nội. Ghi danh, học theo lộ trình, làm bài kiểm
            tra và nhận chứng chỉ khi hoàn thành.
          </p>
        </div>
        <div className="pointer-events-none absolute -right-16 -top-16 h-64 w-64 rounded-full bg-white/10" />
        <div className="pointer-events-none absolute -bottom-20 right-24 h-48 w-48 rounded-full bg-white/10" />
      </section>

      <CatalogFilters categories={categories.data ?? []} {...filters} />

      {courses.error !== null ? (
        <Alert>Không tải được danh sách khóa học: {courses.error}</Alert>
      ) : courses.data.content.length === 0 ? (
        <Empty>
          <p>Không tìm thấy khóa học nào phù hợp.</p>
          {(keyword || categoryId || level) && (
            <LinkButton href="/" variant="secondary" className="mt-4">
              Xem tất cả khóa học
            </LinkButton>
          )}
        </Empty>
      ) : (
        <>
          <p className="text-sm text-slate-500">{courses.data.totalElements} khóa học</p>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {courses.data.content.map((c) => (
              <CourseCard key={c.id} course={c} />
            ))}
          </div>
          <Pagination page={courses.data.page} totalPages={courses.data.totalPages} hrefFor={hrefFor} />
        </>
      )}
    </div>
  );
}
