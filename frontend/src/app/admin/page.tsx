import type { Metadata } from "next";
import { CategoryManager } from "@/components/admin/category-manager";
import { UserRolesForm } from "@/components/admin/user-roles-form";
import { SectionHeading } from "@/components/instructor/form-helpers";
import { Alert, Card, PageTitle } from "@/components/ui";
import { errorMessage } from "@/lib/errors";
import { gateway } from "@/lib/server/gateway";
import type { Category } from "@/lib/types";

export const metadata: Metadata = { title: "Quản trị" };

export default async function AdminPage() {
  let tree: Category[] | null = null;
  let loadError: string | null = null;
  try {
    tree = await gateway<Category[]>("/api/categories/tree");
  } catch (e) {
    loadError = errorMessage(e);
  }

  return (
    <div className="space-y-6">
      <PageTitle title="Quản trị hệ thống" subtitle="Cấp quyền người dùng và quản lý danh mục khóa học." />
      <div className="grid items-start gap-6 lg:grid-cols-[2fr_3fr]">
        <Card>
          <SectionHeading title="Cấp quyền người dùng" description="Đặt lại vai trò cho một tài khoản theo mã người dùng." />
          <UserRolesForm />
        </Card>
        <Card>
          <SectionHeading
            title="Danh mục"
            description="Danh mục gốc và danh mục con (một cấp). Không xóa được danh mục còn danh mục con hoặc còn khóa học."
          />
          {tree ? <CategoryManager tree={tree} /> : <Alert>Không tải được danh mục: {loadError}</Alert>}
        </Card>
      </div>
    </div>
  );
}
