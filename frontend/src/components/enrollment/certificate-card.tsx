import { AwardIcon } from "lucide-react";
import { ContourPattern } from "@/components/common/decor";
import { BrandMark } from "@/components/layout/brand";
import { VerificationLink } from "@/components/enrollment/verification-link";
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
 * Chứng chỉ khổ A4 ngang, trình bày như bằng của trường: giấy trắng, viền kép màu chính,
 * dấu mộc vàng thành tích. Khi in, header/footer của layout bị ẩn bằng CSS in ở trang,
 * còn bản thân thẻ thì bỏ bóng đổ và co theo khổ giấy.
 */
export function CertificateCard({ certificate }: { certificate: Certificate }) {
  return (
    <div className="certificate relative isolate mx-auto w-full max-w-5xl overflow-hidden rounded-xl bg-card p-2.5 text-foreground shadow-raised ring-1 ring-border sm:aspect-[297/210] sm:p-4 print:h-[190mm] print:max-w-none print:rounded-none print:p-0 print:shadow-none print:ring-0">
      {/* Hình mờ đường đồng mức: dấu ấn tài nguyên – môi trường, đủ nhạt để không lấn chữ khi in. */}
      <ContourPattern className="-z-10 text-primary/6" />

      <div className="relative flex h-full min-h-[480px] flex-col items-center justify-between rounded-lg border-[6px] border-double border-primary px-[6%] py-8 text-center sm:min-h-0 sm:py-[4.5%]">
        <div className="pointer-events-none absolute inset-1.5 rounded-sm border border-primary/25" aria-hidden />

        <div className="flex flex-col items-center gap-2.5">
          <span className="flex size-12 items-center justify-center rounded-xl bg-primary text-primary-foreground">
            <BrandMark className="size-7" />
          </span>
          <div className="leading-tight">
            <p className="text-base font-bold tracking-tight">
              HUNRE <span className="font-medium text-muted-foreground">Learning</span>
            </p>
            <p className="mt-0.5 text-caption text-muted-foreground">Trường Đại học Tài nguyên và Môi trường Hà Nội</p>
          </div>
        </div>

        <div className="w-full py-6 sm:py-0">
          <p className="text-xs font-semibold tracking-[0.35em] text-primary sm:text-sm">CHỨNG NHẬN HOÀN THÀNH</p>
          <p className="mt-5 text-sm text-muted-foreground sm:mt-7">Chứng nhận học viên</p>
          <p className="mt-2 font-serif text-3xl leading-tight font-semibold tracking-tight text-balance break-words sm:text-5xl">{certificate.learnerName}</p>
          <div className="mx-auto mt-4 flex w-1/2 items-center gap-3 text-primary/40" aria-hidden>
            <span className="h-px flex-1 bg-current" />
            <span className="size-1.5 rotate-45 bg-current" />
            <span className="h-px flex-1 bg-current" />
          </div>
          <p className="mt-4 text-sm text-muted-foreground">đã hoàn thành khóa học</p>
          <p className="mx-auto mt-2 max-w-3xl text-lg font-semibold text-balance text-primary-strong sm:text-2xl">{certificate.courseTitle}</p>
        </div>

        <div className="grid w-full grid-cols-1 items-end gap-x-4 gap-y-5 text-xs sm:grid-cols-3 sm:text-sm">
          <div className="space-y-1 sm:text-left">
            <p className="text-muted-foreground">{issuedDate(certificate.issuedAt)}</p>
            <p className="text-muted-foreground">
              Mã chứng chỉ: <span className="block font-mono font-medium break-all text-foreground">{certificate.certificateCode}</span>
            </p>
          </div>

          <div className="order-first flex justify-center sm:order-none">
            <Seal />
          </div>

          <div className="flex flex-col items-center">
            <div className="min-w-36 border-t border-foreground/30 pt-1.5 text-center">
              <p className="font-medium">HUNRE Learning</p>
              <p className="text-xs text-muted-foreground">Đơn vị cấp chứng chỉ</p>
            </div>
          </div>
        </div>
        <VerificationLink code={certificate.certificateCode} />
      </div>
    </div>
  );
}

/** Dấu mộc vàng: vòng ngoài liền, vòng trong đứt nét, huy chương ở giữa. */
function Seal() {
  return (
    <div className="relative flex size-20 items-center justify-center rounded-full bg-achievement-soft text-achievement ring-2 ring-achievement sm:size-24" aria-hidden>
      <div className="absolute inset-1.5 rounded-full border border-dashed border-achievement/60" />
      <AwardIcon className="size-9 sm:size-11" strokeWidth={1.6} />
    </div>
  );
}
