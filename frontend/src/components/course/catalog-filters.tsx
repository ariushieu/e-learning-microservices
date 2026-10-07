import { SearchIcon, XIcon } from "lucide-react";
import Link from "next/link";
import { NativeSelect } from "@/components/common/native-select";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { label } from "@/lib/format";
import type { Category, CourseLevel } from "@/lib/types";
import { cn } from "@/lib/utils";
import { CategoryOptions, COURSE_LEVELS } from "./form-helpers";

export const levels: { value: CourseLevel; label: string }[] = COURSE_LEVELS.map((l) => ({ value: l, label: label(l) }));

export const catalogSorts = [
  { value: "createdAt,desc", label: "Mới nhất" },
  { value: "studentCount,desc", label: "Nhiều học viên" },
  { value: "price,asc", label: "Giá thấp → cao" },
  { value: "price,desc", label: "Giá cao → thấp" },
] as const;

/** Đổi danh mục giữ bộ lọc và cách sắp xếp, nhưng quay về trang đầu. */
export function CategoryChips({ categories, categoryId, keyword, level, sort }: {
  categories: Category[]; categoryId: string; keyword: string; level: string; sort: string;
}) {
  const activeCategoryId = categories.find((category) =>
    String(category.id) === categoryId || category.subCategories.some((child) => String(child.id) === categoryId)
  )?.id;
  const activeChipId = activeCategoryId === undefined ? categoryId : String(activeCategoryId);
  return (
    <nav aria-label="Lọc nhanh theo danh mục" className="mb-4 flex min-w-0 max-w-full gap-2 overflow-x-auto py-2">
      {[{ id: "", name: "Tất cả" }, ...categories.map((c) => ({ id: String(c.id), name: c.name }))].map((c) => {
        const query = new URLSearchParams();
        for (const [key, value] of Object.entries({ keyword, level, sort, categoryId: c.id })) {
          if (value) query.set(key, value);
        }
        const selected = activeChipId === c.id;
        return (
          <Link key={c.id} href={`/?${query}`} aria-current={selected ? "true" : undefined}
            className={cn("shrink-0 rounded-full border px-4 py-2 text-sm font-medium outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
              selected ? "border-primary/20 bg-primary-soft text-primary-strong" : "border-border bg-card text-muted-foreground hover:bg-muted hover:text-foreground")}
          >{c.name}</Link>
        );
      })}
    </nav>
  );
}

const FORM_ID = "catalog-search";

/**
 * Ô tìm kiếm (trong dải hero) và bộ lọc (trên danh sách) nằm ở hai chỗ nhưng vẫn là MỘT form GET thuần:
 * ô lọc gắn vào form qua thuộc tính `form`, nên tìm kiếm giữ nguyên bộ lọc và ngược lại,
 * kể cả khi trình duyệt tắt JavaScript.
 */
export function CatalogSearch({ keyword }: { keyword: string }) {
  return (
    <form id={FORM_ID} method="get" action="/" role="search" className="flex max-w-2xl flex-col gap-2 sm:flex-row">
      <div className="relative flex-1">
        <SearchIcon className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden />
        <Input
          key={keyword}
          type="search"
          name="keyword"
          defaultValue={keyword}
          placeholder="Tìm khóa học theo tên, nội dung..."
          aria-label="Từ khóa"
          className="h-11 border-transparent bg-card pl-10 text-foreground shadow-raised pointer-coarse:h-11"
        />
      </div>
      <Button type="submit" variant="secondary" size="lg" className="h-11 px-5 pointer-coarse:h-11">
        Tìm kiếm
      </Button>
    </form>
  );
}

/** Danh mục (kèm danh mục con) và trình độ; đặt trong <Toolbar>. */
export function CatalogFilters({
  categories,
  keyword,
  categoryId,
  level,
  sort,
}: {
  categories: Category[];
  keyword: string;
  categoryId: string;
  level: string;
  sort: string;
}) {
  const filtered = keyword || categoryId || level;
  return (
    <>
      <div className="w-full sm:w-60">
        <NativeSelect key={categoryId} form={FORM_ID} name="categoryId" defaultValue={categoryId} aria-label="Danh mục">
          <option value="">Tất cả danh mục</option>
          <CategoryOptions tree={categories} />
        </NativeSelect>
      </div>
      <div className="w-full sm:w-44">
        <NativeSelect key={level} form={FORM_ID} name="level" defaultValue={level} aria-label="Trình độ">
          <option value="">Mọi trình độ</option>
          {levels.map((l) => (
            <option key={l.value} value={l.value}>
              {l.label}
            </option>
          ))}
        </NativeSelect>
      </div>
      <div className="w-full sm:w-48">
        <NativeSelect key={sort} form={FORM_ID} name="sort" defaultValue={sort} aria-label="Sắp xếp">
          {catalogSorts.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
        </NativeSelect>
      </div>
      <Button type="submit" form={FORM_ID} variant="outline">
        Lọc
      </Button>
      {filtered && (
        <Button asChild variant="ghost">
          <Link href="/">
            <XIcon /> Xóa lọc
          </Link>
        </Button>
      )}
    </>
  );
}
