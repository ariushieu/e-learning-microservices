import { ActivityIcon, BookCheckIcon, GaugeIcon, TrophyIcon, UserPlusIcon, UsersIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { DataTableCard } from "@/components/common/data-table-card";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { ProgressMeter } from "@/components/common/progress-meter";
import { Section } from "@/components/common/section";
import { Stat, StatGrid } from "@/components/common/stat";
import { attempt } from "@/components/course/queries";
import { DailyBars } from "@/components/enrollment/daily-bars";
import { ListPage } from "@/components/templates/list-page";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { hasRole } from "@/lib/auth-shared";
import { formatNumber } from "@/lib/format";
import { gateway, getSession } from "@/lib/server/gateway";
import type { InstructorAnalytics } from "@/lib/types";
import { cn } from "@/lib/utils";

export const metadata: Metadata = { title: "Thống kê giảng dạy" };

const PERIODS = [7, 30, 90] as const;

const percent = (value: number) => `${formatNumber(value, 1)}%`;

export default async function InstructorAnalyticsPage({ searchParams }: PageProps<"/instructor/analytics">) {
  const session = await getSession();
  if (!session) redirect("/login?next=/instructor/analytics");
  if (!hasRole(session, "ROLE_INSTRUCTOR", "ROLE_ADMIN")) redirect("/khong-co-quyen");
  const query = await searchParams;
  const raw = Number(Array.isArray(query.days) ? query.days[0] : query.days);
  const days = PERIODS.find((p) => p === raw) ?? 30;
  const result = await attempt(gateway<InstructorAnalytics>(`/api/instructor/analytics?days=${days}`));

  return (
    <ListPage
      eyebrow="Giảng dạy"
      title="Thống kê"
      description={
        hasRole(session, "ROLE_ADMIN")
          ? "Ghi danh, tiến độ và hoàn thành trên mọi khóa học của hệ thống."
          : "Ghi danh, tiến độ và hoàn thành trên các khóa học bạn dạy."
      }
      toolbar={
        <nav aria-label="Khoảng thời gian" className="inline-flex rounded-lg bg-muted p-1">
          {PERIODS.map((p) => (
            <Link
              key={p}
              href={`/instructor/analytics?days=${p}`}
              aria-current={p === days ? "page" : undefined}
              className={cn(
                "rounded-md px-3 py-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground",
                p === days && "bg-card text-foreground shadow-sm",
              )}
            >
              {p} ngày
            </Link>
          ))}
        </nav>
      }
    >
      {result.error !== null ? (
        <ErrorAlert title="Không tải được thống kê" message={result.error} />
      ) : result.data.totals.courses === 0 ? (
        <EmptyState icon={ActivityIcon} title="Chưa có số liệu" description="Thống kê xuất hiện khi khóa học của bạn được xuất bản và có học viên ghi danh." />
      ) : (
        <div className="space-y-10">
          <StatGrid>
            <Stat label="Học viên đang theo học" value={formatNumber(result.data.totals.learners)} icon={UsersIcon} tone="info"
              hint={`${formatNumber(result.data.totals.enrollments)} lượt ghi danh trên ${formatNumber(result.data.totals.courses)} khóa`} />
            <Stat label={`Ghi danh mới (${days} ngày)`} value={formatNumber(result.data.totals.newEnrollments)} icon={UserPlusIcon} tone="primary" />
            <Stat label="Tỉ lệ hoàn thành" value={percent(result.data.totals.completionRate)} icon={TrophyIcon} tone="achievement"
              hint={`${formatNumber(result.data.totals.completed)} lượt đã hoàn thành`} />
            <Stat label="Tiến độ trung bình" value={percent(result.data.totals.averageProgress)} icon={GaugeIcon} tone="neutral" />
            <Stat label={`Học viên hoạt động (${days} ngày)`} value={formatNumber(result.data.totals.activeLearners)} icon={ActivityIcon} tone="info" />
            <Stat label={`Bài học hoàn thành (${days} ngày)`} value={formatNumber(result.data.totals.lessonsCompleted)} icon={BookCheckIcon} tone="primary" />
          </StatGrid>

          <div className="grid gap-6 lg:grid-cols-2">
            <DailyBars title="Ghi danh mới mỗi ngày" unit="lượt ghi danh" tone="primary"
              points={result.data.daily.map((d) => ({ date: d.date, value: d.enrollments }))} />
            <DailyBars title="Bài học hoàn thành mỗi ngày" unit="bài học" tone="info"
              points={result.data.daily.map((d) => ({ date: d.date, value: d.lessonsCompleted }))} />
          </div>

          <Section title="Theo từng khóa học" count={result.data.courses.length}>
            <DataTableCard isEmpty={false} empty={null}>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Khóa học</TableHead>
                    <TableHead className="text-right">Học viên</TableHead>
                    <TableHead className="hidden text-right sm:table-cell">Mới ({days} ngày)</TableHead>
                    <TableHead className="hidden text-right md:table-cell">Đã hủy</TableHead>
                    <TableHead className="w-48">Hoàn thành</TableHead>
                    <TableHead className="hidden text-right lg:table-cell">Tiến độ TB</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {result.data.courses.map((c) => (
                    <TableRow key={c.courseId}>
                      <TableCell className="max-w-64 py-3">
                        <Link href={`/instructor/courses/${c.courseId}?tab=hoc-vien`} className="line-clamp-2 font-medium hover:text-primary hover:underline">
                          {c.title}
                        </Link>
                      </TableCell>
                      <TableCell className="text-right tabular-nums">{formatNumber(c.enrollments)}</TableCell>
                      <TableCell className="hidden text-right tabular-nums sm:table-cell">{formatNumber(c.newEnrollments)}</TableCell>
                      <TableCell className="hidden text-right tabular-nums text-muted-foreground md:table-cell">{formatNumber(c.cancelled)}</TableCell>
                      <TableCell>
                        <ProgressMeter value={c.completionRate} label={null} detail={`${formatNumber(c.completed)}/${formatNumber(c.enrollments)} · ${percent(c.completionRate)}`} size="sm" />
                      </TableCell>
                      <TableCell className="hidden text-right tabular-nums lg:table-cell">{percent(c.averageProgress)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </DataTableCard>
          </Section>
        </div>
      )}
    </ListPage>
  );
}
