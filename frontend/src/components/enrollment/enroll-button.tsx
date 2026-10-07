"use client";

import { Loader2Icon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { ErrorAlert } from "@/components/common/error-alert";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/client";
import { ApiError, errorMessage } from "@/lib/errors";
import type { Enrollment } from "@/lib/types";

export function EnrollButton({ courseId, label = "Ghi danh ngay" }: { courseId: number; label?: string }) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function enroll() {
    setLoading(true);
    setError(null);
    try {
      await api<Enrollment>("/api/enrollments", { method: "POST", body: { courseId } });
    } catch (e) {
      // 409: đã ghi danh từ trước (ví dụ ở tab khác) — cứ vào học.
      if (!(e instanceof ApiError && e.status === 409)) {
        setError(errorMessage(e));
        setLoading(false);
        return;
      }
    }
    router.push(`/learn/${courseId}`);
  }

  return (
    <div className="space-y-3">
      <Button size="lg" onClick={enroll} disabled={loading} className="h-10 w-full text-base">
        {loading && <Loader2Icon className="animate-spin" />}
        {label}
      </Button>
      {error && <ErrorAlert message={error} />}
    </div>
  );
}
