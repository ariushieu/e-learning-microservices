// Định dạng hiển thị dùng chung cho mọi trang.

/**
 * Giờ hiển thị luôn theo giờ Việt Nam. Không cố định thì trang render ở server (container Docker
 * chạy giờ UTC) lệch 7 tiếng so với trình duyệt, React báo lỗi hydration và người dùng thấy sai giờ.
 */
export const TIME_ZONE = "Asia/Ho_Chi_Minh";

export function formatPrice(price?: number | string | null) {
  const n = Number(price ?? 0);
  return n === 0 ? "Miễn phí" : `${n.toLocaleString("vi-VN")} ₫`;
}

export function formatDate(value?: string | null) {
  if (!value) return "";
  return new Date(value).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short", timeZone: TIME_ZONE });
}

export function formatDay(value?: string | null) {
  if (!value) return "";
  return new Date(value).toLocaleDateString("vi-VN", { dateStyle: "medium", timeZone: TIME_ZONE });
}

export function formatDuration(seconds?: number | null) {
  const s = Number(seconds ?? 0);
  if (s < 60) return `${s} giây`;
  const m = Math.round(s / 60);
  return m < 60 ? `${m} phút` : `${Math.floor(m / 60)} giờ ${m % 60} phút`;
}

export function formatNumber(value?: number | string | null, fractionDigits = 0) {
  return Number(value ?? 0).toLocaleString("vi-VN", { maximumFractionDigits: fractionDigits });
}

/** Nhãn tiếng Việt cho các giá trị enum của backend. */
export const LABELS: Record<string, string> = {
  PUBLISHED: "Đã xuất bản",
  DRAFT: "Bản nháp",
  PENDING_REVIEW: "Chờ duyệt",
  ARCHIVED: "Đã lưu trữ",
  ACTIVE: "Đang học",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
  IN_PROGRESS: "Đang làm",
  SUBMITTED: "Đã nộp",
  EXPIRED: "Hết giờ",
  BEGINNER: "Cơ bản",
  INTERMEDIATE: "Trung cấp",
  ADVANCED: "Nâng cao",
  VIDEO: "Video",
  ARTICLE: "Bài đọc",
  FILE: "Tài liệu",
  QUIZ: "Bài kiểm tra",
  SINGLE_CHOICE: "Một đáp án",
  MULTIPLE_CHOICE: "Nhiều đáp án",
  TRUE_FALSE: "Đúng / Sai",
  ROLE_STUDENT: "Học viên",
  ROLE_INSTRUCTOR: "Giảng viên",
  ROLE_ADMIN: "Quản trị viên",
};

export function label(value?: string | null) {
  return value ? (LABELS[value] ?? value) : "";
}

/** Chữ cái đầu của họ tên, dùng cho avatar. */
export function initials(name?: string | null) {
  const parts = (name ?? "").trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  const last = parts[parts.length - 1][0] ?? "";
  return (parts.length > 1 ? parts[0][0] + last : last).toUpperCase();
}
