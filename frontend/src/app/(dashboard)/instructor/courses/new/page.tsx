import { FolderTreeIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { CourseForm } from "@/components/course/course-form";
import { FormPage } from "@/components/templates/form-page";
import { hasRole } from "@/lib/auth-shared";
import { errorMessage } from "@/lib/errors";
import { gateway, getSession } from "@/lib/server/gateway";
import type { Category } from "@/lib/types";

export const metadata: Metadata = { title: "Tạo khóa học" };

export default async function NewCoursePage() {
  const session = await getSession();
  if (!session) redirect("/login?next=/instructor/courses/new");

  let categories: Category[] = [];
  let loadError: string | null = null;
  try {
    categories = await gateway<Category[]>("/api/categories/tree");
  } catch (e) {
    loadError = errorMessage(e);
  }
  const isAdmin = hasRole(session, "ROLE_ADMIN");

  return (
    <FormPage
      title="Tạo khóa học mới"
      description="Khóa học mới ở trạng thái bản nháp; bạn soạn nội dung rồi xuất bản sau."
      crumbs={[{ href: "/instructor", label: "Khóa học tôi dạy" }, { label: "Tạo khóa học" }]}
    >
      {loadError ? (
        <ErrorAlert title="Không tải được danh mục" message={loadError} />
      ) : categories.length === 0 ? (
        <EmptyState
          icon={FolderTreeIcon}
          title="Chưa có danh mục nào nên chưa tạo được khóa học"
          description={
            isAdmin ? (
              <>
                Hãy{" "}
                <Link href="/admin/categories" className="font-medium text-primary hover:underline">
                  tạo danh mục ở trang quản trị
                </Link>{" "}
                trước.
              </>
            ) : (
              "Hãy nhờ quản trị viên tạo danh mục trước."
            )
          }
        />
      ) : (
        <CourseForm categories={categories} instructorName={session.fullName} />
      )}
    </FormPage>
  );
}
