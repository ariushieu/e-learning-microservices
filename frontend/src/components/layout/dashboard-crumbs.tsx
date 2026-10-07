"use client";

import { usePathname } from "next/navigation";

// Tên khu vực trên thanh trên của khu giảng dạy/quản trị, theo đường dẫn.
const AREAS: { test: RegExp; area: string; page: string }[] = [
  { test: /^\/instructor\/courses\/new$/, area: "Giảng dạy", page: "Tạo khóa học" },
  { test: /^\/instructor\/courses\/\d+/, area: "Giảng dạy", page: "Soạn khóa học" },
  { test: /^\/instructor\/quizzes\/\d+/, area: "Giảng dạy", page: "Soạn bài kiểm tra" },
  { test: /^\/instructor/, area: "Giảng dạy", page: "Khóa học tôi dạy" },
  { test: /^\/admin\/users/, area: "Quản trị", page: "Người dùng & quyền" },
  { test: /^\/admin\/categories/, area: "Quản trị", page: "Danh mục" },
  { test: /^\/admin/, area: "Quản trị", page: "Tổng quan" },
];

export function DashboardCrumbs() {
  const pathname = usePathname();
  const match = AREAS.find((a) => a.test.test(pathname));
  if (!match) return null;
  return (
    <nav aria-label="Đường dẫn" className="min-w-0">
      <ol className="flex items-center gap-1.5 truncate text-sm">
        <li className="text-muted-foreground">{match.area}</li>
        <li className="text-muted-foreground" aria-hidden>
          /
        </li>
        <li aria-current="page" className="truncate font-medium">
          {match.page}
        </li>
      </ol>
    </nav>
  );
}
