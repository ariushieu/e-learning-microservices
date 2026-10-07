"use client";

import { CheckIcon, CircleCheckIcon, Loader2Icon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { ErrorAlert } from "@/components/common/error-alert";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { LessonProgress, LessonProgressStatus } from "@/lib/types";

/**
 * Ghi tiến độ cho bài đang mở.
 *
 * Chỉ báo IN_PROGRESS khi bài chưa có tiến độ: backend ghi đè trạng thái theo request, gửi
 * IN_PROGRESS cho bài đã xong sẽ hạ nó về "đang học" và có thể kéo khóa đã hoàn thành về ACTIVE.
 *
 * Cùng lý do, "Đánh dấu hoàn thành" phải chờ lệnh IN_PROGRESS gửi lúc mở bài chạy xong. Bấm
 * nhanh thì hai request đi song song, COMPLETED có thể tới backend trước rồi bị IN_PROGRESS
 * ghi đè — chạy thử E2E đã gặp đúng chuyện này.
 */
export function LessonActions({
  courseId,
  lessonId,
  durationSeconds,
  status,
}: {
  courseId: number;
  lessonId: number;
  durationSeconds: number;
  status: LessonProgressStatus | null;
}) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const started = useRef<number | null>(null);
  const starting = useRef<Promise<unknown>>(Promise.resolve());

  useEffect(() => {
    if (status || started.current === lessonId) return;
    started.current = lessonId;
    starting.current = api<LessonProgress>(`/api/lessons/${lessonId}/progress`, {
      method: "PUT",
      body: { courseId, status: "IN_PROGRESS" },
    }).catch(() => {});
  }, [courseId, lessonId, status]);

  async function complete() {
    setLoading(true);
    setError(null);
    try {
      await starting.current;
      await api<LessonProgress>(`/api/lessons/${lessonId}/progress`, {
        method: "PUT",
        body: { courseId, status: "COMPLETED", watchedSeconds: durationSeconds },
      });
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  if (status === "COMPLETED") {
    return (
      <span className="inline-flex h-9 w-full items-center justify-center gap-2 rounded-lg bg-achievement-soft px-4 text-sm font-medium text-achievement-strong sm:w-auto">
        <CircleCheckIcon className="size-4 text-achievement" aria-hidden /> Đã hoàn thành
      </span>
    );
  }

  return (
    <div className="flex w-full flex-col items-stretch gap-2 sm:w-auto sm:items-center">
      <Button size="lg" onClick={complete} disabled={loading} className="px-4">
        {loading ? <Loader2Icon className="animate-spin" /> : <CheckIcon />} Đánh dấu hoàn thành
      </Button>
      {error && <ErrorAlert message={error} />}
    </div>
  );
}
