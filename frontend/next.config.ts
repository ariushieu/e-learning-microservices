import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Chỉ bật "standalone" khi build image Docker (Dockerfile đặt NEXT_OUTPUT=standalone): image chỉ
  // cần server.js và phần node_modules thật sự dùng. Trên máy để mặc định, nên `pnpm build` rồi
  // `pnpm start` chạy bình thường — bật standalone thì `next start` báo không hỗ trợ.
  output: process.env.NEXT_OUTPUT === "standalone" ? "standalone" : undefined,
  poweredByHeader: false,
};

export default nextConfig;
