import { StatusBadge } from "@/components/common/status-badge";
import { Badge } from "@/components/ui/badge";
import { label } from "@/lib/format";
import type { AttemptStatus, QuestionType } from "@/lib/types";

export const questionTypeLabels: Record<QuestionType, string> = {
  SINGLE_CHOICE: label("SINGLE_CHOICE"),
  MULTIPLE_CHOICE: label("MULTIPLE_CHOICE"),
  TRUE_FALSE: label("TRUE_FALSE"),
};

export function QuestionTypeTag({ type }: { type: QuestionType }) {
  return <Badge variant="secondary">{questionTypeLabels[type] ?? type}</Badge>;
}

export function AttemptStatusBadge({ status }: { status: AttemptStatus }) {
  return <StatusBadge status={status} />;
}

/** Điểm phần trăm, BigDecimal bên Java có thể về dạng số hoặc chuỗi. */
export function formatScore(value?: number | string | null) {
  return `${Number(value ?? 0).toFixed(2)}%`;
}

/** Số điểm của một câu, bỏ phần thập phân thừa (1.00 → 1). */
export function formatPoints(value?: number | string | null) {
  return Number(value ?? 0).toLocaleString("vi-VN", { maximumFractionDigits: 2 });
}

export function formatTimeLimit(minutes?: number | null) {
  return minutes ? `${minutes} phút` : "Không giới hạn";
}

export function formatMaxAttempts(max: number) {
  return max > 0 ? `${max} lần` : "Không giới hạn";
}

/** "mm:ss" hoặc "h:mm:ss" cho đồng hồ đếm ngược. */
export function formatClock(totalSeconds: number) {
  const s = Math.max(0, Math.floor(totalSeconds));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  const mm = String(m).padStart(h > 0 ? 2 : 1, "0");
  const ss = String(sec).padStart(2, "0");
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
}
