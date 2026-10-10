import type { Metadata } from "next";
import { ForgotPasswordForm } from "@/components/auth/auth-form";

export const metadata: Metadata = { title: "Quên mật khẩu" };

export default function ForgotPasswordPage() {
  return (
    <div className="space-y-8">
      <div className="space-y-2">
        <h1 className="text-title">Quên mật khẩu</h1>
        <p className="text-muted-foreground">Nhập email đã đăng ký, chúng tôi sẽ gửi liên kết để bạn đặt mật khẩu mới.</p>
      </div>
      <ForgotPasswordForm />
    </div>
  );
}
