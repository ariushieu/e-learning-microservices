import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * Danh sách nhãn – giá trị (<dl>). `layout="grid"`: ô số liệu xếp lưới (kết quả bài kiểm tra);
 * `layout="rows"`: mỗi dòng nhãn trái, giá trị phải (hồ sơ, thẻ ghi danh).
 */
export function FactList({ layout = "rows", columns = 2, className, children }: { layout?: "rows" | "grid"; columns?: 2 | 3 | 4; className?: string; children: ReactNode }) {
  return (
    <dl
      data-layout={layout}
      className={cn(
        "group/facts text-sm",
        layout === "rows" ? "divide-y" : cn("grid gap-x-6 gap-y-5", { 2: "grid-cols-2", 3: "grid-cols-2 sm:grid-cols-3", 4: "grid-cols-2 sm:grid-cols-4" }[columns]),
        className,
      )}
    >
      {children}
    </dl>
  );
}

export function Fact({ label, value, hint, icon: Icon }: { label: ReactNode; value: ReactNode; hint?: ReactNode; icon?: LucideIcon }) {
  return (
    <div className="group-data-[layout=rows]/facts:flex group-data-[layout=rows]/facts:items-baseline group-data-[layout=rows]/facts:justify-between group-data-[layout=rows]/facts:gap-4 group-data-[layout=rows]/facts:py-2.5 group-data-[layout=rows]/facts:first:pt-0 group-data-[layout=rows]/facts:last:pb-0">
      <dt className="flex items-center gap-1.5 text-muted-foreground">
        {Icon && <Icon className="size-4" aria-hidden />}
        {label}
      </dt>
      <dd className="font-medium tabular-nums group-data-[layout=grid]/facts:mt-1 group-data-[layout=grid]/facts:text-lg group-data-[layout=grid]/facts:font-semibold group-data-[layout=rows]/facts:text-right">
        {value}
        {hint && <span className="block text-xs font-normal text-muted-foreground">{hint}</span>}
      </dd>
    </div>
  );
}
