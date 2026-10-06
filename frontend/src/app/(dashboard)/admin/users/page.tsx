import type { Metadata } from "next";
import { UserRolesForm } from "@/components/auth/user-roles-form";
import { PageHeader } from "@/components/common/page-header";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export const metadata: Metadata = { title: "Người dùng & quyền" };

export default function AdminUsersPage() {
  return (
    <>
      <PageHeader title="Người dùng & quyền" description="Cấp quyền giảng viên hoặc quản trị viên cho một tài khoản." />
      <Card className="max-w-2xl">
        <CardHeader>
          <CardTitle>Cấp vai trò</CardTitle>
          <CardDescription>Nhập mã người dùng (xem ở trang Hồ sơ của họ) rồi chọn vai trò.</CardDescription>
        </CardHeader>
        <CardContent>
          <UserRolesForm />
        </CardContent>
      </Card>
    </>
  );
}
