import type { ReactNode } from "react";
import { DetailHero, DetailPage } from "@/components/templates/detail-page";

export function VerificationShell({ children }: { children: ReactNode }) {
  return (
    <DetailPage
      hero={
        <DetailHero
          crumbs={[{ href: "/", label: "Trang chủ" }, { label: "Xác minh chứng chỉ" }]}
          eyebrow="Chứng chỉ HUNRE Learning"
          title="Xác minh chứng chỉ"
          description="Đối chiếu thông tin chứng chỉ hoàn thành khóa học theo mã được chia sẻ."
        />
      }
    >
      {children}
    </DetailPage>
  );
}

