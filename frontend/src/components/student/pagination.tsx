import Link from "next/link";

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
  const base = "rounded-lg px-3 py-2 text-sm font-medium";

  return (
    <nav className="flex flex-wrap items-center justify-center gap-1" aria-label="Phân trang">
      {page > 0 && (
        <Link href={hrefFor(page - 1)} className={`${base} text-slate-600 hover:bg-slate-100`}>
          ‹ Trước
        </Link>
      )}
      {pages.map((p) =>
        p === page ? (
          <span key={p} className={`${base} bg-indigo-600 text-white`} aria-current="page">
            {p + 1}
          </span>
        ) : (
          <Link key={p} href={hrefFor(p)} className={`${base} text-slate-600 hover:bg-slate-100`}>
            {p + 1}
          </Link>
        ),
      )}
      {page < totalPages - 1 && (
        <Link href={hrefFor(page + 1)} className={`${base} text-slate-600 hover:bg-slate-100`}>
          Sau ›
        </Link>
      )}
    </nav>
  );
}
