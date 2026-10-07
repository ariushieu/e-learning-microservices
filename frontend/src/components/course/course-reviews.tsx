import { MessageSquareIcon, StarIcon } from "lucide-react";
import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { Pagination } from "@/components/common/pagination";
import { Section } from "@/components/common/section";
import { Button } from "@/components/ui/button";
import { formatDay, formatNumber } from "@/lib/format";
import { gateway } from "@/lib/server/gateway";
import type { Course, Page } from "@/lib/types";
import { attempt } from "./queries";
import { ReviewForm } from "./review-form";
import type { CourseReview, MyCourseReview } from "./review-types";

export async function CourseReviews({ course, loggedIn, page }: { course: Course; loggedIn: boolean; page: number }) {
  const [result, mine] = await Promise.all([
    attempt(gateway<Page<CourseReview>>(`/api/courses/${course.id}/reviews?page=${page}&size=5`)),
    loggedIn ? attempt(gateway<MyCourseReview>(`/api/courses/${course.id}/reviews/me`)) : Promise.resolve(null),
  ]);
  return (
    <div id="reviews" className="scroll-mt-24">
      <Section title="Đánh giá từ học viên">
        <div className="space-y-6">
          <div className="flex flex-wrap items-center gap-5 rounded-xl bg-primary-soft p-6 text-primary-strong">
            <span className="text-display tabular-nums">{course.ratingCount ? formatNumber(course.ratingAvg, 1) : "—"}<span className="text-lg font-normal"> / 5</span></span>
            <div><div className="flex gap-1" aria-hidden>{[1,2,3,4,5].map(n => <StarIcon key={n} className={`size-5 ${n <= Math.round(course.ratingAvg) && course.ratingCount ? "fill-current" : "opacity-30"}`} />)}</div>
              <p className="mt-2 text-sm">{formatNumber(course.ratingCount)} đánh giá · Từ người đã ghi danh</p></div>
          </div>
          {mine?.error && <ErrorAlert title="Không kiểm tra được quyền đánh giá" message={mine.error} />}
          {mine?.data?.canReview ? <ReviewForm key={mine.data.review?.updatedAt ?? "new"} courseId={course.id} review={mine.data.review} /> : !mine?.error && (
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border p-4 text-sm text-muted-foreground">
              <p>{course.status !== "PUBLISHED" && course.status !== "ARCHIVED" ? "Đánh giá sẽ mở khi khóa học được xuất bản." : loggedIn ? "Bạn cần ghi danh khóa học để chia sẻ đánh giá. Nếu vừa ghi danh, hãy tải lại sau vài giây." : "Đăng nhập và ghi danh để chia sẻ trải nghiệm của bạn."}</p>
              {!loggedIn && <Button asChild variant="outline"><Link href={`/login?next=${encodeURIComponent(`/courses/${course.id}#reviews`)}`}>Đăng nhập</Link></Button>}
            </div>
          )}
          {result.error !== null ? <ErrorAlert title="Không tải được nhận xét" message={result.error} /> : <>
            {result.data.content.length ? <div className="divide-y rounded-xl border bg-card px-5 sm:px-6">
              {result.data.content.map(review => <article key={review.id} className="py-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="flex min-w-0 items-center gap-3"><span aria-hidden className="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary-soft font-semibold text-primary-strong">{review.authorName.slice(0,1).toUpperCase()}</span><div className="min-w-0"><h3 className="break-words font-medium">{review.authorName}</h3><time dateTime={review.createdAt} className="text-caption text-muted-foreground">{formatDay(review.createdAt)}</time></div></div>
                  <span className="inline-flex items-center gap-1 text-sm font-semibold text-primary" aria-label={`${review.rating} trên 5 sao`}><StarIcon aria-hidden className="size-4 fill-current" />{review.rating}/5</span>
                </div>
                {review.comment && <p className="mt-3 whitespace-pre-wrap break-words text-sm leading-relaxed">{review.comment}</p>}
              </article>)}
            </div> : <EmptyState icon={MessageSquareIcon} title={page ? "Trang này chưa có đánh giá" : "Chưa có đánh giá nào"} description="Một chia sẻ chân thành có thể giúp ích cho rất nhiều người học." action={page > 0 ? <Button asChild variant="outline"><Link href={`/courses/${course.id}#reviews`}>Về trang đánh giá đầu</Link></Button> : undefined} />}
            <Pagination page={page} totalPages={result.data.totalPages} hrefFor={p => `/courses/${course.id}?reviewPage=${p + 1}#reviews`} />
          </>}
        </div>
      </Section>
    </div>
  );
}
