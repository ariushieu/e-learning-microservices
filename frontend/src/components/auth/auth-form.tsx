"use client";

import { Loader2Icon } from "lucide-react";
import Link from "next/link";
import { useActionState, type ReactNode } from "react";
import { ErrorAlert } from "@/components/common/error-alert";
import { FieldHint, FormField } from "@/components/common/form-field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { loginAction, registerAction, type FormState } from "@/lib/server/auth-actions";

export function LoginForm({ next }: { next: string }) {
  const [state, action, pending] = useActionState<FormState, FormData>(loginAction, {});
  return (
    <form action={action} className="space-y-5">
      <input type="hidden" name="next" value={next} />
      {state.error && <ErrorAlert message={state.error} />}
      <FormField id="email" label="Email" error={state.fieldErrors?.email}>
        <Input id="email" name="email" type="email" required autoComplete="email" placeholder="ban@hunre.edu.vn" defaultValue={state.values?.email} />
      </FormField>
      <FormField id="password" label="Mật khẩu">
        <Input id="password" name="password" type="password" required autoComplete="current-password" />
      </FormField>
      <SubmitButton pending={pending}>Đăng nhập</SubmitButton>
      <SwitchLink question="Chưa có tài khoản?" href={`/register?next=${encodeURIComponent(next)}`}>
        Đăng ký
      </SwitchLink>
    </form>
  );
}

export function RegisterForm({ next }: { next: string }) {
  const [state, action, pending] = useActionState<FormState, FormData>(registerAction, {});
  const err = state.fieldErrors ?? {};
  return (
    // Use server validation so Unicode email errors appear inline in Vietnamese,
    // instead of being intercepted by the browser's native email tooltip.
    <form action={action} className="space-y-5" noValidate>
      <input type="hidden" name="next" value={next} />
      {state.error && <ErrorAlert message={state.error} />}
      <FormField id="fullName" label="Họ và tên" error={err.fullName}>
        <Input id="fullName" name="fullName" required maxLength={150} autoComplete="name" defaultValue={state.values?.fullName} />
      </FormField>
      <FormField id="email" label="Email" error={err.email}>
        <Input
          id="email"
          name="email"
          type="email"
          required
          autoComplete="email"
          maxLength={255}
          aria-invalid={Boolean(err.email)}
          placeholder="ban@hunre.edu.vn"
          defaultValue={state.values?.email}
        />
      </FormField>
      <FormField
        id="phone"
        error={err.phone}
        label={
          <>
            Số điện thoại <span className="font-normal text-muted-foreground">(không bắt buộc)</span>
          </>
        }
      >
        <Input id="phone" name="phone" maxLength={30} autoComplete="tel" aria-invalid={Boolean(err.phone)} defaultValue={state.values?.phone} />
      </FormField>
      <div className="space-y-2">
        <div className="grid gap-5 sm:grid-cols-2 sm:gap-4">
          <FormField id="password" label="Mật khẩu" error={err.password}>
            <Input id="password" name="password" type="password" required minLength={6} maxLength={50} autoComplete="new-password" />
          </FormField>
          <FormField id="confirmPassword" label="Nhập lại" error={err.confirmPassword}>
            <Input id="confirmPassword" name="confirmPassword" type="password" required autoComplete="new-password" />
          </FormField>
        </div>
        <FieldHint>Mật khẩu từ 6 đến 50 ký tự.</FieldHint>
      </div>
      <SubmitButton pending={pending}>Tạo tài khoản</SubmitButton>
      <SwitchLink question="Đã có tài khoản?" href={`/login?next=${encodeURIComponent(next)}`}>
        Đăng nhập
      </SwitchLink>
    </form>
  );
}

function SubmitButton({ pending, children }: { pending: boolean; children: ReactNode }) {
  return (
    <Button type="submit" size="lg" className="w-full" disabled={pending}>
      {pending && <Loader2Icon className="animate-spin" aria-hidden />}
      {children}
    </Button>
  );
}

function SwitchLink({ question, href, children }: { question: string; href: string; children: ReactNode }) {
  return (
    <p className="border-t pt-5 text-center text-sm text-muted-foreground">
      {question}{" "}
      <Link href={href} className="font-medium text-primary underline-offset-4 hover:underline">
        {children}
      </Link>
    </p>
  );
}
