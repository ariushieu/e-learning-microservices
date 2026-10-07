import Link from "next/link";
import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export type Crumb = { href?: string; label: string };

/** Đường dẫn ngắn phía trên tiêu đề: Khám phá › Danh mục › Khóa học. */
export function Crumbs({ items, tone = "default" }: { items: Crumb[]; tone?: "default" | "inverse" }) {
  const inverse = tone === "inverse";
  return (
    <nav aria-label="Đường dẫn" className="mb-3">
      <ol className={cn("flex flex-wrap items-center gap-1.5 text-sm", inverse ? "text-white/70" : "text-muted-foreground")}>
        {items.map((c, i) => (
          <li key={`${c.label}-${i}`} className="flex min-w-0 items-center gap-1.5">
            {i > 0 && <span aria-hidden>/</span>}
            {c.href ? (
              <Link href={c.href} className={cn("truncate transition-colors", inverse ? "hover:text-white" : "hover:text-foreground")}>
                {c.label}
              </Link>
            ) : (
              <span aria-current="page" className={cn("truncate", inverse ? "text-white" : "text-foreground")}>
                {c.label}
              </span>
            )}
          </li>
        ))}
      </ol>
    </nav>
  );
}

/** Tiêu đề trang: đường dẫn, nhãn nhỏ, tên, mô tả và nút hành động bên phải. */
export function PageHeader({
  title,
  description,
  actions,
  eyebrow,
  crumbs,
  className,
}: {
  title: ReactNode;
  description?: ReactNode;
  actions?: ReactNode;
  eyebrow?: ReactNode;
  crumbs?: Crumb[];
  className?: string;
}) {
  return (
    <div className={cn("mb-8", className)}>
      {crumbs && <Crumbs items={crumbs} />}
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="min-w-0 space-y-1.5">
          {eyebrow && <div className="text-eyebrow text-primary">{eyebrow}</div>}
          <h1 className="text-title">{title}</h1>
          {description && <p className="max-w-2xl text-muted-foreground">{description}</p>}
        </div>
        {actions && <div className="flex min-w-0 flex-wrap items-center gap-2 max-sm:w-full">{actions}</div>}
      </div>
    </div>
  );
}
