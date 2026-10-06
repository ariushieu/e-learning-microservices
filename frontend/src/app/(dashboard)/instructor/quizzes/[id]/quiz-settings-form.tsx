"use client";

import { InfoIcon, Loader2Icon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { ErrorAlert } from "@/components/common/error-alert";
import {
  backendFieldErrors,
  quizToSettings,
  QuizSettingsFields,
  settingsToPayload,
  validateQuizSettings,
  type FieldErrors,
} from "@/components/quiz/quiz-settings-fields";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { Quiz } from "@/lib/types";

export function QuizSettingsForm({ quiz }: { quiz: Quiz }) {
  const router = useRouter();
  const [values, setValues] = useState(() => quizToSettings(quiz));
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const archived = quiz.status === "ARCHIVED";

  async function submit(e: FormEvent) {
    e.preventDefault();
    const found = validateQuizSettings(values);
    setErrors(found);
    setError(null);
    if (Object.keys(found).length) return;

    setSaving(true);
    try {
      // PUT ghi đè cả lessonId nên phải gửi lại giá trị cũ để không bị mất liên kết bài học.
      await api(`/api/quizzes/${quiz.id}`, {
        method: "PUT",
        body: { lessonId: quiz.lessonId, ...settingsToPayload(values) },
      });
      toast.success("Đã lưu cài đặt");
      router.refresh();
    } catch (err) {
      setErrors(backendFieldErrors(err));
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Card>
      <form onSubmit={submit} noValidate className="flex flex-col gap-(--card-spacing)">
        <CardHeader>
          <CardTitle className="text-subheading">Cài đặt</CardTitle>
          <CardDescription>Tiêu đề, thời gian và cách chấm điểm.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-5">
          {archived && (
            <Alert>
              <InfoIcon />
              <AlertDescription>Bài đã lưu trữ nên không sửa được cài đặt.</AlertDescription>
            </Alert>
          )}
          {error && <ErrorAlert message={error} />}
          <QuizSettingsFields
            values={values}
            errors={errors}
            onChange={setValues}
            disabled={saving || archived}
            compact
          />
        </CardContent>
        <CardFooter className="justify-end">
          <Button type="submit" disabled={saving || archived}>
            {saving && <Loader2Icon className="animate-spin" />}
            Lưu cài đặt
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}
