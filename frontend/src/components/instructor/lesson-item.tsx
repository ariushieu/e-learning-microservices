"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { Lesson, LessonResource } from "@/lib/types";
import { Alert, Button, Input, formatDuration } from "@/components/ui";
import { FieldError, fieldErrorMap, formErrorMessage, lessonTypeLabels, text } from "./form-helpers";
import { LessonForm } from "./lesson-form";

const typeIcons: Record<string, string> = { VIDEO: "▶", ARTICLE: "≡", FILE: "⎙", QUIZ: "?" };

export function LessonItem({ lesson, index }: { lesson: Lesson; index: number }) {
  const router = useRouter();
  const [mode, setMode] = useState<"view" | "edit" | "resources">("view");
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function remove() {
    if (!window.confirm(`Xóa bài học "${lesson.title}"? Tài liệu đính kèm cũng bị xóa.`)) return;
    setDeleting(true);
    setError(null);
    try {
      await api(`/api/lessons/${lesson.id}`, { method: "DELETE" });
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
      setDeleting(false);
    }
  }

  return (
    <li className="py-3">
      <div className="flex flex-wrap items-center gap-3">
        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-sm text-slate-600">
          {typeIcons[lesson.type] ?? "•"}
        </span>
        <div className="min-w-0 flex-1">
          <div className="truncate font-medium text-slate-800">
            {index}. {lesson.title}
          </div>
          <div className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
            <span>{lessonTypeLabels[lesson.type] ?? lesson.type}</span>
            <span>{formatDuration(lesson.durationSeconds)}</span>
            <span>Thứ tự {lesson.position}</span>
            {lesson.resources.length > 0 && <span>{lesson.resources.length} tài liệu</span>}
            {lesson.isPreview && (
              <span className="rounded-full bg-sky-100 px-2 py-0.5 font-medium text-sky-700">Xem thử</span>
            )}
            {(lesson.type === "VIDEO" || lesson.type === "FILE") && !lesson.contentUrl && (
              <span className="text-amber-600">Chưa có đường dẫn</span>
            )}
          </div>
        </div>
        <div className="flex gap-1">
          <Button variant="ghost" className="px-3 py-1.5" onClick={() => setMode(mode === "edit" ? "view" : "edit")}>
            Sửa
          </Button>
          <Button
            variant="ghost"
            className="px-3 py-1.5"
            onClick={() => setMode(mode === "resources" ? "view" : "resources")}
          >
            Tài liệu ({lesson.resources.length})
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
      {mode === "edit" && (
        <div className="mt-3">
          <LessonForm
            sectionId={lesson.sectionId}
            lesson={lesson}
            defaultPosition={lesson.position}
            onDone={() => setMode("view")}
            onCancel={() => setMode("view")}
          />
        </div>
      )}
      {mode === "resources" && <LessonResources lesson={lesson} />}
    </li>
  );
}

function LessonResources({ lesson }: { lesson: Lesson }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
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
      router.refresh();
    } catch (err) {
      setErrors(fieldErrorMap(err));
      setError(formErrorMessage(err));
    } finally {
      setPending(false);
    }
  }

  async function remove(r: LessonResource) {
    if (!window.confirm(`Xóa tài liệu "${r.name}"?`)) return;
    setDeletingId(r.id);
    setError(null);
    try {
      await api(`/api/lessons/${lesson.id}/resources/${r.id}`, { method: "DELETE" });
      router.refresh();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="mt-3 space-y-3 rounded-lg border border-slate-200 bg-slate-50 p-4">
      <h4 className="text-sm font-semibold text-slate-800">Tài liệu đính kèm</h4>
      {error && <Alert>{error}</Alert>}
      {lesson.resources.length === 0 ? (
        <p className="text-sm text-slate-500">Chưa có tài liệu nào.</p>
      ) : (
        <ul className="divide-y divide-slate-200 rounded-lg border border-slate-200 bg-white">
          {lesson.resources.map((r) => (
            <li key={r.id} className="flex items-center justify-between gap-3 px-3 py-2 text-sm">
              <a
                href={r.fileUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="min-w-0 truncate font-medium text-indigo-600 hover:underline"
              >
                {r.name}
              </a>
              <Button
                variant="ghost"
                className="px-2 py-1 text-rose-600"
                loading={deletingId === r.id}
                onClick={() => remove(r)}
              >
                Xóa
              </Button>
            </li>
          ))}
        </ul>
      )}
      <form onSubmit={add} className="grid gap-2 sm:grid-cols-[1fr_1.5fr_auto] sm:items-start">
        <div>
          <Input name="name" required maxLength={200} placeholder="Tên tài liệu, VD: Slide bài 1" aria-label="Tên tài liệu" />
          <FieldError message={errors.name} />
        </div>
        <div>
          <Input name="fileUrl" type="url" required maxLength={500} placeholder="https://..." aria-label="Đường dẫn tệp" />
          <FieldError message={errors.fileUrl} />
        </div>
        <Button type="submit" variant="secondary" loading={pending}>
          Thêm tài liệu
        </Button>
      </form>
    </div>
  );
}
