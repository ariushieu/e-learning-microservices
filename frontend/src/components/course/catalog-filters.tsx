import { SearchIcon, XIcon } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { label } from "@/lib/format";
import type { Category, CourseLevel } from "@/lib/types";
import { cn } from "@/lib/utils";
import { AutoSubmitSelect } from "./auto-submit-select";
import { COURSE_LEVELS } from "./form-helpers";

export const levels: { value: CourseLevel; label: string }[] = COURSE_LEVELS.map((l) => ({ value: l, label: label(l) }));

export const catalogSorts = [
  { value: "createdAt,desc", label: "Mới nhất" },
  { value: "studentCount,desc", label: "Nhiều học viên" },
  { value: "price,asc", label: "Giá thấp → cao" },
  { value: "price,desc", label: "Giá cao → thấp" },
] as const;

/**
 * Chỗ chọn danh mục duy nhất của trang: hàng chip danh mục cha, và hàng chip danh mục con khi
 * danh mục cha đang chọn có con. Đổi danh mục giữ bộ lọc và cách sắp xếp, nhưng quay về trang đầu.
 */
export function CategoryChips({ categories, categoryId, keyword, level, sort }: {
  categories: Category[]; categoryId: string; keyword: string; level: string; sort: string;
}) {
  const activeParent = categories.find((category) =>
    String(category.id) === categoryId || category.subCategories.some((child) => String(child.id) === categoryId)
  );
  const activeChipId = activeParent === undefined ? categoryId : String(activeParent.id);
  const hrefFor = (id: string) => {
    const query = new URLSearchParams();
    for (const [key, value] of Object.entries({ keyword, level, sort, categoryId: id })) {
      if (value) query.set(key, value);
    }
    const s = query.toString();
    return s ? `/?${s}` : "/";
  };
  const chip = (selected: boolean, small = false) =>
    cn("shrink-0 rounded-full border font-medium outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
      small ? "px-3 py-1 text-xs" : "px-4 py-2 text-sm",
      selected ? "border-primary/20 bg-primary-soft text-primary-strong" : "border-border bg-card text-muted-foreground hover:bg-muted hover:text-foreground");
  const children = activeParent?.subCategories ?? [];
  return (
    <div className="mb-2 space-y-1">
      <nav aria-label="Lọc nhanh theo danh mục" className="flex min-w-0 max-w-full gap-2 overflow-x-auto py-2">
        {[{ id: "", name: "Tất cả" }, ...categories.map((c) => ({ id: String(c.id), name: c.name }))].map((c) => (
          <Link key={c.id} href={hrefFor(c.id)} aria-current={activeChipId === c.id ? "true" : undefined} className={chip(activeChipId === c.id)}>
            {c.name}
          </Link>
        ))}
      </nav>
      {activeParent && children.length > 0 && (
        <nav aria-label={`Danh mục con của ${activeParent.name}`} className="flex min-w-0 max-w-full flex-wrap gap-2 pb-2">
          <Link href={hrefFor(String(activeParent.id))} aria-current={categoryId === String(activeParent.id) ? "true" : undefined}
            className={chip(categoryId === String(activeParent.id), true)}>
            Tất cả {activeParent.name}
          </Link>
          {children.map((child) => (
            <Link key={child.id} href={hrefFor(String(child.id))} aria-current={categoryId === String(child.id) ? "true" : undefined}
              className={chip(categoryId === String(child.id), true)}>
              {child.name}
            </Link>
          ))}
        </nav>
      )}
    </div>
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

/**
 * Trình độ và sắp xếp; đặt trong <Toolbar>. Danh mục chọn bằng chip ở trên nên ở đây chỉ giữ
 * giá trị đang chọn. Đổi ô chọn là áp dụng ngay; nút "Lọc" chỉ hiện khi trình duyệt tắt JavaScript.
 */
export function CatalogFilters({
  keyword,
  categoryId,
  level,
  sort,
}: {
  keyword: string;
  categoryId: string;
  level: string;
  sort: string;
}) {
  const filtered = keyword || categoryId || level;
  return (
    <>
      {categoryId && <input type="hidden" form={FORM_ID} name="categoryId" value={categoryId} />}
      <div className="w-full sm:w-44">
        <AutoSubmitSelect key={level} form={FORM_ID} name="level" defaultValue={level} aria-label="Trình độ">
          <option value="">Mọi trình độ</option>
          {levels.map((l) => (
            <option key={l.value} value={l.value}>
              {l.label}
            </option>
          ))}
        </AutoSubmitSelect>
      </div>
      <div className="w-full sm:w-48">
        <AutoSubmitSelect key={sort} form={FORM_ID} name="sort" defaultValue={sort} aria-label="Sắp xếp">
          {catalogSorts.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
        </AutoSubmitSelect>
      </div>
      <noscript>
        <Button type="submit" form={FORM_ID} variant="outline">
          Lọc
        </Button>
      </noscript>
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
