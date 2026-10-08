import { BookOpenIcon, CheckCheckIcon, MessageSquareIcon, ArrowUpRightIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { FormField } from "@/components/common/form-field";
import { NativeSelect } from "@/components/common/native-select";
import { Pagination } from "@/components/common/pagination";
import { ReviewReply } from "@/components/course/review-reply";
import { RatingStars } from "@/components/course/rating-stars";
import { attempt } from "@/components/course/queries";
import type { InstructorReviews } from "@/components/course/review-types";
import { ListPage } from "@/components/templates/list-page";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { hasRole } from "@/lib/auth-shared";
import { formatDay, formatNumber } from "@/lib/format";
import { gateway, getSession } from "@/lib/server/gateway";

export const metadata: Metadata = { title: "Đánh giá từ học viên" };

export default async function InstructorReviewsPage({ searchParams }: PageProps<"/instructor/reviews">) {
  const session = await getSession();
  if (!session) redirect("/login?next=/instructor/reviews");
  if (!hasRole(session, "ROLE_INSTRUCTOR", "ROLE_ADMIN")) redirect("/khong-co-quyen");
  const query = await searchParams;
  const single = (value: string | string[] | undefined) => (Array.isArray(value) ? value[0] : value);
  const status = single(query.replied) ?? "false";
  const courseId = single(query.courseId) ?? "";
  const rawPage = single(query.page) ?? "1";
  const valid =
    ["false", "true", "all"].includes(status) &&
    (!courseId || /^\d{1,18}$/.test(courseId)) &&
    /^\d{1,6}$/.test(rawPage);
  const page = Math.max(0, Number(rawPage) - 1);
  const urlFor = (index: number) => {
    const params = new URLSearchParams({ replied: status, page: String(index + 1) });
    if (courseId) params.set("courseId", courseId);
    return `/instructor/reviews?${params}`;
  };
  const params = new URLSearchParams({ page: String(page), size: "10" });
  if (status !== "all") params.set("replied", status);
  if (courseId) params.set("courseId", courseId);
  const result = valid
    ? await attempt(gateway<InstructorReviews>(`/api/instructor/reviews?${params}`))
    : { data: null, error: "Bộ lọc hoặc số trang không hợp lệ." };
  if (result.data && page > 0 && page >= result.data.reviews.totalPages)
    redirect(urlFor(Math.max(0, result.data.reviews.totalPages - 1)));
  const data = result.data;

  return (
    <ListPage
      eyebrow="Kết nối với học viên"
      title="Đánh giá & phản hồi"
      description="Lắng nghe góp ý, giải đáp thắc mắc và đồng hành cùng người học."
      actions={
        <Button asChild variant="outline">
          <Link href="/instructor">
            <BookOpenIcon aria-hidden /> Khóa học tôi dạy
          </Link>
        </Button>
      }
    >
      {result.error ? (
        <ErrorAlert title="Không tải được đánh giá" message={result.error} />
      ) : (
        data && (
          <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-5 rounded-xl bg-primary-soft p-6 text-primary-strong">
              <div className="flex items-center gap-4">
                <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-card">
                  <MessageSquareIcon aria-hidden className="size-6" />
                </span>
                <div>
                  <p className="text-sm font-medium">Đánh giá chờ phản hồi{courseId ? " · Khóa đã chọn" : ""}</p>
                  <p className="mt-1 text-display tabular-nums" data-testid="inbox-unreplied-count">
                    {formatNumber(data.unrepliedCount)}
                  </p>
                </div>
              </div>
              <p className="max-w-sm text-sm leading-relaxed">
                Một phản hồi tận tâm giúp học viên thấy ý kiến của mình được lắng nghe.
              </p>
            </div>
            <form
              method="get"
              className="grid items-end gap-4 rounded-xl border bg-card p-5 shadow-card sm:grid-cols-[minmax(0,1fr)_minmax(0,1.5fr)_auto]"
              aria-label="Lọc đánh giá"
            >
              <FormField id="review-status" label="Trạng thái">
                <NativeSelect id="review-status" name="replied" defaultValue={status} key={status}>
                  <option value="false">Chưa trả lời</option>
                  <option value="true">Đã trả lời</option>
                  <option value="all">Tất cả</option>
                </NativeSelect>
              </FormField>
              <FormField id="review-course" label="Khóa học">
                <NativeSelect id="review-course" name="courseId" defaultValue={courseId} key={courseId}>
                  <option value="">Tất cả khóa học</option>
                  {courseId && !data.courses.some((c) => String(c.id) === courseId) && (
                    <option value={courseId}>Khóa #{courseId}</option>
                  )}
                  {data.courses.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.title}
                    </option>
                  ))}
                </NativeSelect>
              </FormField>
              <Button type="submit">Áp dụng</Button>
            </form>
            <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-muted-foreground">
              <p>
                {formatNumber(data.reviews.totalElements)} đánh giá
                {status === "false" ? " chưa trả lời" : status === "true" ? " đã trả lời" : ""}
              </p>
              <span>Mới nhất trước</span>
            </div>
            {data.reviews.content.length ? (
              <div className="space-y-4">
                {data.reviews.content.map(({ courseId: id, courseTitle, review }) => (
                  <article key={review.id} className="min-w-0 rounded-xl border bg-card p-5 shadow-card sm:p-6">
                    <div className="mb-5 flex flex-wrap items-center justify-between gap-3 border-b pb-4">
                      <Link
                        href={`/courses/${id}#reviews`}
                        className="inline-flex min-w-0 max-w-full items-center gap-2 text-sm font-medium text-primary hover:underline"
                      >
                        <BookOpenIcon aria-hidden className="size-4 shrink-0" />
                        <span className="break-words">{courseTitle}</span>
                        <ArrowUpRightIcon aria-hidden className="size-4 shrink-0" />
                      </Link>
                      <Badge variant="secondary">{review.reply ? "Đã trả lời" : "Chờ phản hồi"}</Badge>
                    </div>
                    <div className="flex flex-wrap items-start justify-between gap-4">
                      <div className="flex min-w-0 items-center gap-3">
                        <span
                          aria-hidden
                          className="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary-soft font-semibold text-primary-strong"
                        >
                          {review.authorName.slice(0, 1).toUpperCase()}
                        </span>
                        <div className="min-w-0">
                          <h2 className="break-words font-semibold">{review.authorName}</h2>
                          <time dateTime={review.createdAt} className="text-caption text-muted-foreground">
                            {formatDay(review.createdAt)}
                          </time>
                        </div>
                      </div>
                      <span className="text-primary">
                        <RatingStars rating={review.rating} />
                      </span>
                    </div>
                    {review.comment ? (
                      <p className="mt-4 whitespace-pre-wrap break-words text-sm leading-relaxed">{review.comment}</p>
                    ) : (
                      <p className="mt-4 text-sm text-muted-foreground">Học viên chỉ đánh giá số sao.</p>
                    )}
                    <ReviewReply
                      key={`${review.id}:${review.repliedAt ?? "empty"}`}
                      courseId={id}
                      review={review}
                      canReply
                    />
                  </article>
                ))}
              </div>
            ) : (
              <EmptyState
                icon={CheckCheckIcon}
                title={status === "false" ? "Đã phản hồi hết đánh giá" : "Chưa có đánh giá phù hợp"}
                description="Bạn có thể đổi bộ lọc hoặc quay lại khi có góp ý mới từ học viên."
              />
            )}
            <Pagination page={page} totalPages={data.reviews.totalPages} hrefFor={urlFor} />
          </div>
        )
      )}
    </ListPage>
  );
}
