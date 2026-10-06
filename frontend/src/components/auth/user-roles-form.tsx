"use client";

import { CircleCheckIcon, Loader2Icon, UserCheckIcon } from "lucide-react";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { RoleBadges } from "@/components/auth/role-badge";
import { ErrorAlert } from "@/components/common/error-alert";
import { FieldError } from "@/components/common/field-error";
import { FieldHint, FormActions, FormField, FormSection } from "@/components/common/form-field";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { api } from "@/lib/client";
import { initials, label } from "@/lib/format";
import { fieldErrorMap, formErrorMessage } from "@/lib/forms";
import type { Role, User } from "@/lib/types";

const ROLES: { value: Role; hint: string }[] = [
  { value: "ROLE_STUDENT", hint: "Ghi danh, học, làm bài kiểm tra" },
  { value: "ROLE_INSTRUCTOR", hint: "Tạo và quản lý khóa học, bài kiểm tra" },
  { value: "ROLE_ADMIN", hint: "Toàn quyền: cấp quyền người dùng, quản lý danh mục" },
];

/** Đặt lại toàn bộ vai trò của một tài khoản theo mã người dùng (backend chưa có API tìm người dùng). */
export function UserRolesForm() {
  const [userId, setUserId] = useState("");
  const [roles, setRoles] = useState<Role[]>(["ROLE_STUDENT"]);
  const [confirming, setConfirming] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [result, setResult] = useState<User | null>(null);

  function toggle(role: Role, checked: boolean) {
    setRoles((prev) => (checked ? [...prev.filter((r) => r !== role), role] : prev.filter((r) => r !== role)));
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setErrors({});
    const id = Number(userId);
    if (!Number.isInteger(id) || id <= 0) return setErrors({ userId: "Mã người dùng phải là số nguyên dương." });
    if (roles.length === 0) return setErrors({ roles: "Chọn ít nhất một vai trò." });
    setConfirming(true);
  }

  async function save() {
    setConfirming(false);
    setPending(true);
    try {
      const user = await api<User>(`/api/users/${Number(userId)}/roles`, { method: "PATCH", body: { roles } });
      setResult(user);
      toast.success(`Đã cập nhật vai trò cho ${user.fullName}`);
    } catch (err) {
      setErrors(fieldErrorMap(err));
      setError(formErrorMessage(err));
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="max-w-4xl space-y-8">
      {error && <ErrorAlert message={error} />}

      <FormSection title="Tài khoản" description="Backend chưa có tìm kiếm người dùng: nhập mã mà người đó xem được ở trang Hồ sơ.">
        <FormField id="userId" label="Mã người dùng" hint="Người dùng xem mã của mình ở trang Hồ sơ." error={errors.userId}>
          <Input id="userId" inputMode="numeric" value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="VD: 12" className="max-w-40" />
        </FormField>
        {result && <UpdatedUser user={result} />}
      </FormSection>

      <FormSection title="Vai trò" description="Vai trò được thay thế toàn bộ. Người dùng phải đăng nhập lại để nhận vai trò mới.">
        <fieldset className="space-y-3">
          <legend className="sr-only">Vai trò</legend>
          <div className="grid gap-2">
            {ROLES.map((r) => (
              <label
                key={r.value}
                className="flex cursor-pointer items-start gap-3 rounded-lg border p-3 transition-colors hover:bg-muted/60 has-[[data-state=checked]]:border-primary/40 has-[[data-state=checked]]:bg-primary-soft/60"
              >
                <Checkbox checked={roles.includes(r.value)} onCheckedChange={(c) => toggle(r.value, c === true)} className="mt-0.5" />
                <span className="min-w-0">
                  <span className="block text-sm font-medium">{label(r.value)}</span>
                  <span className="block text-xs text-muted-foreground">{r.hint}</span>
                </span>
              </label>
            ))}
          </div>
          <FieldError message={errors.roles} />
          {!errors.roles && <FieldHint>Chọn ít nhất một vai trò.</FieldHint>}
        </fieldset>
      </FormSection>

      <FormActions>
        <Button type="submit" size="lg" disabled={pending}>
          {pending ? <Loader2Icon className="animate-spin" aria-hidden /> : <UserCheckIcon aria-hidden />}
          Cập nhật vai trò
        </Button>
      </FormActions>

      <AlertDialog open={confirming} onOpenChange={setConfirming}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Đặt lại vai trò cho người dùng #{userId}?</AlertDialogTitle>
            <AlertDialogDescription>
              Vai trò mới: {roles.map(label).join(", ")}. Mọi vai trò khác của tài khoản này sẽ bị gỡ.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction onClick={save}>Xác nhận</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </form>
  );
}

/** Tài khoản vừa được đặt lại vai trò (API trả về người dùng sau khi cập nhật). */
function UpdatedUser({ user }: { user: User }) {
  return (
    <div className="flex items-center gap-3 rounded-lg border bg-muted/40 p-3" aria-live="polite">
      <Avatar className="size-10">
        <AvatarFallback className="bg-primary-soft font-semibold text-primary-strong">{initials(user.fullName)}</AvatarFallback>
      </Avatar>
      <div className="min-w-0 flex-1 space-y-1">
        <p className="flex items-center gap-1.5 text-sm font-medium">
          <span className="truncate">{user.fullName}</span>
          <span className="shrink-0 font-normal text-muted-foreground">#{user.id}</span>
        </p>
        <p className="truncate text-xs text-muted-foreground">{user.email}</p>
        <RoleBadges roles={user.roles} />
      </div>
      <CircleCheckIcon className="size-4 shrink-0 text-success" aria-label="Đã lưu" />
    </div>
  );
}
