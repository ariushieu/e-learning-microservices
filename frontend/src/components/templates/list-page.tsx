import type { ReactNode } from "react";
import { PageHeader, type Crumb } from "@/components/common/page-header";
import { cn } from "@/lib/utils";

/**
 * Khuôn trang DANH SÁCH: tiêu đề → thanh công cụ (tìm, lọc) → nội dung (lưới thẻ / bảng / dòng)
 * → phân trang. Dùng cho: trang chủ, khóa học của tôi, thông báo, khóa học tôi dạy, quản trị.
 */
export function ListPage({
  title,
  description,
  eyebrow,
  crumbs,
  actions,
  toolbar,
  pagination,
  width = "full",
  children,
}: {
  title: ReactNode;
  description?: ReactNode;
  eyebrow?: ReactNode;
  crumbs?: Crumb[];
  actions?: ReactNode;
  /** Thường là <Toolbar>. */
  toolbar?: ReactNode;
  /** Thường là <Pagination> hoặc <PrevNextPagination>. */
  pagination?: ReactNode;
  /** "narrow" cho danh sách để đọc (thông báo). */
  width?: "full" | "narrow";
  children: ReactNode;
}) {
  return (
    <div className={cn("w-full", width === "narrow" && "mx-auto max-w-3xl")}>
      <PageHeader title={title} description={description} eyebrow={eyebrow} crumbs={crumbs} actions={actions} />
      {toolbar}
      {children}
      {pagination}
    </div>
  );
}

/** Lưới thẻ chuẩn cho danh sách (khóa học, chứng chỉ...): 1 → 2 → 3 cột. */
export function CardGrid({ className, children }: { className?: string; children: ReactNode }) {
  return <div className={cn("grid gap-6 sm:grid-cols-2 lg:grid-cols-3", className)}>{children}</div>;
}
