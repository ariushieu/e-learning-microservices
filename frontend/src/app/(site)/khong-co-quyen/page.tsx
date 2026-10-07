import type { Metadata } from "next";
import Link from "next/link";
import { StatusPage } from "@/components/common/status-page";
import { Button } from "@/components/ui/button";

export const metadata: Metadata = { title: "Không có quyền" };

export default function ForbiddenPage() {
  return (
    <StatusPage
      code="403"
      title="Bạn không có quyền vào trang này"
      description="Trang này dành cho giảng viên hoặc quản trị viên. Nếu vừa được cấp quyền, hãy đăng xuất rồi đăng nhập lại."
      action={
        <>
          <Button asChild size="lg">
            <Link href="/">Về trang chủ</Link>
          </Button>
          <Button asChild size="lg" variant="outline">
            <Link href="/profile">Xem mã người dùng</Link>
          </Button>
        </>
      }
    />
  );
}
