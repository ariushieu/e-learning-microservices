import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Chỉ bật "standalone" khi build image Docker (Dockerfile đặt NEXT_OUTPUT=standalone): image chỉ
  // cần server.js và phần node_modules thật sự dùng. Trên máy để mặc định, nên `pnpm build` rồi
  // `pnpm start` chạy bình thường — bật standalone thì `next start` báo không hỗ trợ.
  output: process.env.NEXT_OUTPUT === "standalone" ? "standalone" : undefined,
  poweredByHeader: false,
  // Chuyển trước khi render. Nếu gọi redirect() trong page, loading.tsx của (dashboard) làm Next
  // gửi khung "đang tải" trước rồi mới chuyển ở trình duyệt (không còn mã 307).
  async redirects() {
    return [{ source: "/admin", destination: "/admin/users", permanent: false }];
  },
};

export default nextConfig;
