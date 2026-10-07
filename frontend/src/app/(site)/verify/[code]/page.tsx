import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ErrorAlert } from "@/components/common/error-alert";
import { Fact, FactList } from "@/components/common/fact-list";
import { Section } from "@/components/common/section";
import { StatusBadge } from "@/components/common/status-badge";
import { VerificationShell } from "@/components/enrollment/verification-shell";
import { Card, CardContent } from "@/components/ui/card";
import { ApiError } from "@/lib/errors";
import { formatDay } from "@/lib/format";
import { gateway } from "@/lib/server/gateway";
import type { CertificateVerification } from "@/lib/types";

export const metadata: Metadata = {
  title: "Xác minh chứng chỉ",
  robots: { index: false, follow: false },
};

export default async function VerifyCertificatePage({ params }: PageProps<"/verify/[code]">) {
  const { code } = await params;
  if (!/^CERT-[A-Za-z0-9-]{1,35}$/.test(code)) notFound();

  let certificate: CertificateVerification;
  try {
    certificate = await gateway<CertificateVerification>(
      `/api/certificates/verify/${encodeURIComponent(code)}`,
      { anonymous: true },
    );
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) notFound();
    return (
      <VerificationShell>
        <ErrorAlert
          title="Chưa thể xác minh chứng chỉ"
          message={error instanceof ApiError && error.status === 422
            ? error.message
            : "Dịch vụ xác minh đang tạm thời không khả dụng. Vui lòng tải lại trang sau ít phút."}
        />
      </VerificationShell>
    );
  }

  return (
    <VerificationShell>
      <Section
        title="Thông tin chứng chỉ"
        description="Chứng chỉ này được cấp sau khi học viên hoàn thành khóa học trên HUNRE Learning."
        actions={<StatusBadge status="COMPLETED">Hợp lệ</StatusBadge>}
      >
        <Card>
          <CardContent className="py-2">
            <FactList layout="grid" className="grid-cols-1 sm:grid-cols-2 [&_dd]:[overflow-wrap:anywhere]">
              <Fact label="Học viên" value={certificate.learnerName} />
              <Fact label="Khóa học" value={certificate.courseTitle} />
              <Fact label="Ngày cấp" value={<time dateTime={certificate.issuedAt}>{formatDay(certificate.issuedAt)}</time>} />
              <Fact label="Mã chứng chỉ" value={<span className="font-mono text-sm">{certificate.certificateCode}</span>} />
            </FactList>
          </CardContent>
        </Card>
      </Section>
    </VerificationShell>
  );
}

