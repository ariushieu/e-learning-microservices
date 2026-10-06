import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * Họa tiết đường đồng mức (bản đồ địa hình) — dấu ấn tài nguyên – môi trường. Chỉ để trang trí,
 * đặt trong khối `relative overflow-hidden`; màu theo `currentColor`.
 */
export function ContourPattern({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 400 200" preserveAspectRatio="xMidYMid slice" fill="none" stroke="currentColor" aria-hidden className={cn("pointer-events-none absolute inset-0 size-full", className)}>
      <path d="M-20 160c40-30 80-34 120-20s70 18 110-6 90-40 130-22 60 24 80 18" />
      <path d="M-20 130c45-28 85-30 125-16s72 14 108-10 88-36 128-20 62 22 79 16" />
      <path d="M-20 100c48-26 90-26 128-12s70 10 104-14 84-30 126-16 64 18 82 12" />
      <path d="M-20 70c50-22 92-22 130-8s68 6 100-16 80-26 122-12 66 14 88 8" />
      <path d="M-20 40c52-20 94-18 132-6s66 2 96-18 76-22 118-10 68 10 94 4" />
      <path d="M210 120c18-14 40-16 56-6s22 26 6 34-44 6-58-6-12-16-4-22Z" />
      <path d="M226 122c10-7 22-8 31-2s12 15 3 19-24 3-32-3-7-9-2-14Z" />
    </svg>
  );
}

/** Phần tử trải hết chiều ngang trong layout (site). Nội dung bên trong vẫn canh theo cột 72rem. */
export function FullBleed({ className, inner, children }: { className?: string; inner?: string; children: ReactNode }) {
  return (
    <div className={cn("col-span-full! col-start-1!", className)}>
      <div className={cn("mx-auto w-full max-w-6xl px-4 sm:px-6", inner)}>{children}</div>
    </div>
  );
}
