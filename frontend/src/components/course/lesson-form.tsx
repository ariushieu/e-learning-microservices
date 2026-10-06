"use client";

import { Loader2Icon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useId, useState, type FormEvent } from "react";
import { toast } from "sonner";
import { ErrorAlert } from "@/components/common/error-alert";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { api } from "@/lib/client";
import { fieldErrorMap, formErrorMessage, optional, optionalNumber, text } from "@/lib/forms";
import type { Lesson, LessonInput, LessonType } from "@/lib/types";
import { Field, NativeSelect } from "./form-helpers";
import { lessonTypeLabels } from "./icons";

/**
 * Thêm bài học vào chương (không có `lesson`) hoặc sửa bài học (có `lesson`).
 * PUT ghi đè mọi trường nên form luôn gửi đủ object, giá trị điền sẵn từ đề cương
 * (chủ khóa luôn nhận được content/contentUrl đầy đủ).
 */
export function LessonForm({
  sectionId,
  lesson,
  defaultPosition,
  onDone,
  onCancel,
}: {
  sectionId: number;
  lesson?: Lesson;
  defaultPosition: number;
  onDone: () => void;
  onCancel: () => void;
}) {
  const router = useRouter();
  const id = useId();
  const [type, setType] = useState<LessonType>(lesson?.type ?? "VIDEO");
  const [isPreview, setIsPreview] = useState(lesson?.isPreview ?? false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const types: LessonType[] = lesson?.type === "QUIZ" ? ["VIDEO", "ARTICLE", "FILE", "QUIZ"] : ["VIDEO", "ARTICLE", "FILE"];
  const needsUrl = type === "VIDEO" || type === "FILE";

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const minutes = optionalNumber(fd.get("durationMinutes")) ?? 0;
    const body: LessonInput = {
      title: text(fd.get("title")),
      type,
      contentUrl: needsUrl ? optional(fd.get("contentUrl")) : undefined,
      content: optional(fd.get("content")),
      durationSeconds: Math.max(0, Math.round(minutes * 60)),
      position: optionalNumber(fd.get("position")) ?? 0,
      isPreview,
    };

    setPending(true);
    setError(null);
    setErrors({});
    try {
      if (lesson) {
        await api<Lesson>(`/api/lessons/${lesson.id}`, { method: "PUT", body });
        toast.success("Đã lưu bài học");
      } else {
        await api<Lesson>(`/api/sections/${sectionId}/lessons`, { method: "POST", body });
        toast.success("Đã thêm bài học");
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

  const minutesDefault = lesson ? Math.round((lesson.durationSeconds / 60) * 100) / 100 : "";

  return (
    <form onSubmit={onSubmit} className="space-y-5">
      {error && <ErrorAlert message={error} />}

      <div className="grid gap-5 sm:grid-cols-[1fr_180px]">
        <Field id={`${id}-title`} label="Tên bài học" required error={errors.title}>
          <Input id={`${id}-title`} name="title" required maxLength={200} defaultValue={lesson?.title} autoFocus />
        </Field>
        <Field id={`${id}-type`} label="Loại bài" error={errors.type}>
          <NativeSelect id={`${id}-type`} name="type" value={type} onChange={(e) => setType(e.target.value as LessonType)}>
            {types.map((t) => (
              <option key={t} value={t}>
                {lessonTypeLabels[t]}
              </option>
            ))}
          </NativeSelect>
        </Field>
      </div>

      {needsUrl && (
        <Field
          id={`${id}-url`}
          label={type === "VIDEO" ? "Đường dẫn video" : "Đường dẫn tệp"}
          hint={type === "VIDEO" ? "Link YouTube hoặc file .mp4" : "Link tải tệp (PDF, slide...)"}
          error={errors.contentUrl}
        >
          <Input id={`${id}-url`} name="contentUrl" type="url" maxLength={500} defaultValue={lesson?.contentUrl ?? ""} placeholder="https://..." />
        </Field>
      )}

      <Field id={`${id}-content`} label={type === "ARTICLE" ? "Nội dung bài viết" : "Mô tả thêm (không bắt buộc)"} error={errors.content}>
        <Textarea
          id={`${id}-content`}
          name="content"
          rows={type === "ARTICLE" ? 10 : 3}
          className={type === "ARTICLE" ? "min-h-48" : undefined}
          defaultValue={lesson?.content ?? ""}
        />
      </Field>

      <div className="grid gap-5 sm:grid-cols-2">
        <Field id={`${id}-duration`} label="Thời lượng (phút)" error={errors.durationSeconds}>
          <Input id={`${id}-duration`} name="durationMinutes" type="number" min={0} step="any" defaultValue={minutesDefault} />
        </Field>
        <Field id={`${id}-position`} label="Thứ tự" hint="Số nhỏ hiện trước" error={errors.position}>
          <Input id={`${id}-position`} name="position" type="number" min={0} step={1} defaultValue={lesson?.position ?? defaultPosition} />
        </Field>
      </div>

      <label className="flex cursor-pointer items-start gap-3 rounded-lg border p-3 transition-colors hover:bg-muted/50">
        <Checkbox checked={isPreview} onCheckedChange={(c) => setIsPreview(c === true)} className="mt-0.5" />
        <span>
          <span className="block text-sm font-medium">Cho xem thử</span>
          <span className="block text-xs text-muted-foreground">Ai cũng xem được nội dung bài này, không cần ghi danh.</span>
        </span>
      </label>

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel} disabled={pending}>
          Hủy
        </Button>
        <Button type="submit" disabled={pending}>
          {pending && <Loader2Icon className="animate-spin" />}
          {lesson ? "Lưu bài học" : "Thêm bài học"}
        </Button>
      </div>
    </form>
  );
}
