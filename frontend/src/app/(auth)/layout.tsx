import { CheckCircle2Icon } from "lucide-react";
import { Brand } from "@/components/layout/brand";

const POINTS = ["Học theo lộ trình, lưu tiến độ từng bài", "Bài kiểm tra chấm điểm tự động", "Nhận chứng chỉ khi hoàn thành khóa học"];

/** Đăng nhập, đăng ký: một nửa giới thiệu, một nửa là form. */
export default function AuthLayout({ children }: LayoutProps<"/">) {
  return (
    <div className="grid min-h-svh lg:grid-cols-2">
      <div className="flex flex-col p-6 sm:p-10">
        <Brand />
        <div className="flex flex-1 items-center justify-center py-10">
          <div className="w-full max-w-sm">{children}</div>
        </div>
      </div>
      <div className="relative hidden flex-col justify-end overflow-hidden bg-primary p-12 text-primary-foreground lg:flex">
        <div className="absolute -top-24 -right-24 size-96 rounded-full bg-white/10" />
        <div className="absolute top-1/3 -left-16 size-64 rounded-full bg-white/5" />
        <div className="relative space-y-6">
          <h2 className="text-3xl font-semibold tracking-tight text-balance">Học mọi lúc, mọi nơi cùng giảng viên HUNRE</h2>
          <ul className="space-y-3 text-primary-foreground/85">
            {POINTS.map((p) => (
              <li key={p} className="flex items-center gap-3">
                <CheckCircle2Icon className="size-5 shrink-0" /> {p}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
