import { AwardIcon, ClipboardCheckIcon, LineChartIcon } from "lucide-react";
import { ContourPattern } from "@/components/common/decor";
import { Brand } from "@/components/layout/brand";

const POINTS = [
  { icon: LineChartIcon, title: "Học theo lộ trình", text: "Lưu tiến độ từng bài, học tiếp đúng chỗ đã dừng." },
  { icon: ClipboardCheckIcon, title: "Kiểm tra tự chấm", text: "Bài kiểm tra có đếm giờ, xem đáp án và giải thích." },
  { icon: AwardIcon, title: "Chứng chỉ hoàn thành", text: "Nhận chứng chỉ có mã xác minh khi học xong khóa." },
];

/** Đăng nhập, đăng ký: form bên trái, giới thiệu trên nền xanh lá đậm bên phải. */
export default function AuthLayout({ children }: LayoutProps<"/">) {
  return (
    <div className="grid min-h-svh bg-card lg:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)]">
      <div className="flex flex-col p-6 sm:p-10">
        <Brand />
        <div className="flex flex-1 items-center justify-center py-10">
          <div className="w-full max-w-sm">{children}</div>
        </div>
        <p className="text-caption text-muted-foreground">Trường Đại học Tài nguyên và Môi trường Hà Nội</p>
      </div>
      <div className="relative isolate hidden flex-col justify-between overflow-hidden bg-sidebar p-12 text-white lg:flex">
        <ContourPattern className="-z-10 text-white/[0.08]" />
        <div className="text-eyebrow text-sidebar-primary">HUNRE E-Learning</div>
        <div className="max-w-md space-y-10">
          <h2 className="text-display text-white">Học mọi lúc, mọi nơi cùng giảng viên HUNRE</h2>
          <ul className="space-y-6">
            {POINTS.map((p) => (
              <li key={p.title} className="flex gap-4">
                <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-white/10 ring-1 ring-white/15">
                  <p.icon className="size-5" />
                </span>
                <div>
                  <p className="font-semibold">{p.title}</p>
                  <p className="text-sm text-white/75">{p.text}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
        <p className="text-sm text-white/60">Sản phẩm môn học kiến trúc microservices</p>
      </div>
    </div>
  );
}
