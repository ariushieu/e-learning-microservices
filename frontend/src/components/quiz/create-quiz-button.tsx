"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Alert, Button, Card } from "@/components/ui";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { Quiz } from "@/lib/types";
import {
  backendFieldErrors,
  emptyQuizSettings,
  QuizSettingsFields,
  settingsToPayload,
  validateQuizSettings,
  type FieldErrors,
} from "./quiz-settings-fields";

/** Nút "Tạo bài kiểm tra" mở form ngay tại chỗ; tạo xong chuyển sang trang soạn câu hỏi. */
export function CreateQuizButton({ courseId }: { courseId: number }) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [values, setValues] = useState(emptyQuizSettings);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  function close() {
    setOpen(false);
    setValues(emptyQuizSettings);
    setErrors({});
    setError(null);
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    const found = validateQuizSettings(values);
    setErrors(found);
    setError(null);
    if (Object.keys(found).length) return;

    setSaving(true);
    try {
      const quiz = await api<Quiz>("/api/quizzes", {
        method: "POST",
        body: { courseId, ...settingsToPayload(values) },
      });
      router.push(`/instructor/quizzes/${quiz.id}`);
    } catch (err) {
      setErrors(backendFieldErrors(err));
      setError(errorMessage(err));
      setSaving(false);
    }
  }

  if (!open) {
    return <Button onClick={() => setOpen(true)}>+ Tạo bài kiểm tra</Button>;
  }

  return (
    <Card className="w-full">
      <form onSubmit={submit} className="space-y-4" noValidate>
        <h3 className="text-lg font-semibold text-slate-900">Bài kiểm tra mới</h3>
        {error && <Alert>{error}</Alert>}
        <QuizSettingsFields values={values} errors={errors} onChange={setValues} disabled={saving} showShuffle={false} />
        <p className="text-xs text-slate-500">Bài kiểm tra được tạo ở trạng thái nháp, thêm câu hỏi rồi mới xuất bản.</p>
        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={close} disabled={saving}>
            Hủy
          </Button>
          <Button type="submit" loading={saving}>
            Tạo và soạn câu hỏi
          </Button>
        </div>
      </form>
    </Card>
  );
}
