import Link from "next/link";
import { Input, Select } from "@/components/ui";
import type { Category, CourseLevel } from "@/lib/types";

export const levels: { value: CourseLevel; label: string }[] = [
  { value: "BEGINNER", label: "Cơ bản" },
  { value: "INTERMEDIATE", label: "Trung cấp" },
  { value: "ADVANCED", label: "Nâng cao" },
];

/** Form GET thuần: lọc được cả khi trình duyệt tắt JavaScript. */
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
    <form method="get" action="/" className="grid gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[1fr_220px_160px_auto]">
      <Input type="search" name="keyword" defaultValue={keyword} placeholder="Tìm khóa học theo tên, nội dung..." aria-label="Từ khóa" />
      <Select name="categoryId" defaultValue={categoryId} aria-label="Danh mục">
        <option value="">Tất cả danh mục</option>
        {categories.map((c) => (
          <CategoryOptions key={c.id} category={c} />
        ))}
      </Select>
      <Select name="level" defaultValue={level} aria-label="Trình độ">
        <option value="">Mọi trình độ</option>
        {levels.map((l) => (
          <option key={l.value} value={l.value}>
            {l.label}
          </option>
        ))}
      </Select>
      <div className="flex gap-2">
        <button className="flex-1 rounded-lg bg-indigo-600 px-5 py-2 text-sm font-medium text-white hover:bg-indigo-700">Tìm</button>
        {filtered && (
          <Link href="/" className="rounded-lg px-3 py-2 text-sm text-slate-600 hover:bg-slate-100">
            Xóa lọc
          </Link>
        )}
      </div>
    </form>
  );
}

function CategoryOptions({ category }: { category: Category }) {
  return (
    <>
      <option value={category.id}>{category.name}</option>
      {category.subCategories?.map((sub) => (
        <option key={sub.id} value={sub.id}>
          {"   — "}
          {sub.name}
        </option>
      ))}
    </>
  );
}
