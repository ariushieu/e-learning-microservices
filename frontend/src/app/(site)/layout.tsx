import { SiteFooter } from "@/components/layout/site-footer";
import { SiteHeader } from "@/components/layout/site-header";

/**
 * Lưới 3 cột: lề | nội dung (max-w-6xl trừ lề trong, thẳng hàng với header) | lề. Mọi phần tử con tự nằm ở cột giữa, riêng
 * <FullBleed> (dải hero, banner) trải hết chiều ngang mà trang không phải tự bọc container.
 */
export default function SiteLayout({ children }: LayoutProps<"/">) {
  return (
    <div className="flex min-h-svh flex-col">
      <SiteHeader />
      <main className="grid flex-1 grid-cols-[1fr_min(70rem,calc(100%-2rem))_1fr] content-start pt-10 pb-16 *:col-start-2 sm:grid-cols-[1fr_min(69rem,calc(100%-3rem))_1fr]">
        {children}
      </main>
      <SiteFooter />
    </div>
  );
}
