"use client";

import { AwardIcon, BellIcon, BookOpenIcon, ClipboardCheckIcon, TrophyIcon, type LucideIcon } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { IconTile } from "@/components/common/icon-tile";
import type { Tone } from "@/components/common/status-badge";
import { api } from "@/lib/client";
import { formatDate } from "@/lib/format";
import { safeNotificationHtml } from "@/lib/html";
import type { Notification } from "@/lib/types";
import { cn } from "@/lib/utils";

const KINDS: Record<string, { icon: LucideIcon; tone: Tone }> = {
  ENROLLMENT_SUCCESS: { icon: BookOpenIcon, tone: "info" },
  COURSE_COMPLETED: { icon: TrophyIcon, tone: "achievement" },
  QUIZ_GRADED: { icon: ClipboardCheckIcon, tone: "primary" },
  CERTIFICATE_ISSUED: { icon: AwardIcon, tone: "achievement" },
};

/** Icon và tông màu theo loại thông báo; loại mới chưa biết hiện chuông xám. Dùng chung với chuông trên header. */
export function notificationKind(type: string) {
  return KINDS[type] ?? { icon: BellIcon, tone: "neutral" as Tone };
}

/**
 * Trang mở khi bấm thông báo. Chỉ nhận đường dẫn nội bộ: "//x.com" hay "/\x.com" trình duyệt
 * hiểu là trang ngoài. Không có link thì về trang Thông báo.
 */
export function notificationHref(n: Pick<Notification, "linkUrl">) {
  const url = n.linkUrl;
  return url && url.startsWith("/") && !/^\/[/\\]/.test(url) ? url : "/notifications";
}

/** Đánh dấu đã đọc, không chờ: người dùng đã bấm đi tiếp, lỗi thì lần sau vẫn hiện chưa đọc. */
export function markNotificationRead(id: number) {
  return api(`/api/notifications/${id}/read`, { method: "PATCH" }).catch(() => undefined);
}

/** Một dòng thông báo. Có trang đích thì bấm là mở trang đó; không có thì chỉ đánh dấu đã đọc. */
export function NotificationItem({ notification: n }: { notification: Notification }) {
  const router = useRouter();
  const [read, setRead] = useState(n.read);
  const kind = notificationKind(n.type);
  const href = notificationHref(n);

  async function markRead() {
    if (read) return;
    setRead(true);
    try {
      await api(`/api/notifications/${n.id}/read`, { method: "PATCH" });
      router.refresh();
    } catch {
      setRead(false);
    }
  }

  const className = cn(
    "relative flex w-full gap-4 px-5 py-4 text-left transition-colors outline-none hover:bg-muted/60 focus-visible:bg-muted/60 focus-visible:ring-2 focus-visible:ring-ring/50 focus-visible:ring-inset",
    !read && "bg-primary-soft/40",
  );
  const body = (
    <>
      {!read && <span className="absolute top-1/2 left-1.5 size-2 -translate-y-1/2 rounded-full bg-primary" aria-label="Chưa đọc" />}
      <IconTile icon={kind.icon} tone={kind.tone} />
      <div className="min-w-0 flex-1">
        <p className={cn("text-sm", read ? "font-medium" : "font-semibold")}>{n.title}</p>
        {/* Nội dung có thẻ <b> từ mẫu; safeNotificationHtml chỉ giữ lại đúng thẻ đó. */}
        <p className="mt-0.5 text-sm break-words text-muted-foreground" dangerouslySetInnerHTML={{ __html: safeNotificationHtml(n.content) }} />
        <time dateTime={n.createdAt} className="mt-1.5 block text-caption text-muted-foreground">
          {formatDate(n.createdAt)}
        </time>
      </div>
    </>
  );

  if (href === "/notifications") {
    return (
      <button type="button" onClick={markRead} className={className}>
        {body}
      </button>
    );
  }
  return (
    <Link
      href={href}
      onClick={() => {
        if (!read) void markNotificationRead(n.id);
      }}
      className={className}
    >
      {body}
    </Link>
  );
}
