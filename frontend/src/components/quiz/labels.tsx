import type { AttemptStatus, QuestionType } from "@/lib/types";

export const questionTypeLabels: Record<QuestionType, string> = {
  SINGLE_CHOICE: "Một đáp án",
  MULTIPLE_CHOICE: "Nhiều đáp án",
  TRUE_FALSE: "Đúng / Sai",
};

export function QuestionTypeTag({ type }: { type: QuestionType }) {
  return (
    <span className="inline-block rounded-full bg-indigo-50 px-2.5 py-0.5 text-xs font-medium text-indigo-700">
      {questionTypeLabels[type] ?? type}
    </span>
  );
}

const attemptStatus: Record<AttemptStatus, { label: string; color: string }> = {
  IN_PROGRESS: { label: "Đang làm", color: "bg-sky-100 text-sky-700" },
  SUBMITTED: { label: "Đã nộp", color: "bg-emerald-100 text-emerald-700" },
  EXPIRED: { label: "Hết giờ", color: "bg-rose-100 text-rose-700" },
};

export function AttemptStatusBadge({ status }: { status: AttemptStatus }) {
  const s = attemptStatus[status] ?? { label: status, color: "bg-slate-100 text-slate-600" };
  return <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${s.color}`}>{s.label}</span>;
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
