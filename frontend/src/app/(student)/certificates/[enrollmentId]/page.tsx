import type { Metadata } from "next";
import { CertificateCard } from "@/components/student/certificate-card";
import { PrintButton } from "@/components/student/print-button";
import { isId } from "@/components/student/queries";
import { Empty, LinkButton } from "@/components/ui";
import { gatewayOrNull, getSession } from "@/lib/server/gateway";
import type { Certificate } from "@/lib/types";

export const metadata: Metadata = { title: "Chứng chỉ" };

// Layout gốc (navbar, footer, lề của <main>) không thuộc trang này nên không gắn được `print:hidden`;
// ẩn chúng bằng CSS in riêng của trang, chỉ có hiệu lực khi đang ở trang chứng chỉ.
const printCss = `
@page { size: A4 landscape; margin: 10mm; }
@media print {
  body header, body footer { display: none !important; }
  body main { padding: 0 !important; max-width: none !important; }
  body { background: #fff !important; print-color-adjust: exact; -webkit-print-color-adjust: exact; }
}
`;

export default async function CertificatePage({ params }: PageProps<"/certificates/[enrollmentId]">) {
  const { enrollmentId } = await params;
  const [certificate, session] = await Promise.all([
    isId(enrollmentId) ? gatewayOrNull<Certificate>(`/api/enrollments/${enrollmentId}/certificate`) : null,
    getSession(),
  ]);

  if (!certificate) {
    return (
      <Empty>
        <p className="font-medium text-slate-700">Bạn chưa có chứng chỉ cho khóa học này.</p>
        <p className="mt-1 text-sm">Chứng chỉ được cấp tự động khi bạn hoàn thành tất cả bài học.</p>
        <LinkButton href="/my-courses" variant="secondary" className="mt-4">
          Về khóa học của tôi
        </LinkButton>
      </Empty>
    );
  }

  return (
    <div className="space-y-6 print:space-y-0">
      <style>{printCss}</style>
      <div className="flex flex-wrap items-center justify-between gap-3 print:hidden">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Chứng chỉ của bạn</h1>
          <p className="text-slate-500">{certificate.courseTitle}</p>
        </div>
        <div className="flex gap-2">
          <LinkButton href={`/learn/${certificate.courseId}`} variant="secondary">
            Về khóa học
          </LinkButton>
          <PrintButton />
        </div>
      </div>
      <CertificateCard certificate={certificate} learnerName={session?.fullName || "Học viên"} />
    </div>
  );
}
