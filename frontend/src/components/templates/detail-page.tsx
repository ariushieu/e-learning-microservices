import type { ReactNode } from "react";
import { ContourPattern, FullBleed } from "@/components/common/decor";
import { Crumbs, type Crumb } from "@/components/common/page-header";
import { cn } from "@/lib/utils";

/**
 * Dải tiêu đề của trang CHI TIẾT: nền xanh lá đậm trải hết chiều ngang, họa tiết đồng mức,
 * đường dẫn, tiêu đề, mô tả, dòng thông tin (meta) và ảnh/khối bên phải (media).
 * Chỉ dùng trong layout (site), và phải là phần tử đầu tiên của trang (tự kéo sát header).
 */
export function DetailHero({
  crumbs,
  eyebrow,
  title,
  description,
  meta,
  actions,
  media,
}: {
  crumbs?: Crumb[];
  eyebrow?: ReactNode;
  title: ReactNode;
  description?: ReactNode;
  /** Các <HeroMeta>: giảng viên, thời lượng, số bài... */
  meta?: ReactNode;
  actions?: ReactNode;
  media?: ReactNode;
}) {
  return (
    <FullBleed className="relative isolate -mt-10 mb-10 overflow-hidden bg-sidebar text-white" inner="relative py-10 lg:py-14">
      <ContourPattern className="-z-10 text-white/[0.07]" />
      <div className={cn("grid items-center gap-8", media && "lg:grid-cols-[minmax(0,1fr)_22rem]")}>
        <div className="min-w-0 space-y-4">
          {crumbs && <Crumbs items={crumbs} tone="inverse" />}
          {eyebrow && <div className="text-eyebrow text-sidebar-primary">{eyebrow}</div>}
          <h1 className="text-display text-white">{title}</h1>
          {description && <p className="max-w-2xl text-lg leading-relaxed text-white/80">{description}</p>}
          {meta && <div className="flex flex-wrap items-center gap-x-5 gap-y-2 text-sm text-white/80 [&_svg]:size-4 [&_svg]:text-white/60">{meta}</div>}
          {actions && <div className="flex flex-wrap items-center gap-3 pt-2">{actions}</div>}
        </div>
        {media && <div className="hidden overflow-hidden rounded-xl ring-1 ring-white/15 lg:block">{media}</div>}
      </div>
    </FullBleed>
  );
}

/** Một mục meta trong DetailHero: icon + chữ. */
export function HeroMeta({ icon, children }: { icon?: ReactNode; children: ReactNode }) {
  return (
    <span className="inline-flex items-center gap-1.5">
      {icon}
      {children}
    </span>
  );
}

/**
 * Khuôn trang CHI TIẾT: DetailHero (truyền qua `hero`) → cột nội dung chính + cột phụ dính (sticky)
 * bên phải (giá, nút ghi danh, tóm tắt). Trên điện thoại cột phụ lên trước nội dung chính.
 * Dùng cho: chi tiết khóa học, giới thiệu bài kiểm tra, kết quả lượt làm.
 */
export function DetailPage({ hero, aside, children }: { hero: ReactNode; aside?: ReactNode; children: ReactNode }) {
  return (
    <>
      {hero}
      <div className={cn("grid items-start gap-8", aside && "lg:grid-cols-[minmax(0,1fr)_22rem] lg:gap-10")}>
        <div className="order-2 min-w-0 space-y-10 lg:order-1">{children}</div>
        {aside && <aside className="order-1 space-y-4 lg:sticky lg:top-24 lg:order-2">{aside}</aside>}
      </div>
    </>
  );
}
