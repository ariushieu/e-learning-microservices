import { AwardIcon } from "lucide-react";
import type { ReactNode } from "react";
import { Badge } from "@/components/ui/badge";
import { label } from "@/lib/format";
import { cn } from "@/lib/utils";

export type Tone = "neutral" | "primary" | "info" | "success" | "warning" | "danger" | "achievement";

/** Màu nền nhạt + chữ đậm cho từng tông; dùng chung cho mọi huy hiệu, ô icon, ô thông báo. */
export const TONE_CLASSES: Record<Tone, { soft: string; dot: string }> = {
  neutral: { soft: "bg-muted text-muted-foreground", dot: "bg-muted-foreground/60" },
  primary: { soft: "bg-primary-soft text-primary-strong", dot: "bg-primary" },
  info: { soft: "bg-info-soft text-info-strong", dot: "bg-info" },
  success: { soft: "bg-success-soft text-success-strong", dot: "bg-success" },
  warning: { soft: "bg-warning-soft text-warning-strong", dot: "bg-warning" },
  danger: { soft: "bg-destructive-soft text-destructive-strong", dot: "bg-destructive" },
  achievement: { soft: "bg-achievement-soft text-achievement-strong", dot: "bg-achievement" },
};

/**
 * Trạng thái backend → tông màu. Hoàn thành / đạt dùng vàng thành tích (có icon huy chương),
 * không dùng xanh lá để khỏi lẫn với màu chính. Trạng thái mới chưa có ở đây hiện màu xám.
 */
export const STATUS_TONES: Record<string, Tone> = {
  PUBLISHED: "success",
  SUBMITTED: "success",
  ACTIVE: "info",
  IN_PROGRESS: "info",
  COMPLETED: "achievement",
  PASSED: "achievement",
  DRAFT: "neutral",
  ARCHIVED: "neutral",
  PENDING_REVIEW: "warning",
  EXPIRED: "warning",
  CANCELLED: "danger",
  FAILED: "danger",
};

/** Huy hiệu trạng thái cho mọi enum (khóa học, ghi danh, bài kiểm tra, lượt làm). */
export function StatusBadge({ status, className, children }: { status: string; className?: string; children?: ReactNode }) {
  const tone = STATUS_TONES[status] ?? "neutral";
  return (
    <Badge variant="outline" className={cn("gap-1.5 border-0 font-medium", TONE_CLASSES[tone].soft, className)}>
      {tone === "achievement" ? (
        <AwardIcon className="size-3" aria-hidden />
      ) : (
        <span className={cn("size-1.5 rounded-full", TONE_CLASSES[tone].dot)} aria-hidden />
      )}
      {children ?? label(status)}
    </Badge>
  );
}
