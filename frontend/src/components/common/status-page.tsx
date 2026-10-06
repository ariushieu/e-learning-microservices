import Link from "next/link";
import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";

/** Trang báo lỗi toàn màn hình: 404, 403, lỗi tải trang. */
export function StatusPage({ code, title, description, action }: { code: string; title: string; description: string; action?: ReactNode }) {
  return (
    <div className="flex min-h-[60svh] flex-col items-center justify-center px-6 text-center">
      <p className="text-sm font-semibold text-primary">{code}</p>
      <h1 className="mt-2 text-3xl font-semibold tracking-tight">{title}</h1>
      <p className="mt-3 max-w-md text-muted-foreground">{description}</p>
      <div className="mt-8">
        {action ?? (
          <Button asChild size="lg">
            <Link href="/">Về trang chủ</Link>
          </Button>
        )}
      </div>
    </div>
  );
}
