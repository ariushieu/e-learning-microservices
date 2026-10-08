import { Skeleton } from "@/components/ui/skeleton";

export default function LoadingUsers() {
  return <div className="space-y-5" aria-label="Đang tải người dùng"><Skeleton className="h-9 w-64" /><Skeleton className="h-20 w-full" /><Skeleton className="h-80 w-full" /></div>;
}
