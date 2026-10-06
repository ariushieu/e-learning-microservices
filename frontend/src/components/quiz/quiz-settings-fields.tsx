"use client";

import { Field, Input, Textarea } from "@/components/ui";
import { ApiError } from "@/lib/errors";
import type { Quiz } from "@/lib/types";

/** Giá trị form cấu hình bài kiểm tra; ô số giữ dạng chuỗi để người dùng xóa trống được. */
export interface QuizSettingsValues {
  title: string;
  description: string;
  timeLimitMinutes: string;
  passScore: string;
  maxAttempts: string;
  shuffleQuestions: boolean;
}

export type FieldErrors = Partial<Record<keyof QuizSettingsValues, string>>;

export const emptyQuizSettings: QuizSettingsValues = {
  title: "",
  description: "",
  timeLimitMinutes: "",
  passScore: "50",
  maxAttempts: "3",
  shuffleQuestions: false,
};

export function quizToSettings(quiz: Quiz): QuizSettingsValues {
  return {
    title: quiz.title,
    description: quiz.description ?? "",
    timeLimitMinutes: quiz.timeLimitMinutes == null ? "" : String(quiz.timeLimitMinutes),
    passScore: String(Number(quiz.passScore)),
    maxAttempts: String(quiz.maxAttempts),
    shuffleQuestions: quiz.shuffleQuestions,
  };
}

const isInteger = (v: string) => /^\d+$/.test(v.trim());

/** Cùng ràng buộc với CreateQuizRequest / UpdateQuizRequest bên quiz-service. */
export function validateQuizSettings(v: QuizSettingsValues): FieldErrors {
  const errors: FieldErrors = {};
  if (!v.title.trim()) errors.title = "Tiêu đề không được để trống";
  else if (v.title.trim().length > 200) errors.title = "Tiêu đề không được vượt quá 200 ký tự";
  if (v.description.length > 1000) errors.description = "Mô tả không được vượt quá 1000 ký tự";
  if (v.timeLimitMinutes.trim() && (!isInteger(v.timeLimitMinutes) || Number(v.timeLimitMinutes) < 1)) {
    errors.timeLimitMinutes = "Thời gian làm bài là số nguyên, tối thiểu 1 phút";
  }
  const pass = Number(v.passScore);
  if (!v.passScore.trim() || Number.isNaN(pass) || pass < 0 || pass > 100) {
    errors.passScore = "Điểm đạt phải từ 0 đến 100";
  }
  if (!isInteger(v.maxAttempts)) errors.maxAttempts = "Số lần làm bài là số nguyên không âm";
  return errors;
}

export function settingsToPayload(v: QuizSettingsValues) {
  return {
    title: v.title.trim(),
    description: v.description.trim() || undefined,
    timeLimitMinutes: v.timeLimitMinutes.trim() ? Number(v.timeLimitMinutes) : null,
    passScore: Number(v.passScore),
    maxAttempts: Number(v.maxAttempts),
    shuffleQuestions: v.shuffleQuestions,
  };
}

/** Lỗi theo từng ô từ backend (VALIDATION_FAILED), chỉ giữ những ô form này có. */
export function backendFieldErrors(e: unknown): FieldErrors {
  const result: FieldErrors = {};
  if (e instanceof ApiError) {
    for (const f of e.fieldErrors) {
      if (f.field in emptyQuizSettings) result[f.field as keyof QuizSettingsValues] = f.message;
    }
  }
  return result;
}

export function FieldError({ message }: { message?: string }) {
  return message ? <span className="block text-xs text-rose-600">{message}</span> : null;
}

export function QuizSettingsFields({
  values,
  errors,
  onChange,
  disabled,
  showShuffle = true,
}: {
  values: QuizSettingsValues;
  errors: FieldErrors;
  onChange: (v: QuizSettingsValues) => void;
  disabled?: boolean;
  showShuffle?: boolean;
}) {
  const set = <K extends keyof QuizSettingsValues>(key: K, value: QuizSettingsValues[K]) =>
    onChange({ ...values, [key]: value });

  return (
    <div className="space-y-4">
      <Field label="Tiêu đề *">
        <Input
          value={values.title}
          onChange={(e) => set("title", e.target.value)}
          maxLength={200}
          disabled={disabled}
          placeholder="Ví dụ: Kiểm tra chương 1"
        />
        <FieldError message={errors.title} />
      </Field>
      <Field label="Mô tả">
        <Textarea
          rows={3}
          value={values.description}
          onChange={(e) => set("description", e.target.value)}
          maxLength={1000}
          disabled={disabled}
        />
        <FieldError message={errors.description} />
      </Field>
      <div className="grid gap-4 sm:grid-cols-3">
        <Field label="Thời gian (phút)" hint="Để trống nếu không giới hạn">
          <Input
            type="number"
            min={1}
            value={values.timeLimitMinutes}
            onChange={(e) => set("timeLimitMinutes", e.target.value)}
            disabled={disabled}
          />
          <FieldError message={errors.timeLimitMinutes} />
        </Field>
        <Field label="Điểm đạt (%)" hint="Từ 0 đến 100">
          <Input
            type="number"
            min={0}
            max={100}
            step="0.01"
            value={values.passScore}
            onChange={(e) => set("passScore", e.target.value)}
            disabled={disabled}
          />
          <FieldError message={errors.passScore} />
        </Field>
        <Field label="Số lần làm tối đa" hint="0 = không giới hạn">
          <Input
            type="number"
            min={0}
            value={values.maxAttempts}
            onChange={(e) => set("maxAttempts", e.target.value)}
            disabled={disabled}
          />
          <FieldError message={errors.maxAttempts} />
        </Field>
      </div>
      {showShuffle && (
        <label className="flex items-center gap-2 text-sm text-slate-700">
          <input
            type="checkbox"
            className="h-4 w-4 rounded border-slate-300 text-indigo-600"
            checked={values.shuffleQuestions}
            onChange={(e) => set("shuffleQuestions", e.target.checked)}
            disabled={disabled}
          />
          Xáo trộn thứ tự câu hỏi mỗi lần làm bài
        </label>
      )}
    </div>
  );
}
