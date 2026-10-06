import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { CourseQuizzes, QuizList } from "@/components/student/course-quizzes";
import { LessonTypeIcon, lessonTypeLabels } from "@/components/student/icons";
import { LearnSidebar } from "@/components/student/learn-sidebar";
import { LessonActions } from "@/components/student/lesson-actions";
import { LessonContent } from "@/components/student/lesson-content";
import { attempt, getCourse, isId } from "@/components/student/queries";
import { Alert, Card, Empty, LinkButton, ProgressBar, formatDuration } from "@/components/ui";
import { gateway, gatewayOrNull } from "@/lib/server/gateway";
import type { CourseProgress, Lesson, LessonProgressStatus, Quiz, Section } from "@/lib/types";

export async function generateMetadata({ params }: PageProps<"/learn/[courseId]">): Promise<Metadata> {
  const { courseId } = await params;
  const course = isId(courseId) ? await getCourse(courseId).catch(() => null) : null;
  return { title: course ? `Học: ${course.title}` : "Vào học" };
}

export default async function LearnPage({ params, searchParams }: PageProps<"/learn/[courseId]">) {
  const { courseId } = await params;
  const { lesson: lessonParam } = await searchParams;
  if (!isId(courseId)) notFound();

  // 404 tiến độ = chưa ghi danh khóa này.
  const [progress, course] = await Promise.all([
    gatewayOrNull<CourseProgress>(`/api/progress?courseId=${courseId}`),
    getCourse(courseId),
  ]);
  if (!progress || progress.status === "CANCELLED") redirect(`/courses/${courseId}`);
  if (!course) notFound();

  const [sections, quizzes] = await Promise.all([
    gateway<Section[]>(`/api/courses/${courseId}/curriculum`),
    attempt(gateway<Quiz[]>(`/api/quizzes?courseId=${courseId}`)),
  ]);
  const published = quizzes.data?.filter((q) => q.status === "PUBLISHED") ?? null;

  const status = new Map<number, LessonProgressStatus>(progress.lessons.map((p) => [p.lessonId, p.status]));
  const lessons = sections.flatMap((s) => s.lessons);
  const wanted = Array.isArray(lessonParam) ? lessonParam[0] : lessonParam;
  const current =
    lessons.find((l) => String(l.id) === wanted) ??
    lessons.find((l) => status.get(l.id) !== "COMPLETED") ??
    lessons[0] ??
    null;
  const index = current ? lessons.indexOf(current) : -1;
  const percent = Math.round(progress.progressPercent);
  const finished = progress.status === "COMPLETED" || percent >= 100;

  return (
    <div className="space-y-6">
      <header className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <Link href={`/courses/${course.id}`} className="text-sm text-indigo-600 hover:underline">
          ‹ Trang khóa học
        </Link>
        <h1 className="mt-1 text-2xl font-bold text-slate-900">{course.title}</h1>
        <div className="mt-4 flex flex-wrap items-center gap-4">
          <div className="min-w-48 flex-1">
            <ProgressBar value={percent} />
          </div>
          <span className="text-sm text-slate-600">
            <span className="font-semibold text-slate-900">{percent}%</span> · {progress.completedLessonsCount}/
            {progress.totalLessonsCount} bài đã hoàn thành
          </span>
        </div>
      </header>

      {finished && (
        <Alert kind="success">
          <span className="font-semibold">Chúc mừng! Bạn đã hoàn thành khóa học.</span>{" "}
          <Link href={`/certificates/${progress.enrollmentId}`} className="font-medium underline">
            Xem chứng chỉ của bạn
          </Link>
        </Alert>
      )}

      <div className="grid gap-6 lg:grid-cols-[320px_minmax(0,1fr)]">
        <div className="order-2 lg:order-1">
          <LearnSidebar courseId={course.id} sections={sections} currentLessonId={current?.id ?? null} progress={status} />
        </div>
        <div className="order-1 space-y-8 lg:order-2">
          {current ? (
            <LessonPanel
              lesson={current}
              status={status.get(current.id) ?? null}
              prev={lessons[index - 1] ?? null}
              next={lessons[index + 1] ?? null}
              quizzes={published?.filter((q) => q.lessonId === current.id) ?? []}
            />
          ) : (
            <Empty>Khóa học chưa có bài học nào.</Empty>
          )}
          <CourseQuizzes quizzes={published} error={quizzes.error} />
        </div>
      </div>
    </div>
  );
}

function LessonPanel({
  lesson,
  status,
  prev,
  next,
  quizzes,
}: {
  lesson: Lesson;
  status: LessonProgressStatus | null;
  prev: Lesson | null;
  next: Lesson | null;
  quizzes: Quiz[];
}) {
  const href = (l: Lesson) => `/learn/${lesson.courseId}?lesson=${l.id}`;
  return (
    <Card className="space-y-6">
      <div>
        <div className="flex items-center gap-2 text-sm text-slate-500">
          <LessonTypeIcon type={lesson.type} />
          {lessonTypeLabels[lesson.type]} · {formatDuration(lesson.durationSeconds)}
        </div>
        <h2 className="mt-1 text-xl font-semibold text-slate-900">{lesson.title}</h2>
      </div>

      <LessonContent lesson={lesson} />
      {quizzes.length > 0 && <QuizList quizzes={quizzes} />}

      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-5">
        {prev ? (
          <LinkButton href={href(prev)} variant="secondary">
            ‹ Bài trước
          </LinkButton>
        ) : (
          <span />
        )}
        <LessonActions
          courseId={lesson.courseId}
          lessonId={lesson.id}
          durationSeconds={lesson.durationSeconds}
          status={status}
        />
        {next ? (
          <LinkButton href={href(next)} variant="secondary">
            Bài tiếp ›
          </LinkButton>
        ) : (
          <span />
        )}
      </div>
    </Card>
  );
}
