import { Brand } from "./brand";

export function SiteFooter() {
  return (
    <footer className="border-t">
      <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-3 px-4 py-8 text-sm text-muted-foreground sm:flex-row sm:px-6">
        <Brand />
        <p>Trường Đại học Tài nguyên và Môi trường Hà Nội · Sản phẩm môn học kiến trúc microservices</p>
      </div>
    </footer>
  );
}
