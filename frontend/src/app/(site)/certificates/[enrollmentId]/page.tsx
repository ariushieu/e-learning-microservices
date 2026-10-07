import { ArrowLeftIcon, AwardIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { PageHeader } from "@/components/common/page-header";
import { isId } from "@/components/course/queries";
import { CertificateCard } from "@/components/enrollment/certificate-card";
import { PrintButton } from "@/components/enrollment/print-button";
import { Button } from "@/components/ui/button";
import { gatewayOrNull } from "@/lib/server/gateway";
import type { Certificate } from "@/lib/types";

export const metadata: Metadata = { title: "Chứng chỉ" };

// Header/footer và lề của <main> thuộc layout chung nên không gắn được `print:hidden`;
// ẩn chúng bằng CSS in riêng của trang, chỉ có hiệu lực khi đang ở trang chứng chỉ.
const printCss = `
@page { size: A4 landscape; margin: 10mm; }
@media print {
  body header, body footer { display: none !important; }
  body main { padding: 0 !important; max-width: none !important; }
  body { background: white !important; print-color-adjust: exact; -webkit-print-color-adjust: exact; }
}
`;

export default async function CertificatePage({ params }: PageProps<"/certificates/[enrollmentId]">) {
  const { enrollmentId } = await params;
  const certificate = isId(enrollmentId)
    ? await gatewayOrNull<Certificate>(`/api/enrollments/${enrollmentId}/certificate`)
    : null;

  if (!certificate) {
    return (
      <EmptyState
        icon={AwardIcon}
        title="Bạn chưa có chứng chỉ cho khóa học này"
        description="Chứng chỉ được cấp tự động khi bạn hoàn thành tất cả bài học."
        action={
          <Button asChild variant="outline">
            <Link href="/my-courses">Về khóa học của tôi</Link>
          </Button>
        }
      />
    );
  }

  return (
    <div className="[overflow-wrap:anywhere]">
      <style>{printCss}</style>
      <div className="print:hidden">
        <PageHeader
          crumbs={[{ href: "/my-courses", label: "Khóa học của tôi" }, { label: "Chứng chỉ" }]}
          eyebrow="Chứng chỉ"
          title={certificate.courseTitle}
          description="Chứng chỉ hoàn thành khóa học của bạn. Bấm “In chứng chỉ” để in hoặc lưu thành PDF."
          actions={
            <>
              <Button asChild variant="outline" size="lg">
                <Link href={`/learn/${certificate.courseId}`}>
                  <ArrowLeftIcon /> Về khóa học
                </Link>
              </Button>
              <PrintButton />
            </>
          }
        />
      </div>
      <CertificateCard certificate={certificate} />
    </div>
  );
}
