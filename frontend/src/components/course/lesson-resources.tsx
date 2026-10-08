"use client";

import { ExternalLinkIcon, Loader2Icon, PaperclipIcon, Trash2Icon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useId, useState, type FormEvent } from "react";
import { toast } from "sonner";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { FormField } from "@/components/common/form-field";
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
import { Input } from "@/components/ui/input";
import { api } from "@/lib/client";
import { fieldErrorMap, formErrorMessage } from "@/lib/forms";
import type { LessonResource } from "@/lib/types";

function safeLink(value: string) {
  try {
    const url = new URL(value);
    return ["http:", "https:"].includes(url.protocol) && !url.username && !url.password;
  } catch {
    return false;
  }
}

export function LessonResources({
  lessonId,
  initialResources,
}: {
  lessonId: number;
  initialResources: LessonResource[];
}) {
  const id = useId();
  const router = useRouter();
  const [resources, setResources] = useState(initialResources);
  const [name, setName] = useState("");
  const [fileUrl, setFileUrl] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [deleting, setDeleting] = useState<LessonResource | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  async function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    setErrors({});
    try {
      const resource = await api<LessonResource>(`/api/lessons/${lessonId}/resources`, {
        method: "POST",
        body: { name: name.trim(), fileUrl: fileUrl.trim() },
      });
      setResources((items) => [...items, resource]);
      setName("");
      setFileUrl("");
      toast.success("Đã thêm tài liệu đính kèm");
      router.refresh();
    } catch (cause) {
      setErrors(fieldErrorMap(cause));
      setError(formErrorMessage(cause));
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!deleting || busy) return;
    setBusy(true);
    setDeleteError(null);
    try {
      await api(`/api/lessons/${lessonId}/resources/${deleting.id}`, {
        method: "DELETE",
      });
      setResources((items) => items.filter((item) => item.id !== deleting.id));
      setDeleting(null);
      toast.success("Đã xóa tài liệu đính kèm");
      router.refresh();
    } catch (cause) {
      setDeleteError(formErrorMessage(cause));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="min-w-0 space-y-4 border-t pt-5" aria-labelledby={`${id}-heading`}>
      <div>
        <h3 id={`${id}-heading`} className="text-subheading">
          Tài liệu đính kèm
        </h3>
        <p className="mt-1 text-sm text-muted-foreground">
          Thêm liên kết tới PDF, slide hoặc tài liệu tham khảo. Tài liệu được lưu riêng với nội dung bài.
        </p>
      </div>
      {resources.length ? (
        <ul className="divide-y rounded-lg border">
          {resources.map((resource) => (
            <li key={resource.id} className="flex min-w-0 items-center gap-3 p-3">
              <PaperclipIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
              <div className="min-w-0 flex-1 break-all text-sm">
                {safeLink(resource.fileUrl) ? (
                  <a
                    href={resource.fileUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="font-medium text-primary underline-offset-4 hover:underline"
                  >
                    {resource.name}
                    <ExternalLinkIcon className="ml-1 inline size-3" aria-hidden />
                  </a>
                ) : (
                  <span>{resource.name} — Liên kết không hợp lệ, hãy xóa và thêm lại.</span>
                )}
              </div>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                disabled={busy}
                aria-label={`Xóa tài liệu ${resource.name}`}
                onClick={() => {
                  setDeleteError(null);
                  setDeleting(resource);
                }}
              >
                <Trash2Icon aria-hidden />
              </Button>
            </li>
          ))}
        </ul>
      ) : (
        <EmptyState icon={PaperclipIcon} title="Chưa có tài liệu đính kèm" />
      )}
      <form onSubmit={add} className="space-y-4" noValidate>
        {error && <ErrorAlert message={error} />}
        <FormField id={`${id}-name`} label="Tên tài liệu" required error={errors.name}>
          <Input
            id={`${id}-name`}
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={200}
            disabled={busy}
            aria-invalid={Boolean(errors.name)}
          />
        </FormField>
        <FormField
          id={`${id}-url`}
          label="URL tài liệu"
          required
          error={errors.fileUrl}
          hint="Địa chỉ đầy đủ bắt đầu bằng http:// hoặc https://."
        >
          <Input
            id={`${id}-url`}
            type="url"
            value={fileUrl}
            onChange={(event) => setFileUrl(event.target.value)}
            maxLength={500}
            disabled={busy}
            aria-invalid={Boolean(errors.fileUrl)}
            placeholder="https://..."
          />
        </FormField>
        <Button type="submit" variant="outline" disabled={busy}>
          {busy ? <Loader2Icon className="animate-spin" aria-hidden /> : <PaperclipIcon aria-hidden />}
          Thêm tài liệu
        </Button>
      </form>
      <AlertDialog
        open={Boolean(deleting)}
        onOpenChange={(open) => {
          if (!open && !busy) setDeleting(null);
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa tài liệu đính kèm?</AlertDialogTitle>
            <AlertDialogDescription className="break-all">
              Tài liệu “{deleting?.name}” sẽ không còn xuất hiện trong bài học. Tệp tại địa chỉ gốc vẫn được giữ.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deleteError && <ErrorAlert message={deleteError} />}
          <AlertDialogFooter>
            <AlertDialogCancel disabled={busy}>Giữ lại</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={busy}
              onClick={(event) => {
                event.preventDefault();
                void remove();
              }}
            >
              {busy && <Loader2Icon className="animate-spin" aria-hidden />}
              Xác nhận xóa tài liệu
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </section>
  );
}
