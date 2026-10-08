import { AwardIcon, BookOpenIcon, ChevronLeftIcon, ChevronRightIcon, ClockIcon, TrophyIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { Callout } from "@/components/common/callout";
import { EmptyState } from "@/components/common/empty-state";
import { LessonTypeIcon } from "@/components/course/icons";
import { attempt, getCourse, isId } from "@/components/course/queries";
import { CurriculumSheet } from "@/components/enrollment/curriculum-sheet";
import { LearnSidebar } from "@/components/enrollment/learn-sidebar";
import { LessonActions } from "@/components/enrollment/lesson-actions";
import { LessonContent } from "@/components/enrollment/lesson-content";
import { QuizProgressSync } from "@/components/enrollment/quiz-progress-sync";
import { CourseQuizzes, QuizList } from "@/components/quiz/course-quizzes";
import { FocusLayout } from "@/components/templates/focus-layout";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { formatDuration, label } from "@/lib/format";
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
  const summary = `${progress.completedLessonsCount}/${progress.totalLessonsCount} bài · ${percent}%`;

  const curriculum = (
    <LearnSidebar courseId={course.id} sections={sections} currentLessonId={current?.id ?? null} progress={status} />
  );

  return (
    <FocusLayout
      back={{ href: `/courses/${course.id}`, label: "Trang khóa học" }}
      title={course.title}
      progress={{ value: progress.progressPercent, summary }}
      panel={{ title: "Nội dung khóa học", subtitle: `${summary} hoàn thành`, content: curriculum }}
      mobileBar={<CurriculumSheet summary={`${summary} hoàn thành`}>{curriculum}</CurriculumSheet>}
    >
      {!finished && (
        <QuizProgressSync courseId={course.id} pendingLessonIds={[
          ...new Set((published ?? []).flatMap((quiz) =>
            quiz.lessonId && status.get(quiz.lessonId) !== "COMPLETED" ? [quiz.lessonId] : [],
          )),
        ]} />
      )}
      {finished && (
        <Callout
          icon={TrophyIcon}
          tone="achievement"
          title="Chúc mừng! Bạn đã hoàn thành khóa học."
          action={
            <Button asChild variant="outline">
              <Link href={`/certificates/${progress.enrollmentId}`}>
                <AwardIcon /> Xem chứng chỉ của bạn
              </Link>
            </Button>
          }
        >
          Chứng chỉ của bạn đã sẵn sàng để xem và in.
        </Callout>
      )}

      {current ? (
        <LessonPanel
          lesson={current}
          status={status.get(current.id) ?? null}
          prev={lessons[index - 1] ?? null}
          next={lessons[index + 1] ?? null}
          position={index + 1}
          total={lessons.length}
          quizzes={published?.filter((q) => q.lessonId === current.id) ?? []}
        />
      ) : (
        <EmptyState icon={BookOpenIcon} title="Khóa học chưa có bài học nào" description="Giảng viên chưa thêm nội dung cho khóa học này." />
      )}

      <Separator />
      <CourseQuizzes quizzes={published} error={quizzes.error} />
    </FocusLayout>
  );
}

function LessonPanel({
  lesson,
  status,
  prev,
  next,
  position,
  total,
  quizzes,
}: {
  lesson: Lesson;
  status: LessonProgressStatus | null;
  prev: Lesson | null;
  next: Lesson | null;
  position: number;
  total: number;
  quizzes: Quiz[];
}) {
  const href = (l: Lesson) => `/learn/${lesson.courseId}?lesson=${l.id}`;
  return (
    <article className="space-y-6">
      <div className="space-y-2">
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
          <span className="text-eyebrow text-primary tabular-nums">
            Bài {position}/{total}
          </span>
          <span className="flex items-center gap-1.5">
            <LessonTypeIcon type={lesson.type} />
            {label(lesson.type)}
          </span>
          <span className="flex items-center gap-1.5">
            <ClockIcon className="size-4" aria-hidden />
            {formatDuration(lesson.durationSeconds)}
          </span>
        </div>
        <h2 className="text-title">{lesson.title}</h2>
      </div>

      <LessonContent lesson={lesson} />

      {quizzes.length > 0 && (
        <section className="space-y-3">
          <h3 className="text-subheading">Bài kiểm tra của bài học</h3>
          <p className="text-sm text-muted-foreground">Đạt bài kiểm tra thì bài này tự hoàn thành.</p>
          <QuizList quizzes={quizzes} />
        </section>
      )}

      <div className="flex flex-col gap-3 border-t pt-6 sm:flex-row sm:items-center sm:justify-between">
        <div className="order-2 sm:order-1 sm:w-40">
          {prev && (
            <Button asChild variant="outline" size="lg" className="w-full sm:w-auto">
              <Link href={href(prev)}>
                <ChevronLeftIcon /> Bài trước
              </Link>
            </Button>
          )}
        </div>
        <div className="order-1 sm:order-2">
          <LessonActions courseId={lesson.courseId} lessonId={lesson.id} durationSeconds={lesson.durationSeconds} status={status} />
        </div>
        <div className="order-3 flex sm:w-40 sm:justify-end">
          {next && (
            <Button asChild variant="outline" size="lg" className="w-full sm:w-auto">
              <Link href={href(next)}>
                Bài tiếp <ChevronRightIcon />
              </Link>
            </Button>
          )}
        </div>
      </div>
    </article>
  );
}
