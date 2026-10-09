"use client";

import { LayersIcon, Loader2Icon, MoreHorizontalIcon, PencilIcon, PlusIcon, Trash2Icon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useId, useState, type FormEvent } from "react";
import { toast } from "sonner";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { FieldError } from "@/components/common/field-error";
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
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
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
import { fieldErrorMap, formErrorMessage, optionalNumber, text } from "@/lib/forms";
import type { Section } from "@/lib/types";
import { nextPosition } from "./form-helpers";
import { LessonForm } from "./lesson-form";
import { LessonItem } from "./lesson-item";

export function CurriculumBuilder({ courseId, sections }: { courseId: number; sections: Section[] }) {
  const totalLessons = sections.reduce((n, s) => n + s.lessons.length, 0);
  const totalSeconds = sections.reduce((n, s) => n + s.lessons.reduce((m, l) => m + (l.durationSeconds ?? 0), 0), 0);

  return (
    <div className="space-y-4">
      <div className="space-y-1">
        <h2 className="text-heading">Đề cương</h2>
        <p className="text-sm text-muted-foreground">
          <span className="font-medium text-foreground tabular-nums">
            {sections.length} chương · {totalLessons} bài học · {formatDuration(totalSeconds)}
          </span>
          . Sắp xếp theo số thứ tự (nhỏ hiện trước); bài “xem thử” ai cũng xem được, không cần ghi danh.
        </p>
      </div>
      {sections.length === 0 ? (
        <EmptyState icon={LayersIcon} title="Khóa học chưa có chương nào" description="Thêm chương đầu tiên ở bên dưới." />
      ) : (
        sections.map((s, i) => <SectionCard key={s.id} section={s} index={i + 1} />)
      )}
      <AddSectionForm courseId={courseId} defaultPosition={nextPosition(sections)} />
    </div>
  );
}

function AddSectionForm({ courseId, defaultPosition }: { courseId: number; defaultPosition: number }) {
  const router = useRouter();
  const id = useId();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    const fd = new FormData(form);
    setPending(true);
    setError(null);
    setErrors({});
    try {
      await api<Section>(`/api/courses/${courseId}/sections`, {
        method: "POST",
        body: { title: text(fd.get("title")), position: optionalNumber(fd.get("position")) ?? defaultPosition },
      });
      form.reset();
      toast.success("Đã thêm chương");
      router.refresh();
    } catch (err) {
      setErrors(fieldErrorMap(err));
      setError(formErrorMessage(err));
    } finally {
      setPending(false);
    }
  }

  return (
    <Card className="border border-dashed bg-muted/30 shadow-none ring-0">
      <CardHeader>
        <CardTitle className="text-subheading">Thêm chương mới</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} className="space-y-4">
          {error && <ErrorAlert message={error} />}
          <div className="grid gap-4 sm:grid-cols-[1fr_120px_auto] sm:items-start">
            <FormField id={`${id}-title`} label="Tên chương" required error={errors.title}>
              <Input id={`${id}-title`} name="title" required maxLength={200} placeholder="VD: Chương 1 - Giới thiệu" className="bg-card" />
            </FormField>
            {/* key đổi theo vị trí kế tiếp để ô số tự cập nhật sau khi thêm chương */}
            <FormField id={`${id}-position`} label="Thứ tự" error={errors.position}>
              <Input
                key={defaultPosition}
                id={`${id}-position`}
                name="position"
                type="number"
                min={0}
                step={1}
                defaultValue={defaultPosition}
                className="bg-card"
              />
            </FormField>
            <Button type="submit" disabled={pending} className="sm:mt-6">
              {pending ? <Loader2Icon className="animate-spin" /> : <PlusIcon />} Thêm chương
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

function SectionCard({ section, index }: { section: Section; index: number }) {
  const router = useRouter();
  const [renaming, setRenaming] = useState(false);
  const [adding, setAdding] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [pending, setPending] = useState<"save" | "delete" | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const seconds = section.lessons.reduce((m, l) => m + (l.durationSeconds ?? 0), 0);

  async function rename(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setPending("save");
    setError(null);
    setErrors({});
    try {
      // PUT đặt lại position về 0 nếu thiếu, nên luôn gửi kèm thứ tự.
      await api<Section>(`/api/sections/${section.id}`, {
        method: "PUT",
        body: { title: text(fd.get("title")), position: optionalNumber(fd.get("position")) ?? section.position },
      });
      setRenaming(false);
      toast.success("Đã lưu chương");
      router.refresh();
    } catch (err) {
      setErrors(fieldErrorMap(err));
      setError(formErrorMessage(err));
    } finally {
      setPending(null);
    }
  }

  async function remove() {
    setPending("delete");
    setError(null);
    try {
      await api(`/api/sections/${section.id}`, { method: "DELETE" });
      toast.success("Đã xóa chương");
      router.refresh();
    } catch (err) {
      setError(errorMessage(err));
      setPending(null);
    }
  }

  return (
    <Card className="gap-0 py-0">
      <CardHeader className="border-b bg-muted/40 pt-(--card-spacing)">
        {renaming ? (
          <form onSubmit={rename} className="col-span-full flex flex-wrap items-start gap-2">
            <div className="min-w-48 flex-1 space-y-1">
              <Input name="title" required maxLength={200} defaultValue={section.title} aria-label="Tên chương" autoFocus />
              <FieldError message={errors.title} />
            </div>
            <div className="w-24 space-y-1">
              <Input name="position" type="number" min={0} step={1} defaultValue={section.position} aria-label="Thứ tự" />
              <FieldError message={errors.position} />
            </div>
            <Button type="submit" disabled={pending === "save"}>
              {pending === "save" && <Loader2Icon className="animate-spin" />} Lưu
            </Button>
            <Button type="button" variant="ghost" onClick={() => setRenaming(false)}>
              Hủy
            </Button>
          </form>
        ) : (
          <>
            {/* Tên chương thường đã có "Chương 1. ..." nên chỉ thêm nhãn "Phần n" nhỏ, khớp trang học. */}
            <CardTitle className="min-w-0">
              <span className="block text-caption font-medium text-muted-foreground">Phần {index}</span>
              <span className="block text-subheading">{section.title}</span>
            </CardTitle>
            <CardDescription className="tabular-nums">
              {section.lessons.length} bài học · {formatDuration(seconds)}
            </CardDescription>
            <CardAction>
              <DropdownMenu modal={false}>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="icon-sm" aria-label={`Thao tác với chương ${section.title}`} disabled={pending === "delete"}>
                    {pending === "delete" ? <Loader2Icon className="animate-spin" /> : <MoreHorizontalIcon />}
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-40">
                  <DropdownMenuItem onSelect={() => setRenaming(true)}>
                    <PencilIcon /> Đổi tên
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem variant="destructive" onSelect={() => setConfirmDelete(true)}>
                    <Trash2Icon /> Xóa chương
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </CardAction>
          </>
        )}
      </CardHeader>

      <CardContent className="px-0">
        {error && (
          <div className="px-4 pt-3">
            <ErrorAlert message={error} />
          </div>
        )}
        {section.lessons.length === 0 ? (
          <p className="px-4 py-4 text-sm text-muted-foreground">Chương này chưa có bài học.</p>
        ) : (
          <ul className="divide-y">
            {section.lessons.map((l, i) => (
              <LessonItem key={l.id} lesson={l} index={i + 1} />
            ))}
          </ul>
        )}
        <div className="border-t px-4 py-3">
          <Button variant="ghost" onClick={() => setAdding(true)}>
            <PlusIcon /> Thêm bài học
          </Button>
        </div>
      </CardContent>

      <Dialog open={adding} onOpenChange={setAdding}>
        <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>Thêm bài học mới</DialogTitle>
            <DialogDescription>
              Phần {index} · {section.title}
            </DialogDescription>
          </DialogHeader>
          <LessonForm
            sectionId={section.id}
            defaultPosition={nextPosition(section.lessons)}
            onDone={() => setAdding(false)}
            onCancel={() => setAdding(false)}
          />
        </DialogContent>
      </Dialog>

      <AlertDialog open={confirmDelete} onOpenChange={setConfirmDelete}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa chương “{section.title}”?</AlertDialogTitle>
            <AlertDialogDescription>
              {section.lessons.length
                ? `${section.lessons.length} bài học bên trong cũng bị xóa. Không thể hoàn tác.`
                : "Không thể hoàn tác."}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction variant="destructive" onClick={remove}>
              Xóa chương
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  );
}
