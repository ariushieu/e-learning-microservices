import "server-only";
import { cache } from "react";
import { getMyEnrollments } from "@/components/course/queries";
import { gateway } from "@/lib/server/gateway";
import type { Certificate, Enrollment, Quiz, QuizAttempt } from "@/lib/types";

/** Chứng chỉ của các khóa đã hoàn thành; khóa nào lỗi khi tải thì bỏ qua, không làm hỏng cả trang. */
export const getMyCertificates = cache(async (enrollments?: Enrollment[]): Promise<Certificate[]> => {
  const list = enrollments ?? (await getMyEnrollments());
  const done = list.filter((e) => e.status === "COMPLETED");
  const certificates = await Promise.all(
    done.map((e) => gateway<Certificate>(`/api/enrollments/${e.id}/certificate`).catch(() => null)),
  );
  return certificates
    .filter((c): c is Certificate => c !== null)
    .sort((a, b) => b.issuedAt.localeCompare(a.issuedAt));
});

export interface QuizResult {
  attempt: QuizAttempt;
  courseId: number;
  courseTitle: string;
}

/** Các lượt đã nộp ở bài kiểm tra của những khóa đang học hoặc đã xong, mới nhất trước. */
export const getMyQuizResults = cache(async (enrollments: Enrollment[]): Promise<QuizResult[]> => {
  const courses = enrollments.filter((e) => e.status !== "CANCELLED");
  const quizLists = await Promise.all(
    courses.map((e) => gateway<Quiz[]>(`/api/quizzes?courseId=${e.courseId}`).catch(() => [] as Quiz[])),
  );
  const pairs = quizLists.flatMap((quizzes, i) => quizzes.map((q) => ({ quiz: q, enrollment: courses[i] })));
  const attempts = await Promise.all(
    pairs.map(({ quiz }) => gateway<QuizAttempt[]>(`/api/quizzes/${quiz.id}/attempts`).catch(() => [] as QuizAttempt[])),
  );
  return attempts
    .flatMap((list, i) =>
      list
        .filter((a) => a.status !== "IN_PROGRESS" && a.submittedAt)
        .map((a) => ({ attempt: a, courseId: pairs[i].enrollment.courseId, courseTitle: pairs[i].enrollment.courseTitle })),
    )
    .sort((a, b) => (b.attempt.submittedAt ?? "").localeCompare(a.attempt.submittedAt ?? ""));
});
