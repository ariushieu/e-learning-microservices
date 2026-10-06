/** Thông báo lỗi ngay dưới một ô nhập. */
export function FieldError({ message }: { message?: string | null }) {
  return message ? <p className="text-sm text-destructive">{message}</p> : null;
}
