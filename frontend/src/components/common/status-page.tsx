import Link from "next/link";
import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { ContourPattern } from "./decor";

/** Trang báo lỗi toàn màn hình: 404, 403, lỗi tải trang. Mã lớn, một câu nói rõ chuyện gì, một nút làm tiếp. */
export function StatusPage({ code, title, description, action }: { code: string; title: string; description: string; action?: ReactNode }) {
  return (
    <div className="relative isolate flex min-h-[60svh] flex-col items-center justify-center overflow-hidden px-6 py-16 text-center">
      {/* Họa tiết đồng mức mờ dần ra mép để không tranh chỗ với chữ. */}
      <ContourPattern className="-z-10 text-primary/10 [mask-image:radial-gradient(closest-side,black,transparent)]" />
      <p className="text-display text-primary tabular-nums">{code}</p>
      <h1 className="mt-4 text-title">{title}</h1>
      <p className="mt-3 max-w-md break-words text-muted-foreground">{description}</p>
      <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
        {action ?? (
          <Button asChild size="lg">
            <Link href="/">Về trang chủ</Link>
          </Button>
        )}
      </div>
    </div>
  );
}
