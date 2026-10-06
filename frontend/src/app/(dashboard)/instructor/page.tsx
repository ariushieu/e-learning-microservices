import { BookOpenIcon, ExternalLinkIcon, FilePenLineIcon, GlobeIcon, PlusIcon, SettingsIcon, UsersIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { CourseCover } from "@/components/common/course-cover";
import { DataTableCard } from "@/components/common/data-table-card";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { Stat } from "@/components/common/stat";
import { StatusBadge } from "@/components/common/status-badge";
import { DashboardPage } from "@/components/templates/dashboard-page";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { errorMessage } from "@/lib/errors";
import { formatDay, formatNumber, formatPrice } from "@/lib/format";
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
    const page = await gateway<Page<CourseSummary>>(`/api/courses?instructorId=${session.userId}&size=50&sort=createdAt,desc`);
    courses = page.content;
    total = page.totalElements;
  } catch (e) {
    loadError = errorMessage(e);
  }

  const published = courses.filter((c) => c.status === "PUBLISHED").length;
  const drafts = courses.filter((c) => c.status === "DRAFT" || c.status === "PENDING_REVIEW").length;
  const students = courses.reduce((n, c) => n + (c.studentCount ?? 0), 0);

  return (
    <DashboardPage
      title="Khóa học tôi dạy"
      description="Tạo, soạn nội dung và xuất bản khóa học của bạn."
      actions={
        <Button asChild size="lg">
          <Link href="/instructor/courses/new">
            <PlusIcon /> Tạo khóa học
          </Link>
        </Button>
      }
      stats={
        !loadError && (
          <>
            <Stat label="Tổng số khóa" value={formatNumber(total)} icon={BookOpenIcon} tone="primary" />
            <Stat label="Đã xuất bản" value={formatNumber(published)} icon={GlobeIcon} tone="success" />
            <Stat label="Bản nháp" value={formatNumber(drafts)} icon={FilePenLineIcon} tone="neutral" />
            <Stat label="Tổng học viên" value={formatNumber(students)} icon={UsersIcon} tone="info" />
          </>
        )
      }
    >
      {loadError ? (
        <ErrorAlert title="Không tải được danh sách khóa học" message={loadError} />
      ) : (
        <div className="space-y-3">
          <DataTableCard
            title="Danh sách khóa học"
            isEmpty={courses.length === 0}
            empty={
              <EmptyState
                icon={BookOpenIcon}
                title="Bạn chưa có khóa học nào"
                description="Tạo khóa học đầu tiên, soạn chương và bài học rồi xuất bản cho học viên."
                action={
                  <Button asChild variant="outline">
                    <Link href="/instructor/courses/new">
                      <PlusIcon /> Tạo khóa học đầu tiên
                    </Link>
                  </Button>
                }
              />
            }
          >
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Khóa học</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="hidden text-right md:table-cell">Bài học</TableHead>
                  <TableHead className="hidden text-right md:table-cell">Học viên</TableHead>
                  <TableHead className="hidden lg:table-cell">Xuất bản</TableHead>
                  <TableHead className="text-right">
                    <span className="sr-only">Thao tác</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {courses.map((c) => (
                  <TableRow key={c.id}>
                    <TableCell className="py-3">
                      <div className="flex max-w-96 min-w-56 items-center gap-3">
                        <CourseCover
                          title={c.title}
                          category={c.categoryName}
                          thumbnailUrl={c.thumbnailUrl}
                          size="sm"
                          className="hidden w-20 shrink-0 rounded-md sm:flex"
                        />
                        <div className="min-w-0">
                          <Link href={`/instructor/courses/${c.id}`} className="block truncate font-medium hover:text-primary">
                            {c.title}
                          </Link>
                          <p className="truncate text-xs text-muted-foreground">
                            {c.categoryName} · {formatPrice(c.price)}
                          </p>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <StatusBadge status={c.status} />
                    </TableCell>
                    <TableCell className="hidden text-right tabular-nums md:table-cell">{c.totalLessons}</TableCell>
                    <TableCell className="hidden text-right tabular-nums md:table-cell">{formatNumber(c.studentCount)}</TableCell>
                    <TableCell className="hidden text-muted-foreground lg:table-cell">{c.publishedAt ? formatDay(c.publishedAt) : "—"}</TableCell>
                    <TableCell>
                      <div className="flex justify-end gap-1">
                        <Button asChild variant="outline" size="sm">
                          <Link href={`/instructor/courses/${c.id}`}>
                            <SettingsIcon /> Quản lý
                          </Link>
                        </Button>
                        <Button asChild variant="ghost" size="sm">
                          <Link href={`/courses/${c.id}`}>
                            <ExternalLinkIcon /> Xem trang khóa
                          </Link>
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </DataTableCard>
          {total > courses.length && (
            <p className="text-sm text-muted-foreground">
              Đang hiển thị {courses.length} khóa mới nhất trên tổng {total}.
            </p>
          )}
        </div>
      )}
    </DashboardPage>
  );
}
