import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "@/lib/utils";
import { IconTile } from "./icon-tile";

/** Chưa có dữ liệu: icon, một câu nói rõ chuyện gì, một câu hướng dẫn, và (nếu có) nút làm tiếp. */
export function EmptyState({
  icon,
  title,
  description,
  action,
  className,
}: {
  icon?: LucideIcon;
  title: string;
  description?: ReactNode;
  action?: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("flex flex-col items-center justify-center rounded-xl border border-dashed bg-card/60 px-6 py-14 text-center", className)}>
      {icon && <IconTile icon={icon} tone="primary" size="lg" className="mb-4" />}
      <h3 className="text-subheading">{title}</h3>
      {description && <p className="mt-1.5 max-w-sm text-sm text-muted-foreground">{description}</p>}
      {action && <div className="mt-6">{action}</div>}
    </div>
  );
}
