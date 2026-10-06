"use client";

import { Loader2Icon } from "lucide-react";
import Link from "next/link";
import { useActionState } from "react";
import { ErrorAlert } from "@/components/common/error-alert";
import { FieldError } from "@/components/common/field-error";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { loginAction, registerAction, type FormState } from "@/lib/server/auth-actions";

export function LoginForm({ next }: { next: string }) {
  const [state, action, pending] = useActionState<FormState, FormData>(loginAction, {});
  return (
    <form action={action} className="space-y-5">
      <input type="hidden" name="next" value={next} />
      {state.error && <ErrorAlert message={state.error} />}
      <div className="space-y-2">
        <Label htmlFor="email">Email</Label>
        <Input id="email" name="email" type="email" required autoComplete="email" placeholder="ban@hunre.edu.vn" defaultValue={state.values?.email} />
        <FieldError message={state.fieldErrors?.email} />
      </div>
      <div className="space-y-2">
        <Label htmlFor="password">Mật khẩu</Label>
        <Input id="password" name="password" type="password" required autoComplete="current-password" />
      </div>
      <Button type="submit" size="lg" className="w-full" disabled={pending}>
        {pending && <Loader2Icon className="animate-spin" />} Đăng nhập
      </Button>
      <p className="text-center text-sm text-muted-foreground">
        Chưa có tài khoản?{" "}
        <Link href={`/register?next=${encodeURIComponent(next)}`} className="font-medium text-primary hover:underline">
          Đăng ký
        </Link>
      </p>
    </form>
  );
}

export function RegisterForm({ next }: { next: string }) {
  const [state, action, pending] = useActionState<FormState, FormData>(registerAction, {});
  const err = state.fieldErrors ?? {};
  return (
    <form action={action} className="space-y-4">
      <input type="hidden" name="next" value={next} />
      {state.error && <ErrorAlert message={state.error} />}
      <div className="space-y-2">
        <Label htmlFor="fullName">Họ và tên</Label>
        <Input id="fullName" name="fullName" required maxLength={150} autoComplete="name" defaultValue={state.values?.fullName} />
        <FieldError message={err.fullName} />
      </div>
      <div className="space-y-2">
        <Label htmlFor="email">Email</Label>
        <Input id="email" name="email" type="email" required autoComplete="email" placeholder="ban@hunre.edu.vn" defaultValue={state.values?.email} />
        <FieldError message={err.email} />
      </div>
      <div className="space-y-2">
        <Label htmlFor="phone">
          Số điện thoại <span className="font-normal text-muted-foreground">(không bắt buộc)</span>
        </Label>
        <Input id="phone" name="phone" maxLength={20} autoComplete="tel" defaultValue={state.values?.phone} />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="password">Mật khẩu</Label>
          <Input id="password" name="password" type="password" required minLength={6} maxLength={50} autoComplete="new-password" />
          <FieldError message={err.password} />
        </div>
        <div className="space-y-2">
          <Label htmlFor="confirmPassword">Nhập lại</Label>
          <Input id="confirmPassword" name="confirmPassword" type="password" required autoComplete="new-password" />
          <FieldError message={err.confirmPassword} />
        </div>
      </div>
      <p className="text-xs text-muted-foreground">Mật khẩu từ 6 đến 50 ký tự.</p>
      <Button type="submit" size="lg" className="w-full" disabled={pending}>
        {pending && <Loader2Icon className="animate-spin" />} Tạo tài khoản
      </Button>
      <p className="text-center text-sm text-muted-foreground">
        Đã có tài khoản?{" "}
        <Link href={`/login?next=${encodeURIComponent(next)}`} className="font-medium text-primary hover:underline">
          Đăng nhập
        </Link>
      </p>
    </form>
  );
}
