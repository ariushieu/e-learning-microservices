import type { Metadata } from "next";
import { UserRolesForm } from "@/components/auth/user-roles-form";
import { DashboardPage } from "@/components/templates/dashboard-page";

export const metadata: Metadata = { title: "Người dùng & quyền" };

export default function AdminUsersPage() {
  return (
    <DashboardPage
      title="Người dùng & quyền"
      description="Cấp quyền giảng viên hoặc quản trị viên cho một tài khoản."
      crumbs={[{ href: "/admin", label: "Quản trị" }, { label: "Người dùng & quyền" }]}
    >
      <UserRolesForm />
    </DashboardPage>
  );
}
