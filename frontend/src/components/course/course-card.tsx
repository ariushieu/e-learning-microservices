import { BookOpenIcon, ClockIcon, ListVideoIcon } from "lucide-react";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { formatDuration, formatPrice, label } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { CourseSummary } from "@/lib/types";

/** Ảnh bìa khóa học; chưa có ảnh thì hiện nền dịu kèm biểu tượng sách. */
export function CourseThumb({
  course,
  aspect = "aspect-video",
  className,
}: {
  course: { id: number; title: string; thumbnailUrl: string | null };
  aspect?: string;
  className?: string;
}) {
  if (course.thumbnailUrl) {
    return (
      // Ảnh bìa do giảng viên dán link từ bất kỳ đâu, next/image cần khai báo trước từng tên miền.
      // eslint-disable-next-line @next/next/no-img-element
      <img src={course.thumbnailUrl} alt={course.title} className={cn(aspect, "w-full bg-muted object-cover", className)} />
    );
  }
  return (
    <div className={cn(aspect, "flex w-full items-center justify-center bg-muted", className)} aria-hidden>
      <BookOpenIcon className="size-8 text-muted-foreground/60" />
    </div>
  );
}

export function PriceTag({ price, className }: { price: number; className?: string }) {
  const free = Number(price ?? 0) === 0;
  return (
    <span className={cn("font-semibold tabular-nums", free ? "text-emerald-600" : "text-foreground", className)}>
      {formatPrice(price)}
    </span>
  );
}

export function CourseCard({ course }: { course: CourseSummary }) {
  return (
    <Link
      href={`/courses/${course.id}`}
      className="group flex flex-col overflow-hidden rounded-xl bg-card ring-1 ring-foreground/10 transition-shadow hover:shadow-md focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <CourseThumb course={course} />
      <div className="flex flex-1 flex-col gap-1.5 p-4">
        <p className="truncate text-xs text-muted-foreground">{course.categoryName}</p>
        <h3 className="line-clamp-2 leading-snug font-semibold group-hover:text-primary">{course.title}</h3>
        <p className="truncate text-sm text-muted-foreground">{course.instructorName || "Giảng viên HUNRE"}</p>
        <div className="mt-auto flex flex-wrap items-center gap-x-3 gap-y-1.5 pt-2 text-xs text-muted-foreground">
          <span className="inline-flex items-center gap-1">
            <ListVideoIcon className="size-3.5" />
            {course.totalLessons} bài
          </span>
          <span className="inline-flex items-center gap-1">
            <ClockIcon className="size-3.5" />
            {formatDuration(course.totalDurationSeconds)}
          </span>
          <Badge variant="secondary">{label(course.level)}</Badge>
        </div>
        <div className="mt-2 border-t pt-3">
          <PriceTag price={course.price} />
        </div>
      </div>
    </Link>
  );
}
