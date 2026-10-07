"use client";

import { BellIcon, CheckCheckIcon } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { toast } from "sonner";
import { IconTile } from "@/components/common/icon-tile";
import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Skeleton } from "@/components/ui/skeleton";
import { api } from "@/lib/client";
import { formatDate } from "@/lib/format";
import { notificationText, safeNotificationHtml } from "@/lib/html";
import type { Notification, Page } from "@/lib/types";
import { cn } from "@/lib/utils";
import { markNotificationRead, notificationHref, notificationKind } from "./notification-item";

// Mất luồng tức thời (mạng chập chờn, notification-service khởi động lại) thì hỏi số chưa đọc
// định kỳ như trước, tới khi trình duyệt tự nối lại được.
const FALLBACK_POLL_MS = 30_000;

/**
 * Chuông trên header: số chưa đọc và 5 thông báo mới nhất khi bấm.
 *
 * Số và thông báo mới đến qua luồng Server-Sent Events `/api/notifications/stream`: có thông báo
 * là chuông đổi số và hiện toast ngay, không phải chờ lần hỏi tiếp theo. Đọc ở tab khác thì số
 * ở tab này cũng giảm theo.
 */
export function NotificationBell() {
  const router = useRouter();
  const pathname = usePathname();
  const [count, setCount] = useState(0);
  const [items, setItems] = useState<Notification[] | null>(null);
  const [open, setOpen] = useState(false);

  // Luồng mở một lần cho cả phiên, không mở lại mỗi lần chuyển trang; trình xử lý sự kiện đọc
  // trang hiện tại qua ref.
  const pathnameRef = useRef(pathname);
  useEffect(() => {
    pathnameRef.current = pathname;
  }, [pathname]);

  useEffect(() => {
    let alive = true;
    let poll: ReturnType<typeof setInterval> | undefined;
    const loadCount = () =>
      api<number>("/api/notifications/unread-count")
        .then((n) => alive && setCount(n ?? 0))
        .catch(() => undefined);

    const source = new EventSource("/api/notifications/stream");
    source.addEventListener("unread-count", (e) => {
      setCount((JSON.parse((e as MessageEvent<string>).data) as { unreadCount: number }).unreadCount);
    });
    source.addEventListener("notification", (e) => {
      const n = JSON.parse((e as MessageEvent<string>).data) as Notification;
      setItems((prev) => (prev ? [n, ...prev.filter((x) => x.id !== n.id)].slice(0, 5) : prev));
      const Icon = notificationKind(n.type).icon;
      toast(n.title, {
        description: notificationText(n.content),
        icon: <Icon className="size-4" />,
        action: {
          label: "Xem",
          onClick: () => {
            void markNotificationRead(n.id);
            router.push(notificationHref(n));
          },
        },
      });
      if (pathnameRef.current === "/notifications") router.refresh();
    });
    source.onopen = () => {
      clearInterval(poll);
      poll = undefined;
    };
    source.onerror = () => {
      if (poll) return;
      void loadCount();
      poll = setInterval(loadCount, FALLBACK_POLL_MS);
    };

    return () => {
      alive = false;
      source.close();
      clearInterval(poll);
    };
  }, [router]);

  function onOpenChange(next: boolean) {
    setOpen(next);
    if (next) {
      api<Page<Notification>>("/api/notifications?size=5")
        .then((p) => setItems(p.content))
        .catch(() => setItems([]));
    }
  }

  function openItem(n: Notification) {
    setOpen(false);
    if (n.read) return;
    setItems((prev) => prev?.map((x) => (x.id === n.id ? { ...x, read: true } : x)) ?? prev);
    void markNotificationRead(n.id);
  }

  async function readAll() {
    try {
      await api("/api/notifications/read", { method: "PATCH" });
      setCount(0);
      setItems((prev) => prev?.map((x) => ({ ...x, read: true })) ?? prev);
      if (pathname === "/notifications") router.refresh();
    } catch {
      toast.error("Không đánh dấu được, thử lại sau.");
    }
  }

  return (
    <Popover open={open} onOpenChange={onOpenChange}>
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
        <div className="flex items-center justify-between gap-2 border-b px-4 py-2.5">
          <span className="flex items-center gap-2">
            <span className="text-subheading">Thông báo</span>
            {count > 0 && <span className="rounded-full bg-primary-soft px-2 py-0.5 text-xs font-medium text-primary-strong tabular-nums">{count} chưa đọc</span>}
          </span>
          {count > 0 && (
            <Button variant="ghost" size="sm" className="text-primary" onClick={readAll}>
              <CheckCheckIcon />
              Đọc tất cả
            </Button>
          )}
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
                      href={notificationHref(n)}
                      onClick={() => openItem(n)}
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
            <Link href="/notifications" onClick={() => setOpen(false)}>
              Xem tất cả
            </Link>
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  );
}
