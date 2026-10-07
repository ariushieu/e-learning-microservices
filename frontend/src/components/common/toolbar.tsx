import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/** Thanh công cụ trên danh sách: tìm kiếm, bộ lọc bên trái; sắp xếp, số kết quả bên phải. */
export function Toolbar({ start, end, className }: { start?: ReactNode; end?: ReactNode; className?: string }) {
  return (
    <div className={cn("mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between", className)}>
      <div className="flex min-w-0 flex-1 flex-wrap items-center gap-2">{start}</div>
      {end && <div className="flex shrink-0 items-center gap-2 text-sm text-muted-foreground">{end}</div>}
    </div>
  );
}
