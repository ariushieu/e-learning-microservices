"use client";

import { Loader2Icon, UserCheckIcon } from "lucide-react";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { ErrorAlert } from "@/components/common/error-alert";
import { FieldError } from "@/components/common/field-error";
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
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api } from "@/lib/client";
import { label } from "@/lib/format";
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
    <form onSubmit={onSubmit} className="space-y-6">
      {error && <ErrorAlert message={error} />}
      <div className="space-y-2">
        <Label htmlFor="userId">Mã người dùng</Label>
        <Input id="userId" inputMode="numeric" value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="VD: 12" className="max-w-40" />
        <p className="text-xs text-muted-foreground">Người dùng xem mã của mình ở trang Hồ sơ.</p>
        <FieldError message={errors.userId} />
      </div>

      <div className="space-y-3">
        <Label>Vai trò</Label>
        <div className="grid gap-2">
          {ROLES.map((r) => (
            <label key={r.value} className="flex cursor-pointer items-start gap-3 rounded-lg border p-3 transition-colors hover:bg-muted/50 has-[[data-state=checked]]:border-primary/40 has-[[data-state=checked]]:bg-primary/5">
              <Checkbox checked={roles.includes(r.value)} onCheckedChange={(c) => toggle(r.value, c === true)} className="mt-0.5" />
              <span>
                <span className="block text-sm font-medium">{label(r.value)}</span>
                <span className="block text-xs text-muted-foreground">{r.hint}</span>
              </span>
            </label>
          ))}
        </div>
        <FieldError message={errors.roles} />
        <p className="text-xs text-muted-foreground">Vai trò được thay thế toàn bộ. Người dùng phải đăng nhập lại để nhận vai trò mới.</p>
      </div>

      <Button type="submit" size="lg" disabled={pending}>
        {pending ? <Loader2Icon className="animate-spin" /> : <UserCheckIcon />} Cập nhật vai trò
      </Button>

      {result && (
        <div className="rounded-lg border bg-muted/40 p-4 text-sm">
          <p className="font-medium">
            {result.fullName} <span className="font-normal text-muted-foreground">· {result.email}</span>
          </p>
          <div className="mt-2 flex flex-wrap gap-1.5">
            {result.roles.map((r) => (
              <Badge key={r} variant="secondary">
                {label(r)}
              </Badge>
            ))}
          </div>
        </div>
      )}

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
