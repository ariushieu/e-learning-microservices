import type { Metadata } from "next";
import Link from "next/link";
import {
  BookOpenIcon,
  FolderTreeIcon,
  GraduationCapIcon,
  MessageSquareIcon,
  LockKeyholeIcon,
  ShieldCheckIcon,
  UserPlusIcon,
  UsersIcon,
} from "lucide-react";
import { RecentUsers, type UserStats } from "@/components/auth/user-overview";
import { ErrorAlert } from "@/components/common/error-alert";
import { Section } from "@/components/common/section";
import { Stat, StatGrid } from "@/components/common/stat";
import { DashboardPage } from "@/components/templates/dashboard-page";
import { Button } from "@/components/ui/button";
import { errorMessage } from "@/lib/errors";
import { gateway } from "@/lib/server/gateway";
import type { InstructorReviews } from "@/components/course/review-types";
import type { Category, CourseSummary, Page, User } from "@/lib/types";

export const metadata: Metadata = { title: "Tổng quan quản trị" };

export default async function AdminIndex() {
  const [statistics, recent, courses, categories, reviews] = await Promise.allSettled([
    gateway<UserStats>("/api/users/stats"),
    gateway<Page<User>>("/api/users?sort=createdAt,desc&size=5"),
    gateway<Page<CourseSummary>>("/api/courses?size=1"),
    gateway<Category[]>("/api/categories/tree"),
    gateway<InstructorReviews>("/api/instructor/reviews?replied=false&size=1"),
  ]);
  // Mỗi số liệu nội dung lấy từ service riêng; service nào lỗi thì chỉ thẻ đó hiện "—".
  const value = <T,>(r: PromiseSettledResult<T>, pick: (v: T) => number) => (r.status === "fulfilled" ? pick(r.value) : "—");
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
      <Section title="Nội dung học tập" description="Khóa học đang mở cho học viên, danh mục và đánh giá cần phản hồi.">
        <StatGrid>
          <Link href="/courses" className={linkStyle}>
            <Stat label="Khóa đã xuất bản" value={value(courses, (p) => p.totalElements)} icon={BookOpenIcon} hint="Xem trang khóa học" />
          </Link>
          <Link href="/admin/categories" className={linkStyle}>
            <Stat
              label="Danh mục"
              value={value(categories, (tree) => tree.reduce((n, c) => n + 1 + (c.subCategories?.length ?? 0), 0))}
              icon={FolderTreeIcon}
              tone="neutral"
              hint="Quản lý danh mục"
            />
          </Link>
          <Link href="/instructor/reviews" className={linkStyle}>
            <Stat
              label="Đánh giá chờ phản hồi"
              value={value(reviews, (r) => r.unrepliedCount)}
              icon={MessageSquareIcon}
              tone="info"
              hint="Trả lời đánh giá"
            />
          </Link>
        </StatGrid>
      </Section>
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
