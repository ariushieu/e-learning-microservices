import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { TONE_CLASSES, type Tone } from "./status-badge";

const SIZES = { sm: "size-8 rounded-lg [&_svg]:size-4", md: "size-10 rounded-xl [&_svg]:size-5", lg: "size-12 rounded-xl [&_svg]:size-6" };

/** Ô vuông chứa icon, nền theo tông màu. Dùng trong Stat, EmptyState, dòng danh sách. */
export function IconTile({ icon: Icon, tone = "primary", size = "md", className }: { icon: LucideIcon; tone?: Tone; size?: keyof typeof SIZES; className?: string }) {
  return (
    <span className={cn("flex shrink-0 items-center justify-center", SIZES[size], TONE_CLASSES[tone].soft, className)} aria-hidden>
      <Icon />
    </span>
  );
}
