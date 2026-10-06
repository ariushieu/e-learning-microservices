import type { Metadata } from "next";
import { Card } from "@/components/ui";
import { LoginForm } from "../auth-form";

export const metadata: Metadata = { title: "Đăng nhập" };

export default async function LoginPage({ searchParams }: PageProps<"/login">) {
  const { next } = await searchParams;
  return (
    <div className="mx-auto max-w-md">
      <h1 className="mb-1 text-2xl font-bold">Đăng nhập</h1>
      <p className="mb-6 text-slate-500">Tiếp tục học từ chỗ bạn dừng lại.</p>
      <Card>
        <LoginForm next={typeof next === "string" ? next : "/"} />
      </Card>
    </div>
  );
}
