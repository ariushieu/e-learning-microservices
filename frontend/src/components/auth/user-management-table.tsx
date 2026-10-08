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
import { Card, CardContent } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from "@/components/ui/alert-dialog";
import { api } from "@/lib/client";
import { formatDate } from "@/lib/format";
import { formErrorMessage } from "@/lib/forms";
import type { User } from "@/lib/types";

const STATUS_LABELS = {
  ACTIVE: "Đang hoạt động",
  LOCKED: "Đã khóa",
  PENDING: "Chờ kích hoạt",
};

export function UserManagementTable({ users, callerId }: { users: User[]; callerId: number }) {
  const router = useRouter();
  const [editing, setEditing] = useState<User | null>(null);
  const [changing, setChanging] = useState<User | null>(null);
  const [statusOpen, setStatusOpen] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function changeStatus() {
    if (!changing || pending) return;
    setPending(true);
    setError(null);
    const status = changing.status === "LOCKED" ? "ACTIVE" : "LOCKED";
    try {
      await api<User>(`/api/users/${changing.id}/status`, {
        method: "PATCH",
        body: { status },
      });
      toast.success(
        `${status === "LOCKED" ? "Đã khóa" : "Đã mở khóa"} tài khoản ${changing.fullName}`,
      );
      setStatusOpen(false);
      router.refresh();
    } catch (cause) {
      setError(formErrorMessage(cause));
    } finally {
      setPending(false);
    }
  }

  function actions(user: User) {
    const protectedAccount = user.id === callerId || user.roles.includes("ROLE_ADMIN");
    return (
      <div className="flex flex-wrap gap-2 sm:justify-end">
        <Button variant="outline" size="sm" onClick={() => setEditing(user)}>
          Cấp quyền
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={protectedAccount && user.status !== "LOCKED"}
          title={protectedAccount ? "Không thể khóa chính mình hoặc quản trị viên" : undefined}
          onClick={() => {
            setError(null);
            setChanging(user);
            setStatusOpen(true);
          }}
        >
          {user.status === "LOCKED" ? "Mở khóa" : "Khóa"}
        </Button>
      </div>
    );
  }

  return (
    <>
      {users.length > 0 && (
        <ul aria-label="Danh sách người dùng" className="space-y-3 sm:hidden">
          {users.map((user) => (
            <li key={user.id}>
              <Card>
                <CardContent className="space-y-3">
                  <div className="space-y-1">
                    <p className="font-medium break-words">{user.fullName}</p>
                    <p className="text-sm break-all text-muted-foreground">{user.email}</p>
                    {user.phone && <p className="text-sm text-muted-foreground">{user.phone}</p>}
                    <p className="text-xs text-muted-foreground">
                      #{user.id} · {formatDate(user.createdAt)}
                    </p>
                  </div>
                  <RoleBadges roles={user.roles} />
                  <StatusBadge status={user.status}>{STATUS_LABELS[user.status]}</StatusBadge>
                  {actions(user)}
                </CardContent>
              </Card>
            </li>
          ))}
        </ul>
      )}
      <DataTableCard
        className="hidden sm:block"
        isEmpty={users.length === 0}
        empty={
          <EmptyState
            icon={UsersIcon}
            title="Không tìm thấy người dùng"
            description="Thử từ khóa khác hoặc bỏ bớt bộ lọc."
          />
        }
      >
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Người dùng</TableHead>
              <TableHead>Vai trò</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead>Ngày tạo</TableHead>
              <TableHead className="text-right">Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {users.map((user) => {
              return (
                <TableRow key={user.id}>
                  <TableCell className="min-w-56 max-w-72 whitespace-normal">
                    <p className="font-medium break-words">{user.fullName}</p>
                    <p className="text-xs break-all text-muted-foreground">{user.email}</p>
                    {user.phone && <p className="text-xs text-muted-foreground">{user.phone}</p>}
                    <span className="text-xs text-muted-foreground">#{user.id}</span>
                  </TableCell>
                  <TableCell>
                    <RoleBadges roles={user.roles} />
                  </TableCell>
                  <TableCell>
                    <StatusBadge status={user.status}>{STATUS_LABELS[user.status]}</StatusBadge>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {formatDate(user.createdAt)}
                  </TableCell>
                  <TableCell>{actions(user)}</TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </DataTableCard>
      <Dialog
        open={editing !== null}
        onOpenChange={(open) => {
          if (!open) setEditing(null);
        }}
      >
        <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>Cấp quyền người dùng</DialogTitle>
            <DialogDescription>
              Kiểm tra tài khoản và chọn các vai trò cần giữ lại.
            </DialogDescription>
          </DialogHeader>
          {editing && (
            <UserRolesForm
              key={editing.id}
              user={editing}
              onSaved={() => {
                setEditing(null);
                router.refresh();
              }}
            />
          )}
        </DialogContent>
      </Dialog>
      <AlertDialog
        open={statusOpen}
        onOpenChange={(open) => {
          if (!pending) setStatusOpen(open);
        }}
      >
        <AlertDialogContent aria-busy={pending}>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {changing?.status === "LOCKED" ? "Mở khóa" : "Khóa"} tài khoản {changing?.fullName}?
            </AlertDialogTitle>
            <AlertDialogDescription className="break-words">
              {changing?.email}.{" "}
              {changing?.status === "LOCKED"
                ? "Người dùng sẽ đăng nhập lại được. Các phiên đã thu hồi không được khôi phục."
                : "Người dùng không thể đăng nhập hoặc làm mới phiên. Phiên truy cập hiện tại có thể còn dùng được tối đa 15 phút."}
            </AlertDialogDescription>
          </AlertDialogHeader>
          {error && <ErrorAlert message={error} />}
          <AlertDialogFooter>
            <AlertDialogCancel disabled={pending}>Hủy</AlertDialogCancel>
            <AlertDialogAction
              disabled={pending}
              onClick={(event) => {
                event.preventDefault();
                void changeStatus();
              }}
            >
              {pending && <Loader2Icon className="animate-spin" aria-hidden />}
              Xác nhận
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
