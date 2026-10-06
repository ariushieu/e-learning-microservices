import { TONE_CLASSES, type Tone } from "@/components/common/status-badge";
import { Badge } from "@/components/ui/badge";
import { label } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { Role } from "@/lib/types";

// Vai trò không phải trạng thái nên không nằm trong STATUS_TONES; tô màu riêng để phân biệt nhanh.
const ROLE_TONES: Record<Role, Tone> = {
  ROLE_STUDENT: "info",
  ROLE_INSTRUCTOR: "primary",
  ROLE_ADMIN: "warning",
};

/** Huy hiệu vai trò (Học viên, Giảng viên, Quản trị viên), cùng kiểu với StatusBadge. */
export function RoleBadge({ role }: { role: Role }) {
  const tone = ROLE_TONES[role] ?? "neutral";
  return (
    <Badge variant="outline" className={cn("gap-1.5 border-0 font-medium", TONE_CLASSES[tone].soft)}>
      <span className={cn("size-1.5 rounded-full", TONE_CLASSES[tone].dot)} aria-hidden />
      {label(role)}
    </Badge>
  );
}

export function RoleBadges({ roles, className }: { roles: Role[]; className?: string }) {
  return (
    <div className={cn("flex flex-wrap gap-1.5", className)}>
      {roles.map((r) => (
        <RoleBadge key={r} role={r} />
      ))}
    </div>
  );
}
