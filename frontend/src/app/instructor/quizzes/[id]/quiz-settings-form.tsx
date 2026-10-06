"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import {
  backendFieldErrors,
  quizToSettings,
  QuizSettingsFields,
  settingsToPayload,
  validateQuizSettings,
  type FieldErrors,
} from "@/components/quiz/quiz-settings-fields";
import { Alert, Button, Card } from "@/components/ui";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { Quiz } from "@/lib/types";

export function QuizSettingsForm({ quiz }: { quiz: Quiz }) {
  const router = useRouter();
  const [values, setValues] = useState(() => quizToSettings(quiz));
  const [errors, setErrors] = useState<FieldErrors>({});
  const [message, setMessage] = useState<{ kind: "error" | "success"; text: string } | null>(null);
  const [saving, setSaving] = useState(false);
  const archived = quiz.status === "ARCHIVED";

  async function submit(e: FormEvent) {
    e.preventDefault();
    const found = validateQuizSettings(values);
    setErrors(found);
    setMessage(null);
    if (Object.keys(found).length) return;

    setSaving(true);
    try {
      // PUT ghi đè cả lessonId nên phải gửi lại giá trị cũ để không bị mất liên kết bài học.
      await api(`/api/quizzes/${quiz.id}`, {
        method: "PUT",
        body: { lessonId: quiz.lessonId, ...settingsToPayload(values) },
      });
      setMessage({ kind: "success", text: "Đã lưu cài đặt." });
      router.refresh();
    } catch (err) {
      setErrors(backendFieldErrors(err));
      setMessage({ kind: "error", text: errorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  return (
    <Card>
      <form onSubmit={submit} noValidate className="space-y-4">
        <h2 className="font-semibold text-slate-900">Cài đặt bài kiểm tra</h2>
        {archived && <Alert kind="info">Bài đã lưu trữ nên không sửa được cài đặt.</Alert>}
        <QuizSettingsFields values={values} errors={errors} onChange={setValues} disabled={saving || archived} />
        {message && <Alert kind={message.kind}>{message.text}</Alert>}
        <div className="flex justify-end">
          <Button type="submit" loading={saving} disabled={archived}>
            Lưu cài đặt
          </Button>
        </div>
      </form>
    </Card>
  );
}
