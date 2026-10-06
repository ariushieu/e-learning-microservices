"use client";

import { BellIcon } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { api } from "@/lib/client";
import { formatDate } from "@/lib/format";
import { safeNotificationHtml } from "@/lib/html";
import type { Notification, Page } from "@/lib/types";
import { cn } from "@/lib/utils";

/** Chuông trên header: số chưa đọc (hỏi lại mỗi 20 giây) và 5 thông báo mới nhất khi bấm. */
export function NotificationBell() {
  const pathname = usePathname();
  const [count, setCount] = useState(0);
  const [items, setItems] = useState<Notification[] | null>(null);

  useEffect(() => {
    let alive = true;
    const load = () =>
      api<number>("/api/notifications/unread-count")
        .then((n) => alive && setCount(n ?? 0))
        .catch(() => undefined);
    load();
    const timer = setInterval(load, 20_000);
    return () => {
      alive = false;
      clearInterval(timer);
    };
  }, [pathname]);

  function onOpenChange(open: boolean) {
    if (open) {
      api<Page<Notification>>("/api/notifications?size=5")
        .then((p) => setItems(p.content))
        .catch(() => setItems([]));
    }
  }

  return (
    <Popover onOpenChange={onOpenChange}>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon-lg" className="relative" aria-label="Thông báo">
          <BellIcon className="size-5" />
          {count > 0 && (
            <span className="absolute top-1 right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-semibold text-white">
              {count > 99 ? "99+" : count}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-80 p-0">
        <div className="flex items-center justify-between border-b px-4 py-3">
          <span className="font-medium">Thông báo</span>
          {count > 0 && <span className="text-xs text-muted-foreground">{count} chưa đọc</span>}
        </div>
        <div className="max-h-80 overflow-y-auto">
          {items === null ? (
            <p className="px-4 py-6 text-center text-sm text-muted-foreground">Đang tải…</p>
          ) : items.length === 0 ? (
            <p className="px-4 py-6 text-center text-sm text-muted-foreground">Chưa có thông báo nào.</p>
          ) : (
            items.map((n) => (
              <Link
                key={n.id}
                href="/notifications"
                className={cn("block border-b px-4 py-3 text-sm last:border-0 hover:bg-muted/60", !n.read && "bg-primary/5")}
              >
                <div className="font-medium">{n.title}</div>
                <p className="mt-0.5 line-clamp-2 text-muted-foreground" dangerouslySetInnerHTML={{ __html: safeNotificationHtml(n.content) }} />
                <div className="mt-1 text-xs text-muted-foreground">{formatDate(n.createdAt)}</div>
              </Link>
            ))
          )}
        </div>
        <div className="border-t p-2">
          <Button asChild variant="ghost" className="w-full">
            <Link href="/notifications">Xem tất cả</Link>
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  );
}
