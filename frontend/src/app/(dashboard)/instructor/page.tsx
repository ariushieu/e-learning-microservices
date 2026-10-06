import { BookOpenIcon, ExternalLinkIcon, FilePenLineIcon, GlobeIcon, PlusIcon, SettingsIcon, UsersIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { PageHeader } from "@/components/common/page-header";
import { Stat } from "@/components/common/stat";
import { StatusBadge } from "@/components/common/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
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
    <div className="space-y-6">
      <PageHeader
        title="Khóa học tôi dạy"
        description="Tạo, soạn nội dung và xuất bản khóa học của bạn."
        actions={
          <Button asChild size="lg">
            <Link href="/instructor/courses/new">
              <PlusIcon /> Tạo khóa học
            </Link>
          </Button>
        }
      />

      {loadError ? (
        <ErrorAlert title="Không tải được danh sách khóa học" message={loadError} />
      ) : (
        <>
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <Stat label="Tổng số khóa" value={formatNumber(total)} icon={BookOpenIcon} />
            <Stat label="Đã xuất bản" value={formatNumber(published)} icon={GlobeIcon} />
            <Stat label="Bản nháp" value={formatNumber(drafts)} icon={FilePenLineIcon} />
            <Stat label="Tổng học viên" value={formatNumber(students)} icon={UsersIcon} />
          </div>

          {courses.length === 0 ? (
            <EmptyState
              icon={BookOpenIcon}
              title="Bạn chưa có khóa học nào"
              description="Tạo khóa học đầu tiên, soạn chương và bài học rồi xuất bản cho học viên."
              action={
                <Button asChild>
                  <Link href="/instructor/courses/new">
                    <PlusIcon /> Tạo khóa học đầu tiên
                  </Link>
                </Button>
              }
            />
          ) : (
            <Card className="gap-0 py-0">
              <Table>
                <TableHeader>
                  <TableRow className="hover:bg-transparent">
                    <TableHead className="pl-4">Khóa học</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead className="text-right">Bài học</TableHead>
                    <TableHead className="text-right">Học viên</TableHead>
                    <TableHead>Xuất bản</TableHead>
                    <TableHead className="pr-4 text-right">
                      <span className="sr-only">Thao tác</span>
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {courses.map((c) => (
                    <TableRow key={c.id}>
                      <TableCell className="max-w-80 py-3 pl-4">
                        <Link href={`/instructor/courses/${c.id}`} className="block truncate font-medium hover:text-primary">
                          {c.title}
                        </Link>
                        <p className="truncate text-xs text-muted-foreground">
                          {c.categoryName} · {formatPrice(c.price)}
                        </p>
                      </TableCell>
                      <TableCell>
                        <StatusBadge status={c.status} />
                      </TableCell>
                      <TableCell className="text-right tabular-nums">{c.totalLessons}</TableCell>
                      <TableCell className="text-right tabular-nums">{formatNumber(c.studentCount)}</TableCell>
                      <TableCell className="text-muted-foreground">{c.publishedAt ? formatDay(c.publishedAt) : "—"}</TableCell>
                      <TableCell className="pr-4">
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
            </Card>
          )}
          {total > courses.length && (
            <p className="text-sm text-muted-foreground">
              Đang hiển thị {courses.length} khóa mới nhất trên tổng {total}.
            </p>
          )}
        </>
      )}
    </div>
  );
}
