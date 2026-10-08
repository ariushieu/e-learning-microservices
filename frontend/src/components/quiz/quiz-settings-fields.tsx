"use client";

import { useId } from "react";
import { FormField } from "@/components/common/form-field";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
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
  shuffleOptions: boolean;
}

export type FieldErrors = Partial<Record<keyof QuizSettingsValues, string>>;

export const emptyQuizSettings: QuizSettingsValues = {
  title: "",
  description: "",
  timeLimitMinutes: "",
  passScore: "50",
  maxAttempts: "3",
  shuffleQuestions: false,
  shuffleOptions: false,
};

export function quizToSettings(quiz: Quiz): QuizSettingsValues {
  return {
    title: quiz.title,
    description: quiz.description ?? "",
    timeLimitMinutes: quiz.timeLimitMinutes == null ? "" : String(quiz.timeLimitMinutes),
    passScore: String(Number(quiz.passScore)),
    maxAttempts: String(quiz.maxAttempts),
    shuffleQuestions: quiz.shuffleQuestions,
    shuffleOptions: quiz.shuffleOptions,
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
    shuffleOptions: v.shuffleOptions,
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

/**
 * Các ô cấu hình bài kiểm tra, dùng chung cho form tạo mới và form cài đặt.
 * `compact` xếp các ô số thành một cột (dùng trong cột hẹp bên phải trang soạn bài).
 */
export function QuizSettingsFields({
  values,
  errors,
  onChange,
  disabled,
  showShuffle = true,
  compact = false,
}: {
  values: QuizSettingsValues;
  errors: FieldErrors;
  onChange: (v: QuizSettingsValues) => void;
  disabled?: boolean;
  showShuffle?: boolean;
  compact?: boolean;
}) {
  const id = useId();
  const set = <K extends keyof QuizSettingsValues>(key: K, value: QuizSettingsValues[K]) =>
    onChange({ ...values, [key]: value });

  return (
    <div className="space-y-5">
      <FormField id={`${id}-title`} label="Tiêu đề" required error={errors.title}>
        <Input
          id={`${id}-title`}
          value={values.title}
          onChange={(e) => set("title", e.target.value)}
          maxLength={200}
          disabled={disabled}
          placeholder="Ví dụ: Kiểm tra chương 1"
          aria-invalid={Boolean(errors.title)}
        />
      </FormField>
      <FormField id={`${id}-description`} label="Mô tả" error={errors.description}>
        <Textarea
          id={`${id}-description`}
          rows={3}
          value={values.description}
          onChange={(e) => set("description", e.target.value)}
          maxLength={1000}
          disabled={disabled}
          placeholder="Nội dung, phạm vi kiến thức của bài kiểm tra"
          aria-invalid={Boolean(errors.description)}
        />
      </FormField>
      <div className={compact ? "space-y-5" : "grid gap-5 sm:grid-cols-3"}>
        <FormField
          id={`${id}-time`}
          label="Thời gian (phút)"
          hint="Để trống nếu không giới hạn"
          error={errors.timeLimitMinutes}
        >
          <Input
            id={`${id}-time`}
            type="number"
            min={1}
            inputMode="numeric"
            value={values.timeLimitMinutes}
            onChange={(e) => set("timeLimitMinutes", e.target.value)}
            disabled={disabled}
            aria-invalid={Boolean(errors.timeLimitMinutes)}
          />
        </FormField>
        <FormField id={`${id}-pass`} label="Điểm đạt (%)" hint="Từ 0 đến 100" error={errors.passScore}>
          <Input
            id={`${id}-pass`}
            type="number"
            min={0}
            max={100}
            step="0.01"
            value={values.passScore}
            onChange={(e) => set("passScore", e.target.value)}
            disabled={disabled}
            aria-invalid={Boolean(errors.passScore)}
          />
        </FormField>
        <FormField id={`${id}-attempts`} label="Số lần làm tối đa" hint="0 = không giới hạn" error={errors.maxAttempts}>
          <Input
            id={`${id}-attempts`}
            type="number"
            min={0}
            inputMode="numeric"
            value={values.maxAttempts}
            onChange={(e) => set("maxAttempts", e.target.value)}
            disabled={disabled}
            aria-invalid={Boolean(errors.maxAttempts)}
          />
        </FormField>
      </div>
      {showShuffle && (
        <div className="space-y-3">
          <div className="flex items-start justify-between gap-4 rounded-lg border p-3">
            <div className="space-y-1">
              <Label htmlFor={`${id}-shuffle`}>Xáo trộn câu hỏi</Label>
              <p className="text-xs text-muted-foreground">Giữ nguyên thứ tự khi tải lại trong cùng lượt làm</p>
            </div>
            <Switch
              id={`${id}-shuffle`}
              checked={values.shuffleQuestions}
              onCheckedChange={(checked) => set("shuffleQuestions", checked)}
              disabled={disabled}
            />
          </div>
          <div className="flex items-start justify-between gap-4 rounded-lg border p-3">
            <div className="space-y-1">
              <Label htmlFor={`${id}-shuffle-options`}>Xáo trộn đáp án</Label>
              <p className="text-xs text-muted-foreground">Giữ nguyên trong cùng lượt làm; câu Đúng/Sai không xáo</p>
            </div>
            <Switch
              id={`${id}-shuffle-options`}
              checked={values.shuffleOptions}
              onCheckedChange={(checked) => set("shuffleOptions", checked)}
              disabled={disabled}
            />
          </div>
        </div>
      )}
    </div>
  );
}
