import Link from "next/link";
import { getSession } from "@/lib/server/gateway";
import { hasRole } from "@/lib/auth-shared";
import { logoutAction } from "@/lib/server/auth-actions";
import { NotificationBell } from "./notification-bell";

export async function Navbar() {
  const session = await getSession();
  const isInstructor = hasRole(session, "ROLE_INSTRUCTOR", "ROLE_ADMIN");
  const isAdmin = hasRole(session, "ROLE_ADMIN");

  return (
    <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/90 backdrop-blur">
      <nav className="mx-auto flex h-16 max-w-7xl items-center gap-6 px-4">
        <Link href="/" className="flex items-center gap-2 text-lg font-bold text-indigo-700">
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-indigo-600 text-sm text-white">EL</span>
          HUNRE E-Learning
        </Link>
        <div className="flex items-center gap-1 text-sm text-slate-600">
          <NavLink href="/">Khóa học</NavLink>
          {session && <NavLink href="/my-courses">Khóa của tôi</NavLink>}
          {isInstructor && <NavLink href="/instructor">Giảng dạy</NavLink>}
          {isAdmin && <NavLink href="/admin">Quản trị</NavLink>}
        </div>
        <div className="ml-auto flex items-center gap-3">
          {session ? (
            <>
              <NotificationBell />
              <Link href="/profile" className="hidden text-right sm:block">
                <div className="text-sm font-medium text-slate-800">{session.fullName}</div>
                <div className="text-xs text-slate-500">{roleLabel(session.roles)}</div>
              </Link>
              <form action={logoutAction}>
                <button className="rounded-lg px-3 py-2 text-sm text-slate-600 hover:bg-slate-100">Đăng xuất</button>
              </form>
            </>
          ) : (
            <>
              <Link href="/login" className="rounded-lg px-3 py-2 text-sm text-slate-700 hover:bg-slate-100">
                Đăng nhập
              </Link>
              <Link href="/register" className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700">
                Đăng ký
              </Link>
            </>
          )}
        </div>
      </nav>
    </header>
  );
}

function NavLink({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <Link href={href} className="rounded-lg px-3 py-2 hover:bg-slate-100 hover:text-slate-900">
      {children}
    </Link>
  );
}

function roleLabel(roles: string[]) {
  if (roles.includes("ROLE_ADMIN")) return "Quản trị viên";
  if (roles.includes("ROLE_INSTRUCTOR")) return "Giảng viên";
  return "Học viên";
}
