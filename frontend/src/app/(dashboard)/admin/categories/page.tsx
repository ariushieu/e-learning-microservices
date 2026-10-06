import type { Metadata } from "next";
import { ErrorAlert } from "@/components/common/error-alert";
import { CategoryManager } from "@/components/course/category-manager";
import { DashboardPage } from "@/components/templates/dashboard-page";
import { errorMessage } from "@/lib/errors";
import { gateway } from "@/lib/server/gateway";
import type { Category } from "@/lib/types";

export const metadata: Metadata = { title: "Danh mục" };

export default async function CategoriesPage() {
  let tree: Category[] | null = null;
  let loadError: string | null = null;
  try {
    tree = await gateway<Category[]>("/api/categories/tree");
  } catch (e) {
    loadError = errorMessage(e);
  }

  return (
    <DashboardPage
      title="Danh mục khóa học"
      description="Danh mục gốc và danh mục con (một cấp), dùng để phân loại và lọc khóa học."
    >
      {tree ? <CategoryManager tree={tree} /> : <ErrorAlert title="Không tải được danh mục" message={loadError ?? ""} />}
    </DashboardPage>
  );
}
