"use client";

import {
  FileIcon,
  Loader2Icon,
  MoreHorizontalIcon,
  PaperclipIcon,
  PencilIcon,
  Trash2Icon,
  TriangleAlertIcon,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { ErrorAlert } from "@/components/common/error-alert";
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
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import { formatDuration } from "@/lib/format";
import type { Lesson } from "@/lib/types";
import { LESSON_TYPE_ICONS, lessonTypeLabels } from "./icons";
import { LessonResources } from "./lesson-resources";
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
          <LessonResources key={lesson.id} lessonId={lesson.id} initialResources={lesson.resources} />
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
