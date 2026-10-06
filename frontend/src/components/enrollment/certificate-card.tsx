import { GraduationCapIcon } from "lucide-react";
import { TIME_ZONE } from "@/lib/format";
import type { Certificate } from "@/lib/types";

function issuedDate(value: string) {
  const parts = new Intl.DateTimeFormat("vi-VN", {
    timeZone: TIME_ZONE,
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).formatToParts(new Date(value));
  const get = (type: string) => parts.find((p) => p.type === type)?.value ?? "";
  return `Ngày ${get("day")} tháng ${get("month")} năm ${get("year")}`;
}

/**
 * Chứng chỉ khổ A4 ngang. Khi in, header/footer của layout bị ẩn bằng CSS in ở trang,
 * còn bản thân thẻ thì bỏ bóng đổ và co theo khổ giấy.
 */
export function CertificateCard({ certificate, learnerName }: { certificate: Certificate; learnerName: string }) {
  return (
    <div className="certificate mx-auto w-full max-w-5xl rounded-xl bg-white p-3 text-zinc-900 shadow-sm ring-1 ring-foreground/10 sm:aspect-[297/210] print:h-[190mm] print:max-w-none print:rounded-none print:p-0 print:shadow-none print:ring-0">
      <div className="flex h-full min-h-[420px] flex-col items-center justify-between rounded-lg border-4 border-double border-primary/70 px-[7%] py-[5%] text-center sm:min-h-0">
        <div className="flex flex-col items-center gap-2">
          <span className="flex size-10 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <GraduationCapIcon className="size-5" />
          </span>
          <p className="text-sm font-semibold tracking-tight">
            HUNRE <span className="text-zinc-500">Learning</span>
          </p>
        </div>

        <div className="w-full">
          <p className="text-xs font-semibold tracking-[0.35em] text-primary sm:text-sm">CHỨNG NHẬN HOÀN THÀNH</p>
          <p className="mt-6 text-sm text-zinc-500 sm:mt-8">Chứng nhận học viên</p>
          <p className="mt-2 font-serif text-3xl font-semibold tracking-tight text-balance sm:text-5xl">{learnerName}</p>
          <div className="mx-auto mt-4 h-px w-1/2 bg-zinc-200" />
          <p className="mt-4 text-sm text-zinc-500">đã hoàn thành khóa học</p>
          <p className="mt-2 text-lg font-semibold text-balance sm:text-2xl">{certificate.courseTitle}</p>
        </div>

        <div className="flex w-full flex-wrap items-end justify-between gap-6 text-left text-xs sm:text-sm">
          <div className="space-y-1">
            <p className="text-zinc-500">{issuedDate(certificate.issuedAt)}</p>
            <p className="text-zinc-500">
              Mã chứng chỉ: <span className="font-mono font-medium text-zinc-900">{certificate.certificateCode}</span>
            </p>
          </div>
          <div className="min-w-40 text-center">
            <div className="border-t border-zinc-300 pt-1.5 font-medium">HUNRE Learning</div>
            <p className="text-xs text-zinc-500">Đơn vị cấp chứng chỉ</p>
          </div>
        </div>
      </div>
    </div>
  );
}
