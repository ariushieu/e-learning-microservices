import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { StatCard } from "@/components/instructor/form-helpers";
import { Alert, Badge, Empty, LinkButton, PageTitle, formatDate, formatPrice } from "@/components/ui";
import { errorMessage } from "@/lib/errors";
import { gateway, getSession } from "@/lib/server/gateway";
import type { CourseSummary, Page } from "@/lib/types";

export const metadata: Metadata = { title: "Khóa học tôi dạy" };

export default async function InstructorPage() {
  const session = await getSession();
  if (!session) redirect("/login?next=/instructor");

  let courses: CourseSummary[] = [];
  let total = 0;
  let loadError: string | null = null;
  try {
    // instructorId = chính mình nên backend trả cả bản nháp và khóa đã lưu trữ.
    const page = await gateway<Page<CourseSummary>>(
      `/api/courses?instructorId=${session.userId}&size=50&sort=createdAt,desc`,
    );
    courses = page.content;
    total = page.totalElements;
  } catch (e) {
    loadError = errorMessage(e);
  }

  const published = courses.filter((c) => c.status === "PUBLISHED").length;
  const drafts = courses.filter((c) => c.status === "DRAFT" || c.status === "PENDING_REVIEW").length;
  const students = courses.reduce((n, c) => n + (c.studentCount ?? 0), 0);

  return (
    <div className="space-y-6">
      <PageTitle
        title="Khóa học tôi dạy"
        subtitle="Tạo, soạn nội dung và xuất bản khóa học của bạn."
        action={<LinkButton href="/instructor/courses/new">+ Tạo khóa học</LinkButton>}
      />

      {loadError ? (
        <Alert>Không tải được danh sách khóa học: {loadError}</Alert>
      ) : (
        <>
          <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
            <StatCard label="Tổng số khóa" value={total} />
            <StatCard label="Đã xuất bản" value={published} />
            <StatCard label="Bản nháp" value={drafts} />
            <StatCard label="Tổng học viên" value={students} />
          </div>
          {total > courses.length && (
            <p className="text-sm text-slate-500">Đang hiển thị {courses.length} khóa mới nhất trên tổng {total}.</p>
          )}

          {courses.length === 0 ? (
            <Empty>
              <p>Bạn chưa có khóa học nào.</p>
              <LinkButton href="/instructor/courses/new" className="mt-4">
                Tạo khóa học đầu tiên
              </LinkButton>
            </Empty>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
              <table className="w-full min-w-[760px] text-left text-sm">
                <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-4 py-3 font-medium">Khóa học</th>
                    <th className="px-4 py-3 font-medium">Trạng thái</th>
                    <th className="px-4 py-3 text-right font-medium">Bài học</th>
                    <th className="px-4 py-3 text-right font-medium">Học viên</th>
                    <th className="px-4 py-3 font-medium">Xuất bản lúc</th>
                    <th className="px-4 py-3 text-right font-medium">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {courses.map((c) => (
                    <tr key={c.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3">
                        <Link
                          href={`/instructor/courses/${c.id}`}
                          className="font-medium text-slate-900 hover:text-indigo-600"
                        >
                          {c.title}
                        </Link>
                        <div className="text-xs text-slate-500">
                          {c.categoryName} · {formatPrice(c.price)}
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <Badge value={c.status} />
                      </td>
                      <td className="px-4 py-3 text-right">{c.totalLessons}</td>
                      <td className="px-4 py-3 text-right">{c.studentCount}</td>
                      <td className="px-4 py-3 text-slate-500">{c.publishedAt ? formatDate(c.publishedAt) : "—"}</td>
                      <td className="px-4 py-3">
                        <div className="flex justify-end gap-2">
                          <LinkButton href={`/instructor/courses/${c.id}`} className="px-3 py-1.5">
                            Quản lý
                          </LinkButton>
                          <LinkButton href={`/courses/${c.id}`} variant="secondary" className="px-3 py-1.5">
                            Xem trang khóa
                          </LinkButton>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
}
