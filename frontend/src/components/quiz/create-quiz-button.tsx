"use client";

import { Loader2Icon, PlusIcon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { ErrorAlert } from "@/components/common/error-alert";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
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

/** Nút "Tạo bài kiểm tra" mở hộp thoại tạo mới; tạo xong chuyển sang trang soạn câu hỏi. */
export function CreateQuizButton({ courseId }: { courseId: number }) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [values, setValues] = useState(emptyQuizSettings);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  function onOpenChange(next: boolean) {
    if (saving) return;
    setOpen(next);
    if (!next) {
      setValues(emptyQuizSettings);
      setErrors({});
      setError(null);
    }
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

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogTrigger asChild>
        <Button>
          <PlusIcon /> Tạo bài kiểm tra
        </Button>
      </DialogTrigger>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-xl">
        <form onSubmit={submit} className="space-y-5" noValidate>
          <DialogHeader>
            <DialogTitle>Bài kiểm tra mới</DialogTitle>
            <DialogDescription>
              Bài kiểm tra được tạo ở trạng thái nháp, thêm câu hỏi rồi mới xuất bản.
            </DialogDescription>
          </DialogHeader>
          {error && <ErrorAlert message={error} />}
          <QuizSettingsFields
            values={values}
            errors={errors}
            onChange={setValues}
            disabled={saving}
            showShuffle={false}
          />
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline" disabled={saving}>
                Hủy
              </Button>
            </DialogClose>
            <Button type="submit" disabled={saving}>
              {saving && <Loader2Icon className="animate-spin" />}
              Tạo và soạn câu hỏi
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
