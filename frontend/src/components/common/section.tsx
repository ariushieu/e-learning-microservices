import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * Một phần trong trang: tiêu đề (kèm số đếm), mô tả, nút bên phải, rồi nội dung.
 * Thay cho việc tự viết <h2 className="text-lg font-semibold"> ở từng trang.
 */
export function Section({
  title,
  description,
  count,
  actions,
  id,
  className,
  children,
}: {
  title: ReactNode;
  description?: ReactNode;
  count?: number;
  actions?: ReactNode;
  id?: string;
  className?: string;
  children: ReactNode;
}) {
  return (
    <section id={id} className={cn("scroll-mt-24 space-y-4", className)}>
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="min-w-0 space-y-1">
          <h2 className="flex items-center gap-2 text-heading">
            {title}
            {count !== undefined && (
              <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground tabular-nums">{count}</span>
            )}
          </h2>
          {description && <p className="text-sm text-muted-foreground">{description}</p>}
        </div>
        {actions && <div className="flex min-w-0 flex-wrap items-center gap-2 max-sm:w-full">{actions}</div>}
      </div>
      {children}
    </section>
  );
}
