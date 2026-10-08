"use client";

import { ArrowLeftIcon, FolderTreeIcon, LayoutDashboardIcon, PlusCircleIcon, UsersIcon } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";
import { useSession } from "@/components/session-provider";
import { Brand } from "./brand";

const TEACHING = [
  { href: "/instructor", label: "Khóa học tôi dạy", icon: LayoutDashboardIcon, exact: true },
  { href: "/instructor/courses/new", label: "Tạo khóa học", icon: PlusCircleIcon, exact: true },
];

const ADMIN = [
  { href: "/admin", label: "Tổng quan", icon: LayoutDashboardIcon, exact: true },
  { href: "/admin/users", label: "Người dùng & quyền", icon: UsersIcon, exact: false },
  { href: "/admin/categories", label: "Danh mục", icon: FolderTreeIcon, exact: false },
];

export function AppSidebar() {
  const pathname = usePathname();
  const { hasRole } = useSession();
  const isActive = (href: string, exact: boolean) =>
    exact ? pathname === href : pathname === href || pathname.startsWith(`${href}/`);

  const group = (title: string, items: typeof TEACHING) => (
    <SidebarGroup>
      <SidebarGroupLabel>{title}</SidebarGroupLabel>
      <SidebarGroupContent>
        <SidebarMenu>
          {items.map((item) => (
            <SidebarMenuItem key={item.href}>
              <SidebarMenuButton asChild isActive={isActive(item.href, item.exact)}>
                <Link href={item.href}>
                  <item.icon />
                  <span>{item.label}</span>
                </Link>
              </SidebarMenuButton>
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );

  return (
    <Sidebar>
      <SidebarHeader className="h-16 justify-center border-b border-sidebar-border px-4">
        <Brand href="/instructor" tone="inverse" />
      </SidebarHeader>
      <SidebarContent>
        {hasRole("ROLE_INSTRUCTOR", "ROLE_ADMIN") && group("Giảng dạy", TEACHING)}
        {hasRole("ROLE_ADMIN") && group("Quản trị", ADMIN)}
      </SidebarContent>
      <SidebarFooter>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton asChild>
              <Link href="/">
                <ArrowLeftIcon />
                <span>Về trang học viên</span>
              </Link>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
    </Sidebar>
  );
}
