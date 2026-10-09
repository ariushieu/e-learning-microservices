import { AwardIcon, BookCheckIcon, MedalIcon, TrophyIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { IconTile } from "@/components/common/icon-tile";
import { Section } from "@/components/common/section";
import { attempt } from "@/components/course/queries";
import { ListPage } from "@/components/templates/list-page";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { formatNumber, initials } from "@/lib/format";
import { gateway, getSession } from "@/lib/server/gateway";
import type { Leaderboard, LeaderboardEntry } from "@/lib/types";
import { cn } from "@/lib/utils";

export const metadata: Metadata = { title: "Bảng xếp hạng" };

const PERIODS = [
  { value: "week", label: "Tuần này" },
  { value: "all", label: "Toàn thời gian" },
] as const;

const SOURCE: Record<Leaderboard["source"], string> = {
  REDIS: "Đọc từ bộ nhớ đệm Redis",
  DATABASE: "Vừa tính lại từ cơ sở dữ liệu và lưu vào Redis",
  DATABASE_ONLY: "Tính từ cơ sở dữ liệu (Redis tạm thời không dùng được)",
};

export default async function LeaderboardPage({ searchParams }: PageProps<"/leaderboard">) {
  const session = await getSession();
  if (!session) redirect("/login?next=/leaderboard");
  const sp = await searchParams;
  const period = sp.period === "all" ? "all" : "week";
  const board = await attempt(gateway<Leaderboard>(`/api/leaderboard?period=${period}&limit=20`));

  return (
    <ListPage
      eyebrow="Thi đua học tập"
      title="Bảng xếp hạng"
      description="Mỗi bài học hoàn thành được 10 điểm, mỗi khóa học hoàn thành được thêm 100 điểm."
      toolbar={
        <nav aria-label="Khoảng thời gian" className="inline-flex rounded-lg bg-muted p-1">
          {PERIODS.map((p) => (
            <Link
              key={p.value}
              href={`/leaderboard?period=${p.value}`}
              aria-current={p.value === period ? "page" : undefined}
              className={cn(
                "rounded-md px-3 py-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground",
                p.value === period && "bg-card text-foreground shadow-sm",
              )}
            >
              {p.label}
            </Link>
          ))}
        </nav>
      }
    >
      {board.error !== null ? (
        <ErrorAlert title="Không tải được bảng xếp hạng" message={board.error} />
      ) : (
        <div className="space-y-8">
          <MyStanding board={board.data} period={period} />
          {board.data.entries.length === 0 ? (
            <EmptyState
              icon={TrophyIcon}
              title={period === "week" ? "Tuần này chưa ai có điểm" : "Chưa ai có điểm"}
              description="Hoàn thành một bài học để là người đầu tiên lên bảng."
            />
          ) : (
            <>
              <Podium entries={board.data.entries.slice(0, 3)} myId={session.userId} />
              {board.data.entries.length > 3 && (
                <Section title="Các vị trí tiếp theo">
                  <RankTable entries={board.data.entries.slice(3)} myId={session.userId} />
                </Section>
              )}
            </>
          )}
          <p className="text-caption text-muted-foreground">
            {formatNumber(board.data.totalLearners)} học viên có điểm · {SOURCE[board.data.source]}
          </p>
        </div>
      )}
    </ListPage>
  );
}

function MyStanding({ board, period }: { board: Leaderboard; period: string }) {
  const me = board.me;
  return (
    <Card className="border-primary/30 bg-primary-soft/40">
      <CardContent className="flex flex-wrap items-center gap-x-8 gap-y-4">
        <div className="flex items-center gap-4">
          <IconTile icon={me ? MedalIcon : BookCheckIcon} tone={me ? "achievement" : "primary"} />
          <div>
            <p className="text-caption text-muted-foreground">Vị trí của bạn</p>
            <p className="text-heading tabular-nums">
              {me ? `Hạng ${me.rank}` : "Chưa có điểm"}
              {me && <span className="text-sm font-normal text-muted-foreground"> / {formatNumber(board.totalLearners)}</span>}
            </p>
          </div>
        </div>
        {me ? (
          <dl className="flex flex-wrap gap-x-8 gap-y-2">
            <Stat label="Điểm" value={me.points} />
            <Stat label="Bài học xong" value={me.completedLessons} />
            <Stat label="Khóa học xong" value={me.completedCourses} />
          </dl>
        ) : (
          <p className="text-sm text-muted-foreground">
            {period === "week" ? "Tuần này bạn chưa hoàn thành bài học nào." : "Bạn chưa hoàn thành bài học nào."}{" "}
            <Link href="/my-courses" className="font-medium text-primary underline-offset-4 hover:underline">
              Học tiếp ngay
            </Link>
          </p>
        )}
      </CardContent>
    </Card>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex flex-col-reverse">
      <dt className="text-caption text-muted-foreground">{label}</dt>
      <dd className="text-lg font-semibold tabular-nums">{formatNumber(value)}</dd>
    </div>
  );
}

const PODIUM_STYLE = [
  { icon: TrophyIcon, label: "Hạng nhất", ring: "ring-achievement", tile: "achievement" as const },
  { icon: MedalIcon, label: "Hạng nhì", ring: "ring-info", tile: "info" as const },
  { icon: AwardIcon, label: "Hạng ba", ring: "ring-primary", tile: "primary" as const },
];

function Podium({ entries, myId }: { entries: LeaderboardEntry[]; myId: number }) {
  return (
    <ol className="grid gap-4 sm:grid-cols-3" aria-label="Ba vị trí dẫn đầu">
      {entries.map((e, i) => {
        const style = PODIUM_STYLE[i];
        return (
          <li key={e.userId}>
            <Card className={cn("h-full", e.userId === myId && "border-primary")}>
              <CardContent className="flex flex-col items-center gap-3 text-center">
                <IconTile icon={style.icon} tone={style.tile} />
                <Avatar className={cn("size-16 ring-2 ring-offset-2", style.ring)}>
                  <AvatarFallback className="bg-primary-soft text-lg font-semibold text-primary-strong">{initials(e.name)}</AvatarFallback>
                </Avatar>
                <div className="min-w-0 space-y-0.5">
                  <p className="text-caption text-muted-foreground">{style.label}</p>
                  <p className="font-semibold break-words">
                    {e.name}
                    {e.userId === myId && <span className="text-muted-foreground"> (bạn)</span>}
                  </p>
                </div>
                <p className="text-display text-2xl tabular-nums">
                  {formatNumber(e.points)} <span className="text-sm font-normal text-muted-foreground">điểm</span>
                </p>
                <p className="text-caption text-muted-foreground">
                  {formatNumber(e.completedLessons)} bài · {formatNumber(e.completedCourses)} khóa
                </p>
              </CardContent>
            </Card>
          </li>
        );
      })}
    </ol>
  );
}

function RankTable({ entries, myId }: { entries: LeaderboardEntry[]; myId: number }) {
  return (
    <Card className="py-0">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-16">Hạng</TableHead>
            <TableHead>Học viên</TableHead>
            <TableHead className="hidden text-right sm:table-cell">Bài học</TableHead>
            <TableHead className="hidden text-right sm:table-cell">Khóa học</TableHead>
            <TableHead className="text-right">Điểm</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {entries.map((e) => (
            <TableRow key={e.userId} className={cn(e.userId === myId && "bg-primary-soft/50")} aria-current={e.userId === myId ? "true" : undefined}>
              <TableCell className="font-medium tabular-nums">{e.rank}</TableCell>
              <TableCell className="max-w-56">
                <span className="flex items-center gap-3">
                  <Avatar className="size-8">
                    <AvatarFallback className="bg-muted text-xs">{initials(e.name)}</AvatarFallback>
                  </Avatar>
                  <span className="truncate font-medium">
                    {e.name}
                    {e.userId === myId && <span className="text-muted-foreground"> (bạn)</span>}
                  </span>
                </span>
              </TableCell>
              <TableCell className="hidden text-right tabular-nums sm:table-cell">{formatNumber(e.completedLessons)}</TableCell>
              <TableCell className="hidden text-right tabular-nums sm:table-cell">{formatNumber(e.completedCourses)}</TableCell>
              <TableCell className="text-right font-semibold tabular-nums">{formatNumber(e.points)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Card>
  );
}
