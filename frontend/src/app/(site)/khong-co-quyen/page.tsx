import { StatusPage } from "@/components/common/status-page";

export default function ForbiddenPage() {
  return <StatusPage code="403" title="Bạn không có quyền vào trang này" description="Trang này dành cho giảng viên hoặc quản trị viên." />;
}
