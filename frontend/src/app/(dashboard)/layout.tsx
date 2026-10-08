import { AppSidebar } from "@/components/layout/app-sidebar";
import { getReviewInboxSummary } from "@/components/course/instructor-review-query";
import { getSession } from "@/lib/server/gateway";
import { hasRole } from "@/lib/auth-shared";
import { DashboardCrumbs } from "@/components/layout/dashboard-crumbs";
import { UserMenu } from "@/components/layout/user-menu";
import { NotificationBell } from "@/components/notification/notification-bell";
import { Separator } from "@/components/ui/separator";
import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";

/** Khu giảng viên và quản trị: sidebar xanh đậm bên trái, nội dung rộng bên phải. */
export default async function DashboardLayout({ children }: LayoutProps<"/">) {
  const session = await getSession();
  const summary = hasRole(session, "ROLE_INSTRUCTOR", "ROLE_ADMIN") ? await getReviewInboxSummary() : null;
  return (
    <SidebarProvider>
      <AppSidebar unrepliedCount={summary?.data?.unrepliedCount ?? null} />
      <SidebarInset className="min-w-0 bg-background">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-2 border-b bg-card/85 px-4 backdrop-blur-md">
          <SidebarTrigger className="-ml-1" />
          <Separator orientation="vertical" className="mr-2 data-[orientation=vertical]:h-4" />
          <DashboardCrumbs />
          <div className="ml-auto flex items-center gap-2">
            <NotificationBell />
            <UserMenu />
          </div>
        </header>
        <div className="mx-auto w-full max-w-6xl flex-1 px-4 pt-8 pb-16 sm:px-6">{children}</div>
      </SidebarInset>
    </SidebarProvider>
  );
}
