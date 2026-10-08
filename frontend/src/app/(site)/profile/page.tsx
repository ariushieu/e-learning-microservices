import { AwardIcon, BookOpenIcon, ShieldAlertIcon, type LucideIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { RoleBadges } from "@/components/auth/role-badge";
import { SessionList, type LoginSession } from "@/components/auth/session-list";
import { LoginActivity, type LoginEvent } from "@/components/auth/login-activity";
import { Callout } from "@/components/common/callout";
import { Fact, FactList } from "@/components/common/fact-list";
import { FormSection } from "@/components/common/form-field";
import { IconTile } from "@/components/common/icon-tile";
import { FormPage } from "@/components/templates/form-page";
import { ChangePasswordForm, ProfileDetailsForm } from "@/components/auth/profile-forms";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { formatDate, initials } from "@/lib/format";
import { gateway } from "@/lib/server/gateway";
import type { Enrollment, Page, User } from "@/lib/types";

export const metadata: Metadata = { title: "Hồ sơ" };

// LABELS dùng ACTIVE = "Đang học" cho ghi danh, nên trạng thái tài khoản cần nhãn riêng.
const ACCOUNT_STATUS: Record<User["status"], string> = {
  ACTIVE: "Đang hoạt động",
  PENDING: "Chờ kích hoạt",
  LOCKED: "Đã khóa",
};

export default async function ProfilePage() {
  const [me, enrollments, sessions, activity] = await Promise.all([
    gateway<User>("/api/auth/me"),
    gateway<Page<Enrollment>>("/api/enrollments?size=100").catch(() => null),
    gateway<LoginSession[]>("/api/auth/sessions").catch(() => null),
    gateway<Page<LoginEvent>>("/api/auth/login-events?page=0&size=10").catch(() => null),
  ]);
  const completed = enrollments?.content.filter((e) => e.status === "COMPLETED").length ?? 0;

  return (
    <FormPage
      title="Hồ sơ của tôi"
      description="Thông tin tài khoản HUNRE E-Learning của bạn."
      actions={
        <Button asChild variant="outline" size="lg">
          <Link href="/my-courses">Khóa học của tôi</Link>
        </Button>
      }
    >
      {me.failedLoginsSinceLastSuccess > 0 && (
        <Callout
          icon={ShieldAlertIcon}
          tone="warning"
          title={`Có ${me.failedLoginsSinceLastSuccess} lần đăng nhập sai vào tài khoản của bạn`}
        >
          <p>Kể từ lần đăng nhập thành công trước đó. Nếu không phải bạn, hãy kiểm tra và bảo vệ tài khoản.</p>
          <div className="mt-2 flex flex-wrap gap-x-4 gap-y-2">
            <Link className="font-medium underline underline-offset-4" href="#login-sessions">
              Xem phiên đăng nhập
            </Link>
            <Link className="font-medium underline underline-offset-4" href="#change-password">
              Đổi mật khẩu
            </Link>
          </div>
        </Callout>
      )}
      <Card>
        <CardContent className="flex flex-col gap-6 md:flex-row md:items-center">
          <div className="flex min-w-0 flex-1 items-center gap-4 sm:gap-5">
            <Avatar className="size-16 sm:size-20">
              <AvatarFallback className="bg-primary-soft text-xl font-semibold text-primary-strong sm:text-2xl">{initials(me.fullName)}</AvatarFallback>
            </Avatar>
            <div className="min-w-0 space-y-1.5">
              <h2 className="truncate text-heading">{me.fullName}</h2>
              <p className="truncate text-muted-foreground">{me.email}</p>
              <RoleBadges roles={me.roles} />
            </div>
          </div>
          <dl className="grid shrink-0 grid-cols-2 gap-3 border-t pt-5 md:border-t-0 md:border-l md:pt-0 md:pl-6">
            <MiniStat icon={BookOpenIcon} label="Khóa đã ghi danh" value={enrollments?.totalElements ?? 0} />
            <MiniStat icon={AwardIcon} tone="achievement" label="Đã hoàn thành" value={completed} />
          </dl>
        </CardContent>
      </Card>

      <FormSection title="Thông tin cá nhân" description="Họ tên và liên hệ hiển thị cho giảng viên của khóa bạn học.">
        <ProfileDetailsForm user={me} />
      </FormSection>

      <FormSection
        title={<span id="change-password" className="scroll-mt-24">Đổi mật khẩu</span>}
        description="Dùng mật khẩu hiện tại để đặt mật khẩu mới. Các phiên đăng nhập trên thiết bị khác sẽ cần đăng nhập lại."
      >
        <ChangePasswordForm />
      </FormSection>

      <FormSection
        title={<span id="login-sessions" className="scroll-mt-24">Phiên đăng nhập</span>}
        description="Xem thiết bị đã đăng nhập và đăng xuất những phiên bạn không còn sử dụng."
      >
        <SessionList
          key={sessions?.map((session) => `${session.id}:${session.current}`).join(",") ?? "error"}
          initial={sessions}
        />
      </FormSection>

      <FormSection
        title="Hoạt động đăng nhập"
        description="10 lần gần nhất trong 90 ngày. Chỉ bạn xem được lịch sử của mình."
      >
        <LoginActivity initial={activity} />
      </FormSection>

      <FormSection
        title="Tài khoản"
        description="Cần quyền giảng viên? Gửi mã người dùng cho quản trị viên, rồi đăng nhập lại sau khi được cấp quyền."
      >
        <FactList>
          <Fact
            label="Mã người dùng"
            value={<span className="font-mono select-all">#{me.id}</span>}
            hint="Gửi mã này cho quản trị viên khi cần cấp quyền"
          />
          <Fact label="Vai trò" value={<RoleBadges roles={me.roles} className="justify-end" />} />
          <Fact label="Trạng thái" value={ACCOUNT_STATUS[me.status] ?? me.status} />
          <Fact label="Ngày tạo" value={formatDate(me.createdAt)} />
        </FactList>
      </FormSection>
    </FormPage>
  );
}

function MiniStat({ icon, label, value, tone = "primary" }: { icon: LucideIcon; label: string; value: number; tone?: "primary" | "achievement" }) {
  return (
    <div className="flex items-center gap-3">
      <IconTile icon={icon} tone={tone} size="sm" />
      <div className="flex flex-col-reverse">
        <dt className="text-caption text-muted-foreground">{label}</dt>
        <dd className="text-lg leading-tight font-semibold tabular-nums">{value}</dd>
      </div>
    </div>
  );
}
