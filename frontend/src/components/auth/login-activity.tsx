"use client";

import { useState } from "react";
import { HistoryIcon, Loader2Icon, RefreshCwIcon } from "lucide-react";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { StatusBadge } from "@/components/common/status-badge";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { api } from "@/lib/client";
import { formatDate } from "@/lib/format";
import { formErrorMessage } from "@/lib/forms";
import type { Page } from "@/lib/types";

export interface LoginEvent {
  success: boolean;
  device: string;
  createdAt: string;
}

export function LoginActivity({ initial }: { initial: Page<LoginEvent> | null }) {
  const [events, setEvents] = useState(initial);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(initial === null ? "Không tải được hoạt động đăng nhập." : null);

  async function reload() {
    setLoading(true);
    setError(null);
    try {
      setEvents(await api<Page<LoginEvent>>("/api/auth/login-events?page=0&size=10"));
    } catch (err) {
      setError(formErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-4" aria-busy={loading}>
      <div className="flex justify-end">
        <Button variant="outline" size="sm" disabled={loading} onClick={reload}>
          {loading ? <Loader2Icon className="animate-spin" /> : <RefreshCwIcon />}
          {loading ? "Đang tải…" : error ? "Thử lại" : "Tải lại hoạt động"}
        </Button>
      </div>
      {error && <ErrorAlert message={error} />}
      {events?.content.length === 0 && (
        <EmptyState
          icon={HistoryIcon}
          title="Chưa có hoạt động đăng nhập"
          description="Các lần đăng nhập trong 90 ngày gần nhất sẽ xuất hiện tại đây."
        />
      )}
      {!!events?.content.length && (
        <Table className="table-fixed">
          <TableHeader>
            <TableRow>
              <TableHead>Thiết bị / thời gian</TableHead>
              <TableHead className="w-32 text-right">Kết quả</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {events.content.map((event, index) => (
              <TableRow key={`${event.createdAt}:${index}`}>
                <TableCell className="whitespace-normal">
                  <p className="font-medium">{event.device}</p>
                  <time dateTime={event.createdAt} className="text-caption text-muted-foreground">
                    {formatDate(event.createdAt)}
                  </time>
                </TableCell>
                <TableCell className="text-right">
                  <StatusBadge status={event.success ? "PUBLISHED" : "FAILED"}>
                    {event.success ? "Thành công" : "Sai mật khẩu"}
                  </StatusBadge>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
