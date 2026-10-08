"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { api } from "@/lib/client";
import type { CourseProgress } from "@/lib/types";

/** Chờ Kafka có giới hạn khi quay lại trang học, kể cả nút Back hoặc đổi tab. */
export function QuizProgressSync({ courseId, pendingLessonIds }: {
  courseId: number;
  pendingLessonIds: number[];
}) {
  const router = useRouter();
  const pendingKey = pendingLessonIds.join(",");

  useEffect(() => {
    if (!pendingKey) return;
    const pending = new Set(pendingKey.split(",").map(Number));
    let cancelled = false;
    let running = false;
    let remaining = 12;
    let timer: ReturnType<typeof setTimeout> | undefined;

    async function poll() {
      if (cancelled || running || remaining <= 0 || document.visibilityState === "hidden") return;
      running = true;
      remaining -= 1;
      try {
        const progress = await api<CourseProgress>(`/api/progress?courseId=${courseId}`);
        if (cancelled) return;
        if (progress.status === "CANCELLED" || progress.lessons.some(
          (lesson) => pending.has(lesson.lessonId) && lesson.status === "COMPLETED",
        )) {
          remaining = 0;
          router.refresh();
        }
      } catch {
        // Không che nội dung học vì một lần kiểm tra nền lỗi. Nút hoàn thành vẫn hoạt động.
      } finally {
        running = false;
        if (!cancelled && remaining > 0) timer = setTimeout(poll, 1000);
      }
    }

    function resume() {
      if (document.visibilityState === "hidden" || cancelled) return;
      clearTimeout(timer);
      remaining = 12;
      void poll();
    }

    void poll();
    window.addEventListener("focus", resume);
    window.addEventListener("pageshow", resume);
    document.addEventListener("visibilitychange", resume);
    return () => {
      cancelled = true;
      clearTimeout(timer);
      window.removeEventListener("focus", resume);
      window.removeEventListener("pageshow", resume);
      document.removeEventListener("visibilitychange", resume);
    };
  }, [courseId, pendingKey, router]);

  return null;
}
