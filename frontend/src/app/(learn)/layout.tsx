/** Trang học chiếm cả màn hình (thanh tiêu đề riêng, không header/footer chung) — giống Udemy, Coursera. */
export default function LearnLayout({ children }: LayoutProps<"/">) {
  return <div className="min-h-svh bg-muted/30">{children}</div>;
}
