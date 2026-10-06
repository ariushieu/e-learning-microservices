import { FullBleed } from "@/components/common/decor";
import { Skeleton } from "@/components/ui/skeleton";

/** Khung chờ cho trang giới thiệu bài kiểm tra và trang kết quả: dải hero + nội dung + cột phụ. */
export function QuizDetailSkeleton() {
  return (
    <>
      <FullBleed className="-mt-10 mb-10 bg-sidebar" inner="space-y-4 py-10 lg:py-14">
        <Skeleton className="h-4 w-48 bg-white/10" />
        <Skeleton className="h-3 w-24 bg-white/10" />
        <Skeleton className="h-10 w-2/3 bg-white/15" />
        <Skeleton className="h-4 w-96 max-w-full bg-white/10" />
      </FullBleed>
      <div className="grid items-start gap-8 lg:grid-cols-[minmax(0,1fr)_22rem] lg:gap-10">
        <div className="order-2 space-y-4 lg:order-1">
          <Skeleton className="h-6 w-48" />
          <Skeleton className="h-40 rounded-xl" />
          <Skeleton className="h-40 rounded-xl" />
        </div>
        <Skeleton className="order-1 h-72 rounded-xl lg:order-2" />
      </div>
    </>
  );
}
