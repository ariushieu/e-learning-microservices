"use client";

import { BookOpenIcon, TrendingDownIcon } from "lucide-react";
import { useEffect, useState } from "react";
import { DataTableCard } from "@/components/common/data-table-card";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { ProgressMeter } from "@/components/common/progress-meter";
import { Stat, StatGrid } from "@/components/common/stat";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { api } from "@/lib/client";
import { ApiError, errorMessage } from "@/lib/errors";
import { formatNumber } from "@/lib/format";
import type { Section } from "@/lib/types";
import { DROP_THRESHOLD, lessonCompletionRows, type LearningSummary } from "./course-learning-summary-model";

type Result = { key: string } & (
  | { data: LearningSummary; error?: never; unavailable?: never }
  | { data?: never; error: string; unavailable: boolean }
);

const percent = (value: number) => `${formatNumber(value, 2)}%`;

export function CourseLearningSummary({ courseId, sections, revision }: {
  courseId: number;
  sections: Section[] | null;
  revision: number;
}) {
  const [result, setResult] = useState<Result | null>(null);
  const path = `/api/courses/${courseId}/learners/summary`;
  const key = `${path}:${revision}`;
  const current = result?.key === key ? result : null;

  useEffect(() => {
    let active = true;
    api<LearningSummary>(path).then(
      (data) => { if (active) setResult({ key, data }); },
      (error) => { if (active) setResult({ key, error: errorMessage(error), unavailable: error instanceof ApiError && error.status === 404 }); },
    );
    return () => { active = false; };
  }, [path, key]);

  if (!current) {
    return <div role="status" aria-label="Đang tải số liệu học tập" className="space-y-4">
      <StatGrid>{[0, 1, 2, 3].map((id) => <Skeleton key={id} className="h-28 w-full" />)}</StatGrid>
      <Skeleton className="h-36 w-full" /><span className="sr-only">Đang tải số liệu học tập…</span>
    </div>;
  }
  if (current.unavailable) {
    return <EmptyState icon={BookOpenIcon} title="Số liệu học tập chưa sẵn sàng" description="Hãy thử làm mới sau khi khóa học được xuất bản." />;
  }
  if (current.error !== undefined) {
    return <ErrorAlert title="Không tải được số liệu học tập" message={current.error} />;
  }

  const summary = current.data;
  const rows = lessonCompletionRows(sections ?? [], summary.lessons);

  return <div className="space-y-4" aria-label="Số liệu học tập toàn khóa">
    <StatGrid>
      <Stat label="Đang học" value={formatNumber(summary.active)} />
      <Stat label="Đã hoàn thành" value={formatNumber(summary.completed)} />
      <Stat label="Tiến độ trung bình" value={percent(summary.averageProgress)} />
      <Stat label="Tỉ lệ hoàn thành" value={percent(summary.completionRate)} />
    </StatGrid>
    <p className="text-sm text-muted-foreground">
      Số liệu toàn khóa, không đổi theo bộ lọc học viên. Tiến độ và tỉ lệ không tính lượt đã hủy.
      {" "}{formatNumber(summary.cancelled)} lượt đã hủy · {formatNumber(summary.certificatesIssued)} chứng chỉ đã cấp.
    </p>
    <div className="space-y-3">
      <h3 className="text-subheading">Tỉ lệ hoàn thành từng bài</h3>
      <p className="text-sm text-muted-foreground">
        Đánh dấu bài có tỉ lệ giảm từ {DROP_THRESHOLD} điểm phần trăm so với bài trước trong đề cương.
        Học viên có thể vẫn đang học các bài này.
      </p>
      {sections === null ? (
        <ErrorAlert title="Không tải được đề cương" message="Tải lại trang để xem tên và thứ tự bài học. Số liệu toàn khóa vẫn được hiển thị ở trên." />
      ) : (
        <DataTableCard isEmpty={rows.length === 0}
          empty={<EmptyState icon={BookOpenIcon} title="Khóa học chưa có bài học" description="Thêm bài vào đề cương để theo dõi tỉ lệ hoàn thành." />}>
          <Table className="table-fixed">
            <TableHeader><TableRow>
              <TableHead className="w-3/5">Bài học</TableHead>
              <TableHead>Hoàn thành</TableHead>
            </TableRow></TableHeader>
            <TableBody>{rows.map((lesson, index) => (
              <TableRow key={lesson.id} className={lesson.highlighted ? "bg-warning-soft hover:bg-warning-soft" : undefined}>
                <TableCell className="whitespace-normal py-4 align-top">
                  <span className="block break-words font-medium">{index + 1}. {lesson.title}</span>
                  <span className="mt-1 block break-words text-xs text-muted-foreground">{lesson.sectionTitle}</span>
                  {lesson.highlighted && <p className="mt-2 flex items-start gap-1.5 text-xs text-warning-strong">
                    <TrendingDownIcon className="mt-0.5 size-4 shrink-0" aria-hidden />
                    <span>Giảm {formatNumber(lesson.drop, 2)} điểm % so với bài trước</span>
                  </p>}
                </TableCell>
                <TableCell className="whitespace-normal py-4">
                  <ProgressMeter value={lesson.completionRate} label={null} detail={percent(lesson.completionRate)} size="sm" />
                  <p className="mt-2 text-xs text-muted-foreground">
                    {formatNumber(lesson.completedCount)} / {formatNumber(summary.active + summary.completed)} học viên
                  </p>
                </TableCell>
              </TableRow>
            ))}</TableBody>
          </Table>
        </DataTableCard>
      )}
    </div>
  </div>;
}
