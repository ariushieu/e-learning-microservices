import type { Metadata } from "next";
import { Be_Vietnam_Pro } from "next/font/google";
import { Navbar } from "@/components/navbar";
import { SessionProvider } from "@/components/session-provider";
import { getSession } from "@/lib/server/gateway";
import "./globals.css";

const font = Be_Vietnam_Pro({
  variable: "--font-be-vietnam",
  subsets: ["vietnamese", "latin"],
  weight: ["400", "500", "600", "700"],
});

export const metadata: Metadata = {
  title: { default: "HUNRE E-Learning", template: "%s · HUNRE E-Learning" },
  description: "Nền tảng học trực tuyến — sản phẩm môn học kiến trúc microservices, HUNRE",
};

export default async function RootLayout({ children }: LayoutProps<"/">) {
  const session = await getSession();
  return (
    <html lang="vi" className={`${font.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col font-sans">
        <SessionProvider session={session}>
          <Navbar />
          <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-8">{children}</main>
          <footer className="border-t border-slate-200 bg-white py-6 text-center text-sm text-slate-500">
            HUNRE E-Learning · Spring Boot microservices + Next.js
          </footer>
        </SessionProvider>
      </body>
    </html>
  );
}
