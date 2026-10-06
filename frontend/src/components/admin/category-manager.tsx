"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { Category } from "@/lib/types";
import { Alert, Button, Empty, Field, Input, Select, Textarea } from "@/components/ui";
import {
  FieldError,
  fieldErrorMap,
  formErrorMessage,
  optional,
  optionalNumber,
  text,
} from "@/components/instructor/form-helpers";

export function CategoryManager({ tree }: { tree: Category[] }) {
  const [creating, setCreating] = useState(false);

  return (
    <div className="space-y-4">
      {tree.length === 0 ? (
        <Empty>Chưa có danh mục nào. Tạo danh mục đầu tiên để giảng viên tạo được khóa học.</Empty>
      ) : (
        <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
          {tree.map((root) => (
            <li key={root.id}>
              <CategoryRow category={root} roots={tree} />
              {root.subCategories?.length > 0 && (
                <ul className="divide-y divide-slate-100 border-t border-slate-100 bg-slate-50/60">
                  {root.subCategories.map((sub) => (
                    <li key={sub.id} className="pl-6">
                      <CategoryRow category={sub} roots={tree} />
                    </li>
                  ))}
                </ul>
              )}
            </li>
          ))}
        </ul>
      )}

      {creating ? (
        <CategoryForm roots={tree} onDone={() => setCreating(false)} />
      ) : (
        <Button variant="secondary" onClick={() => setCreating(true)}>
          + Thêm danh mục
        </Button>
      )}
    </div>
  );
}

function CategoryRow({ category, roots }: { category: Category; roots: Category[] }) {
  const router = useRouter();
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function remove() {
    if (!window.confirm(`Xóa danh mục "${category.name}"?`)) return;
    setDeleting(true);
    setError(null);
    try {
      await api(`/api/categories/${category.id}`, { method: "DELETE" });
      router.refresh();
    } catch (e) {
      // 422 khi còn danh mục con hoặc còn khóa học: thông báo của backend đã nói rõ lý do.
      setError(errorMessage(e));
      setDeleting(false);
    }
  }

  if (editing) {
    return (
      <div className="p-3">
        <CategoryForm roots={roots} category={category} onDone={() => setEditing(false)} />
      </div>
    );
  }

  return (
    <div className="px-4 py-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          <div className="font-medium text-slate-900">
            {category.parentId && <span className="mr-1 text-slate-400">↳</span>}
            {category.name}
          </div>
          <div className="text-xs text-slate-500">
            <span className="font-mono">{category.slug}</span> · Thứ tự {category.position}
            {category.description && ` · ${category.description}`}
          </div>
        </div>
        <div className="flex gap-1">
          <Button variant="ghost" className="px-3 py-1.5" onClick={() => setEditing(true)}>
            Sửa
          </Button>
          <Button variant="ghost" className="px-3 py-1.5 text-rose-600" loading={deleting} onClick={remove}>
            Xóa
          </Button>
        </div>
      </div>
      {error && (
        <div className="mt-2">
          <Alert>{error}</Alert>
        </div>
      )}
    </div>
  );
}

/** Tạo (không có `category`) hoặc sửa danh mục; PUT ghi đè mọi trường nên gửi đủ object. */
function CategoryForm({ roots, category, onDone }: { roots: Category[]; category?: Category; onDone: () => void }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Chỉ một cấp lồng nhau: cha phải là danh mục gốc, và không được là chính nó.
  const parentChoices = roots.filter((r) => r.id !== category?.id);
  const hasChildren = (category?.subCategories?.length ?? 0) > 0;

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const parent = text(fd.get("parentId"));
    const body = {
      name: text(fd.get("name")),
      slug: optional(fd.get("slug")),
      parentId: parent ? Number(parent) : null,
      description: optional(fd.get("description")),
      position: optionalNumber(fd.get("position")) ?? 0,
    };
    setPending(true);
    setError(null);
    setErrors({});
    try {
      if (category) {
        await api<Category>(`/api/categories/${category.id}`, { method: "PUT", body });
      } else {
        await api<Category>("/api/categories", { method: "POST", body });
      }
      onDone();
      router.refresh();
    } catch (err) {
      setErrors(fieldErrorMap(err));
      setError(formErrorMessage(err));
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-4 rounded-lg border border-indigo-100 bg-indigo-50/40 p-4">
      <h3 className="text-sm font-semibold text-slate-800">
        {category ? `Sửa danh mục: ${category.name}` : "Thêm danh mục mới"}
      </h3>
      {error && <Alert>{error}</Alert>}
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <Field label="Tên danh mục *">
            <Input name="name" required maxLength={150} defaultValue={category?.name} />
          </Field>
          <FieldError message={errors.name} />
        </div>
        <div>
          <Field label="Slug" hint="Để trống thì tự sinh từ tên.">
            <Input name="slug" maxLength={180} defaultValue={category?.slug} />
          </Field>
          <FieldError message={errors.slug} />
        </div>
        <div>
          <Field
            label="Danh mục cha"
            hint={hasChildren ? "Danh mục đang có danh mục con nên phải là danh mục gốc." : "Chỉ hỗ trợ một cấp lồng nhau."}
          >
            <Select name="parentId" defaultValue={category?.parentId ?? ""} disabled={hasChildren}>
              <option value="">— Không có (danh mục gốc) —</option>
              {parentChoices.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name}
                </option>
              ))}
            </Select>
          </Field>
          <FieldError message={errors.parentId} />
        </div>
        <div>
          <Field label="Thứ tự hiển thị">
            <Input name="position" type="number" min={0} step={1} defaultValue={category?.position ?? 0} />
          </Field>
          <FieldError message={errors.position} />
        </div>
      </div>
      <div>
        <Field label="Mô tả">
          <Textarea name="description" rows={2} maxLength={500} defaultValue={category?.description ?? ""} />
        </Field>
        <FieldError message={errors.description} />
      </div>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="ghost" onClick={onDone} disabled={pending}>
          Hủy
        </Button>
        <Button type="submit" loading={pending}>
          {category ? "Lưu" : "Tạo danh mục"}
        </Button>
      </div>
    </form>
  );
}
