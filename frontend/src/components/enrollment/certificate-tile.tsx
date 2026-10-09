import { AwardIcon, ExternalLinkIcon, PrinterIcon } from "lucide-react";
import Link from "next/link";
import { ContourPattern } from "@/components/common/decor";
import { Button } from "@/components/ui/button";
import { formatDay } from "@/lib/format";
import type { Certificate } from "@/lib/types";

/** Chứng chỉ thu nhỏ trong bộ sưu tập: khung đôi như bản in, mã xác minh và lối tắt in, xác minh. */
export function CertificateTile({ certificate: c }: { certificate: Certificate }) {
  return (
    <article className="flex h-full flex-col overflow-hidden rounded-2xl bg-card shadow-sm ring-1 ring-border transition-shadow hover:shadow-raised">
      <div className="relative isolate m-3 flex-1 overflow-hidden rounded-xl border-2 border-primary/30 p-5 text-center outline outline-1 outline-offset-4 outline-primary/20">
        <ContourPattern className="-z-10 text-primary/[0.06]" />
        <span className="mx-auto mb-3 flex size-12 items-center justify-center rounded-full bg-achievement-soft text-achievement-strong ring-4 ring-achievement-soft/60">
          <AwardIcon className="size-6" aria-hidden />
        </span>
        <p className="text-eyebrow text-primary">Chứng nhận hoàn thành</p>
        <h3 className="mt-2 line-clamp-2 text-subheading">{c.courseTitle}</h3>
        <p className="mt-2 text-sm text-muted-foreground">{c.learnerName}</p>
        <p className="mt-3 text-caption text-muted-foreground">Cấp ngày {formatDay(c.issuedAt)}</p>
        <p className="mt-1 truncate font-mono text-[11px] text-muted-foreground" title={c.certificateCode}>
          {c.certificateCode}
        </p>
      </div>
      <div className="flex flex-wrap gap-2 border-t px-4 py-3">
        <Button asChild size="sm">
          <Link href={`/certificates/${c.enrollmentId}`}>
            <PrinterIcon /> Xem và in
          </Link>
        </Button>
        <Button asChild size="sm" variant="ghost">
          <Link href={`/verify/${encodeURIComponent(c.certificateCode)}`}>
            <ExternalLinkIcon /> Trang xác minh
          </Link>
        </Button>
      </div>
    </article>
  );
}
