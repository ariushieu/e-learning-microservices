"use client";

import { RefreshCwIcon, UsersIcon } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { DataTableCard } from "@/components/common/data-table-card";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { NativeSelect } from "@/components/common/native-select";
import { Pagination } from "@/components/common/pagination";
import { ProgressMeter } from "@/components/common/progress-meter";
import { Section } from "@/components/common/section";
import { StatusBadge } from "@/components/common/status-badge";
import { CourseLearningSummary } from "@/components/enrollment/course-learning-summary";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { api } from "@/lib/client";
import { ApiError, errorMessage } from "@/lib/errors";
import { formatDay, formatNumber } from "@/lib/format";
import type { EnrollmentStatus, Page, Section as CourseSection } from "@/lib/types";

interface CourseLearner {
  enrollmentId: number;
  userId: number;
  learnerName: string | null;
  status: EnrollmentStatus;
  progressPercent: number;
  enrolledAt: string;
  lastAccessedAt: string | null;
  completedAt: string | null;
  certificateCode: string | null;
}

const SORT_OPTIONS = [
  ["enrolledAt,desc", "Ghi danh mới nhất"],
  ["enrolledAt,asc", "Ghi danh cũ nhất"],
  ["progressPercent,desc", "Tiến độ cao nhất"],
  ["progressPercent,asc", "Tiến độ thấp nhất"],
] as const;

type Result = { key: string } & (
  | { data: Page<CourseLearner>; error?: never; unavailable?: never }
  | { data?: never; error: string; unavailable: boolean }
);

export function CourseLearners({ courseId, sections }: { courseId: number; sections: CourseSection[] | null }) {
  const searchParams = useSearchParams();
  const pathname = usePathname();
  const router = useRouter();
  const rawStatus = searchParams.get("learnerStatus") ?? "";
  const status = ["ACTIVE", "COMPLETED", "CANCELLED"].includes(rawStatus) ? rawStatus : "";
  const rawSort = searchParams.get("learnerSort");
  const sort = SORT_OPTIONS.find(([value]) => value === rawSort)?.[0] ?? "enrolledAt,desc";
  const rawPage = Number(searchParams.get("learnerPage") ?? 1);
  const page = Number.isSafeInteger(rawPage) && rawPage > 0 && rawPage <= 2147483647 ? rawPage - 1 : 0;
  const [revision, setRevision] = useState(0);
  const [result, setResult] = useState<Result | null>(null);
  const query = new URLSearchParams({ page: String(page), size: "10", sort });
  if (status) query.set("status", status);
  const path = `/api/courses/${courseId}/learners?${query}`;
  const key = `${path}:${revision}`;
  const current = result?.key === key ? result : null;

  useEffect(() => {
    let active = true;
    api<Page<CourseLearner>>(path).then(
      (data) => { if (active) setResult({ key, data }); },
      (error) => { if (active) setResult({ key, error: errorMessage(error), unavailable: error instanceof ApiError && error.status === 404 }); },
    );
    // Never show an older response after the filter, course or page has changed.
    return () => { active = false; };
  }, [path, key]);

  function hrefFor(nextPage: number, changes?: { learnerStatus?: string; learnerSort?: string }) {
    const params = new URLSearchParams(searchParams.toString());
    params.set("learnerPage", String(nextPage + 1));
    for (const [name, value] of Object.entries(changes ?? {})) {
      if (value) params.set(name, value); else params.delete(name);
    }
    return `${pathname}?${params}#hoc-vien`;
  }

  return (
    <Section
      id="hoc-vien"
      title="Học viên của khóa"
      description="Theo dõi tiến độ và ngày hoàn thành của từng học viên."
      count={current?.data?.totalElements}
      actions={<Button variant="outline" disabled={!current} onClick={() => setRevision((value) => value + 1)}><RefreshCwIcon /> Làm mới</Button>}
    >
      <CourseLearningSummary courseId={courseId} sections={sections} revision={revision} />
      <div className="grid gap-3 sm:flex sm:flex-wrap">
        <label className="space-y-1 text-sm sm:min-w-44">
          <span>Trạng thái học</span>
          <NativeSelect value={status} onChange={(event) => router.replace(hrefFor(0, { learnerStatus: event.target.value }), { scroll: false })}>
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">Đang học</option>
            <option value="COMPLETED">Hoàn thành</option>
            <option value="CANCELLED">Đã hủy</option>
          </NativeSelect>
        </label>
        <label className="space-y-1 text-sm sm:min-w-48">
          <span>Sắp xếp học viên</span>
          <NativeSelect value={sort} onChange={(event) => router.replace(hrefFor(0, { learnerSort: event.target.value }), { scroll: false })}>
            {SORT_OPTIONS.map(([value, title]) => <option key={value} value={value}>{title}</option>)}
          </NativeSelect>
        </label>
      </div>

      {!current ? (
        <div role="status" aria-label="Đang tải học viên" className="space-y-3"><Skeleton className="h-12 w-full" /><Skeleton className="h-24 w-full" /><span className="sr-only">Đang tải học viên…</span></div>
      ) : current.unavailable ? (
        <EmptyState icon={UsersIcon} title="Danh sách học viên chưa sẵn sàng" description="Hãy thử làm mới sau khi khóa học được xuất bản." />
      ) : current.error !== undefined ? (
        <ErrorAlert title="Không tải được học viên" message={current.error} />
      ) : (
        <>
          <DataTableCard
            isEmpty={current.data.content.length === 0}
            empty={<EmptyState icon={UsersIcon} title={page > 0 ? "Trang này chưa có học viên" : status ? "Chưa có học viên ở trạng thái này" : "Khóa học chưa có học viên"}
              description="Danh sách sẽ cập nhật khi có người ghi danh khóa học."
              action={page > 0 ? <Button asChild variant="outline"><Link href={hrefFor(0)}>Về trang đầu</Link></Button> : undefined} />}
          >
            <Table className="table-fixed">
              <TableHeader>
                <TableRow>
                  <TableHead className="w-1/2 lg:w-1/4">Học viên</TableHead>
                  <TableHead className="hidden lg:table-cell">Trạng thái</TableHead>
                  <TableHead>Tiến độ</TableHead>
                  <TableHead className="hidden lg:table-cell">Ghi danh</TableHead>
                  <TableHead className="hidden lg:table-cell">Hoàn thành</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {current.data.content.map((learner) => (
                  <TableRow key={learner.enrollmentId}>
                    <TableCell className="whitespace-normal py-4 align-top">
                      <span className="block break-words font-medium">{learner.learnerName || `Học viên #${learner.userId}`}</span>
                      <div className="mt-2 lg:hidden"><StatusBadge status={learner.status} /></div>
                    </TableCell>
                    <TableCell className="hidden lg:table-cell"><StatusBadge status={learner.status} /></TableCell>
                    <TableCell className="whitespace-normal py-4">
                      <ProgressMeter value={learner.progressPercent} size="sm" />
                      <dl className="mt-3 space-y-1 text-caption text-muted-foreground lg:hidden">
                        <div><dt>Ghi danh</dt><dd>{formatDay(learner.enrolledAt)}</dd></div>
                        <div><dt>Hoàn thành</dt><dd>{formatDay(learner.completedAt) || "—"}</dd></div>
                      </dl>
                    </TableCell>
                    <TableCell className="hidden whitespace-normal text-muted-foreground lg:table-cell">{formatDay(learner.enrolledAt)}</TableCell>
                    <TableCell className="hidden whitespace-normal text-muted-foreground lg:table-cell">{formatDay(learner.completedAt) || "—"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </DataTableCard>
          {current.data.totalElements > 0 && <p className="text-sm text-muted-foreground" role="status">{formatNumber(current.data.totalElements)} học viên{current.data.totalPages > 1 && ` · Trang ${formatNumber(page + 1)} / ${formatNumber(current.data.totalPages)}`}</p>}
          <Pagination page={page} totalPages={current.data.totalPages} hrefFor={hrefFor} />
        </>
      )}
    </Section>
  );
}
