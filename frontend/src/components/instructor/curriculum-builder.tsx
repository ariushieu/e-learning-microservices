"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { Section } from "@/lib/types";
import { Alert, Button, Empty, Field, Input, formatDuration } from "@/components/ui";
import { FieldError, fieldErrorMap, formErrorMessage, optionalNumber, text } from "./form-helpers";
import { LessonForm } from "./lesson-form";
import { LessonItem } from "./lesson-item";

/** Vị trí kế tiếp: lớn hơn mọi vị trí đang có, bắt đầu từ 1. */
function nextPosition(items: { position: number }[]): number {
  return items.length ? Math.max(...items.map((i) => i.position)) + 1 : 1;
}

export function CurriculumBuilder({ courseId, sections }: { courseId: number; sections: Section[] }) {
  const totalLessons = sections.reduce((n, s) => n + s.lessons.length, 0);
  const totalSeconds = sections.reduce((n, s) => n + s.lessons.reduce((m, l) => m + (l.durationSeconds ?? 0), 0), 0);

  return (
    <div className="space-y-4">
      <p className="text-sm text-slate-500">
        {sections.length} chương · {totalLessons} bài học · {formatDuration(totalSeconds)}
      </p>
      {sections.length === 0 ? (
        <Empty>Khóa học chưa có chương nào. Thêm chương đầu tiên ở bên dưới.</Empty>
      ) : (
        sections.map((s, i) => <SectionCard key={s.id} section={s} index={i + 1} />)
      )}
      <AddSectionForm courseId={courseId} defaultPosition={nextPosition(sections)} />
    </div>
  );
}

function AddSectionForm({ courseId, defaultPosition }: { courseId: number; defaultPosition: number }) {
  const router = useRouter();
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
      router.refresh();
    } catch (err) {
      setErrors(fieldErrorMap(err));
      setError(formErrorMessage(err));
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-3 rounded-xl border border-dashed border-slate-300 bg-white p-4">
      <h3 className="text-sm font-semibold text-slate-800">Thêm chương mới</h3>
      {error && <Alert>{error}</Alert>}
      <div className="grid gap-3 sm:grid-cols-[1fr_120px_auto] sm:items-end">
        <div>
          <Field label="Tên chương *">
            <Input name="title" required maxLength={200} placeholder="VD: Chương 1 - Giới thiệu" />
          </Field>
          <FieldError message={errors.title} />
        </div>
        <div>
          {/* key đổi theo vị trí kế tiếp để ô số tự cập nhật sau khi thêm chương */}
          <Field label="Thứ tự">
            <Input key={defaultPosition} name="position" type="number" min={0} step={1} defaultValue={defaultPosition} />
          </Field>
          <FieldError message={errors.position} />
        </div>
        <Button type="submit" loading={pending}>
          Thêm chương
        </Button>
      </div>
    </form>
  );
}

function SectionCard({ section, index }: { section: Section; index: number }) {
  const router = useRouter();
  const [renaming, setRenaming] = useState(false);
  const [adding, setAdding] = useState(false);
  const [pending, setPending] = useState<"save" | "delete" | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

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
      router.refresh();
    } catch (err) {
      setErrors(fieldErrorMap(err));
      setError(formErrorMessage(err));
    } finally {
      setPending(null);
    }
  }

  async function remove() {
    const extra = section.lessons.length ? ` và ${section.lessons.length} bài học bên trong` : "";
    if (!window.confirm(`Xóa chương "${section.title}"${extra}? Không thể hoàn tác.`)) return;
    setPending("delete");
    setError(null);
    try {
      await api(`/api/sections/${section.id}`, { method: "DELETE" });
      router.refresh();
    } catch (err) {
      setError(errorMessage(err));
      setPending(null);
    }
  }

  return (
    <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-3">
        {renaming ? (
          <form onSubmit={rename} className="flex flex-1 flex-wrap items-start gap-2">
            <div className="min-w-48 flex-1">
              <Input name="title" required maxLength={200} defaultValue={section.title} aria-label="Tên chương" autoFocus />
              <FieldError message={errors.title} />
            </div>
            <div className="w-24">
              <Input name="position" type="number" min={0} step={1} defaultValue={section.position} aria-label="Thứ tự" />
              <FieldError message={errors.position} />
            </div>
            <Button type="submit" loading={pending === "save"}>
              Lưu
            </Button>
            <Button type="button" variant="ghost" onClick={() => setRenaming(false)}>
              Hủy
            </Button>
          </form>
        ) : (
          <>
            <div>
              <h3 className="font-semibold text-slate-900">
                Chương {index}: {section.title}
              </h3>
              <p className="text-xs text-slate-500">
                Thứ tự {section.position} · {section.lessons.length} bài học
              </p>
            </div>
            <div className="flex gap-1">
              <Button variant="ghost" className="px-3 py-1.5" onClick={() => setRenaming(true)}>
                Đổi tên
              </Button>
              <Button
                variant="ghost"
                className="px-3 py-1.5 text-rose-600"
                loading={pending === "delete"}
                onClick={remove}
              >
                Xóa chương
              </Button>
            </div>
          </>
        )}
      </div>

      <div className="px-5 py-2">
        {error && (
          <div className="py-2">
            <Alert>{error}</Alert>
          </div>
        )}
        {section.lessons.length === 0 ? (
          <p className="py-3 text-sm text-slate-500">Chương này chưa có bài học.</p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {section.lessons.map((l, i) => (
              <LessonItem key={l.id} lesson={l} index={i + 1} />
            ))}
          </ul>
        )}
        <div className="py-3">
          {adding ? (
            <LessonForm
              sectionId={section.id}
              defaultPosition={nextPosition(section.lessons)}
              onDone={() => setAdding(false)}
              onCancel={() => setAdding(false)}
            />
          ) : (
            <Button variant="secondary" onClick={() => setAdding(true)}>
              + Thêm bài học
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
