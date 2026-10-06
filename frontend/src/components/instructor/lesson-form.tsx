"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { api } from "@/lib/client";
import type { Lesson, LessonInput, LessonType } from "@/lib/types";
import { Alert, Button, Field, Input, Select, Textarea } from "@/components/ui";
import { FieldError, fieldErrorMap, formErrorMessage, lessonTypeLabels, optional, optionalNumber, text } from "./form-helpers";

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
  const [type, setType] = useState<LessonType>(lesson?.type ?? "VIDEO");
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
      isPreview: fd.get("isPreview") === "on",
    };

    setPending(true);
    setError(null);
    setErrors({});
    try {
      if (lesson) {
        await api<Lesson>(`/api/lessons/${lesson.id}`, { method: "PUT", body });
      } else {
        await api<Lesson>(`/api/sections/${sectionId}/lessons`, { method: "POST", body });
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
    <form onSubmit={onSubmit} className="space-y-4 rounded-lg border border-indigo-100 bg-indigo-50/40 p-4">
      <h4 className="text-sm font-semibold text-slate-800">{lesson ? `Sửa bài: ${lesson.title}` : "Thêm bài học mới"}</h4>
      {error && <Alert>{error}</Alert>}

      <div className="grid gap-4 sm:grid-cols-[1fr_180px]">
        <div>
          <Field label="Tên bài học *">
            <Input name="title" required maxLength={200} defaultValue={lesson?.title} />
          </Field>
          <FieldError message={errors.title} />
        </div>
        <div>
          <Field label="Loại bài">
            <Select name="type" value={type} onChange={(e) => setType(e.target.value as LessonType)}>
              {types.map((t) => (
                <option key={t} value={t}>
                  {lessonTypeLabels[t]}
                </option>
              ))}
            </Select>
          </Field>
          <FieldError message={errors.type} />
        </div>
      </div>

      {needsUrl && (
        <div>
          <Field
            label={type === "VIDEO" ? "Đường dẫn video" : "Đường dẫn tệp"}
            hint={type === "VIDEO" ? "Link YouTube hoặc file .mp4" : "Link tải tệp (PDF, slide...)"}
          >
            <Input name="contentUrl" type="url" maxLength={500} defaultValue={lesson?.contentUrl ?? ""} placeholder="https://..." />
          </Field>
          <FieldError message={errors.contentUrl} />
        </div>
      )}

      <div>
        <Field label={type === "ARTICLE" ? "Nội dung bài viết" : "Mô tả thêm (không bắt buộc)"}>
          <Textarea name="content" rows={type === "ARTICLE" ? 10 : 3} defaultValue={lesson?.content ?? ""} />
        </Field>
        <FieldError message={errors.content} />
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div>
          <Field label="Thời lượng (phút)">
            <Input name="durationMinutes" type="number" min={0} step="any" defaultValue={minutesDefault} />
          </Field>
          <FieldError message={errors.durationSeconds} />
        </div>
        <div>
          <Field label="Thứ tự" hint="Số nhỏ hiện trước">
            <Input name="position" type="number" min={0} step={1} defaultValue={lesson?.position ?? defaultPosition} />
          </Field>
          <FieldError message={errors.position} />
        </div>
        <label className="flex items-center gap-2 self-center pt-5 text-sm text-slate-700">
          <input
            type="checkbox"
            name="isPreview"
            defaultChecked={lesson?.isPreview ?? false}
            className="h-4 w-4 rounded border-slate-300 text-indigo-600"
          />
          Cho xem thử (không cần ghi danh)
        </label>
      </div>

      <div className="flex justify-end gap-2">
        <Button type="button" variant="ghost" onClick={onCancel} disabled={pending}>
          Hủy
        </Button>
        <Button type="submit" loading={pending}>
          {lesson ? "Lưu bài học" : "Thêm bài học"}
        </Button>
      </div>
    </form>
  );
}
