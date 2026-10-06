"use client";

import { FileIcon, Loader2Icon, MoreHorizontalIcon, PaperclipIcon, PencilIcon, PlusIcon, Trash2Icon, TriangleAlertIcon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { ErrorAlert } from "@/components/common/error-alert";
import { FieldError } from "@/components/common/field-error";
import { IconTile } from "@/components/common/icon-tile";
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
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import { formatDuration } from "@/lib/format";
import { fieldErrorMap, formErrorMessage, text } from "@/lib/forms";
import type { Lesson, LessonResource } from "@/lib/types";
import { LESSON_TYPE_ICONS, lessonTypeLabels } from "./icons";
import { LessonForm } from "./lesson-form";

/** Một dòng bài học trong trình soạn đề cương: sửa, tài liệu đính kèm, xóa. */
export function LessonItem({ lesson, index }: { lesson: Lesson; index: number }) {
  const router = useRouter();
  const [dialog, setDialog] = useState<"edit" | "resources" | "delete" | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const missingUrl = (lesson.type === "VIDEO" || lesson.type === "FILE") && !lesson.contentUrl;

  async function remove() {
    setDeleting(true);
    setError(null);
    try {
      await api(`/api/lessons/${lesson.id}`, { method: "DELETE" });
      toast.success("Đã xóa bài học");
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
      setDeleting(false);
    }
  }

  const close = (open: boolean) => !open && setDialog(null);

  return (
    <li className="px-4 py-3">
      <div className="flex items-center gap-3">
        <IconTile icon={LESSON_TYPE_ICONS[lesson.type] ?? FileIcon} tone="neutral" size="sm" />
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium">
            <span className="text-muted-foreground tabular-nums">{index}.</span> {lesson.title}
          </p>
          <div className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
            <span>{lessonTypeLabels[lesson.type] ?? lesson.type}</span>
            <span className="tabular-nums">{formatDuration(lesson.durationSeconds)}</span>
            {lesson.resources.length > 0 && (
              <span className="inline-flex items-center gap-1">
                <PaperclipIcon className="size-3" />
                {lesson.resources.length} tài liệu
              </span>
            )}
            {missingUrl && (
              <span className="inline-flex items-center gap-1 font-medium text-warning-strong">
                <TriangleAlertIcon className="size-3" /> Chưa có đường dẫn
              </span>
            )}
          </div>
        </div>
        {lesson.isPreview && (
          <Badge variant="outline" className="border-0 bg-primary-soft text-primary-strong">
            Xem thử
          </Badge>
        )}
        <DropdownMenu modal={false}>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon-sm" aria-label={`Thao tác với bài ${lesson.title}`} disabled={deleting}>
              {deleting ? <Loader2Icon className="animate-spin" /> : <MoreHorizontalIcon />}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-44">
            <DropdownMenuItem onSelect={() => setDialog("edit")}>
              <PencilIcon /> Sửa bài học
            </DropdownMenuItem>
            <DropdownMenuItem onSelect={() => setDialog("resources")}>
              <PaperclipIcon /> Tài liệu ({lesson.resources.length})
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem variant="destructive" onSelect={() => setDialog("delete")}>
              <Trash2Icon /> Xóa bài học
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
      {error && (
        <div className="mt-2">
          <ErrorAlert message={error} />
        </div>
      )}

      <Dialog open={dialog === "edit"} onOpenChange={close}>
        <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>Sửa bài học</DialogTitle>
            <DialogDescription>{lesson.title}</DialogDescription>
          </DialogHeader>
          <LessonForm
            sectionId={lesson.sectionId}
            lesson={lesson}
            defaultPosition={lesson.position}
            onDone={() => setDialog(null)}
            onCancel={() => setDialog(null)}
          />
        </DialogContent>
      </Dialog>

      <Dialog open={dialog === "resources"} onOpenChange={close}>
        <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>Tài liệu đính kèm</DialogTitle>
            <DialogDescription>{lesson.title}</DialogDescription>
          </DialogHeader>
          <LessonResources lesson={lesson} />
        </DialogContent>
      </Dialog>

      <AlertDialog open={dialog === "delete"} onOpenChange={close}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa bài học “{lesson.title}”?</AlertDialogTitle>
            <AlertDialogDescription>Tài liệu đính kèm cũng bị xóa. Không thể hoàn tác.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction variant="destructive" onClick={remove}>
              Xóa bài học
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </li>
  );
}

function LessonResources({ lesson }: { lesson: Lesson }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [confirm, setConfirm] = useState<LessonResource | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  async function add(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    const fd = new FormData(form);
    setPending(true);
    setError(null);
    setErrors({});
    try {
      await api<LessonResource>(`/api/lessons/${lesson.id}/resources`, {
        method: "POST",
        body: { name: text(fd.get("name")), fileUrl: text(fd.get("fileUrl")) },
      });
      form.reset();
      toast.success("Đã thêm tài liệu");
      router.refresh();
    } catch (err) {
      setErrors(fieldErrorMap(err));
      setError(formErrorMessage(err));
    } finally {
      setPending(false);
    }
  }

  async function remove(r: LessonResource) {
    setDeletingId(r.id);
    setError(null);
    try {
      await api(`/api/lessons/${lesson.id}/resources/${r.id}`, { method: "DELETE" });
      toast.success("Đã xóa tài liệu");
      router.refresh();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="space-y-4">
      {error && <ErrorAlert message={error} />}
      {lesson.resources.length === 0 ? (
        <p className="rounded-lg border border-dashed px-4 py-6 text-center text-sm text-muted-foreground">Chưa có tài liệu nào.</p>
      ) : (
        <ul className="divide-y rounded-lg ring-1 ring-border">
          {lesson.resources.map((r) => (
            <li key={r.id} className="flex items-center gap-3 px-3 py-2 text-sm">
              <FileIcon className="size-4 shrink-0 text-muted-foreground" />
              <a
                href={r.fileUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="min-w-0 flex-1 truncate font-medium hover:text-primary hover:underline"
              >
                {r.name}
              </a>
              <Button
                variant="ghost"
                size="icon-sm"
                aria-label={`Xóa tài liệu ${r.name}`}
                disabled={deletingId === r.id}
                onClick={() => setConfirm(r)}
              >
                {deletingId === r.id ? <Loader2Icon className="animate-spin" /> : <Trash2Icon />}
              </Button>
            </li>
          ))}
        </ul>
      )}
      <form onSubmit={add} className="space-y-3 rounded-lg bg-muted/50 p-3">
        <p className="text-sm font-medium">Thêm tài liệu</p>
        <div className="space-y-2">
          <Input name="name" required maxLength={200} placeholder="Tên tài liệu, VD: Slide bài 1" aria-label="Tên tài liệu" className="bg-card" />
          <FieldError message={errors.name} />
        </div>
        <div className="space-y-2">
          <Input name="fileUrl" type="url" required maxLength={500} placeholder="https://..." aria-label="Đường dẫn tệp" className="bg-card" />
          <FieldError message={errors.fileUrl} />
        </div>
        <div className="flex justify-end">
          <Button type="submit" variant="outline" disabled={pending}>
            {pending ? <Loader2Icon className="animate-spin" /> : <PlusIcon />} Thêm tài liệu
          </Button>
        </div>
      </form>

      <AlertDialog open={confirm !== null} onOpenChange={(open) => !open && setConfirm(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa tài liệu “{confirm?.name}”?</AlertDialogTitle>
            <AlertDialogDescription>Học viên sẽ không tải được tài liệu này nữa.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction variant="destructive" onClick={() => confirm && remove(confirm)}>
              Xóa
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
