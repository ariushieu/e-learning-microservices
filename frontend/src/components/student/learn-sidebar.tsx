import Link from "next/link";
import { formatDuration } from "@/components/ui";
import type { LessonProgressStatus, Section } from "@/lib/types";
import { CheckIcon, LessonTypeIcon } from "./icons";

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
  return (
    <nav className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm lg:sticky lg:top-24 lg:max-h-[calc(100vh-7rem)] lg:overflow-y-auto">
      <div className="border-b border-slate-200 px-4 py-3 text-sm font-semibold text-slate-900">Nội dung khóa học</div>
      {sections.map((s, i) => {
        const done = s.lessons.filter((l) => progress.get(l.id) === "COMPLETED").length;
        return (
          <div key={s.id} className="border-b border-slate-100 last:border-b-0">
            <div className="flex items-baseline justify-between gap-2 bg-slate-50 px-4 py-2.5">
              <span className="text-sm font-medium text-slate-800">
                {i + 1}. {s.title}
              </span>
              <span className="shrink-0 text-xs text-slate-500">
                {done}/{s.lessons.length}
              </span>
            </div>
            <ul>
              {s.lessons.map((l) => (
                <li key={l.id}>
                  <SidebarLesson
                    href={`/learn/${courseId}?lesson=${l.id}`}
                    title={l.title}
                    type={l.type}
                    duration={l.durationSeconds}
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

function SidebarLesson({
  href,
  title,
  type,
  duration,
  status,
  active,
}: {
  href: string;
  title: string;
  type: Section["lessons"][number]["type"];
  duration: number;
  status: LessonProgressStatus | null;
  active: boolean;
}) {
  return (
    <Link
      href={href}
      aria-current={active ? "page" : undefined}
      className={`flex items-start gap-3 px-4 py-2.5 text-sm transition ${
        active ? "bg-indigo-50 text-indigo-800" : "text-slate-700 hover:bg-slate-50"
      }`}
    >
      <span
        className={`mt-0.5 grid h-5 w-5 shrink-0 place-items-center rounded-full border ${
          status === "COMPLETED"
            ? "border-emerald-500 bg-emerald-500 text-white"
            : status === "IN_PROGRESS"
              ? "border-indigo-400 bg-white"
              : "border-slate-300 bg-white"
        }`}
        title={status === "COMPLETED" ? "Đã hoàn thành" : status === "IN_PROGRESS" ? "Đang học" : "Chưa học"}
      >
        {status === "COMPLETED" && <CheckIcon className="h-3 w-3" />}
      </span>
      <span className="flex-1">
        <span className={active ? "font-medium" : undefined}>{title}</span>
        <span className="mt-0.5 flex items-center gap-1.5 text-xs text-slate-500">
          <LessonTypeIcon type={type} className="h-3.5 w-3.5" />
          {formatDuration(duration)}
        </span>
      </span>
    </Link>
  );
}
