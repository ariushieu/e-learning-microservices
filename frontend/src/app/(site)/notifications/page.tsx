import { BellIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { PageHeader } from "@/components/common/page-header";
import { NotificationItem } from "@/components/notification/notification-item";
import { Button } from "@/components/ui/button";
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
    <div className="mx-auto max-w-2xl">
      <PageHeader
        title="Thông báo"
        description={unread ? `${unread} thông báo chưa đọc trên trang này` : `${data.totalElements} thông báo`}
      />
      {data.content.length === 0 ? (
        <EmptyState icon={BellIcon} title="Chưa có thông báo" description="Ghi danh khóa học hoặc làm bài kiểm tra để nhận thông báo." />
      ) : (
        <div className="divide-y overflow-hidden rounded-xl border bg-card">
          {data.content.map((n) => (
            <NotificationItem key={n.id} notification={n} />
          ))}
        </div>
      )}
      {data.totalPages > 1 && (
        <div className="mt-6 flex justify-between">
          <Button asChild variant="outline" disabled={data.first} className={data.first ? "invisible" : ""}>
            <Link href={`/notifications?page=${current - 1}`}>Mới hơn</Link>
          </Button>
          <Button asChild variant="outline" className={data.last ? "invisible" : ""}>
            <Link href={`/notifications?page=${current + 1}`}>Cũ hơn</Link>
          </Button>
        </div>
      )}
    </div>
  );
}
