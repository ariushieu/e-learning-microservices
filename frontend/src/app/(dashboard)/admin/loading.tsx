import { Skeleton } from "@/components/ui/skeleton";
import { StatGrid } from "@/components/common/stat";

export default function Loading() {
  return (
    <div aria-label="Đang tải tổng quan" className="space-y-6">
      <Skeleton className="h-9 w-64" />
      <StatGrid>
        {Array.from({ length: 5 }, (_, index) => (
          <Skeleton key={index} className="h-32" />
        ))}
      </StatGrid>
      <Skeleton className="h-80 w-full" />
    </div>
  );
}
