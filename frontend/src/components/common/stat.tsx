import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { IconTile } from "./icon-tile";
import type { Tone } from "./status-badge";

/** Ô số liệu trên dashboard: nhãn, con số lớn, ghi chú, icon theo tông màu. */
export function Stat({ label, value, icon, hint, tone = "primary" }: { label: string; value: string | number; icon?: LucideIcon; hint?: string; tone?: Tone }) {
  return (
    <Card>
      <CardContent className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="text-sm text-muted-foreground">{label}</p>
          <p className="mt-1.5 text-2xl font-semibold tracking-tight tabular-nums">{value}</p>
          {hint && <p className="mt-1 text-xs text-muted-foreground">{hint}</p>}
        </div>
        {icon && <IconTile icon={icon} tone={tone} />}
      </CardContent>
    </Card>
  );
}

/** Hàng ô số liệu, tự xuống 2 cột trên điện thoại. */
export function StatGrid({ children }: { children: ReactNode }) {
  return <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">{children}</div>;
}
