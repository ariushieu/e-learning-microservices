import { ChevronLeftIcon } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";
import { ProgressMeter } from "@/components/common/progress-meter";
import { UserMenu } from "@/components/layout/user-menu";
import { NotificationBell } from "@/components/notification/notification-bell";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";

/**
 * Khuôn TẬP TRUNG (học bài, làm bài kiểm tra): không menu chung, chỉ thanh trên cùng (quay lại,
 * tên, tiến độ, hành động) + nội dung giữa + bảng phụ bên phải (đề cương, danh sách câu hỏi).
 * Màn nhỏ ẩn bảng phụ; trang tự đặt nút mở ngăn kéo qua `mobileBar`.
 * Dùng trong route group (learn), nơi layout không có header/footer chung.
 */
export function FocusLayout({
  back,
  title,
  progress,
  actions,
  panel,
  mobileBar,
  children,
}: {
  back: { href: string; label: string };
  title: ReactNode;
  /** Tiến độ ở thanh trên (desktop) và đầu nội dung (điện thoại). */
  progress?: { value: number; summary: string };
  /** Nút bên phải thanh trên (ví dụ Nộp bài). Mặc định là chuông + menu tài khoản. */
  actions?: ReactNode;
  panel?: { title: ReactNode; subtitle?: ReactNode; content: ReactNode };
  mobileBar?: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="flex min-h-svh flex-col bg-background">
      <header className="sticky top-0 z-40 flex h-16 shrink-0 items-center gap-3 border-b bg-card px-3 sm:px-4">
        <Button asChild variant="ghost" size="lg" className="shrink-0 px-2">
          <Link href={back.href} aria-label={back.label}>
            <ChevronLeftIcon />
            <span className="hidden sm:inline">{back.label}</span>
          </Link>
        </Button>
        <Separator orientation="vertical" className="h-6! self-center" />
        <h1 className="min-w-0 flex-1 truncate text-sm font-semibold sm:text-base">{title}</h1>
        {progress && (
          <div className="hidden shrink-0 items-center gap-3 md:flex">
            <ProgressMeter value={progress.value} label={null} className="w-36" />
            <span className="text-xs text-muted-foreground tabular-nums">{progress.summary}</span>
          </div>
        )}
        <div className="flex shrink-0 items-center gap-1.5">
          {actions ?? (
            <>
              <NotificationBell />
              <UserMenu />
            </>
          )}
        </div>
      </header>

      <div className="flex flex-1">
        <main className="min-w-0 flex-1">
          <div className="mx-auto max-w-5xl space-y-8 px-4 py-6 sm:px-6 lg:px-10 lg:py-8">
            {(progress || mobileBar) && (
              <div className="flex items-center justify-between gap-3 lg:hidden">
                {progress && <ProgressMeter value={progress.value} label="Tiến độ" detail={progress.summary} className="min-w-0 flex-1 md:hidden" />}
                {mobileBar && <div className="ml-auto">{mobileBar}</div>}
              </div>
            )}
            {children}
          </div>
        </main>

        {panel && (
          <aside className="sticky top-16 hidden h-[calc(100svh-4rem)] w-80 shrink-0 flex-col border-l bg-card lg:flex">
            <div className="border-b px-4 py-3.5">
              <p className="text-subheading">{panel.title}</p>
              {panel.subtitle && <p className="text-xs text-muted-foreground tabular-nums">{panel.subtitle}</p>}
            </div>
            <ScrollArea className="min-h-0 flex-1">{panel.content}</ScrollArea>
          </aside>
        )}
      </div>
    </div>
  );
}
