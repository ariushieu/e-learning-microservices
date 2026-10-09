import { Card, CardContent } from "@/components/ui/card";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

export interface DailyPoint {
  date: string;
  value: number;
}

const shortDay = (iso: string) => {
  const [, m, d] = iso.split("-");
  return `${Number(d)}/${Number(m)}`;
};

/**
 * Biểu đồ cột theo ngày, vẽ bằng CSS (không cần thư viện biểu đồ). Kèm bảng ẩn cho trình đọc
 * màn hình; mỗi cột có title để rê chuột xem số.
 */
export function DailyBars({
  title,
  points,
  unit,
  tone = "primary",
}: {
  title: string;
  points: DailyPoint[];
  unit: string;
  tone?: "primary" | "info" | "achievement";
}) {
  const max = Math.max(1, ...points.map((p) => p.value));
  const total = points.reduce((sum, p) => sum + p.value, 0);
  // Nhãn trục ngày thưa ra khi nhiều ngày, để chữ không chồng lên nhau.
  const every = points.length > 31 ? 14 : points.length > 14 ? 7 : 1;
  const bar = { primary: "bg-primary", info: "bg-info", achievement: "bg-achievement" }[tone];

  return (
    <Card>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <h3 className="text-subheading">{title}</h3>
          <p className="text-sm text-muted-foreground">
            Tổng <span className="font-semibold text-foreground tabular-nums">{formatNumber(total)}</span> {unit}
          </p>
        </div>
        <div aria-hidden className="flex h-40 items-end gap-[2px] border-b border-border">
          {points.map((p) => (
            <div key={p.date} className="group relative flex h-full min-w-0 flex-1 items-end" title={`${shortDay(p.date)}: ${p.value} ${unit}`}>
              <div
                className={cn("w-full rounded-t-sm transition-opacity group-hover:opacity-80", p.value === 0 ? "h-px bg-border" : bar)}
                style={p.value === 0 ? undefined : { height: `${Math.max(4, (p.value / max) * 100)}%` }}
              />
            </div>
          ))}
        </div>
        <div aria-hidden className="flex gap-[2px] text-[10px] text-muted-foreground tabular-nums">
          {points.map((p, i) => (
            <span key={p.date} className="min-w-0 flex-1 text-center whitespace-nowrap">
              {(points.length - 1 - i) % every === 0 ? shortDay(p.date) : ""}
            </span>
          ))}
        </div>
        <table className="sr-only">
          <caption>{title}</caption>
          <thead>
            <tr>
              <th>Ngày</th>
              <th>{unit}</th>
            </tr>
          </thead>
          <tbody>
            {points.map((p) => (
              <tr key={p.date}>
                <td>{shortDay(p.date)}</td>
                <td>{p.value}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </CardContent>
    </Card>
  );
}
