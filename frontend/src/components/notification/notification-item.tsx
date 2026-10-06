"use client";

import { AwardIcon, BellIcon, BookOpenCheckIcon, ClipboardCheckIcon, GraduationCapIcon, type LucideIcon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { api } from "@/lib/client";
import { formatDate } from "@/lib/format";
import { safeNotificationHtml } from "@/lib/html";
import type { Notification } from "@/lib/types";
import { cn } from "@/lib/utils";

const ICONS: Record<string, LucideIcon> = {
  ENROLLMENT_SUCCESS: BookOpenCheckIcon,
  COURSE_COMPLETED: GraduationCapIcon,
  CERTIFICATE_ISSUED: AwardIcon,
  QUIZ_GRADED: ClipboardCheckIcon,
};

/** Một dòng thông báo; bấm vào là đánh dấu đã đọc. */
export function NotificationItem({ notification: n }: { notification: Notification }) {
  const router = useRouter();
  const [read, setRead] = useState(n.read);
  const Icon = ICONS[n.type] ?? BellIcon;

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

  return (
    <button
      type="button"
      onClick={markRead}
      className={cn("flex w-full gap-4 px-5 py-4 text-left transition-colors hover:bg-muted/50", !read && "bg-primary/[0.04]")}
    >
      <div className={cn("flex size-9 shrink-0 items-center justify-center rounded-full", read ? "bg-muted text-muted-foreground" : "bg-primary/10 text-primary")}>
        <Icon className="size-4.5" />
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-3">
          <p className={cn("text-sm", read ? "font-medium" : "font-semibold")}>{n.title}</p>
          {!read && <span className="mt-1.5 size-2 shrink-0 rounded-full bg-primary" aria-label="Chưa đọc" />}
        </div>
        {/* Nội dung có thẻ <b> từ mẫu; safeNotificationHtml chỉ giữ lại đúng thẻ đó. */}
        <p className="mt-0.5 text-sm text-muted-foreground" dangerouslySetInnerHTML={{ __html: safeNotificationHtml(n.content) }} />
        <p className="mt-1.5 text-xs text-muted-foreground">{formatDate(n.createdAt)}</p>
      </div>
    </button>
  );
}
