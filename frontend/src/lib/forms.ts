import { ApiError } from "./errors";

// Hàm dùng chung cho mọi form gọi API.

/** Lỗi theo từng ô từ `fieldErrors` của backend: { title: "...", price: "..." }. */
export function fieldErrorMap(e: unknown): Record<string, string> {
  const map: Record<string, string> = {};
  if (e instanceof ApiError) {
    for (const f of e.fieldErrors) map[f.field] = map[f.field] ? `${map[f.field]}. ${f.message}` : f.message;
  }
  return map;
}

/** Thông báo chung đặt trên đầu form. */
export function formErrorMessage(e: unknown): string {
  if (e instanceof ApiError) {
    if (e.fieldErrors.length) return "Dữ liệu chưa hợp lệ, xem lỗi ở từng ô bên dưới.";
    return e.message;
  }
  if (e instanceof Error) return e.message;
  return "Có lỗi xảy ra";
}

/** Chuỗi rỗng thành undefined, để backend dùng giá trị mặc định. */
export function optional(value: FormDataEntryValue | null): string | undefined {
  const s = typeof value === "string" ? value.trim() : "";
  return s ? s : undefined;
}

export function text(value: FormDataEntryValue | null): string {
  return typeof value === "string" ? value.trim() : "";
}

export function optionalNumber(value: FormDataEntryValue | null): number | undefined {
  const s = typeof value === "string" ? value.trim() : "";
  if (!s) return undefined;
  const n = Number(s);
  return Number.isFinite(n) ? n : undefined;
}
