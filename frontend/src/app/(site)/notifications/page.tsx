import { BellIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { PrevNextPagination } from "@/components/common/pagination";
import { NotificationItem } from "@/components/notification/notification-item";
import { ListPage } from "@/components/templates/list-page";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { errorMessage } from "@/lib/errors";
import { gateway } from "@/lib/server/gateway";
import type { Notification, Page } from "@/lib/types";

export const metadata: Metadata = { title: "Thông báo" };

const PAGE_SIZE = 20;

export default async function NotificationsPage({ searchParams }: PageProps<"/notifications">) {
  const { page } = await searchParams;
  const current = Math.max(0, Number(page ?? 0) || 0);
  let data: Page<Notification> | null = null;
  let loadError: string | null = null;
  try {
    data = await gateway<Page<Notification>>(`/api/notifications?page=${current}&size=${PAGE_SIZE}`);
  } catch (e) {
    loadError = errorMessage(e);
  }

  if (!data) {
    return (
      <ListPage title="Thông báo" width="narrow">
        <ErrorAlert title="Không tải được thông báo" message={loadError ?? ""} />
      </ListPage>
    );
  }

  const unread = data.content.filter((n) => !n.read).length;
  return (
    <ListPage
      title="Thông báo"
      description={unread ? `${unread} thông báo chưa đọc trên trang này` : `${data.totalElements} thông báo`}
      width="narrow"
      pagination={
        data.totalPages > 1 && (
          <PrevNextPagination
            first={data.first}
            last={data.last}
            prevHref={`/notifications?page=${current - 1}`}
            nextHref={`/notifications?page=${current + 1}`}
            prevLabel="Mới hơn"
            nextLabel="Cũ hơn"
          />
        )
      }
    >
      {data.content.length === 0 ? (
        <EmptyState
          icon={BellIcon}
          title="Chưa có thông báo"
          description="Ghi danh khóa học hoặc làm bài kiểm tra để nhận thông báo."
          action={
            current > 0 ? (
              <Button asChild variant="outline">
                <Link href="/notifications">Về trang đầu</Link>
              </Button>
            ) : undefined
          }
        />
      ) : (
        <Card className="gap-0 divide-y py-0">
          {data.content.map((n) => (
            <NotificationItem key={n.id} notification={n} />
          ))}
        </Card>
      )}
    </ListPage>
  );
}
