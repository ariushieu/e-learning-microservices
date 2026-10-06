"use client";

import Link from "next/link";
import { useActionState } from "react";
import { Alert, Button, Field, Input } from "@/components/ui";
import { loginAction, registerAction, type FormState } from "@/lib/server/auth-actions";

export function LoginForm({ next }: { next: string }) {
  const [state, action, pending] = useActionState<FormState, FormData>(loginAction, {});
  return (
    <form action={action} className="space-y-4">
      <input type="hidden" name="next" value={next} />
      {state.error && <Alert>{state.error}</Alert>}
      <Field label="Email">
        <Input name="email" type="email" required autoComplete="email" defaultValue={state.values?.email} />
      </Field>
      <FieldError message={state.fieldErrors?.email} />
      <Field label="Mật khẩu">
        <Input name="password" type="password" required autoComplete="current-password" />
      </Field>
      <Button type="submit" loading={pending} className="w-full">
        Đăng nhập
      </Button>
      <p className="text-center text-sm text-slate-500">
        Chưa có tài khoản?{" "}
        <Link href={`/register?next=${encodeURIComponent(next)}`} className="font-medium text-indigo-600 hover:underline">
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
      {state.error && <Alert>{state.error}</Alert>}
      <Field label="Họ và tên">
        <Input name="fullName" required maxLength={150} defaultValue={state.values?.fullName} />
      </Field>
      <FieldError message={err.fullName} />
      <Field label="Email">
        <Input name="email" type="email" required autoComplete="email" defaultValue={state.values?.email} />
      </Field>
      <FieldError message={err.email} />
      <Field label="Số điện thoại (không bắt buộc)">
        <Input name="phone" maxLength={20} defaultValue={state.values?.phone} />
      </Field>
      <Field label="Mật khẩu" hint="Từ 6 đến 50 ký tự">
        <Input name="password" type="password" required minLength={6} maxLength={50} autoComplete="new-password" />
      </Field>
      <FieldError message={err.password} />
      <Field label="Nhập lại mật khẩu">
        <Input name="confirmPassword" type="password" required autoComplete="new-password" />
      </Field>
      <FieldError message={err.confirmPassword} />
      <Button type="submit" loading={pending} className="w-full">
        Tạo tài khoản
      </Button>
      <p className="text-center text-sm text-slate-500">
        Đã có tài khoản?{" "}
        <Link href={`/login?next=${encodeURIComponent(next)}`} className="font-medium text-indigo-600 hover:underline">
          Đăng nhập
        </Link>
      </p>
    </form>
  );
}

function FieldError({ message }: { message?: string }) {
  return message ? <p className="-mt-2 text-sm text-rose-600">{message}</p> : null;
}
