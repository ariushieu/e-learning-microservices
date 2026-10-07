"use client";

import { Loader2Icon, LockKeyholeIcon, SaveIcon, ShieldCheckIcon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { ErrorAlert } from "@/components/common/error-alert";
import { FieldHint, FormActions, FormField } from "@/components/common/form-field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api } from "@/lib/client";
import { fieldErrorMap, formErrorMessage } from "@/lib/forms";
import type { User } from "@/lib/types";
import { logoutToLoginAction, refreshSessionAction } from "@/lib/server/auth-actions";

export function ProfileDetailsForm({ user }: { user: User }) {
  const router = useRouter();
  const [fullName, setFullName] = useState(user.fullName);
  const [phone, setPhone] = useState(user.phone ?? "");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setErrors({});
    try {
      const updated = await api<User>("/api/auth/me", {
        method: "PUT",
        body: { fullName, phone: phone.trim() || null },
      });
      setFullName(updated.fullName);
      setPhone(updated.phone ?? "");
      toast.success("Đã cập nhật thông tin cá nhân");
      // Họ tên trên header đọc từ access token, nên phải lấy token mới thì header mới đổi.
      await refreshSessionAction();
      router.refresh();
    } catch (cause) {
      const fieldErrors = fieldErrorMap(cause);
      setErrors(fieldErrors);
      if (Object.keys(fieldErrors).length === 0) setError(formErrorMessage(cause));
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-5" noValidate>
      {error && <ErrorAlert message={error} />}
      <FormField id="profile-full-name" label="Họ và tên" required error={errors.fullName}>
        <Input
          id="profile-full-name"
          name="fullName"
          autoComplete="name"
          maxLength={150}
          required
          value={fullName}
          aria-invalid={Boolean(errors.fullName)}
          onChange={(event) => setFullName(event.target.value)}
        />
      </FormField>
      <FormField
        id="profile-phone"
        label={<>Số điện thoại <span className="font-normal text-muted-foreground">(không bắt buộc)</span></>}
        hint="Để trống nếu bạn muốn xóa số điện thoại đã lưu."
        error={errors.phone}
      >
        <Input
          id="profile-phone"
          name="phone"
          type="tel"
          autoComplete="tel"
          inputMode="tel"
          maxLength={30}
          value={phone}
          aria-invalid={Boolean(errors.phone)}
          onChange={(event) => setPhone(event.target.value)}
        />
      </FormField>
      <div className="flex flex-col gap-4 border-t pt-4 sm:flex-row sm:items-center sm:justify-between">
        <FieldHint>Email đăng nhập: {user.email}. Email không thể sửa tại đây.</FieldHint>
        <FormActions className="shrink-0">
          <Button type="submit" disabled={saving}>
            {saving ? <Loader2Icon className="animate-spin" aria-hidden /> : <SaveIcon aria-hidden />}
            {saving ? "Đang lưu…" : "Lưu thông tin"}
          </Button>
        </FormActions>
      </div>
    </form>
  );
}

export function ChangePasswordForm() {
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrors({});
    setError(null);
    const form = new FormData(event.currentTarget);
    const currentPassword = String(form.get("currentPassword") ?? "");
    const newPassword = String(form.get("newPassword") ?? "");
    const confirmPassword = String(form.get("confirmPassword") ?? "");
    if (newPassword !== confirmPassword) {
      setErrors({ confirmPassword: "Mật khẩu nhập lại chưa khớp." });
      return;
    }

    setSaving(true);
    try {
      await api<void>("/api/auth/change-password", {
        method: "POST",
        body: { currentPassword, newPassword },
      });
      toast.success("Đổi mật khẩu thành công. Vui lòng đăng nhập lại.");
      await logoutToLoginAction();
    } catch (cause) {
      const fieldErrors = fieldErrorMap(cause);
      setErrors(fieldErrors);
      if (Object.keys(fieldErrors).length === 0) setError(formErrorMessage(cause));
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-5" noValidate>
      {error && <ErrorAlert message={error} />}
      <FormField id="current-password" label="Mật khẩu hiện tại" required error={errors.currentPassword}>
        <Input
          id="current-password"
          name="currentPassword"
          type="password"
          autoComplete="current-password"
          required
          aria-invalid={Boolean(errors.currentPassword)}
        />
      </FormField>
      <div className="grid gap-5 sm:grid-cols-2 sm:gap-4">
        <FormField id="new-password" label="Mật khẩu mới" required error={errors.newPassword}>
          <Input
            id="new-password"
            name="newPassword"
            type="password"
            autoComplete="new-password"
            minLength={6}
            maxLength={50}
            required
            aria-invalid={Boolean(errors.newPassword)}
          />
        </FormField>
        <FormField id="confirm-password" label="Nhập lại mật khẩu mới" required error={errors.confirmPassword}>
          <Input
            id="confirm-password"
            name="confirmPassword"
            type="password"
            autoComplete="new-password"
            minLength={6}
            maxLength={50}
            required
            aria-invalid={Boolean(errors.confirmPassword)}
          />
        </FormField>
      </div>
      <div className="flex flex-col gap-4 border-t pt-4 sm:flex-row sm:items-center sm:justify-between">
        <FieldHint>Mật khẩu từ 6 đến 50 ký tự. Đổi xong, mọi phiên đăng nhập sẽ cần đăng nhập lại.</FieldHint>
        <FormActions className="shrink-0">
          <Button type="submit" disabled={saving}>
            {saving ? <Loader2Icon className="animate-spin" aria-hidden /> : <LockKeyholeIcon aria-hidden />}
            {saving ? "Đang đổi…" : "Đổi mật khẩu"}
          </Button>
        </FormActions>
      </div>
      <p className="flex items-start gap-2 text-xs text-muted-foreground">
        <ShieldCheckIcon className="mt-0.5 size-4 shrink-0" aria-hidden />
        Mật khẩu mới được mã hóa. Bạn sẽ được đưa về màn hình đăng nhập sau khi hoàn tất.
      </p>
    </form>
  );
}
