"use client";

import { Loader2Icon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { Enrollment } from "@/lib/types";

export function CancelEnrollmentButton({ enrollmentId, courseTitle }: { enrollmentId: number; courseTitle: string }) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);

  async function cancel() {
    setLoading(true);
    try {
      await api<Enrollment>(`/api/enrollments/${enrollmentId}/status`, { method: "PATCH", body: { status: "CANCELLED" } });
      toast.success(`Đã hủy ghi danh khóa "${courseTitle}"`);
      router.refresh();
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        {/* Thao tác phụ, hiếm dùng: không đặt ngang hàng với nút học chính. Hộp xác nhận vẫn màu đỏ. */}
        <Button variant="ghost" size="sm" className="text-muted-foreground hover:bg-destructive/10 hover:text-destructive" disabled={loading}>
          {loading && <Loader2Icon className="animate-spin" />}
          Hủy ghi danh
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Hủy ghi danh khóa học?</AlertDialogTitle>
          <AlertDialogDescription>
            Bạn sẽ không vào học được khóa “{courseTitle}” nữa. Bạn có thể ghi danh lại sau.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Không</AlertDialogCancel>
          <AlertDialogAction variant="destructive" onClick={cancel}>
            Hủy ghi danh
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
