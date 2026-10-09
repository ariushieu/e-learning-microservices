import { MessageCircleQuestionIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { Pagination } from "@/components/common/pagination";
import { QuestionCard } from "@/components/course/lesson-qa";
import { attempt } from "@/components/course/queries";
import { ListPage } from "@/components/templates/list-page";
import { hasRole } from "@/lib/auth-shared";
import { formatNumber } from "@/lib/format";
import { gateway, getSession } from "@/lib/server/gateway";
import type { LessonQuestion, Page } from "@/lib/types";
import { cn } from "@/lib/utils";

export const metadata: Metadata = { title: "Hỏi đáp của học viên" };

const FILTERS = [
  { value: "false", label: "Chờ trả lời" },
  { value: "true", label: "Đã trả lời" },
  { value: "all", label: "Tất cả" },
] as const;

export default async function InstructorQuestionsPage({ searchParams }: PageProps<"/instructor/questions">) {
  const session = await getSession();
  if (!session) redirect("/login?next=/instructor/questions");
  if (!hasRole(session, "ROLE_INSTRUCTOR", "ROLE_ADMIN")) redirect("/khong-co-quyen");
  const query = await searchParams;
  const single = (value: string | string[] | undefined) => (Array.isArray(value) ? value[0] : value);
  const answered = FILTERS.find((f) => f.value === single(query.answered))?.value ?? "false";
  const rawPage = single(query.page) ?? "1";
  const page = /^\d{1,6}$/.test(rawPage) ? Math.max(0, Number(rawPage) - 1) : 0;
  const urlFor = (index: number) => `/instructor/questions?answered=${answered}&page=${index + 1}`;

  const params = new URLSearchParams({ page: String(page), size: "10" });
  if (answered !== "all") params.set("answered", answered);
  const result = await attempt(gateway<Page<LessonQuestion>>(`/api/instructor/questions?${params}`));

  return (
    <ListPage
      eyebrow="Kết nối với học viên"
      title="Hỏi đáp"
      description="Câu hỏi học viên đặt trong các bài học của bạn. Trả lời ở đây, học viên nhận thông báo ngay."
      toolbar={
        <nav aria-label="Lọc câu hỏi" className="inline-flex rounded-lg bg-muted p-1">
          {FILTERS.map((f) => (
            <Link
              key={f.value}
              href={`/instructor/questions?answered=${f.value}`}
              aria-current={f.value === answered ? "page" : undefined}
              className={cn(
                "rounded-md px-3 py-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground",
                f.value === answered && "bg-card text-foreground shadow-sm",
              )}
            >
              {f.label}
            </Link>
          ))}
        </nav>
      }
      pagination={
        result.data &&
        result.data.totalPages > 1 && <Pagination page={result.data.page} totalPages={result.data.totalPages} hrefFor={urlFor} />
      }
    >
      {result.error !== null ? (
        <ErrorAlert title="Không tải được câu hỏi" message={result.error} />
      ) : result.data.content.length === 0 ? (
        <EmptyState
          icon={MessageCircleQuestionIcon}
          title={answered === "false" ? "Không còn câu hỏi nào chờ trả lời" : "Chưa có câu hỏi nào"}
          description="Học viên đặt câu hỏi ở phần Hỏi đáp dưới mỗi bài học."
        />
      ) : (
        <div className="space-y-4">
          <p className="text-sm text-muted-foreground">
            <span className="font-medium text-foreground tabular-nums">{formatNumber(result.data.totalElements)}</span> câu hỏi
          </p>
          <ol className="space-y-4">
            {result.data.content.map((q) => (
              <li key={q.id}>
                <QuestionCard question={q} showContext />
              </li>
            ))}
          </ol>
        </div>
      )}
    </ListPage>
  );
}
