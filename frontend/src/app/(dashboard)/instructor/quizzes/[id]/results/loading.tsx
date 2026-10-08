import { DashboardPage } from "@/components/templates/dashboard-page";
import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return <DashboardPage title="Kết quả bài kiểm tra"><div role="status" aria-label="Đang tải kết quả" className="space-y-6">
    <Skeleton className="h-32 w-full" /><Skeleton className="h-72 w-full" />
  </div></DashboardPage>;
}
