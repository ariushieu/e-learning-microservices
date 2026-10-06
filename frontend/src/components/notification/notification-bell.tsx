"use client";

import { BellIcon } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { IconTile } from "@/components/common/icon-tile";
import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Skeleton } from "@/components/ui/skeleton";
import { api } from "@/lib/client";
import { formatDate } from "@/lib/format";
import { safeNotificationHtml } from "@/lib/html";
import type { Notification, Page } from "@/lib/types";
import { cn } from "@/lib/utils";
import { notificationKind } from "./notification-item";

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
            <span className="absolute top-0.5 right-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] leading-none font-semibold text-white tabular-nums ring-2 ring-card">
              {count > 99 ? "99+" : count}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-[min(22rem,calc(100vw-1rem))] gap-0 overflow-hidden p-0">
        <div className="flex items-center justify-between border-b px-4 py-3">
          <span className="text-subheading">Thông báo</span>
          {count > 0 && <span className="rounded-full bg-primary-soft px-2 py-0.5 text-xs font-medium text-primary-strong tabular-nums">{count} chưa đọc</span>}
        </div>
        <div className="max-h-96 overflow-y-auto">
          {items === null ? (
            <div className="space-y-4 p-4" role="status" aria-label="Đang tải">
              {[0, 1, 2].map((i) => (
                <div key={i} className="flex gap-3">
                  <Skeleton className="size-8 shrink-0 rounded-lg" />
                  <div className="flex-1 space-y-1.5">
                    <Skeleton className="h-4 w-3/4" />
                    <Skeleton className="h-3 w-full" />
                  </div>
                </div>
              ))}
            </div>
          ) : items.length === 0 ? (
            <div className="flex flex-col items-center gap-2 px-4 py-8 text-center">
              <IconTile icon={BellIcon} tone="neutral" />
              <p className="text-sm text-muted-foreground">Chưa có thông báo nào.</p>
            </div>
          ) : (
            <ul className="divide-y">
              {items.map((n) => {
                const kind = notificationKind(n.type);
                return (
                  <li key={n.id}>
                    <Link
                      href="/notifications"
                      className={cn("relative flex gap-3 px-4 py-3 text-sm transition-colors hover:bg-muted/60", !n.read && "bg-primary-soft/40")}
                    >
                      {!n.read && <span className="absolute top-4.5 left-1.5 size-1.5 rounded-full bg-primary" aria-label="Chưa đọc" />}
                      <IconTile icon={kind.icon} tone={kind.tone} size="sm" />
                      <div className="min-w-0 flex-1">
                        <p className={cn("line-clamp-1", n.read ? "font-medium" : "font-semibold")}>{n.title}</p>
                        <p className="mt-0.5 line-clamp-2 text-muted-foreground" dangerouslySetInnerHTML={{ __html: safeNotificationHtml(n.content) }} />
                        <time dateTime={n.createdAt} className="mt-1 block text-caption text-muted-foreground">
                          {formatDate(n.createdAt)}
                        </time>
                      </div>
                    </Link>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
        <div className="border-t p-2">
          <Button asChild variant="ghost" className="w-full text-primary">
            <Link href="/notifications">Xem tất cả</Link>
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  );
}
