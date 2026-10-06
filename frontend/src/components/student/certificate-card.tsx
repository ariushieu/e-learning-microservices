import type { Certificate } from "@/lib/types";

function issuedDate(value: string) {
  const parts = new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).formatToParts(new Date(value));
  const get = (type: string) => parts.find((p) => p.type === type)?.value ?? "";
  return `Ngày ${get("day")} tháng ${get("month")} năm ${get("year")}`;
}

/**
 * Chứng chỉ khổ A4 ngang. Khi in, header/footer của layout bị ẩn bằng CSS in ở trang,
 * còn bản thân thẻ thì bỏ bo góc, bóng đổ và co theo khổ giấy.
 */
export function CertificateCard({ certificate, learnerName }: { certificate: Certificate; learnerName: string }) {
  return (
    <div className="mx-auto min-h-[420px] w-full max-w-5xl sm:aspect-[297/210] sm:min-h-0 rounded-2xl print:min-h-0 bg-white p-3 shadow-xl print:h-[190mm] print:max-w-none print:rounded-none print:p-0 print:shadow-none">
      <div className="relative flex h-full flex-col items-center justify-between overflow-hidden rounded-xl border-[6px] border-double border-indigo-800 bg-linear-to-br from-white via-indigo-50/40 to-amber-50/60 px-[6%] py-[5%] text-center">
        <div className="pointer-events-none absolute -left-24 -top-24 h-64 w-64 rounded-full bg-indigo-100/60" />
        <div className="pointer-events-none absolute -bottom-28 -right-20 h-72 w-72 rounded-full bg-amber-100/70" />

        <div className="relative">
          <div className="mx-auto grid h-14 w-14 place-items-center rounded-full bg-indigo-700 text-lg font-bold text-white ring-4 ring-amber-300">
            EL
          </div>
          <p className="mt-3 text-xs font-semibold uppercase tracking-[0.3em] text-indigo-700 sm:text-sm">HUNRE E-Learning</p>
        </div>

        <div className="relative">
          <h1 className="text-2xl font-bold tracking-wide text-indigo-900 sm:text-4xl">CHỨNG NHẬN HOÀN THÀNH</h1>
          <p className="mt-3 text-sm text-slate-500 sm:mt-5 sm:text-base">Chứng nhận học viên</p>
          <p className="mt-1 font-serif text-3xl font-semibold italic text-slate-900 sm:mt-2 sm:text-5xl">{learnerName}</p>
          <div className="mx-auto mt-3 h-px w-2/3 bg-linear-to-r from-transparent via-amber-400 to-transparent" />
          <p className="mt-3 text-sm text-slate-500 sm:mt-5 sm:text-base">đã hoàn thành xuất sắc khóa học</p>
          <p className="mt-1 text-xl font-semibold text-indigo-800 sm:mt-2 sm:text-2xl">{certificate.courseTitle}</p>
        </div>

        <div className="relative flex w-full items-end justify-between gap-4 text-left text-xs text-slate-600 sm:text-sm">
          <div>
            <p className="text-slate-500">Mã chứng chỉ</p>
            <p className="font-mono font-semibold text-slate-800">{certificate.certificateCode}</p>
          </div>
          <div className="text-right">
            <p className="text-slate-500">{issuedDate(certificate.issuedAt)}</p>
            <p className="mt-6 border-t border-slate-400 pt-1 font-semibold text-slate-800 sm:mt-8">HUNRE E-Learning</p>
          </div>
        </div>
      </div>
    </div>
  );
}
