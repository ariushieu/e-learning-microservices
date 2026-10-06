import { FullBleed } from "@/components/common/decor";
import { Skeleton } from "@/components/ui/skeleton";

// Khung chờ theo đúng bố cục trang chi tiết (dải hero + cột phụ) để trang không nhảy khi tải xong.
export default function Loading() {
  return (
    <>
      <FullBleed className="-mt-10 mb-10 bg-sidebar" inner="space-y-4 py-10 lg:py-14">
        <Skeleton className="h-4 w-56 bg-white/10" />
        <Skeleton className="h-10 w-full max-w-xl bg-white/10" />
        <Skeleton className="h-5 w-full max-w-2xl bg-white/10" />
        <Skeleton className="h-4 w-80 max-w-full bg-white/10" />
      </FullBleed>
      <div className="grid items-start gap-8 lg:grid-cols-[minmax(0,1fr)_22rem] lg:gap-10">
        <div className="order-2 space-y-4 lg:order-1">
          <Skeleton className="h-6 w-48" />
          {Array.from({ length: 3 }, (_, i) => (
            <Skeleton key={i} className="h-28 rounded-xl" />
          ))}
        </div>
        <Skeleton className="order-1 h-72 rounded-xl lg:order-2" />
      </div>
    </>
  );
}
