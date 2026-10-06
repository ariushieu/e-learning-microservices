import type { ReactNode } from "react";
import { PageHeader, type Crumb } from "@/components/common/page-header";
import { StatGrid } from "@/components/common/stat";

/**
 * Khuôn trang DASHBOARD (khu giảng dạy, quản trị): tiêu đề → hàng ô số liệu → các Section.
 * Dùng cho: khóa học tôi dạy, soạn khóa học, quản trị.
 */
export function DashboardPage({
  title,
  description,
  crumbs,
  actions,
  stats,
  children,
}: {
  title: ReactNode;
  description?: ReactNode;
  crumbs?: Crumb[];
  actions?: ReactNode;
  /** Các <Stat>. */
  stats?: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="w-full">
      <PageHeader title={title} description={description} crumbs={crumbs} actions={actions} />
      <div className="space-y-10">
        {stats && <StatGrid>{stats}</StatGrid>}
        {children}
      </div>
    </div>
  );
}
