import Link from "next/link";
import { Empty, formatDuration } from "@/components/ui";
import type { Lesson, Section } from "@/lib/types";
import { ChevronIcon, LessonTypeIcon, lessonTypeLabels } from "./icons";
import { LessonContent } from "./lesson-content";

/** Đề cương ở trang chi tiết khóa học. Bài xem thử mở ngay tại chỗ, không cần ghi danh. */
export function CurriculumList({
  sections,
  courseId,
  canLearn,
}: {
  sections: Section[];
  courseId: number;
  canLearn: boolean;
}) {
  if (sections.length === 0) return <Empty>Khóa học chưa có nội dung.</Empty>;

  return (
    <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
      {sections.map((s, i) => (
        <details key={s.id} open={i === 0} className="group border-b border-slate-200 last:border-b-0">
          <summary className="flex cursor-pointer list-none [&::-webkit-details-marker]:hidden items-center gap-3 bg-slate-50 px-4 py-3 hover:bg-slate-100">
            <ChevronIcon className="h-4 w-4 shrink-0 -rotate-90 text-slate-400 transition group-open:rotate-0" />
            <span className="flex-1 font-medium text-slate-900">
              Chương {i + 1}: {s.title}
            </span>
            <span className="text-xs text-slate-500">
              {s.lessons.length} bài · {formatDuration(s.lessons.reduce((t, l) => t + l.durationSeconds, 0))}
            </span>
          </summary>
          <ul className="divide-y divide-slate-100">
            {s.lessons.map((l) => (
              <LessonRow key={l.id} lesson={l} courseId={courseId} canLearn={canLearn} />
            ))}
            {s.lessons.length === 0 && <li className="px-4 py-3 text-sm text-slate-400">Chưa có bài học.</li>}
          </ul>
        </details>
      ))}
    </div>
  );
}

function LessonRow({ lesson, courseId, canLearn }: { lesson: Lesson; courseId: number; canLearn: boolean }) {
  const meta = (
    <>
      <LessonTypeIcon type={lesson.type} className="h-4 w-4 shrink-0 text-slate-400" />
      <span className="flex-1 text-slate-700">{lesson.title}</span>
      {lesson.isPreview && (
        <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-700">Xem thử</span>
      )}
      <span className="hidden w-20 text-xs text-slate-500 sm:inline">{lessonTypeLabels[lesson.type]}</span>
      <span className="w-16 text-right text-xs text-slate-500">{formatDuration(lesson.durationSeconds)}</span>
    </>
  );

  if (canLearn) {
    return (
      <li>
        <Link href={`/learn/${courseId}?lesson=${lesson.id}`} className="flex items-center gap-3 px-4 py-3 text-sm hover:bg-indigo-50/60">
          {meta}
        </Link>
      </li>
    );
  }
  if (lesson.isPreview) {
    return (
      <li>
        <details className="group/preview">
          <summary className="flex cursor-pointer list-none [&::-webkit-details-marker]:hidden items-center gap-3 px-4 py-3 text-sm hover:bg-emerald-50/60">
            {meta}
          </summary>
          <div className="border-t border-slate-100 bg-slate-50/60 p-4">
            <LessonContent lesson={lesson} />
          </div>
        </details>
      </li>
    );
  }
  return <li className="flex items-center gap-3 px-4 py-3 text-sm">{meta}</li>;
}
