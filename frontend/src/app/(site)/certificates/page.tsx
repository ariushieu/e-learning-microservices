import { AwardIcon, CompassIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { attempt } from "@/components/course/queries";
import { CertificateTile } from "@/components/enrollment/certificate-tile";
import { getMyCertificates } from "@/components/enrollment/learning-data";
import { CardGrid, ListPage } from "@/components/templates/list-page";
import { Button } from "@/components/ui/button";

export const metadata: Metadata = { title: "Chứng chỉ của tôi" };

export default async function CertificatesPage() {
  const result = await attempt(getMyCertificates());

  return (
    <ListPage
      eyebrow="Thành tích"
      title="Chứng chỉ của tôi"
      description="Mỗi chứng chỉ có mã riêng; ai có mã đều kiểm tra được ở trang xác minh công khai."
      actions={
        <Button asChild variant="outline" size="lg">
          <Link href="/courses">
            <CompassIcon /> Tìm khóa học mới
          </Link>
        </Button>
      }
    >
      {result.error !== null ? (
        <ErrorAlert title="Không tải được chứng chỉ" message={result.error} />
      ) : result.data.length === 0 ? (
        <EmptyState
          icon={AwardIcon}
          title="Bạn chưa có chứng chỉ nào"
          description="Học hết các bài và đạt bài kiểm tra của một khóa để nhận chứng chỉ."
          action={
            <Button asChild size="lg">
              <Link href="/my-courses">Tiếp tục học</Link>
            </Button>
          }
        />
      ) : (
        <CardGrid>
          {result.data.map((c) => (
            <CertificateTile key={c.id} certificate={c} />
          ))}
        </CardGrid>
      )}
    </ListPage>
  );
}
