import { CircleCheckIcon, CircleDashedIcon, CircleIcon, CirclePlayIcon } from "lucide-react";
import Link from "next/link";
import { formatDuration } from "@/lib/format";
import type { Lesson, LessonProgressStatus, Section } from "@/lib/types";
import { cn } from "@/lib/utils";
import { LessonTypeIcon } from "./icons";

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
        return (
          <div key={s.id} className="border-b last:border-b-0">
            <div className="flex items-start justify-between gap-3 px-4 pt-4 pb-2">
              <div className="min-w-0">
                <p className="text-xs font-medium text-muted-foreground">Phần {i + 1}</p>
                <p className="text-sm font-semibold">{s.title}</p>
              </div>
              <span className="shrink-0 pt-0.5 text-xs text-muted-foreground tabular-nums">
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
  if (status === "COMPLETED") return <CircleCheckIcon className="size-4 text-emerald-600" aria-label="Đã hoàn thành" />;
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
        "relative flex items-start gap-3 px-4 py-2.5 text-sm transition-colors",
        active ? "bg-primary/5 text-foreground" : "text-foreground/80 hover:bg-muted/60 hover:text-foreground",
      )}
    >
      {active && <span className="absolute inset-y-0 left-0 w-0.5 bg-primary" aria-hidden />}
      <span className="mt-0.5 shrink-0">
        <StatusIcon status={status} active={active} />
      </span>
      <span className="min-w-0 flex-1">
        <span className={cn("block leading-snug", active && "font-medium")}>{lesson.title}</span>
        <span className="mt-1 flex items-center gap-1.5 text-xs text-muted-foreground">
          <LessonTypeIcon type={lesson.type} className="size-3.5" />
          {formatDuration(lesson.durationSeconds)}
        </span>
      </span>
    </Link>
  );
}
