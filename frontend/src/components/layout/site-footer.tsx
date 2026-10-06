import Link from "next/link";
import { Brand } from "./brand";

const COLUMNS = [
  {
    title: "Học tập",
    links: [
      { href: "/", label: "Khám phá khóa học" },
      { href: "/my-courses", label: "Khóa học của tôi" },
      { href: "/notifications", label: "Thông báo" },
    ],
  },
  {
    title: "Dự án",
    links: [
      { href: "/design", label: "Hệ thống giao diện" },
      { href: "/instructor", label: "Khu giảng dạy" },
    ],
  },
];

export function SiteFooter() {
  return (
    <footer className="border-t bg-card">
      <div className="mx-auto grid max-w-6xl gap-10 px-4 py-12 sm:px-6 md:grid-cols-[1.5fr_1fr_1fr]">
        <div className="max-w-sm space-y-4">
          <Brand />
          <p className="text-sm leading-relaxed text-muted-foreground">
            Nền tảng học trực tuyến của Trường Đại học Tài nguyên và Môi trường Hà Nội. Sản phẩm môn học kiến
            trúc microservices.
          </p>
        </div>
        {COLUMNS.map((c) => (
          <div key={c.title} className="space-y-3">
            <h2 className="text-eyebrow text-muted-foreground">{c.title}</h2>
            <ul className="space-y-2 text-sm">
              {c.links.map((l) => (
                <li key={l.href}>
                  <Link href={l.href} className="text-foreground/80 transition-colors hover:text-primary">
                    {l.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
      <div className="border-t">
        <p className="mx-auto max-w-6xl px-4 py-5 text-caption text-muted-foreground sm:px-6">
          © {new Date().getFullYear()} HUNRE E-Learning · Spring Boot microservices, Kafka, Redis, Next.js
        </p>
      </div>
    </footer>
  );
}
