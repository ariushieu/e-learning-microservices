import { KeyRoundIcon } from "lucide-react";
import type { Metadata } from "next";
import { LoginForm } from "@/components/auth/auth-form";
import { Callout } from "@/components/common/callout";

export const metadata: Metadata = { title: "Đăng nhập" };

export default async function LoginPage({ searchParams }: PageProps<"/login">) {
  const { next, reset } = await searchParams;
  return (
    <div className="space-y-8">
      <div className="space-y-2">
        <h1 className="text-title">Chào mừng trở lại</h1>
        <p className="text-muted-foreground">Đăng nhập để tiếp tục khóa học của bạn.</p>
      </div>
      {reset === "1" && (
        <Callout icon={KeyRoundIcon} tone="success" title="Đã đặt lại mật khẩu">
          Đăng nhập bằng mật khẩu mới. Các thiết bị khác đã được đăng xuất.
        </Callout>
      )}
      <LoginForm next={typeof next === "string" ? next : "/"} />
    </div>
  );
}
