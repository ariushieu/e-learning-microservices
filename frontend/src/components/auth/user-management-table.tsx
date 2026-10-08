"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2Icon, UsersIcon } from "lucide-react";
import { toast } from "sonner";
import { RoleBadges } from "@/components/auth/role-badge";
import { UserRolesForm } from "@/components/auth/user-roles-form";
import { DataTableCard } from "@/components/common/data-table-card";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { StatusBadge } from "@/components/common/status-badge";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { AlertDialog, AlertDialogContent, AlertDialogHeader, AlertDialogTitle, AlertDialogDescription, AlertDialogFooter, AlertDialogCancel, AlertDialogAction } from "@/components/ui/alert-dialog";
import { api } from "@/lib/client";
import { formatDate } from "@/lib/format";
import { formErrorMessage } from "@/lib/forms";
import type { User } from "@/lib/types";

const STATUS_LABELS = { ACTIVE: "Đang hoạt động", LOCKED: "Đã khóa", PENDING: "Chờ kích hoạt" };

export function UserManagementTable({ users, callerId }: { users: User[]; callerId: number }) {
  const router = useRouter();
  const [editing, setEditing] = useState<User | null>(null);
  const [changing, setChanging] = useState<User | null>(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function changeStatus() {
    if (!changing || pending) return;
    setPending(true);
    setError(null);
    const status = changing.status === "LOCKED" ? "ACTIVE" : "LOCKED";
    try {
      await api<User>(`/api/users/${changing.id}/status`, { method: "PATCH", body: { status } });
      toast.success(`${status === "LOCKED" ? "Đã khóa" : "Đã mở khóa"} tài khoản ${changing.fullName}`);
      setChanging(null);
      router.refresh();
    } catch (cause) { setError(formErrorMessage(cause)); }
    finally { setPending(false); }
  }

  return <>
    <DataTableCard isEmpty={users.length === 0} empty={<EmptyState icon={UsersIcon} title="Không tìm thấy người dùng" description="Thử từ khóa khác hoặc bỏ bớt bộ lọc." />}>
      <Table>
        <TableHeader><TableRow><TableHead>Người dùng</TableHead><TableHead>Vai trò</TableHead><TableHead>Trạng thái</TableHead><TableHead>Ngày tạo</TableHead><TableHead className="text-right">Thao tác</TableHead></TableRow></TableHeader>
        <TableBody>{users.map(user => {
          const protectedAccount = user.id === callerId || user.roles.includes("ROLE_ADMIN");
          return <TableRow key={user.id}>
            <TableCell className="min-w-56 max-w-72 whitespace-normal">
              <p className="font-medium break-words">{user.fullName}</p>
              <p className="text-xs break-all text-muted-foreground">{user.email}</p>
              {user.phone && <p className="text-xs text-muted-foreground">{user.phone}</p>}
              <span className="text-xs text-muted-foreground">#{user.id}</span>
            </TableCell>
            <TableCell><RoleBadges roles={user.roles} /></TableCell>
            <TableCell><StatusBadge status={user.status}>{STATUS_LABELS[user.status]}</StatusBadge></TableCell>
            <TableCell className="text-muted-foreground">{formatDate(user.createdAt)}</TableCell>
            <TableCell><div className="flex justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => setEditing(user)}>Cấp quyền</Button>
              <Button variant="outline" size="sm" disabled={protectedAccount && user.status !== "LOCKED"}
                title={protectedAccount ? "Không thể khóa chính mình hoặc quản trị viên" : undefined}
                onClick={() => { setError(null); setChanging(user); }}>{user.status === "LOCKED" ? "Mở khóa" : "Khóa"}</Button>
            </div></TableCell>
          </TableRow>;
        })}</TableBody>
      </Table>
    </DataTableCard>
    <Dialog open={editing !== null} onOpenChange={open => { if (!open) setEditing(null); }}>
      <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-xl">
        <DialogHeader><DialogTitle>Cấp quyền người dùng</DialogTitle><DialogDescription>Kiểm tra tài khoản và chọn các vai trò cần giữ lại.</DialogDescription></DialogHeader>
        {editing && <UserRolesForm key={editing.id} user={editing} onSaved={() => { setEditing(null); router.refresh(); }} />}
      </DialogContent>
    </Dialog>
    <AlertDialog open={changing !== null} onOpenChange={open => { if (!open && !pending) setChanging(null); }}>
      <AlertDialogContent aria-busy={pending}>
        <AlertDialogHeader>
          <AlertDialogTitle>{changing?.status === "LOCKED" ? "Mở khóa" : "Khóa"} tài khoản {changing?.fullName}?</AlertDialogTitle>
          <AlertDialogDescription className="break-words">{changing?.email}. {changing?.status === "LOCKED" ? "Người dùng sẽ đăng nhập lại được. Các phiên đã thu hồi không được khôi phục." : "Người dùng không thể đăng nhập hoặc làm mới phiên. Phiên truy cập hiện tại có thể còn dùng được tối đa 15 phút."}</AlertDialogDescription>
        </AlertDialogHeader>
        {error && <ErrorAlert message={error} />}
        <AlertDialogFooter>
          <AlertDialogCancel disabled={pending}>Hủy</AlertDialogCancel>
          <AlertDialogAction disabled={pending} onClick={event => { event.preventDefault(); void changeStatus(); }}>
            {pending && <Loader2Icon className="animate-spin" aria-hidden />}Xác nhận
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </>;
}
