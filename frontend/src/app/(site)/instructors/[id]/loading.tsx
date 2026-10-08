import { Skeleton } from "@/components/ui/skeleton";

export default function LoadingInstructorProfile() {
  return (
    <div className="space-y-8" role="status" aria-label="Đang tải hồ sơ giảng viên">
      <Skeleton className="h-60 rounded-xl" />
      <div className="grid gap-4 sm:grid-cols-3">
        {[0, 1, 2].map((id) => (
          <Skeleton key={id} className="h-28 rounded-xl" />
        ))}
      </div>
      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {[0, 1, 2].map((id) => (
          <Skeleton key={id} className="h-80 rounded-xl" />
        ))}
      </div>
    </div>
  );
}
