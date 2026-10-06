import type { Metadata } from "next";
import { Card } from "@/components/ui";
import { RegisterForm } from "../auth-form";

export const metadata: Metadata = { title: "Đăng ký" };

export default async function RegisterPage({ searchParams }: PageProps<"/register">) {
  const { next } = await searchParams;
  return (
    <div className="mx-auto max-w-md">
      <h1 className="mb-1 text-2xl font-bold">Tạo tài khoản</h1>
      <p className="mb-6 text-slate-500">Tài khoản mới là học viên. Muốn giảng dạy, nhờ quản trị viên cấp quyền.</p>
      <Card>
        <RegisterForm next={typeof next === "string" ? next : "/"} />
      </Card>
    </div>
  );
}
