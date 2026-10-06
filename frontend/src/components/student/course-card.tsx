import Link from "next/link";
import { Badge, formatDuration, formatPrice } from "@/components/ui";
import type { CourseSummary } from "@/lib/types";

const gradients = [
  "from-indigo-500 to-violet-600",
  "from-sky-500 to-indigo-600",
  "from-emerald-500 to-teal-600",
  "from-rose-500 to-orange-400",
  "from-amber-500 to-pink-500",
  "from-fuchsia-500 to-purple-700",
];

function initials(title: string) {
  return title
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0])
    .join("")
    .toUpperCase();
}

export function CourseThumb({
  course,
  aspect = "aspect-video",
}: {
  course: { id: number; title: string; thumbnailUrl: string | null };
  aspect?: string;
}) {
  if (course.thumbnailUrl) {
    return (
      // Ảnh bìa do giảng viên dán link từ bất kỳ đâu, next/image cần khai báo trước từng tên miền.
      // eslint-disable-next-line @next/next/no-img-element
      <img src={course.thumbnailUrl} alt={course.title} className={`${aspect} w-full object-cover`} />
    );
  }
  return (
    <div
      className={`grid ${aspect} w-full place-items-center bg-linear-to-br ${gradients[course.id % gradients.length]}`}
    >
      <span className="text-4xl font-bold tracking-wider text-white/90">{initials(course.title)}</span>
    </div>
  );
}

export function CourseCard({ course }: { course: CourseSummary }) {
  return (
    <Link
      href={`/courses/${course.id}`}
      className="group flex flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
    >
      <CourseThumb course={course} />
      <div className="flex flex-1 flex-col gap-2 p-4">
        <div className="flex items-center justify-between gap-2 text-xs">
          <span className="truncate font-medium text-indigo-600">{course.categoryName}</span>
          <Badge value={course.level} />
        </div>
        <h3 className="line-clamp-2 font-semibold text-slate-900 group-hover:text-indigo-700">{course.title}</h3>
        {course.summary && <p className="line-clamp-2 text-sm text-slate-500">{course.summary}</p>}
        <p className="text-xs text-slate-500">{course.instructorName || "Giảng viên HUNRE"}</p>
        <div className="mt-auto flex flex-wrap gap-x-3 gap-y-1 pt-2 text-xs text-slate-500">
          <span>{course.totalLessons} bài học</span>
          <span>{formatDuration(course.totalDurationSeconds)}</span>
          <span>{course.studentCount} học viên</span>
        </div>
        <div className="border-t border-slate-100 pt-3 font-semibold text-slate-900">{formatPrice(course.price)}</div>
      </div>
    </Link>
  );
}
