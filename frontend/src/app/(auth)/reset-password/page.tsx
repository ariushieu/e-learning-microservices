import { LinkIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { ResetPasswordForm } from "@/components/auth/auth-form";
import { Callout } from "@/components/common/callout";

// URL chứa mã đặt lại mật khẩu: không gửi kèm Referer khi rời trang.
export const metadata: Metadata = { title: "Đặt lại mật khẩu", referrer: "no-referrer" };

export default async function ResetPasswordPage({ searchParams }: PageProps<"/reset-password">) {
  const { token } = await searchParams;
  const value = typeof token === "string" ? token.trim() : "";

  return (
    <div className="space-y-8">
      <div className="space-y-2">
        <h1 className="text-title">Đặt mật khẩu mới</h1>
        <p className="text-muted-foreground">Liên kết trong email dùng được một lần và hết hạn sau 30 phút.</p>
      </div>
      {value ? (
        <ResetPasswordForm token={value} />
      ) : (
        <Callout
          icon={LinkIcon}
          tone="warning"
          title="Thiếu mã đặt lại mật khẩu"
          action={
            <Link href="/forgot-password" className="text-sm font-medium text-primary underline-offset-4 hover:underline">
              Gửi liên kết mới
            </Link>
          }
        >
          Hãy mở đúng liên kết trong email, hoặc yêu cầu một liên kết mới.
        </Callout>
      )}
    </div>
  );
}
