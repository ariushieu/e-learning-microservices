import { ApiError } from "@/lib/errors";
import type { Category, CourseLevel, LessonType } from "@/lib/types";

// Tiện ích dùng chung cho các form của giảng viên và quản trị (dùng được cả ở server lẫn client).

/** Gom lỗi theo trường từ ApiError (VALIDATION_FAILED) thành map field -> message. */
export function fieldErrorMap(e: unknown): Record<string, string> {
  const map: Record<string, string> = {};
  if (e instanceof ApiError) {
    for (const f of e.fieldErrors) map[f.field] = map[f.field] ? `${map[f.field]}. ${f.message}` : f.message;
  }
  return map;
}

/** Thông báo chung khi lỗi đã được hiển thị cạnh từng ô nhập. */
export function formErrorMessage(e: unknown): string {
  if (e instanceof ApiError) {
    if (e.fieldErrors.length) return "Dữ liệu chưa hợp lệ, xem lỗi ở từng ô bên dưới.";
    return e.message;
  }
  if (e instanceof Error) return e.message;
  return "Có lỗi xảy ra";
}

export function FieldError({ message }: { message?: string }) {
  return message ? <p className="mt-1 text-sm text-rose-600">{message}</p> : null;
}

/** Chuỗi rỗng thành undefined để JSON bỏ trường đó (backend lưu null thay vì ""). */
export function optional(value: FormDataEntryValue | null): string | undefined {
  const s = typeof value === "string" ? value.trim() : "";
  return s === "" ? undefined : s;
}

export function text(value: FormDataEntryValue | null): string {
  return typeof value === "string" ? value.trim() : "";
}

export function optionalNumber(value: FormDataEntryValue | null): number | undefined {
  const s = text(value);
  if (s === "") return undefined;
  const n = Number(s);
  return Number.isFinite(n) ? n : undefined;
}

const SUB_INDENT = `${String.fromCharCode(160).repeat(4)}↳ `;

/** Danh mục gốc và danh mục con (thụt lề) cho thẻ <select>. */
export function CategoryOptions({ tree }: { tree: Category[] }) {
  return (
    <>
      {tree.flatMap((root) => [
        <option key={root.id} value={root.id}>
          {root.name}
        </option>,
        // Trình duyệt gộp khoảng trắng thường trong <option>, nên thụt lề bằng khoảng trắng không ngắt.
        ...(root.subCategories ?? []).map((sub) => (
          <option key={sub.id} value={sub.id}>
            {SUB_INDENT}
            {sub.name}
          </option>
        )),
      ])}
    </>
  );
}

export const levelLabels: Record<CourseLevel, string> = {
  BEGINNER: "Cơ bản",
  INTERMEDIATE: "Trung cấp",
  ADVANCED: "Nâng cao",
};

export const lessonTypeLabels: Record<LessonType, string> = {
  VIDEO: "Video",
  ARTICLE: "Bài viết",
  FILE: "Tệp tài liệu",
  QUIZ: "Bài kiểm tra",
};

export function SectionHeading({ id, title, description }: { id?: string; title: string; description?: string }) {
  return (
    <div id={id} className="mb-4 scroll-mt-36">
      <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
      {description && <p className="mt-1 text-sm text-slate-500">{description}</p>}
    </div>
  );
}

export function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</div>
      <div className="mt-1 text-2xl font-bold text-slate-900">{value}</div>
    </div>
  );
}
