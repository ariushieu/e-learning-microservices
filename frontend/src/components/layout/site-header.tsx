import Link from "next/link";
import { Button } from "@/components/ui/button";
import { NotificationBell } from "@/components/notification/notification-bell";
import { hasRole } from "@/lib/auth-shared";
import { getSession } from "@/lib/server/gateway";
import { Brand } from "./brand";
import { MainNav, MobileNav } from "./main-nav";
import { UserMenu } from "./user-menu";

export async function SiteHeader() {
  const session = await getSession();
  const links = [
    { href: "/", label: "Trang chủ" },
    { href: "/courses", label: "Khóa học" },
    { href: "/instructors", label: "Giảng viên" },
    ...(session
      ? [
          { href: "/my-courses", label: "Học tập của tôi" },
          { href: "/leaderboard", label: "Xếp hạng" },
        ]
      : []),
    ...(hasRole(session, "ROLE_INSTRUCTOR", "ROLE_ADMIN") ? [{ href: "/instructor", label: "Giảng dạy" }] : []),
  ];

  return (
    <header className="sticky top-0 z-40 border-b bg-card/85 backdrop-blur-md supports-[backdrop-filter]:bg-card/75">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-2 px-4 sm:gap-6 sm:px-6">
        <MobileNav links={links} />
        <Brand />
        <MainNav links={links} />
        <div className="ml-auto flex items-center gap-1.5 sm:gap-2">
          {session ? (
            <>
              <NotificationBell />
              <UserMenu />
            </>
          ) : (
            <>
              <Button asChild variant="ghost" size="lg">
                <Link href="/login">Đăng nhập</Link>
              </Button>
              <Button asChild size="lg">
                <Link href="/register">Đăng ký</Link>
              </Button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
