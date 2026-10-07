import Link from "next/link";
import { cn } from "@/lib/utils";

/** Biểu tượng: mũ tốt nghiệp trên hai lá non — học tập gắn với tài nguyên, môi trường. */
export function BrandMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" className={className} aria-hidden>
      <path d="M2.5 8.5 12 4.5l9.5 4-9.5 4-9.5-4Z" />
      <path d="M19.5 9.5V13" />
      <path d="M12 12.5v7" />
      <path d="M12 19.5c-3.3 0-5.5-2-5.5-5.2 2.6 0 4.6 1.2 5.5 3.2" />
      <path d="M12 17.5c.9-2 2.9-3.2 5.5-3.2 0 3.2-2.2 5.2-5.5 5.2" />
    </svg>
  );
}

/**
 * Logo + tên. `tone="inverse"` dùng trên nền xanh đậm (sidebar, trang đăng nhập).
 * Hai dòng cố định nên không bao giờ tự xuống dòng trên điện thoại.
 */
export function Brand({ href = "/", tone = "default", className }: { href?: string; tone?: "default" | "inverse"; className?: string }) {
  const inverse = tone === "inverse";
  return (
    <Link href={href} className={cn("flex shrink-0 items-center gap-2.5 rounded-lg outline-none focus-visible:ring-3 focus-visible:ring-ring/50", className)}>
      <span
        className={cn(
          "flex size-9 items-center justify-center rounded-xl",
          inverse ? "bg-white/12 text-white ring-1 ring-white/15" : "bg-primary text-primary-foreground shadow-xs",
        )}
      >
        <BrandMark className="size-5.5" />
      </span>
      <span className="flex flex-col leading-none whitespace-nowrap">
        <span className={cn("text-[15px] font-bold tracking-tight", inverse ? "text-white" : "text-foreground")}>HUNRE</span>
        <span className={cn("mt-1 text-[11px] font-medium tracking-wide", inverse ? "text-white/70" : "text-muted-foreground")}>E-Learning</span>
      </span>
    </Link>
  );
}
