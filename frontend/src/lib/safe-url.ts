/** Link tài liệu chỉ nhận HTTP(S), không chứa thông tin đăng nhập. */
export function safeUrl(
  raw: string | null | undefined,
  { allowInternal = false }: { allowInternal?: boolean } = {},
): string | null {
  if (!raw) return null;
  const value = raw.trim();
  // Tránh trình duyệt biến dấu gạch ngược hoặc ký tự điều khiển thành URL khác.
  if (/[\\\u0000-\u001f\u007f]/.test(value)) return null;
  if (allowInternal && value.startsWith("/") && !value.startsWith("//")) return value;
  if (!/^https?:\/\//i.test(value)) return null;
  try {
    const url = new URL(value);
    return url.hostname && !url.username && !url.password ? url.toString() : null;
  } catch {
    return null;
  }
}
