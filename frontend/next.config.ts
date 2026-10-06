import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Gói server tối giản cho Docker: image chỉ cần server.js và phần node_modules thật sự dùng.
  output: "standalone",
  poweredByHeader: false,
};

export default nextConfig;
