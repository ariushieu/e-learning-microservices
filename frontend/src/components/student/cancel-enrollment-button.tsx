"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui";
import { api } from "@/lib/client";
import { errorMessage } from "@/lib/errors";
import type { Enrollment } from "@/lib/types";

export function CancelEnrollmentButton({ enrollmentId, courseTitle }: { enrollmentId: number; courseTitle: string }) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function cancel() {
    if (!confirm(`Hủy ghi danh khóa "${courseTitle}"? Bạn có thể ghi danh lại sau.`)) return;
    setLoading(true);
    setError(null);
    try {
      await api<Enrollment>(`/api/enrollments/${enrollmentId}/status`, { method: "PATCH", body: { status: "CANCELLED" } });
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <Button variant="ghost" onClick={cancel} loading={loading} className="text-rose-600 hover:bg-rose-50">
        Hủy ghi danh
      </Button>
      {error && <p className="w-full text-sm text-rose-600">{error}</p>}
    </>
  );
}
