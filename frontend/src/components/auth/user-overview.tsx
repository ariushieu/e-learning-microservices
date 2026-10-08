import { UsersIcon } from "lucide-react";
import { RoleBadges } from "@/components/auth/role-badge";
import { DataTableCard } from "@/components/common/data-table-card";
import { EmptyState } from "@/components/common/empty-state";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { formatDate } from "@/lib/format";
import type { Role, User } from "@/lib/types";

export interface UserStats {
  total: number;
  byRole: Record<Role, number>;
  byStatus: Record<User["status"], number>;
  newLast7Days: number;
}

export function RecentUsers({ users }: { users: User[] }) {
  return (
    <DataTableCard
      isEmpty={users.length === 0}
      empty={
        <EmptyState
          icon={UsersIcon}
          title="Chưa có tài khoản"
          description="Các tài khoản mới sẽ xuất hiện tại đây."
        />
      }
    >
      <Table className="table-fixed">
        <TableHeader>
          <TableRow>
            <TableHead>Người dùng</TableHead>
            <TableHead>Vai trò</TableHead>
            <TableHead className="hidden md:table-cell">Ngày tạo</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {users.map((user) => (
            <TableRow key={user.id}>
              <TableCell className="whitespace-normal">
                <p className="font-medium break-words">{user.fullName}</p>
                <p className="text-xs break-all text-muted-foreground">{user.email}</p>
                <p className="mt-1 text-xs text-muted-foreground md:hidden">
                  {formatDate(user.createdAt)}
                </p>
              </TableCell>
              <TableCell className="whitespace-normal">
                <RoleBadges roles={user.roles} />
              </TableCell>
              <TableCell className="hidden text-muted-foreground md:table-cell">
                {formatDate(user.createdAt)}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </DataTableCard>
  );
}
