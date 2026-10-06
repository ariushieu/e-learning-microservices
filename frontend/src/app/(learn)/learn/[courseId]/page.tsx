import { AwardIcon, BookOpenIcon, ChevronLeftIcon, ChevronRightIcon, ClockIcon, TrophyIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { EmptyState } from "@/components/common/empty-state";
import { attempt, getCourse, isId } from "@/components/course/queries";
import { CurriculumSheet } from "@/components/enrollment/curriculum-sheet";
import { LessonTypeIcon } from "@/components/enrollment/icons";
import { LearnSidebar } from "@/components/enrollment/learn-sidebar";
import { LessonActions } from "@/components/enrollment/lesson-actions";
import { LessonContent } from "@/components/enrollment/lesson-content";
import { UserMenu } from "@/components/layout/user-menu";
import { NotificationBell } from "@/components/notification/notification-bell";
import { CourseQuizzes, QuizList } from "@/components/quiz/course-quizzes";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { ScrollArea } from "@/components/ui/scroll-area";
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
    <div className="flex min-h-svh flex-col bg-background">
      <header className="sticky top-0 z-40 flex h-14 shrink-0 items-center gap-3 border-b bg-background px-3 sm:px-4">
        <Button asChild variant="ghost" size="lg" className="shrink-0 px-2">
          <Link href={`/courses/${course.id}`} aria-label="Trang khóa học">
            <ChevronLeftIcon />
            <span className="hidden sm:inline">Trang khóa học</span>
          </Link>
        </Button>
        <Separator orientation="vertical" className="h-6! self-center" />
        <h1 className="min-w-0 flex-1 truncate text-sm font-semibold sm:text-base">{course.title}</h1>
        <div className="hidden shrink-0 items-center gap-3 md:flex">
          <Progress value={percent} className="h-1.5 w-32" aria-label="Tiến độ khóa học" />
          <span className="text-xs text-muted-foreground tabular-nums">{summary}</span>
        </div>
        <div className="flex shrink-0 items-center gap-1">
          <NotificationBell />
          <UserMenu />
        </div>
      </header>

      <div className="flex flex-1">
        <main className="min-w-0 flex-1">
          <div className="mx-auto max-w-5xl space-y-8 px-4 py-6 sm:px-6 lg:px-10 lg:py-8">
            <div className="flex items-center justify-between gap-3 lg:hidden">
              <div className="min-w-0 flex-1 space-y-1.5 md:hidden">
                <Progress value={percent} className="h-1.5" aria-label="Tiến độ khóa học" />
                <p className="text-xs text-muted-foreground tabular-nums">{summary}</p>
              </div>
              <div className="ml-auto">
                <CurriculumSheet summary={`${summary} hoàn thành`}>{curriculum}</CurriculumSheet>
              </div>
            </div>

            {finished && (
              <div className="flex flex-col gap-4 rounded-xl border border-emerald-600/20 bg-emerald-50 p-4 sm:flex-row sm:items-center dark:bg-emerald-500/10">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-emerald-700 dark:bg-emerald-500/20 dark:text-emerald-400">
                  <TrophyIcon className="size-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="font-medium text-emerald-900 dark:text-emerald-300">Chúc mừng! Bạn đã hoàn thành khóa học.</p>
                  <p className="text-sm text-emerald-800/80 dark:text-emerald-400/80">Chứng chỉ của bạn đã sẵn sàng để xem và in.</p>
                </div>
                <Button asChild variant="outline" className="shrink-0">
                  <Link href={`/certificates/${progress.enrollmentId}`}>
                    <AwardIcon /> Xem chứng chỉ của bạn
                  </Link>
                </Button>
              </div>
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
          </div>
        </main>

        <aside className="sticky top-14 hidden h-[calc(100svh-3.5rem)] w-80 shrink-0 flex-col border-l bg-background lg:flex">
          <div className="border-b px-4 py-3">
            <p className="text-sm font-semibold">Nội dung khóa học</p>
            <p className="text-xs text-muted-foreground tabular-nums">{summary} hoàn thành</p>
          </div>
          <ScrollArea className="min-h-0 flex-1">{curriculum}</ScrollArea>
        </aside>
      </div>
    </div>
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
          <span className="tabular-nums">
            Bài {position}/{total}
          </span>
          <span className="flex items-center gap-1.5">
            <LessonTypeIcon type={lesson.type} />
            {label(lesson.type)}
          </span>
          <span className="flex items-center gap-1.5">
            <ClockIcon className="size-4" />
            {formatDuration(lesson.durationSeconds)}
          </span>
        </div>
        <h2 className="text-2xl font-semibold tracking-tight text-balance">{lesson.title}</h2>
      </div>

      <LessonContent lesson={lesson} />

      {quizzes.length > 0 && (
        <section className="space-y-3">
          <h3 className="text-sm font-semibold">Bài kiểm tra của bài học</h3>
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
          <LessonActions
            courseId={lesson.courseId}
            lessonId={lesson.id}
            durationSeconds={lesson.durationSeconds}
            status={status}
          />
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
