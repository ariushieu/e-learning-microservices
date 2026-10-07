import type { Metadata } from "next";
import { Be_Vietnam_Pro } from "next/font/google";
import { SessionProvider } from "@/components/session-provider";
import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { getSession } from "@/lib/server/gateway";
import { cn } from "@/lib/utils";
import "./globals.css";

// Font có đủ dấu tiếng Việt; Geist mặc định của shadcn thiếu bộ ký tự này.
const font = Be_Vietnam_Pro({
  variable: "--font-sans",
  subsets: ["vietnamese", "latin"],
  weight: ["400", "500", "600", "700"],
});

export const metadata: Metadata = {
  title: { default: "HUNRE Learning", template: "%s · HUNRE Learning" },
  description: "Nền tảng học trực tuyến — sản phẩm môn học kiến trúc microservices, HUNRE",
};

export default async function RootLayout({ children }: LayoutProps<"/">) {
  const session = await getSession();
  return (
    <html lang="vi" className={cn("h-full antialiased", font.variable)}>
      <body className="min-h-full font-sans">
        <SessionProvider session={session}>
          <TooltipProvider>{children}</TooltipProvider>
          <Toaster position="top-right" theme="light" richColors />
        </SessionProvider>
      </body>
    </html>
  );
}
