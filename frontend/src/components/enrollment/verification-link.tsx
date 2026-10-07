"use client";

import Link from "next/link";
import { useSyncExternalStore } from "react";

const subscribe = () => () => {};
const browserOrigin = () => window.location.origin;
const serverOrigin = () => "";

/** Hiện đúng địa chỉ web đang mở, kể cả khi nhóm đổi host triển khai. */
export function VerificationLink({ code }: { code: string }) {
  const origin = useSyncExternalStore(subscribe, browserOrigin, serverOrigin);
  const path = `/verify/${encodeURIComponent(code)}`;
  return (
    <p className="mt-4 w-full text-caption text-muted-foreground">
      Xác minh tại{" "}
      <Link
        href={path}
        className="rounded-sm text-primary underline underline-offset-4 [overflow-wrap:anywhere] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
      >
        {origin}{path}
      </Link>
    </p>
  );
}

