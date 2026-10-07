import { AwardIcon } from "lucide-react";
import Link from "next/link";
import { EmptyState } from "@/components/common/empty-state";
import { VerificationShell } from "@/components/enrollment/verification-shell";
import { Button } from "@/components/ui/button";

export default function CertificateNotFound() {
  return (
    <VerificationShell>
      <EmptyState
        icon={AwardIcon}
        title="Không tìm thấy chứng chỉ"
        description="Mã chứng chỉ không tồn tại hoặc đã bị xóa. Hãy kiểm tra lại liên kết với người chia sẻ."
        action={<Button asChild variant="outline"><Link href="/">Về trang chủ</Link></Button>}
      />
    </VerificationShell>
  );
}

