import { SearchIcon, XIcon } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { label } from "@/lib/format";
import type { Category, CourseLevel } from "@/lib/types";
import { CategoryOptions, COURSE_LEVELS, NativeSelect } from "./form-helpers";

export const levels: { value: CourseLevel; label: string }[] = COURSE_LEVELS.map((l) => ({ value: l, label: label(l) }));

/**
 * Ô tìm kiếm và hàng bộ lọc (danh mục kèm danh mục con, trình độ) chung một form GET thuần,
 * nên lọc được cả khi trình duyệt tắt JavaScript.
 */
export function CatalogFilters({
  categories,
  keyword,
  categoryId,
  level,
}: {
  categories: Category[];
  keyword: string;
  categoryId: string;
  level: string;
}) {
  const filtered = keyword || categoryId || level;
  return (
    <form method="get" action="/" role="search" className="space-y-3">
      <div className="flex max-w-2xl gap-2">
        <div className="relative flex-1">
          <SearchIcon className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            type="search"
            name="keyword"
            defaultValue={keyword}
            placeholder="Tìm khóa học theo tên, nội dung..."
            aria-label="Từ khóa"
            className="h-10 pl-9"
          />
        </div>
        <Button type="submit" className="h-10 px-4">
          Tìm kiếm
        </Button>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <div className="w-full sm:w-60">
          <NativeSelect name="categoryId" defaultValue={categoryId} aria-label="Danh mục">
            <option value="">Tất cả danh mục</option>
            <CategoryOptions tree={categories} />
          </NativeSelect>
        </div>
        <div className="w-full sm:w-44">
          <NativeSelect name="level" defaultValue={level} aria-label="Trình độ">
            <option value="">Mọi trình độ</option>
            {levels.map((l) => (
              <option key={l.value} value={l.value}>
                {l.label}
              </option>
            ))}
          </NativeSelect>
        </div>
        <Button type="submit" variant="outline">
          Lọc
        </Button>
        {filtered && (
          <Button asChild variant="ghost">
            <Link href="/">
              <XIcon /> Xóa lọc
            </Link>
          </Button>
        )}
      </div>
    </form>
  );
}
