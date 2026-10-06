import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { CourseForm } from "@/components/instructor/course-form";
import { Alert, Card, PageTitle } from "@/components/ui";
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

  return (
    <div className="mx-auto max-w-3xl">
      <PageTitle
        title="Tạo khóa học mới"
        subtitle="Khóa học mới ở trạng thái bản nháp; bạn soạn nội dung rồi xuất bản sau."
      />
      <div className="mb-4 text-sm">
        <Link href="/instructor" className="text-indigo-600 hover:underline">
          ← Khóa học tôi dạy
        </Link>
      </div>

      {loadError ? (
        <Alert>Không tải được danh mục: {loadError}</Alert>
      ) : categories.length === 0 ? (
        <Alert kind="info">
          Chưa có danh mục nào nên chưa tạo được khóa học.{" "}
          {hasRole(session, "ROLE_ADMIN") ? (
            <>
              Hãy{" "}
              <Link href="/admin" className="font-medium underline">
                tạo danh mục ở trang quản trị
              </Link>{" "}
              trước.
            </>
          ) : (
            "Hãy nhờ quản trị viên tạo danh mục trước."
          )}
        </Alert>
      ) : (
        <Card>
          <CourseForm categories={categories} instructorName={session.fullName} />
        </Card>
      )}
    </div>
  );
}
