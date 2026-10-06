import type { Metadata } from "next";
import Link from "next/link";
import { Empty, PageTitle } from "@/components/ui";
import { NotificationItem } from "@/components/account/notification-item";
import { gateway } from "@/lib/server/gateway";
import type { Notification, Page } from "@/lib/types";

export const metadata: Metadata = { title: "Thông báo" };

const PAGE_SIZE = 20;

export default async function NotificationsPage({ searchParams }: PageProps<"/notifications">) {
  const { page } = await searchParams;
  const current = Math.max(0, Number(page ?? 0) || 0);
  const data = await gateway<Page<Notification>>(`/api/notifications?page=${current}&size=${PAGE_SIZE}`);
  const unread = data.content.filter((n) => !n.read).length;

  return (
    <div className="mx-auto max-w-3xl">
      <PageTitle
        title="Thông báo"
        subtitle={`${data.totalElements} thông báo${unread ? ` · ${unread} chưa đọc trên trang này` : ""}`}
      />
      {data.content.length === 0 ? (
        <Empty>Chưa có thông báo nào. Ghi danh hoặc làm bài kiểm tra để nhận thông báo.</Empty>
      ) : (
        <ul className="space-y-3">
          {data.content.map((n) => (
            <NotificationItem key={n.id} notification={n} />
          ))}
        </ul>
      )}
      {data.totalPages > 1 && (
        <div className="mt-6 flex justify-between text-sm">
          {data.first ? <span /> : <Link className="text-indigo-600" href={`/notifications?page=${current - 1}`}>← Mới hơn</Link>}
          {data.last ? <span /> : <Link className="text-indigo-600" href={`/notifications?page=${current + 1}`}>Cũ hơn →</Link>}
        </div>
      )}
    </div>
  );
}
