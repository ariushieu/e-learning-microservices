import { ChevronDownIcon } from "lucide-react";
import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

/**
 * Thẻ <select> gốc của trình duyệt, trông giống Input. Dùng cho form đọc bằng FormData
 * (và form GET chạy được khi tắt JavaScript), nơi Select của Radix không gửi giá trị.
 */
export function NativeSelect({ className, ...props }: ComponentProps<"select">) {
  return (
    <div className="relative">
      <select
        className={cn(
          "h-9 w-full min-w-0 appearance-none rounded-lg border border-input bg-card py-1 pr-8 pl-3 shadow-xs pointer-coarse:h-10 text-base transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:bg-input/50 disabled:opacity-50 md:text-sm",
          className,
        )}
        {...props}
      />
      <ChevronDownIcon className="pointer-events-none absolute top-1/2 right-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
    </div>
  );
}
