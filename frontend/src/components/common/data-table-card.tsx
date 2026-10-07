import type { ReactNode } from "react";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

/**
 * Bảng dữ liệu đặt trong thẻ, có thanh tiêu đề tùy chọn. Bên trong dùng nguyên các phần tử
 * Table/TableHeader/TableRow... của shadcn. Không có dòng nào thì truyền `empty` (thường là EmptyState).
 */
export function DataTableCard({
  title,
  actions,
  empty,
  isEmpty,
  className,
  children,
}: {
  title?: ReactNode;
  actions?: ReactNode;
  empty?: ReactNode;
  isEmpty?: boolean;
  className?: string;
  children: ReactNode;
}) {
  if (isEmpty && empty) return <>{empty}</>;
  return (
    <Card className={cn("gap-0 py-0", className)}>
      {(title || actions) && (
        <div className="flex flex-wrap items-center justify-between gap-3 border-b px-4 py-3">
          {title && <h3 className="text-subheading">{title}</h3>}
          {actions && <div className="flex items-center gap-2">{actions}</div>}
        </div>
      )}
      <div className="[&_td:first-child]:pl-4 [&_td:last-child]:pr-4 [&_th]:h-10 [&_th]:text-xs [&_th]:font-medium [&_th]:text-muted-foreground [&_th:first-child]:pl-4 [&_th:last-child]:pr-4 [&_thead_tr]:bg-muted/40 [&_thead_tr]:hover:bg-muted/40">
        {children}
      </div>
    </Card>
  );
}
