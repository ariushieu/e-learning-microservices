import { Badge } from "@/components/ui/badge";
import { label } from "@/lib/format";
import { cn } from "@/lib/utils";

// Màu theo ý nghĩa: xanh lá = xong/đang mở, vàng = nháp/đang làm, xám = đã đóng, đỏ = hủy.
const TONES: Record<string, string> = {
  PUBLISHED: "bg-emerald-50 text-emerald-700 ring-emerald-600/20",
  COMPLETED: "bg-emerald-50 text-emerald-700 ring-emerald-600/20",
  SUBMITTED: "bg-emerald-50 text-emerald-700 ring-emerald-600/20",
  ACTIVE: "bg-blue-50 text-blue-700 ring-blue-600/20",
  DRAFT: "bg-amber-50 text-amber-800 ring-amber-600/20",
  PENDING_REVIEW: "bg-amber-50 text-amber-800 ring-amber-600/20",
  IN_PROGRESS: "bg-amber-50 text-amber-800 ring-amber-600/20",
  ARCHIVED: "bg-zinc-100 text-zinc-600 ring-zinc-500/20",
  EXPIRED: "bg-zinc-100 text-zinc-600 ring-zinc-500/20",
  CANCELLED: "bg-red-50 text-red-700 ring-red-600/20",
};

/** Huy hiệu trạng thái cho mọi enum (khóa học, ghi danh, bài kiểm tra, lượt làm). */
export function StatusBadge({ status, className }: { status: string; className?: string }) {
  return (
    <Badge
      variant="outline"
      className={cn("border-0 ring-1 ring-inset", TONES[status] ?? "bg-muted text-muted-foreground ring-border", className)}
    >
      {label(status)}
    </Badge>
  );
}
