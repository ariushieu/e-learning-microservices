"use client";

import { BookOpenIcon, LayoutDashboardIcon, LogOutIcon, ShieldIcon, UserIcon } from "lucide-react";
import Link from "next/link";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useSession } from "@/components/session-provider";
import { initials, label } from "@/lib/format";
import { logoutAction } from "@/lib/server/auth-actions";

export function UserMenu() {
  const { session, hasRole } = useSession();
  if (!session) return null;
  const mainRole = hasRole("ROLE_ADMIN") ? "ROLE_ADMIN" : hasRole("ROLE_INSTRUCTOR") ? "ROLE_INSTRUCTOR" : "ROLE_STUDENT";

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="rounded-full outline-none focus-visible:ring-2 focus-visible:ring-ring" aria-label="Tài khoản">
        <Avatar className="size-8">
          <AvatarFallback className="bg-primary/10 text-xs font-semibold text-primary">{initials(session.fullName)}</AvatarFallback>
        </Avatar>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-60">
        <DropdownMenuLabel className="font-normal">
          <div className="truncate font-medium">{session.fullName}</div>
          <div className="truncate text-xs text-muted-foreground">{session.email}</div>
          <div className="mt-1 text-xs text-primary">{label(mainRole)}</div>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuGroup>
          <DropdownMenuItem asChild>
            <Link href="/my-courses">
              <BookOpenIcon /> Khóa học của tôi
            </Link>
          </DropdownMenuItem>
          <DropdownMenuItem asChild>
            <Link href="/profile">
              <UserIcon /> Hồ sơ
            </Link>
          </DropdownMenuItem>
          {hasRole("ROLE_INSTRUCTOR", "ROLE_ADMIN") && (
            <DropdownMenuItem asChild>
              <Link href="/instructor">
                <LayoutDashboardIcon /> Trang giảng dạy
              </Link>
            </DropdownMenuItem>
          )}
          {hasRole("ROLE_ADMIN") && (
            <DropdownMenuItem asChild>
              <Link href="/admin">
                <ShieldIcon /> Quản trị
              </Link>
            </DropdownMenuItem>
          )}
        </DropdownMenuGroup>
        <DropdownMenuSeparator />
        <form action={logoutAction}>
          <DropdownMenuItem asChild variant="destructive">
            <button type="submit" className="w-full">
              <LogOutIcon /> Đăng xuất
            </button>
          </DropdownMenuItem>
        </form>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
