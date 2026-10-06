"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { Alert, Button } from "@/components/ui";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { LessonProgress, LessonProgressStatus } from "@/lib/types";
import { CheckIcon } from "./icons";

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
      <span className="inline-flex items-center gap-2 rounded-lg bg-emerald-50 px-4 py-2 text-sm font-medium text-emerald-700">
        <CheckIcon /> Đã hoàn thành
      </span>
    );
  }

  return (
    <div className="flex flex-col items-center gap-2">
      <Button onClick={complete} loading={loading}>
        <CheckIcon /> Đánh dấu hoàn thành
      </Button>
      {error && <Alert>{error}</Alert>}
    </div>
  );
}
