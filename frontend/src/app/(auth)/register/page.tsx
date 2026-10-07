import type { Metadata } from "next";
import { RegisterForm } from "@/components/auth/auth-form";

export const metadata: Metadata = { title: "Đăng ký" };

export default async function RegisterPage({ searchParams }: PageProps<"/register">) {
  const { next } = await searchParams;
  return (
    <div className="space-y-8">
      <div className="space-y-2">
        <h1 className="text-title">Tạo tài khoản</h1>
        <p className="text-muted-foreground">Tài khoản mới là học viên. Muốn giảng dạy, nhờ quản trị viên cấp quyền.</p>
      </div>
      <RegisterForm next={typeof next === "string" ? next : "/"} />
    </div>
  );
}
