"use client";

import { CircleAlertIcon, Loader2Icon, PlusIcon, XIcon } from "lucide-react";
import { useId, useState, type FormEvent, type ReactNode } from "react";
import { ErrorAlert } from "@/components/common/error-alert";
import { FormField } from "@/components/common/form-field";
import { NativeSelect } from "@/components/common/native-select";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Textarea } from "@/components/ui/textarea";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { Question, QuestionInput, QuestionType } from "@/lib/types";
import { cn } from "@/lib/utils";
import { questionTypeLabels } from "./labels";

interface OptionDraft {
  key: number;
  /** id phương án đã lưu; gửi lại để bài làm cũ vẫn trỏ đúng phương án học viên đã chọn. */
  id?: number;
  content: string;
  isCorrect: boolean;
}

let seq = 0;
const draft = (content = "", isCorrect = false, id?: number): OptionDraft => ({ key: ++seq, id, content, isCorrect });

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
 * Form thêm hoặc sửa một câu hỏi. Có `initial` là sửa (PUT, phương án cũ giữ nguyên id),
 * không có là thêm mới (POST).
 */
export function QuestionForm({
  quizId,
  initial,
  index,
  onSaved,
  onCancel,
}: {
  quizId: number;
  initial?: Question;
  /** Số thứ tự câu đang sửa, chỉ để hiển thị tiêu đề. */
  index?: number;
  onSaved: () => void;
  onCancel: () => void;
}) {
  const id = useId();
  const [content, setContent] = useState(initial?.content ?? "");
  const [type, setType] = useState<QuestionType>(initial?.type ?? "SINGLE_CHOICE");
  const [score, setScore] = useState(initial ? String(Number(initial.score)) : "1");
  const [explanation, setExplanation] = useState(initial?.explanation ?? "");
  const [options, setOptions] = useState<OptionDraft[]>(() =>
    initial
      ? initial.options.map((o) => draft(o.content, Boolean(o.isCorrect), o.id))
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
      options: options.map((o, i) => ({ id: o.id, content: o.content.trim(), isCorrect: o.isCorrect, position: i + 1 })),
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

  const rows = options.map((o, i) => {
    const marker = multiple ? (
      <Checkbox
        checked={o.isCorrect}
        onCheckedChange={(c) => markCorrect(o.key, c === true)}
        disabled={saving}
        aria-label={`Phương án ${letter(i)} là đáp án đúng`}
        title="Đáp án đúng"
      />
    ) : (
      <RadioGroupItem
        value={String(o.key)}
        disabled={saving}
        aria-label={`Phương án ${letter(i)} là đáp án đúng`}
        title="Đáp án đúng"
      />
    );
    return (
      <OptionRow key={o.key} correct={o.isCorrect} marker={marker} letter={letter(i)}>
        <Input
          value={o.content}
          onChange={(e) => updateOption(o.key, { content: e.target.value })}
          maxLength={1000}
          placeholder={`Phương án ${letter(i)}`}
          disabled={saving}
          className="bg-card"
        />
        {!fixedOptions && (
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            onClick={() => setOptions((prev) => prev.filter((x) => x.key !== o.key))}
            disabled={saving || options.length <= 2}
            aria-label={`Xóa phương án ${letter(i)}`}
            title="Xóa phương án"
          >
            <XIcon />
          </Button>
        )}
      </OptionRow>
    );
  });

  const correctKey = options.find((o) => o.isCorrect)?.key;

  return (
    <Card className="ring-primary/30">
      <form onSubmit={submit} noValidate className="flex flex-col gap-(--card-spacing)">
        <CardHeader>
          <CardTitle className="text-subheading">
            {initial ? `Sửa câu ${index != null ? index + 1 : ""}`.trim() : "Thêm câu hỏi"}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-5">
          <FormField id={`${id}-content`} label="Nội dung câu hỏi" required>
            <Textarea
              id={`${id}-content`}
              rows={3}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              disabled={saving}
              placeholder="Nhập câu hỏi"
            />
          </FormField>

          <div className="grid gap-5 sm:grid-cols-2">
            <FormField id={`${id}-type`} label="Loại câu hỏi">
              <NativeSelect
                id={`${id}-type`}
                value={type}
                onChange={(e) => changeType(e.target.value as QuestionType)}
                disabled={saving}
              >
                {(Object.keys(questionTypeLabels) as QuestionType[]).map((t) => (
                  <option key={t} value={t}>
                    {questionTypeLabels[t]}
                  </option>
                ))}
              </NativeSelect>
            </FormField>
            <FormField id={`${id}-score`} label="Điểm" required>
              <Input
                id={`${id}-score`}
                type="number"
                min={0.01}
                step="0.01"
                value={score}
                onChange={(e) => setScore(e.target.value)}
                disabled={saving}
              />
            </FormField>
          </div>

          <fieldset className="space-y-3">
            <legend className="text-sm font-medium">
              Phương án trả lời{" "}
              <span className="font-normal text-muted-foreground">
                ({multiple ? "tích mọi đáp án đúng" : "chọn đúng 1 đáp án đúng"})
              </span>
            </legend>
            {multiple ? (
              <div className="grid gap-2">{rows}</div>
            ) : (
              <RadioGroup
                value={correctKey != null ? String(correctKey) : ""}
                onValueChange={(v) => markCorrect(Number(v), true)}
                disabled={saving}
                className="gap-2"
              >
                {rows}
              </RadioGroup>
            )}
            {!fixedOptions && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setOptions((prev) => [...prev, draft()])}
                disabled={saving}
              >
                <PlusIcon /> Thêm phương án
              </Button>
            )}
          </fieldset>

          <FormField id={`${id}-explanation`} label="Giải thích" hint="Học viên thấy phần này sau khi nộp bài.">
            <Textarea
              id={`${id}-explanation`}
              rows={2}
              value={explanation}
              onChange={(e) => setExplanation(e.target.value)}
              disabled={saving}
              placeholder="Không bắt buộc"
            />
          </FormField>

          {errors.length > 0 && (
            <Alert variant="destructive">
              <CircleAlertIcon />
              <AlertTitle>Câu hỏi chưa hợp lệ</AlertTitle>
              <AlertDescription>
                <ul className="list-inside list-disc space-y-0.5">
                  {errors.map((m) => (
                    <li key={m}>{m}</li>
                  ))}
                </ul>
              </AlertDescription>
            </Alert>
          )}
          {serverError && <ErrorAlert message={serverError} />}
        </CardContent>
        <CardFooter className="justify-end gap-2">
          <Button type="button" variant="outline" onClick={onCancel} disabled={saving}>
            Hủy
          </Button>
          <Button type="submit" disabled={saving}>
            {saving && <Loader2Icon className="animate-spin" />}
            {initial ? "Lưu câu hỏi" : "Thêm câu hỏi"}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}

function OptionRow({
  correct,
  marker,
  letter,
  children,
}: {
  correct: boolean;
  marker: ReactNode;
  letter: string;
  children: ReactNode;
}) {
  return (
    <div
      className={cn(
        "flex items-center gap-3 rounded-lg border px-3 py-2 transition-colors",
        correct ? "border-success/40 bg-success-soft" : "bg-muted/40",
      )}
    >
      {marker}
      <span className={cn("w-4 shrink-0 text-sm font-medium", correct ? "text-success-strong" : "text-muted-foreground")}>{letter}</span>
      {children}
    </div>
  );
}
