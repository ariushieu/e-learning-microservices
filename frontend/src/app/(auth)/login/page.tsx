import type { Metadata } from "next";
import { LoginForm } from "@/components/auth/auth-form";

export const metadata: Metadata = { title: "Đăng nhập" };

export default async function LoginPage({ searchParams }: PageProps<"/login">) {
  const { next } = await searchParams;
  return (
    <div className="space-y-8">
      <div className="space-y-2">
        <h1 className="text-title">Chào mừng trở lại</h1>
        <p className="text-muted-foreground">Đăng nhập để tiếp tục khóa học của bạn.</p>
      </div>
      <LoginForm next={typeof next === "string" ? next : "/"} />
    </div>
  );
}
