import { CircleCheckIcon, CircleDashedIcon, CircleIcon, CirclePlayIcon } from "lucide-react";
import Link from "next/link";
import { LessonTypeIcon } from "@/components/course/icons";
import { formatDuration } from "@/lib/format";
import type { Lesson, LessonProgressStatus, Section } from "@/lib/types";
import { cn } from "@/lib/utils";

/** Mục lục khóa học trên trang học: các phần, bài, trạng thái hoàn thành và bài đang mở. */
export function LearnSidebar({
  courseId,
  sections,
  currentLessonId,
  progress,
}: {
  courseId: number;
  sections: Section[];
  currentLessonId: number | null;
  progress: Map<number, LessonProgressStatus>;
}) {
  if (sections.length === 0) {
    return <p className="px-4 py-6 text-sm text-muted-foreground">Khóa học chưa có nội dung.</p>;
  }

  return (
    <nav aria-label="Nội dung khóa học" className="pb-4">
      {sections.map((s, i) => {
        const done = s.lessons.filter((l) => progress.get(l.id) === "COMPLETED").length;
        const complete = s.lessons.length > 0 && done === s.lessons.length;
        return (
          <div key={s.id} className="border-b last:border-b-0">
            <div className="flex items-start justify-between gap-3 px-4 pt-4 pb-2">
              <div className="min-w-0">
                <p className="text-eyebrow text-muted-foreground">Phần {i + 1}</p>
                <p className="mt-0.5 text-sm leading-snug font-semibold">{s.title}</p>
              </div>
              <span
                className={cn(
                  "mt-0.5 shrink-0 rounded-full px-2 py-0.5 text-xs font-medium tabular-nums",
                  complete ? "bg-achievement-soft text-achievement-strong" : "bg-muted text-muted-foreground",
                )}
                aria-label={`Đã xong ${done}/${s.lessons.length} bài`}
              >
                {done}/{s.lessons.length}
              </span>
            </div>
            <ul className="pb-2">
              {s.lessons.map((l) => (
                <li key={l.id}>
                  <SidebarLesson
                    href={`/learn/${courseId}?lesson=${l.id}`}
                    lesson={l}
                    status={progress.get(l.id) ?? null}
                    active={l.id === currentLessonId}
                  />
                </li>
              ))}
            </ul>
          </div>
        );
      })}
    </nav>
  );
}

function StatusIcon({ status, active }: { status: LessonProgressStatus | null; active: boolean }) {
  if (status === "COMPLETED") return <CircleCheckIcon className="size-4 text-achievement" aria-label="Đã hoàn thành" />;
  if (active) return <CirclePlayIcon className="size-4 text-primary" aria-label="Đang mở" />;
  if (status === "IN_PROGRESS") return <CircleDashedIcon className="size-4 text-muted-foreground" aria-label="Đang học" />;
  return <CircleIcon className="size-4 text-muted-foreground/50" aria-label="Chưa học" />;
}

function SidebarLesson({
  href,
  lesson,
  status,
  active,
}: {
  href: string;
  lesson: Lesson;
  status: LessonProgressStatus | null;
  active: boolean;
}) {
  return (
    <Link
      href={href}
      aria-current={active ? "page" : undefined}
      className={cn(
        "relative flex items-start gap-3 border-l-2 py-2.5 pr-4 pl-3.5 text-sm transition-colors outline-none focus-visible:bg-muted",
        active ? "border-primary bg-primary-soft text-primary-strong" : "border-transparent text-foreground/85 hover:bg-muted hover:text-foreground",
      )}
    >
      <span className="mt-0.5 shrink-0">
        <StatusIcon status={status} active={active} />
      </span>
      <span className="min-w-0 flex-1">
        <span className={cn("line-clamp-2 leading-snug", active && "font-medium")}>{lesson.title}</span>
        <span className={cn("mt-1 flex items-center gap-1.5 text-xs", active ? "text-primary-strong/75" : "text-muted-foreground")}>
          <LessonTypeIcon type={lesson.type} className="size-3.5" />
          {formatDuration(lesson.durationSeconds)}
        </span>
      </span>
    </Link>
  );
}
