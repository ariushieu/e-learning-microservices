import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "@/lib/utils";
import { IconTile } from "./icon-tile";
import { TONE_CLASSES, type Tone } from "./status-badge";

/**
 * Khối thông báo nổi bật trong trang: chúc mừng hoàn thành, nhắc ghi danh, cảnh báo hết hạn...
 * Khác ErrorAlert (lỗi API) và toast (phản hồi sau khi bấm).
 */
export function Callout({
  icon,
  tone = "info",
  title,
  children,
  action,
  className,
}: {
  icon: LucideIcon;
  tone?: Tone;
  title: ReactNode;
  children?: ReactNode;
  action?: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("flex flex-col gap-4 rounded-xl p-4 sm:flex-row sm:items-center", TONE_CLASSES[tone].soft, className)}>
      <IconTile icon={icon} tone={tone} className="bg-card/70" />
      <div className="min-w-0 flex-1">
        <p className="font-semibold">{title}</p>
        {children && <div className="text-sm opacity-90">{children}</div>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}
