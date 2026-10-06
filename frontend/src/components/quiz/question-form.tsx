"use client";

import { useId, useState, type FormEvent } from "react";
import { Alert, Button, Field, Input, Select, Textarea } from "@/components/ui";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { Question, QuestionInput, QuestionType } from "@/lib/types";
import { questionTypeLabels } from "./labels";

interface OptionDraft {
  key: number;
  content: string;
  isCorrect: boolean;
}

let seq = 0;
const draft = (content = "", isCorrect = false): OptionDraft => ({ key: ++seq, content, isCorrect });

const trueFalseOptions = () => [draft("Đúng"), draft("Sai")];
const letter = (i: number) => String.fromCharCode(65 + (i % 26));

/** Cùng luật với QuestionServiceImpl.validateOptions và @Valid của CreateQuestionRequest. */
function validate(content: string, type: QuestionType, score: string, options: OptionDraft[]): string[] {
  const errors: string[] = [];
  if (!content.trim()) errors.push("Nội dung câu hỏi không được để trống.");
  const s = Number(score);
  if (!score.trim() || Number.isNaN(s) || s < 0.01) errors.push("Điểm câu hỏi phải lớn hơn 0.");
  if (options.length < 2) errors.push("Câu hỏi phải có ít nhất 2 phương án trả lời.");
  if (options.some((o) => !o.content.trim())) errors.push("Nội dung phương án không được để trống.");
  if (options.some((o) => o.content.length > 1000)) errors.push("Nội dung phương án không được quá 1000 ký tự.");
  const correct = options.filter((o) => o.isCorrect).length;
  if (type === "MULTIPLE_CHOICE") {
    if (correct < 1) errors.push("Câu nhiều lựa chọn phải có ít nhất 1 đáp án đúng.");
  } else if (correct !== 1) {
    errors.push("Câu một đáp án / Đúng-Sai phải có đúng 1 đáp án đúng.");
  }
  return errors;
}

/**
 * Form thêm hoặc sửa một câu hỏi. Có `initial` là sửa (PUT thay toàn bộ phương án),
 * không có là thêm mới (POST).
 */
export function QuestionForm({
  quizId,
  initial,
  onSaved,
  onCancel,
}: {
  quizId: number;
  initial?: Question;
  onSaved: () => void;
  onCancel: () => void;
}) {
  const radioName = useId();
  const [content, setContent] = useState(initial?.content ?? "");
  const [type, setType] = useState<QuestionType>(initial?.type ?? "SINGLE_CHOICE");
  const [score, setScore] = useState(initial ? String(Number(initial.score)) : "1");
  const [explanation, setExplanation] = useState(initial?.explanation ?? "");
  const [options, setOptions] = useState<OptionDraft[]>(() =>
    initial
      ? initial.options.map((o) => draft(o.content, Boolean(o.isCorrect)))
      : [draft(), draft(), draft(), draft()],
  );
  const [errors, setErrors] = useState<string[]>([]);
  const [serverError, setServerError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const multiple = type === "MULTIPLE_CHOICE";
  const fixedOptions = type === "TRUE_FALSE";

  function changeType(next: QuestionType) {
    setType(next);
    if (next === "TRUE_FALSE") {
      setOptions(trueFalseOptions());
    } else if (type === "TRUE_FALSE") {
      setOptions([draft(), draft(), draft(), draft()]);
    } else if (next === "SINGLE_CHOICE") {
      // Từ nhiều đáp án sang một đáp án: chỉ giữ đáp án đúng đầu tiên.
      setOptions((prev) => {
        const first = prev.find((o) => o.isCorrect)?.key;
        return prev.map((o) => ({ ...o, isCorrect: o.key === first }));
      });
    }
  }

  function updateOption(key: number, patch: Partial<OptionDraft>) {
    setOptions((prev) => prev.map((o) => (o.key === key ? { ...o, ...patch } : o)));
  }

  function markCorrect(key: number, checked: boolean) {
    setOptions((prev) =>
      prev.map((o) => (multiple ? (o.key === key ? { ...o, isCorrect: checked } : o) : { ...o, isCorrect: o.key === key })),
    );
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    const found = validate(content, type, score, options);
    setErrors(found);
    setServerError(null);
    if (found.length) return;

    const body: QuestionInput = {
      content: content.trim(),
      type,
      score: Number(score),
      explanation: explanation.trim() || undefined,
      options: options.map((o, i) => ({ content: o.content.trim(), isCorrect: o.isCorrect, position: i + 1 })),
    };
    setSaving(true);
    try {
      if (initial) {
        await api(`/api/quizzes/${quizId}/questions/${initial.id}`, { method: "PUT", body });
      } else {
        await api(`/api/quizzes/${quizId}/questions`, { method: "POST", body });
      }
      onSaved();
    } catch (err) {
      setServerError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-4 rounded-xl border border-indigo-200 bg-indigo-50/30 p-5">
      <h3 className="font-semibold text-slate-900">{initial ? "Sửa câu hỏi" : "Thêm câu hỏi"}</h3>

      <Field label="Nội dung câu hỏi *">
        <Textarea rows={3} value={content} onChange={(e) => setContent(e.target.value)} disabled={saving} />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Loại câu hỏi">
          <Select value={type} onChange={(e) => changeType(e.target.value as QuestionType)} disabled={saving}>
            {(Object.keys(questionTypeLabels) as QuestionType[]).map((t) => (
              <option key={t} value={t}>
                {questionTypeLabels[t]}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Điểm *">
          <Input
            type="number"
            min={0.01}
            step="0.01"
            value={score}
            onChange={(e) => setScore(e.target.value)}
            disabled={saving}
          />
        </Field>
      </div>

      <fieldset className="space-y-2">
        <legend className="mb-1 text-sm font-medium text-slate-700">
          Phương án trả lời{" "}
          <span className="font-normal text-slate-500">
            ({multiple ? "tích mọi đáp án đúng" : "chọn đúng 1 đáp án đúng"})
          </span>
        </legend>
        {options.map((o, i) => (
          <div key={o.key} className="flex items-center gap-2">
            <input
              type={multiple ? "checkbox" : "radio"}
              name={radioName}
              title="Đáp án đúng"
              aria-label={`Phương án ${letter(i)} là đáp án đúng`}
              className="h-4 w-4 shrink-0 accent-emerald-600"
              checked={o.isCorrect}
              onChange={(e) => markCorrect(o.key, e.target.checked)}
              disabled={saving}
            />
            <span className="w-5 shrink-0 text-sm font-medium text-slate-500">{letter(i)}.</span>
            <Input
              value={o.content}
              onChange={(e) => updateOption(o.key, { content: e.target.value })}
              maxLength={1000}
              placeholder={`Phương án ${letter(i)}`}
              disabled={saving}
            />
            {!fixedOptions && (
              <Button
                type="button"
                variant="ghost"
                className="shrink-0 px-2"
                onClick={() => setOptions((prev) => prev.filter((x) => x.key !== o.key))}
                disabled={saving || options.length <= 2}
                title="Xóa phương án"
              >
                ✕
              </Button>
            )}
          </div>
        ))}
        {!fixedOptions && (
          <Button
            type="button"
            variant="secondary"
            onClick={() => setOptions((prev) => [...prev, draft()])}
            disabled={saving}
          >
            + Thêm phương án
          </Button>
        )}
      </fieldset>

      <Field label="Giải thích" hint="Học viên thấy phần này sau khi nộp bài">
        <Textarea rows={2} value={explanation} onChange={(e) => setExplanation(e.target.value)} disabled={saving} />
      </Field>

      {errors.length > 0 && (
        <Alert>
          <ul className="list-inside list-disc space-y-0.5">
            {errors.map((m) => (
              <li key={m}>{m}</li>
            ))}
          </ul>
        </Alert>
      )}
      {serverError && <Alert>{serverError}</Alert>}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onCancel} disabled={saving}>
          Hủy
        </Button>
        <Button type="submit" loading={saving}>
          {initial ? "Lưu câu hỏi" : "Thêm câu hỏi"}
        </Button>
      </div>
    </form>
  );
}
