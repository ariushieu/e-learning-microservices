import { ArrowRightIcon, AwardIcon, PlayIcon } from "lucide-react";
import Link from "next/link";
import { CourseCover } from "@/components/common/course-cover";
import { ProgressMeter } from "@/components/common/progress-meter";
import { Section } from "@/components/common/section";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import type { Enrollment } from "@/lib/types";

/**
 * Đầu trang chủ của người đã đăng nhập: các khóa đang học gần nhất để vào học tiếp ngay,
 * và số chứng chỉ đã có. Chưa ghi danh khóa nào thì không hiện gì.
 */
export function ContinueLearning({ enrollments, fullName, covers }: { enrollments: Enrollment[]; fullName: string; covers?: Map<number, string | null> }) {
  const active = enrollments
    .filter((e) => e.status === "ACTIVE")
    .sort((a, b) => (b.lastAccessedAt ?? b.enrolledAt).localeCompare(a.lastAccessedAt ?? a.enrolledAt))
    .slice(0, 3);
  const completed = enrollments.filter((e) => e.status === "COMPLETED").length;
  if (active.length === 0 && completed === 0) return null;

  return (
    <Section
      className="mb-10"
      title={`Chào ${fullName.trim().split(/\s+/).pop()}, học tiếp nhé`}
      description={
        completed > 0 ? (
          <span className="inline-flex items-center gap-1.5">
            <AwardIcon className="size-4 text-achievement" aria-hidden />
            Bạn đã hoàn thành {completed} khóa học.
          </span>
        ) : (
          "Các khóa bạn đang học gần đây."
        )
      }
      actions={
        <Button asChild variant="outline">
          <Link href="/my-courses">
            Khóa học của tôi <ArrowRightIcon />
          </Link>
        </Button>
      }
    >
      {active.length > 0 && (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {active.map((e) => (
            <Card key={e.id} className="flex-row items-center gap-4 p-3 transition-shadow hover:shadow-raised">
              <Link href={`/learn/${e.courseId}`} tabIndex={-1} aria-hidden className="w-24 shrink-0 overflow-hidden rounded-lg">
                <CourseCover title={e.courseTitle} thumbnailUrl={covers?.get(e.courseId)} showLabel={false} size="sm" />
              </Link>
              <div className="min-w-0 flex-1 space-y-2">
                <Link href={`/learn/${e.courseId}`} className="line-clamp-2 text-sm font-semibold underline-offset-4 hover:text-primary hover:underline">
                  {e.courseTitle}
                </Link>
                <ProgressMeter value={e.progressPercent} label={null} size="sm" />
                <Button asChild size="sm" className="h-8">
                  <Link href={`/learn/${e.courseId}`}>
                    <PlayIcon /> {Number(e.progressPercent) > 0 ? "Học tiếp" : "Bắt đầu học"}
                  </Link>
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}
    </Section>
  );
}
