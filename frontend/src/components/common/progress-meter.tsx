import { Progress } from "@/components/ui/progress";
import { cn } from "@/lib/utils";

/**
 * Thanh tiến độ kèm nhãn và phần trăm. Nhận đúng `progressPercent` của backend (0–100, có thể lẻ),
 * tự làm tròn — trang không phải tự tính Math.round nữa.
 */
export function ProgressMeter({
  value,
  label = "Tiến độ",
  detail,
  size = "md",
  className,
}: {
  value: number | string | null | undefined;
  label?: string | null;
  detail?: string;
  size?: "sm" | "md";
  className?: string;
}) {
  const percent = Math.max(0, Math.min(100, Math.round(Number(value ?? 0))));
  return (
    <div className={cn("space-y-1.5", className)}>
      {(label || detail) && (
        <div className="flex items-baseline justify-between gap-3 text-xs">
          <span className="text-muted-foreground">{label}</span>
          <span className="font-medium tabular-nums">{detail ?? `${percent}%`}</span>
        </div>
      )}
      <Progress
        value={percent}
        aria-label={label ?? "Tiến độ"}
        className={cn(size === "sm" ? "h-1" : "h-1.5", percent === 100 && "[&>[data-slot=progress-indicator]]:bg-achievement")}
      />
    </div>
  );
}
