"use client";

import Link from "next/link";
import { useEffect } from "react";
import { StatusPage } from "@/components/common/status-page";
import { Brand } from "@/components/layout/brand";
import { Button } from "@/components/ui/button";

export default function ErrorPage({ error, retry }: { error: Error & { digest?: string }; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);
  return (
    <div className="flex min-h-svh flex-col p-6 sm:p-10">
      <Brand />
      <div className="flex flex-1 items-center justify-center">
        <StatusPage
          code="Lỗi"
          title="Có lỗi khi tải trang"
          description={error.message || "Một dịch vụ có thể đang tạm ngưng."}
          action={
            <>
              <Button size="lg" onClick={() => retry()}>
                Thử lại
              </Button>
              <Button asChild size="lg" variant="outline">
                <Link href="/">Về trang chủ</Link>
              </Button>
            </>
          }
        />
      </div>
    </div>
  );
}
