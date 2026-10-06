"use client";

import { CornerDownRightIcon, FolderIcon, FolderTreeIcon, Loader2Icon, PencilIcon, PlusIcon, Trash2Icon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useId, useState, type FormEvent } from "react";
import { toast } from "sonner";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
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
import { Button } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import { fieldErrorMap, formErrorMessage, optional, optionalNumber, text } from "@/lib/forms";
import type { Category } from "@/lib/types";
import { cn } from "@/lib/utils";
import { Field, NativeSelect } from "./form-helpers";

export function CategoryManager({ tree }: { tree: Category[] }) {
  const [creating, setCreating] = useState(false);
  const total = tree.reduce((n, r) => n + 1 + (r.subCategories?.length ?? 0), 0);

  return (
    <Card className="gap-0 pb-0">
      <CardHeader className="border-b">
        <CardTitle>Cây danh mục</CardTitle>
        <CardDescription>
          {total} danh mục. Không xóa được danh mục còn danh mục con hoặc còn khóa học.
        </CardDescription>
        <CardAction>
          <Button onClick={() => setCreating(true)}>
            <PlusIcon /> Thêm danh mục
          </Button>
        </CardAction>
      </CardHeader>
      <CardContent className="px-0">
        {tree.length === 0 ? (
          <div className="p-4">
            <EmptyState
              icon={FolderTreeIcon}
              title="Chưa có danh mục nào"
              description="Tạo danh mục đầu tiên để giảng viên tạo được khóa học."
            />
          </div>
        ) : (
          <ul className="divide-y">
            {tree.map((root) => (
              <li key={root.id}>
                <CategoryRow category={root} roots={tree} />
                {root.subCategories?.length > 0 && (
                  <ul className="divide-y border-t">
                    {root.subCategories.map((sub) => (
                      <li key={sub.id}>
                        <CategoryRow category={sub} roots={tree} />
                      </li>
                    ))}
                  </ul>
                )}
              </li>
            ))}
          </ul>
        )}
      </CardContent>

      <Dialog open={creating} onOpenChange={setCreating}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Thêm danh mục</DialogTitle>
            <DialogDescription>Danh mục mới hiện ngay ở bộ lọc trang chủ và form tạo khóa học.</DialogDescription>
          </DialogHeader>
          <CategoryForm roots={tree} onDone={() => setCreating(false)} />
        </DialogContent>
      </Dialog>
    </Card>
  );
}

function CategoryRow({ category, roots }: { category: Category; roots: Category[] }) {
  const router = useRouter();
  const [editing, setEditing] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const isChild = category.parentId !== null;

  async function remove() {
    setDeleting(true);
    setError(null);
    try {
      await api(`/api/categories/${category.id}`, { method: "DELETE" });
      toast.success(`Đã xóa danh mục ${category.name}`);
      router.refresh();
    } catch (e) {
      // 422 khi còn danh mục con hoặc còn khóa học: thông báo của backend đã nói rõ lý do.
      setError(errorMessage(e));
      setDeleting(false);
    }
  }

  return (
    <div className={cn("px-4 py-3", isChild && "pl-10")}>
      <div className="flex items-center gap-3">
        {isChild ? (
          <CornerDownRightIcon className="size-4 shrink-0 text-muted-foreground" />
        ) : (
          <FolderIcon className="size-4 shrink-0 text-muted-foreground" />
        )}
        <div className="min-w-0 flex-1">
          <p className={cn("truncate text-sm", isChild ? "font-medium" : "font-semibold")}>{category.name}</p>
          <p className="truncate text-xs text-muted-foreground">
            <span className="font-mono">{category.slug}</span> · Thứ tự {category.position}
            {category.description && ` · ${category.description}`}
          </p>
        </div>
        <div className="flex shrink-0 gap-1">
          <Button variant="ghost" size="icon-sm" aria-label={`Sửa danh mục ${category.name}`} onClick={() => setEditing(true)}>
            <PencilIcon />
          </Button>
          <Button
            variant="ghost"
            size="icon-sm"
            className="text-muted-foreground hover:text-destructive"
            aria-label={`Xóa danh mục ${category.name}`}
            disabled={deleting}
            onClick={() => setConfirming(true)}
          >
            {deleting ? <Loader2Icon className="animate-spin" /> : <Trash2Icon />}
          </Button>
        </div>
      </div>
      {error && (
        <div className="mt-2">
          <ErrorAlert message={error} />
        </div>
      )}

      <Dialog open={editing} onOpenChange={setEditing}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Sửa danh mục</DialogTitle>
            <DialogDescription>{category.name}</DialogDescription>
          </DialogHeader>
          <CategoryForm roots={roots} category={category} onDone={() => setEditing(false)} />
        </DialogContent>
      </Dialog>

      <AlertDialog open={confirming} onOpenChange={setConfirming}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa danh mục “{category.name}”?</AlertDialogTitle>
            <AlertDialogDescription>Chỉ xóa được khi danh mục không còn danh mục con và không còn khóa học.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction variant="destructive" onClick={remove}>
              Xóa
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

/** Tạo (không có `category`) hoặc sửa danh mục; PUT ghi đè mọi trường nên gửi đủ object. */
function CategoryForm({ roots, category, onDone }: { roots: Category[]; category?: Category; onDone: () => void }) {
  const router = useRouter();
  const id = useId();
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
        toast.success("Đã lưu danh mục");
      } else {
        await api<Category>("/api/categories", { method: "POST", body });
        toast.success("Đã tạo danh mục");
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
    <form onSubmit={onSubmit} className="space-y-5">
      {error && <ErrorAlert message={error} />}
      <div className="grid gap-5 sm:grid-cols-2">
        <Field id={`${id}-name`} label="Tên danh mục" required error={errors.name}>
          <Input id={`${id}-name`} name="name" required maxLength={150} defaultValue={category?.name} autoFocus />
        </Field>
        <Field id={`${id}-slug`} label="Slug" hint="Để trống thì tự sinh từ tên." error={errors.slug}>
          <Input id={`${id}-slug`} name="slug" maxLength={180} defaultValue={category?.slug} />
        </Field>
        <Field
          id={`${id}-parent`}
          label="Danh mục cha"
          hint={hasChildren ? "Danh mục đang có danh mục con nên phải là danh mục gốc." : "Chỉ hỗ trợ một cấp lồng nhau."}
          error={errors.parentId}
        >
          <NativeSelect id={`${id}-parent`} name="parentId" defaultValue={category?.parentId ?? ""} disabled={hasChildren}>
            <option value="">Không có (danh mục gốc)</option>
            {parentChoices.map((r) => (
              <option key={r.id} value={r.id}>
                {r.name}
              </option>
            ))}
          </NativeSelect>
        </Field>
        <Field id={`${id}-position`} label="Thứ tự hiển thị" error={errors.position}>
          <Input id={`${id}-position`} name="position" type="number" min={0} step={1} defaultValue={category?.position ?? 0} />
        </Field>
      </div>
      <Field id={`${id}-description`} label="Mô tả" error={errors.description}>
        <Textarea id={`${id}-description`} name="description" rows={2} maxLength={500} defaultValue={category?.description ?? ""} />
      </Field>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onDone} disabled={pending}>
          Hủy
        </Button>
        <Button type="submit" disabled={pending}>
          {pending && <Loader2Icon className="animate-spin" />}
          {category ? "Lưu" : "Tạo danh mục"}
        </Button>
      </div>
    </form>
  );
}
