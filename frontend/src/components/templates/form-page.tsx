import type { ReactNode } from "react";
import { PageHeader, type Crumb } from "@/components/common/page-header";

/**
 * Khuôn trang FORM: tiêu đề → các FormSection (nhóm ô) → FormActions.
 * Dùng cho: tạo khóa học, hồ sơ, cài đặt bài kiểm tra.
 */
export function FormPage({
  title,
  description,
  crumbs,
  actions,
  children,
}: {
  title: ReactNode;
  description?: ReactNode;
  crumbs?: Crumb[];
  actions?: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="mx-auto w-full max-w-4xl">
      <PageHeader title={title} description={description} crumbs={crumbs} actions={actions} />
      <div className="space-y-8">{children}</div>
    </div>
  );
}
