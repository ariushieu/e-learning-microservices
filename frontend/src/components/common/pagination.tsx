import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";

/** `page` 0-based như backend; nhãn hiển thị 1-based. */
export function Pagination({
  page,
  totalPages,
  hrefFor,
}: {
  page: number;
  totalPages: number;
  hrefFor: (page: number) => string;
}) {
  if (totalPages <= 1) return null;
  const from = Math.max(0, Math.min(page - 2, totalPages - 5));
  const pages = Array.from({ length: Math.min(5, totalPages) }, (_, i) => from + i);

  return (
    <nav className="mt-10 flex flex-wrap items-center justify-center gap-1" aria-label="Phân trang">
      {page > 0 ? (
        <Button asChild variant="ghost">
          <Link href={hrefFor(page - 1)}>
            <ChevronLeftIcon /> Trước
          </Link>
        </Button>
      ) : (
        <Button variant="ghost" disabled>
          <ChevronLeftIcon /> Trước
        </Button>
      )}
      {pages.map((p) =>
        p === page ? (
          <Button key={p} variant="outline" size="icon" aria-current="page" className="tabular-nums">
            {p + 1}
          </Button>
        ) : (
          <Button key={p} asChild variant="ghost" size="icon" className="tabular-nums">
            <Link href={hrefFor(p)}>{p + 1}</Link>
          </Button>
        ),
      )}
      {page < totalPages - 1 ? (
        <Button asChild variant="ghost">
          <Link href={hrefFor(page + 1)}>
            Sau <ChevronRightIcon />
          </Link>
        </Button>
      ) : (
        <Button variant="ghost" disabled>
          Sau <ChevronRightIcon />
        </Button>
      )}
    </nav>
  );
}

/** Phân trang chỉ có Trước / Sau (danh sách theo thời gian như thông báo). Nhãn tùy chỉnh được. */
export function PrevNextPagination({
  first,
  last,
  prevHref,
  nextHref,
  prevLabel = "Trước",
  nextLabel = "Sau",
}: {
  first: boolean;
  last: boolean;
  prevHref: string;
  nextHref: string;
  prevLabel?: string;
  nextLabel?: string;
}) {
  if (first && last) return null;
  return (
    <nav className="mt-6 flex items-center justify-between" aria-label="Phân trang">
      {first ? (
        <span />
      ) : (
        <Button asChild variant="outline">
          <Link href={prevHref}>
            <ChevronLeftIcon /> {prevLabel}
          </Link>
        </Button>
      )}
      {!last && (
        <Button asChild variant="outline">
          <Link href={nextHref}>
            {nextLabel} <ChevronRightIcon />
          </Link>
        </Button>
      )}
    </nav>
  );
}
