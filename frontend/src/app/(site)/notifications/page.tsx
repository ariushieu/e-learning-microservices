import { BellIcon, BellOffIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { Callout } from "@/components/common/callout";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { PrevNextPagination } from "@/components/common/pagination";
import { EnableInAppButton, NotificationActions } from "@/components/notification/notification-actions";
import { NotificationItem } from "@/components/notification/notification-item";
import { ListPage } from "@/components/templates/list-page";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { errorMessage } from "@/lib/errors";
import { gateway } from "@/lib/server/gateway";
import type { Notification, NotificationPreference, Page } from "@/lib/types";

export const metadata: Metadata = { title: "Thông báo" };

const PAGE_SIZE = 20;

export default async function NotificationsPage({ searchParams }: PageProps<"/notifications">) {
  const { page } = await searchParams;
  const current = Math.max(0, Number(page ?? 0) || 0);
  const [inbox, unreadCount, preferences] = await Promise.all([
    gateway<Page<Notification>>(`/api/notifications?page=${current}&size=${PAGE_SIZE}`).then(
      (page) => ({ data: page, loadError: null }),
      (e: unknown) => ({ data: null, loadError: errorMessage(e) }),
    ),
    // Số chưa đọc và cài đặt chỉ là phần phụ: lỗi thì vẫn hiện hộp thư, chỉ thiếu nút tương ứng.
    gateway<number>("/api/notifications/unread-count").catch(() => null),
    gateway<NotificationPreference>("/api/notifications/preferences").catch(() => null),
  ]);
  const { data, loadError } = inbox;

  if (!data) {
    return (
      <ListPage title="Thông báo" width="narrow">
        <ErrorAlert title="Không tải được thông báo" message={loadError ?? ""} />
      </ListPage>
    );
  }

  const unread = unreadCount ?? data.content.filter((n) => !n.read).length;
  return (
    <ListPage
      title="Thông báo"
      description={unread ? `${unread} thông báo chưa đọc` : `${data.totalElements} thông báo`}
      actions={<NotificationActions unread={unread} preferences={preferences} />}
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
      {preferences && !preferences.inAppEnabled && (
        <Callout
          icon={BellOffIcon}
          tone="warning"
          className="mb-6"
          title="Bạn đang tắt thông báo trong ứng dụng"
          action={<EnableInAppButton preferences={preferences} />}
        >
          Ghi danh, kết quả bài kiểm tra và chứng chỉ mới sẽ không hiện ở đây cho tới khi bật lại.
        </Callout>
      )}
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
