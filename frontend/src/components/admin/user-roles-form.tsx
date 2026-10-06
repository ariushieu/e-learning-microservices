"use client";

import { useState, type FormEvent } from "react";
import { api } from "@/lib/client";
import type { Role, User } from "@/lib/types";
import { Alert, Badge, Button, Field, Input } from "@/components/ui";
import { FieldError, fieldErrorMap, formErrorMessage } from "@/components/instructor/form-helpers";

const ROLES: { value: Role; label: string; hint: string }[] = [
  { value: "ROLE_STUDENT", label: "Học viên", hint: "Ghi danh, học, làm bài kiểm tra" },
  { value: "ROLE_INSTRUCTOR", label: "Giảng viên", hint: "Tạo và quản lý khóa học, bài kiểm tra" },
  { value: "ROLE_ADMIN", label: "Quản trị viên", hint: "Toàn quyền, cấp quyền người dùng, quản lý danh mục" },
];

const ROLE_LABELS: Record<string, string> = Object.fromEntries(ROLES.map((r) => [r.value, r.label]));

export function UserRolesForm() {
  const [roles, setRoles] = useState<Role[]>(["ROLE_STUDENT"]);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [result, setResult] = useState<User | null>(null);

  function toggle(role: Role, checked: boolean) {
    setRoles((prev) => (checked ? [...prev.filter((r) => r !== role), role] : prev.filter((r) => r !== role)));
  }

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const userId = Number(new FormData(e.currentTarget).get("userId"));
    setError(null);
    setErrors({});
    setResult(null);
    if (!Number.isInteger(userId) || userId <= 0) {
      setErrors({ userId: "Mã người dùng phải là số nguyên dương." });
      return;
    }
    if (roles.length === 0) {
      setErrors({ roles: "Chọn ít nhất một vai trò." });
      return;
    }
    if (!window.confirm(`Đặt lại toàn bộ vai trò của người dùng #${userId} thành: ${roles.map((r) => ROLE_LABELS[r]).join(", ")}?`)) {
      return;
    }
    setPending(true);
    try {
      const user = await api<User>(`/api/users/${userId}/roles`, { method: "PATCH", body: { roles } });
      setResult(user);
    } catch (err) {
      setErrors(fieldErrorMap(err));
      setError(formErrorMessage(err));
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-4">
      <p className="text-sm text-slate-500">
        Hệ thống chưa có chức năng tìm người dùng. Người dùng xem <b>mã người dùng</b> của mình ở trang{" "}
        <span className="font-mono">/profile</span> (Hồ sơ) rồi gửi cho bạn.
      </p>
      {error && <Alert>{error}</Alert>}
      <div>
        <Field label="Mã người dùng (ID) *">
          <Input name="userId" type="number" min={1} step={1} required placeholder="VD: 5" className="max-w-40" />
        </Field>
        <FieldError message={errors.userId} />
      </div>

      <fieldset>
        <legend className="text-sm font-medium text-slate-700">Vai trò (thay thế toàn bộ vai trò hiện có)</legend>
        <div className="mt-2 space-y-2">
          {ROLES.map((r) => (
            <label key={r.value} className="flex items-start gap-3 rounded-lg border border-slate-200 p-3 hover:bg-slate-50">
              <input
                type="checkbox"
                checked={roles.includes(r.value)}
                onChange={(e) => toggle(r.value, e.target.checked)}
                className="mt-0.5 h-4 w-4 rounded border-slate-300 text-indigo-600"
              />
              <span>
                <span className="block text-sm font-medium text-slate-800">{r.label}</span>
                <span className="block text-xs text-slate-500">{r.hint}</span>
              </span>
            </label>
          ))}
        </div>
        <FieldError message={errors.roles} />
      </fieldset>

      <Button type="submit" loading={pending}>
        Cập nhật vai trò
      </Button>

      {result && (
        <Alert kind="success">
          <p className="font-medium">Đã cập nhật vai trò cho người dùng #{result.id}.</p>
          <dl className="mt-2 space-y-1">
            <div>
              <dt className="inline text-emerald-800">Họ tên: </dt>
              <dd className="inline">{result.fullName}</dd>
            </div>
            <div>
              <dt className="inline text-emerald-800">Email: </dt>
              <dd className="inline">{result.email}</dd>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <dt className="text-emerald-800">Vai trò:</dt>
              {result.roles.map((r) => (
                <dd key={r}>
                  <Badge value={ROLE_LABELS[r] ?? r} />
                </dd>
              ))}
            </div>
          </dl>
          <p className="mt-2">Người dùng cần đăng xuất và đăng nhập lại để nhận vai trò mới.</p>
        </Alert>
      )}
    </form>
  );
}
