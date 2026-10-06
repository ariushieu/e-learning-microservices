"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert, Button } from "@/components/ui";
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
      <Button onClick={enroll} loading={loading} className="w-full py-3 text-base">
        {label}
      </Button>
      {error && <Alert>{error}</Alert>}
    </div>
  );
}
