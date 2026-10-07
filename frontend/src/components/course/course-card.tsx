import { ClockIcon, ListVideoIcon } from "lucide-react";
import Link from "next/link";
import { CourseCover } from "@/components/common/course-cover";
import { Badge } from "@/components/ui/badge";
import { formatDuration, formatPrice, label } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { CourseSummary } from "@/lib/types";

/** Học phí: miễn phí tô màu chính cho nổi, có phí để màu chữ thường. */
export function PriceTag({ price, className }: { price: number; className?: string }) {
  const free = Number(price ?? 0) === 0;
  return <span className={cn("font-semibold tabular-nums", free ? "text-primary" : "text-foreground", className)}>{formatPrice(price)}</span>;
}

export function CourseCard({ course }: { course: CourseSummary }) {
  return (
    <Link
      href={`/courses/${course.id}`}
      className="group flex flex-col overflow-hidden rounded-xl bg-card shadow-card ring-1 ring-border transition-[box-shadow,translate] duration-200 hover:-translate-y-0.5 hover:shadow-raised focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none motion-reduce:transition-none motion-reduce:hover:translate-y-0"
    >
      <CourseCover title={course.title} thumbnailUrl={course.thumbnailUrl} />
      <div className="flex flex-1 flex-col gap-1.5 p-4">
        <p className="truncate text-caption font-medium text-primary">{course.categoryName}</p>
        <h3 className="line-clamp-2 text-subheading transition-colors group-hover:text-primary">{course.title}</h3>
        <p className="truncate text-sm text-muted-foreground">{course.instructorName || "Giảng viên HUNRE"}</p>
        <div className="mt-auto flex flex-wrap items-center gap-x-3 gap-y-1.5 pt-3 text-xs text-muted-foreground">
          <span className="inline-flex items-center gap-1 tabular-nums">
            <ListVideoIcon className="size-3.5" aria-hidden />
            {course.totalLessons} bài
          </span>
          <span className="inline-flex items-center gap-1 tabular-nums">
            <ClockIcon className="size-3.5" aria-hidden />
            {formatDuration(course.totalDurationSeconds)}
          </span>
        </div>
        <div className="mt-3 flex items-center justify-between gap-3 border-t pt-3">
          <PriceTag price={course.price} />
          <Badge variant="secondary">{label(course.level)}</Badge>
        </div>
      </div>
    </Link>
  );
}
