"use client";

import { useState } from "react";
import { Loader2Icon, MonitorIcon, RefreshCwIcon } from "lucide-react";
import { toast } from "sonner";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent,
  AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { api } from "@/lib/client";
import { formatDate } from "@/lib/format";
import { formErrorMessage } from "@/lib/forms";
import { logoutToLoginAction } from "@/lib/server/auth-actions";

export interface LoginSession {
  id: number;
  createdAt: string;
  device: string;
  current: boolean;
}

export function SessionList({ initial }: { initial: LoginSession[] | null }) {
  const [sessions, setSessions] = useState(initial);
  const [error, setError] = useState(initial === null ? "Không tải được các phiên đăng nhập." : null);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<LoginSession | "others" | null>(null);
  const [open, setOpen] = useState(false);
  const [pending, setPending] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const current = sessions?.find((session) => session.current);

  async function reload() {
    setLoading(true);
    setError(null);
    try {
      setSessions(await api<LoginSession[]>("/api/auth/sessions"));
    } catch (cause) {
      setError(formErrorMessage(cause));
    } finally {
      setLoading(false);
    }
  }

  function confirm(target: LoginSession | "others") {
    setSelected(target);
    setActionError(null);
    setOpen(true);
  }

  async function revoke() {
    if (!selected || pending) return;
    setPending(true);
    setActionError(null);
    try {
      await api<void>(selected === "others"
        ? "/api/auth/sessions/revoke-others"
        : `/api/auth/sessions/${selected.id}`, {
        method: selected === "others" ? "POST" : "DELETE",
      });
      if (selected !== "others" && selected.current) {
        await logoutToLoginAction();
        return;
      }
      toast.success(selected === "others" ? "Đã đăng xuất mọi thiết bị khác" : "Đã đăng xuất phiên đăng nhập");
      setOpen(false);
      await reload();
    } catch (cause) {
      setActionError(formErrorMessage(cause));
      // A concurrent token rotation may have replaced the selected session.
      await reload();
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="space-y-5" aria-label="Quản lý phiên đăng nhập">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-muted-foreground">Các phiên còn hiệu lực, mới nhất trước.</p>
        <Button variant="outline" size="sm" onClick={reload} disabled={loading || pending}>
          <RefreshCwIcon className={loading ? "animate-spin" : undefined} aria-hidden />
          {loading ? "Đang tải…" : "Tải lại phiên"}
        </Button>
      </div>
      {error && <ErrorAlert message={error} />}
      {sessions?.length === 0 && (
        <EmptyState icon={MonitorIcon} title="Không còn phiên đăng nhập đang hoạt động"
          description="Bạn có thể đăng nhập lại để tạo phiên mới." />
      )}
      {!!sessions?.length && (
        <ul className="divide-y" aria-label="Danh sách phiên đăng nhập">
          {sessions.map((session) => (
            <li key={session.id} className="flex flex-col gap-3 py-4 first:pt-0 sm:flex-row sm:items-center sm:justify-between">
              <div className="min-w-0 space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-medium">{session.device}</span>
                  {session.current && <Badge variant="secondary">Thiết bị này</Badge>}
                </div>
                <p className="text-sm text-muted-foreground">Bắt đầu: {formatDate(session.createdAt)}</p>
              </div>
              <Button variant="outline" className="shrink-0" disabled={pending || loading}
                aria-label={`Đăng xuất ${session.current ? "thiết bị này" : session.device}`}
                onClick={() => confirm(session)}>
                Đăng xuất
              </Button>
            </li>
          ))}
        </ul>
      )}
      <div className="space-y-3 border-t pt-4">
        <p className="text-sm text-muted-foreground">
          Sau khi đăng xuất từ xa, thiết bị không thể làm mới phiên. Quyền truy cập đã cấp có thể còn dùng được tối đa 15 phút.
        </p>
        {!!sessions?.length && !current && (
          <p className="text-sm text-muted-foreground">Chưa xác định được phiên hiện tại. Tải lại phiên hoặc đăng nhập lại để tiếp tục.</p>
        )}
        <Button variant="outline" className="h-auto min-h-9 whitespace-normal text-left"
          disabled={pending || loading || !current || !sessions?.some((session) => !session.current)}
          onClick={() => confirm("others")}>
          Đăng xuất mọi thiết bị khác
        </Button>
      </div>
      <AlertDialog open={open} onOpenChange={(value) => { if (!pending) setOpen(value); }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {selected === "others" ? "Đăng xuất mọi thiết bị khác?"
                : selected?.current ? "Đăng xuất thiết bị này?" : `Đăng xuất ${selected?.device ?? "phiên đăng nhập"}?`}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {selected === "others" ? "Bạn vẫn đăng nhập trên thiết bị này. Các thiết bị khác sẽ cần đăng nhập lại."
                : selected?.current ? "Bạn sẽ được đưa về trang đăng nhập."
                  : "Thiết bị này sẽ không thể làm mới phiên và cần đăng nhập lại sau khi quyền truy cập hiện tại hết hạn."}
            </AlertDialogDescription>
          </AlertDialogHeader>
          {actionError && <ErrorAlert message={actionError} />}
          <AlertDialogFooter>
            <AlertDialogCancel disabled={pending}>Hủy</AlertDialogCancel>
            <AlertDialogAction disabled={pending} onClick={(event) => { event.preventDefault(); void revoke(); }}>
              {pending && <Loader2Icon className="animate-spin" aria-hidden />}
              {pending ? "Đang đăng xuất…" : "Xác nhận"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
