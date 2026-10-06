import { InfoIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { ErrorAlert } from "@/components/common/error-alert";
import { PageHeader } from "@/components/common/page-header";
import { CourseForm } from "@/components/course/course-form";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
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
    <div className="space-y-6">
      <Breadcrumb>
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href="/instructor">Khóa học tôi dạy</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbPage>Tạo khóa học</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>
      <PageHeader
        title="Tạo khóa học mới"
        description="Khóa học mới ở trạng thái bản nháp; bạn soạn nội dung rồi xuất bản sau."
      />

      {loadError ? (
        <ErrorAlert title="Không tải được danh mục" message={loadError} />
      ) : categories.length === 0 ? (
        <Alert>
          <InfoIcon />
          <AlertTitle>Chưa có danh mục nào nên chưa tạo được khóa học</AlertTitle>
          <AlertDescription>
            {hasRole(session, "ROLE_ADMIN") ? (
              <p>
                Hãy <Link href="/admin/categories">tạo danh mục ở trang quản trị</Link> trước.
              </p>
            ) : (
              <p>Hãy nhờ quản trị viên tạo danh mục trước.</p>
            )}
          </AlertDescription>
        </Alert>
      ) : (
        <CourseForm categories={categories} instructorName={session.fullName} />
      )}
    </div>
  );
}
