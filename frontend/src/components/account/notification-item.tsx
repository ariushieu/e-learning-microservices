"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { formatDate } from "@/components/ui";
import { api } from "@/lib/client";
import { safeNotificationHtml } from "@/lib/html";
import type { Notification } from "@/lib/types";

const ICONS: Record<string, string> = {
  ENROLLMENT_SUCCESS: "🎓",
  COURSE_COMPLETED: "🏁",
  CERTIFICATE_ISSUED: "📜",
  QUIZ_GRADED: "📝",
};

export function NotificationItem({ notification: n }: { notification: Notification }) {
  const router = useRouter();
  const [read, setRead] = useState(n.read);

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
    <li
      onClick={markRead}
      className={`flex cursor-pointer gap-4 rounded-xl border p-4 transition ${
        read ? "border-slate-200 bg-white" : "border-indigo-200 bg-indigo-50/60 hover:bg-indigo-50"
      }`}
    >
      <div className="text-2xl">{ICONS[n.type] ?? "🔔"}</div>
      <div className="flex-1">
        <div className="flex items-start justify-between gap-3">
          <h3 className="font-semibold text-slate-900">{n.title}</h3>
          {!read && <span className="mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full bg-indigo-600" />}
        </div>
        {/* Nội dung có thẻ <b> từ mẫu; safeNotificationHtml chỉ giữ lại đúng thẻ đó. */}
        <p className="mt-1 text-sm text-slate-700" dangerouslySetInnerHTML={{ __html: safeNotificationHtml(n.content) }} />
        <div className="mt-2 text-xs text-slate-500">{formatDate(n.createdAt)}</div>
      </div>
    </li>
  );
}
