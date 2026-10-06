"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui";

export default function ErrorPage({ error, retry }: { error: Error & { digest?: string }; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);
  return (
    <div className="mx-auto max-w-md py-16 text-center">
      <h1 className="text-xl font-semibold">Có lỗi khi tải trang</h1>
      <p className="mt-2 text-slate-500">{error.message || "Một dịch vụ có thể đang tạm ngưng."}</p>
      <Button className="mt-6" onClick={() => retry()}>
        Thử lại
      </Button>
    </div>
  );
}
