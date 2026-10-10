"use client";

import { Loader2Icon, MailCheckIcon } from "lucide-react";
import Link from "next/link";
import { useActionState, type ReactNode } from "react";
import { Callout } from "@/components/common/callout";
import { ErrorAlert } from "@/components/common/error-alert";
import { FieldHint, FormField } from "@/components/common/form-field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  forgotPasswordAction,
  loginAction,
  registerAction,
  resetPasswordAction,
  type FormState,
} from "@/lib/server/auth-actions";

export function LoginForm({ next }: { next: string }) {
  const [state, action, pending] = useActionState<FormState, FormData>(loginAction, {});
  return (
    <form action={action} className="space-y-5">
      <input type="hidden" name="next" value={next} />
      {state.error && <ErrorAlert message={state.error} />}
      <FormField id="email" label="Email" error={state.fieldErrors?.email}>
        <Input id="email" name="email" type="email" required autoComplete="email" placeholder="ban@hunre.edu.vn" defaultValue={state.values?.email} />
      </FormField>
      <div className="space-y-2">
        <FormField id="password" label="Mật khẩu">
          <Input id="password" name="password" type="password" required autoComplete="current-password" />
        </FormField>
        <p className="text-right text-sm">
          <Link href="/forgot-password" className="font-medium text-primary underline-offset-4 hover:underline">
            Quên mật khẩu?
          </Link>
        </p>
      </div>
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
    // Validate on the server and keep the raw address: type="email" can silently
    // convert a Unicode domain to ASCII punycode before it reaches validation.
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
          type="text"
          inputMode="email"
          autoCapitalize="none"
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

export function ForgotPasswordForm() {
  const [state, action, pending] = useActionState<FormState, FormData>(forgotPasswordAction, {});
  return (
    <form action={action} className="space-y-5" noValidate>
      {state.error && <ErrorAlert message={state.error} />}
      {state.done && (
        <Callout icon={MailCheckIcon} tone="success" title="Đã gửi yêu cầu">
          {state.done}
        </Callout>
      )}
      <FormField id="email" label="Email đã đăng ký" error={state.fieldErrors?.email}>
        <Input
          id="email"
          name="email"
          type="text"
          inputMode="email"
          autoCapitalize="none"
          required
          autoComplete="email"
          maxLength={255}
          aria-invalid={Boolean(state.fieldErrors?.email)}
          placeholder="ban@hunre.edu.vn"
          defaultValue={state.values?.email}
        />
      </FormField>
      <SubmitButton pending={pending}>{state.done ? "Gửi lại liên kết" : "Gửi liên kết đặt lại"}</SubmitButton>
      <SwitchLink question="Nhớ ra mật khẩu rồi?" href="/login">
        Đăng nhập
      </SwitchLink>
    </form>
  );
}

export function ResetPasswordForm({ token }: { token: string }) {
  const [state, action, pending] = useActionState<FormState, FormData>(resetPasswordAction, {});
  const err = state.fieldErrors ?? {};
  return (
    <form action={action} className="space-y-5">
      <input type="hidden" name="token" value={token} />
      {state.error && (
        <div className="space-y-2">
          <ErrorAlert message={state.error} />
          <p className="text-sm">
            <Link href="/forgot-password" className="font-medium text-primary underline-offset-4 hover:underline">
              Gửi liên kết mới
            </Link>
          </p>
        </div>
      )}
      <div className="space-y-2">
        <FormField id="password" label="Mật khẩu mới" error={err.password}>
          <Input id="password" name="password" type="password" required minLength={6} maxLength={50} autoComplete="new-password" aria-invalid={Boolean(err.password)} />
        </FormField>
        <FormField id="confirmPassword" label="Nhập lại mật khẩu mới" error={err.confirmPassword}>
          <Input id="confirmPassword" name="confirmPassword" type="password" required autoComplete="new-password" aria-invalid={Boolean(err.confirmPassword)} />
        </FormField>
        <FieldHint>Mật khẩu từ 6 đến 50 ký tự. Đặt xong, mọi thiết bị đang đăng nhập sẽ phải đăng nhập lại.</FieldHint>
      </div>
      <SubmitButton pending={pending}>Đặt mật khẩu mới</SubmitButton>
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
