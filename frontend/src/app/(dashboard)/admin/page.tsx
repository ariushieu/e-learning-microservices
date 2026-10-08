import type { Metadata } from "next";
import Link from "next/link";
import {
  GraduationCapIcon,
  LockKeyholeIcon,
  ShieldCheckIcon,
  UserPlusIcon,
  UsersIcon,
} from "lucide-react";
import { RecentUsers, type UserStats } from "@/components/auth/user-overview";
import { ErrorAlert } from "@/components/common/error-alert";
import { Section } from "@/components/common/section";
import { Stat } from "@/components/common/stat";
import { DashboardPage } from "@/components/templates/dashboard-page";
import { Button } from "@/components/ui/button";
import { errorMessage } from "@/lib/errors";
import { gateway } from "@/lib/server/gateway";
import type { Page, User } from "@/lib/types";

export const metadata: Metadata = { title: "Tổng quan quản trị" };

export default async function AdminIndex() {
  const [statistics, recent] = await Promise.allSettled([
    gateway<UserStats>("/api/users/stats"),
    gateway<Page<User>>("/api/users?sort=createdAt,desc&size=5"),
  ]);
  const stats = statistics.status === "fulfilled" ? statistics.value : null;
  const linkStyle =
    "rounded-xl outline-none transition-colors hover:bg-primary-soft focus-visible:ring-2 focus-visible:ring-ring [&>div]:h-full";

  return (
    <DashboardPage
      title="Tổng quan quản trị"
      description="Theo dõi người dùng và truy cập nhanh các tài khoản cần quản lý."
      crumbs={[{ label: "Quản trị" }, { label: "Tổng quan" }]}
      stats={
        stats && (
          <>
            <Stat label="Tổng người dùng" value={stats.total} icon={UsersIcon} />
            <Link href="/admin/users?role=ROLE_INSTRUCTOR" className={linkStyle}>
              <Stat
                label="Giảng viên"
                value={stats.byRole.ROLE_INSTRUCTOR}
                icon={GraduationCapIcon}
                hint="Xem danh sách giảng viên"
              />
            </Link>
            <Stat label="Quản trị viên" value={stats.byRole.ROLE_ADMIN} icon={ShieldCheckIcon} />
            <Link href="/admin/users?status=LOCKED" className={linkStyle}>
              <Stat
                label="Bị khóa"
                value={stats.byStatus.LOCKED}
                icon={LockKeyholeIcon}
                tone="danger"
                hint="Xem tài khoản bị khóa"
              />
            </Link>
            <Stat
              label="Mới trong 7 ngày"
              value={stats.newLast7Days}
              icon={UserPlusIcon}
              hint="Trong 7 ngày gần nhất"
            />
          </>
        )
      }
    >
      {statistics.status === "rejected" ? (
        <ErrorAlert title="Không tải được thống kê" message={errorMessage(statistics.reason)} />
      ) : (
        <p className="text-sm text-muted-foreground">
          Một người có thể có nhiều vai trò. Số giảng viên và quản trị viên có thể trùng nhau.
        </p>
      )}
      <Section
        title="Tài khoản mới nhất"
        description="5 tài khoản được tạo gần đây nhất."
        actions={
          <Button asChild variant="outline">
            <Link href="/admin/users">Xem tất cả</Link>
          </Button>
        }
      >
        {recent.status === "fulfilled" ? (
          <RecentUsers users={recent.value.content} />
        ) : (
          <ErrorAlert title="Không tải được tài khoản mới" message={errorMessage(recent.reason)} />
        )}
      </Section>
      {(statistics.status === "rejected" || recent.status === "rejected") && (
        <Button asChild variant="outline">
          <Link href="/admin">Thử lại</Link>
        </Button>
      )}
    </DashboardPage>
  );
}
