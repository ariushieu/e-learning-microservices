import { VerificationShell } from "@/components/enrollment/verification-shell";
import { Skeleton } from "@/components/ui/skeleton";

export default function VerificationLoading() {
  return (
    <VerificationShell>
      <div role="status" aria-label="Đang xác minh chứng chỉ" className="space-y-6">
        <Skeleton className="h-8 w-60 max-w-full" />
        <div className="grid gap-6 rounded-xl border bg-card p-6 sm:grid-cols-2">
          {Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-16" />)}
        </div>
        <span className="sr-only">Đang xác minh chứng chỉ…</span>
      </div>
    </VerificationShell>
  );
}

