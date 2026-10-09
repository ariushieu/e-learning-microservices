import {
  ArrowRightIcon,
  AwardIcon,
  BookOpenIcon,
  CalendarDaysIcon,
  ClipboardCheckIcon,
  MailIcon,
  ShieldAlertIcon,
  TrophyIcon,
  type LucideIcon,
} from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { RoleBadges } from "@/components/auth/role-badge";
import { SessionList, type LoginSession } from "@/components/auth/session-list";
import { LoginActivity, type LoginEvent } from "@/components/auth/login-activity";
import { ChangePasswordForm, ProfileDetailsForm } from "@/components/auth/profile-forms";
import { Callout } from "@/components/common/callout";
import { ContourPattern, FullBleed } from "@/components/common/decor";
import { EmptyState } from "@/components/common/empty-state";
import { Fact, FactList } from "@/components/common/fact-list";
import { CertificateTile } from "@/components/enrollment/certificate-tile";
import { getMyCertificates, getMyQuizResults } from "@/components/enrollment/learning-data";
import { InstructorAvatar } from "@/components/home/sections";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { formatDate, formatDay, formatNumber } from "@/lib/format";
import { gateway } from "@/lib/server/gateway";
import type { Enrollment, Page, User } from "@/lib/types";
import { cn } from "@/lib/utils";

export const metadata: Metadata = { title: "Hồ sơ" };

// LABELS dùng ACTIVE = "Đang học" cho ghi danh, nên trạng thái tài khoản cần nhãn riêng.
const ACCOUNT_STATUS: Record<User["status"], string> = {
  ACTIVE: "Đang hoạt động",
  PENDING: "Chờ kích hoạt",
  LOCKED: "Đã khóa",
};

const SECTIONS = [
  { id: "thanh-tich", label: "Thành tích" },
  { id: "thong-tin", label: "Thông tin cá nhân" },
  { id: "change-password", label: "Đổi mật khẩu" },
  { id: "login-sessions", label: "Phiên đăng nhập" },
  { id: "hoat-dong", label: "Hoạt động đăng nhập" },
  { id: "tai-khoan", label: "Tài khoản" },
];

export default async function ProfilePage() {
  const [me, enrollments, sessions, activity] = await Promise.all([
    gateway<User>("/api/auth/me"),
    gateway<Page<Enrollment>>("/api/enrollments?size=100").catch(() => null),
    gateway<LoginSession[]>("/api/auth/sessions").catch(() => null),
    gateway<Page<LoginEvent>>("/api/auth/login-events?page=0&size=10").catch(() => null),
  ]);
  const list = enrollments?.content ?? [];
  const [certificates, quizResults] = await Promise.all([
    enrollments ? getMyCertificates(list).catch(() => null) : null,
    enrollments ? getMyQuizResults(list).catch(() => null) : null,
  ]);
  const completed = list.filter((e) => e.status === "COMPLETED").length;
  const scores = (quizResults ?? []).map((r) => Number(r.attempt.score ?? 0));

  return (
    <>
      <FullBleed className="relative isolate -mt-10 overflow-hidden bg-sidebar text-white" inner="relative pt-12 pb-24">
        <ContourPattern className="-z-10 text-white/[0.07]" />
        <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
          <InstructorAvatar name={me.fullName} className="size-24 text-3xl ring-white/15" />
          <div className="min-w-0 flex-1 space-y-2">
            <p className="text-eyebrow text-sidebar-primary">Hồ sơ của tôi</p>
            <h1 className="truncate text-display text-white">{me.fullName}</h1>
            <div className="flex flex-wrap items-center gap-x-5 gap-y-2 text-sm text-white/80">
              <span className="inline-flex items-center gap-1.5">
                <MailIcon className="size-4" aria-hidden /> {me.email}
              </span>
              <span className="inline-flex items-center gap-1.5">
                <CalendarDaysIcon className="size-4" aria-hidden /> Thành viên từ {formatDay(me.createdAt)}
              </span>
              <RoleBadges roles={me.roles} />
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button asChild variant="secondary" size="lg">
              <Link href="/my-courses">
                Học tập của tôi <ArrowRightIcon />
              </Link>
            </Button>
          </div>
        </div>
      </FullBleed>

      <div className="relative z-10 -mt-14 mb-10 grid grid-cols-2 overflow-hidden rounded-2xl bg-card shadow-raised ring-1 ring-border lg:grid-cols-4">
        <HeroStat icon={BookOpenIcon} value={enrollments ? formatNumber(enrollments.totalElements) : "—"} label="khóa đã ghi danh" />
        <HeroStat icon={TrophyIcon} value={enrollments ? formatNumber(completed) : "—"} label="khóa hoàn thành" className="border-l" />
        <HeroStat icon={AwardIcon} value={certificates ? formatNumber(certificates.length) : "—"} label="chứng chỉ" className="border-t lg:border-t-0 lg:border-l" />
        <HeroStat
          icon={ClipboardCheckIcon}
          value={scores.length ? formatNumber(scores.reduce((a, b) => a + b, 0) / scores.length, 1) : "—"}
          label={scores.length ? `điểm kiểm tra TB (${scores.length} lượt)` : "chưa làm bài kiểm tra"}
          className="border-t border-l lg:border-t-0"
        />
      </div>

      {me.failedLoginsSinceLastSuccess > 0 && (
        <Callout
          className="mb-8"
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

      <div className="grid gap-8 lg:grid-cols-[13rem_minmax(0,1fr)]">
        <nav aria-label="Mục trong hồ sơ" className="hidden lg:block">
          <ul className="sticky top-24 space-y-1 text-sm">
            {SECTIONS.map((s) => (
              <li key={s.id}>
                <a href={`#${s.id}`} className="block rounded-lg px-3 py-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground">
                  {s.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div className="min-w-0 space-y-10">
          <ProfileBlock id="thanh-tich" title="Thành tích" description="Chứng chỉ bạn đã nhận. Mỗi chứng chỉ có mã xác minh công khai.">
            {certificates === null ? (
              <p className="text-sm text-muted-foreground">Không tải được chứng chỉ.</p>
            ) : certificates.length === 0 ? (
              <EmptyState
                icon={AwardIcon}
                title="Chưa có chứng chỉ nào"
                description="Hoàn thành một khóa học để nhận chứng chỉ đầu tiên."
                action={
                  <Button asChild>
                    <Link href="/my-courses">Tiếp tục học</Link>
                  </Button>
                }
              />
            ) : (
              <div className="space-y-4">
                <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
                  {certificates.slice(0, 3).map((c) => (
                    <CertificateTile key={c.id} certificate={c} />
                  ))}
                </div>
                {certificates.length > 3 && (
                  <Button asChild variant="outline">
                    <Link href="/certificates">
                      Xem cả {certificates.length} chứng chỉ <ArrowRightIcon />
                    </Link>
                  </Button>
                )}
              </div>
            )}
          </ProfileBlock>

          <ProfileBlock id="thong-tin" title="Thông tin cá nhân" description="Họ tên và liên hệ hiển thị cho giảng viên của khóa bạn học.">
            <Card>
              <CardContent>
                <ProfileDetailsForm user={me} />
              </CardContent>
            </Card>
          </ProfileBlock>

          <ProfileBlock
            id="change-password"
            title="Đổi mật khẩu"
            description="Dùng mật khẩu hiện tại để đặt mật khẩu mới. Các phiên đăng nhập trên thiết bị khác sẽ cần đăng nhập lại."
          >
            <Card>
              <CardContent>
                <ChangePasswordForm />
              </CardContent>
            </Card>
          </ProfileBlock>

          <ProfileBlock id="login-sessions" title="Phiên đăng nhập" description="Xem thiết bị đã đăng nhập và đăng xuất những phiên bạn không còn sử dụng.">
            <Card>
              <CardContent>
                <SessionList
                  key={sessions?.map((session) => `${session.id}:${session.current}`).join(",") ?? "error"}
                  initial={sessions}
                />
              </CardContent>
            </Card>
          </ProfileBlock>

          <ProfileBlock id="hoat-dong" title="Hoạt động đăng nhập" description="10 lần gần nhất trong 90 ngày. Chỉ bạn xem được lịch sử của mình.">
            <Card>
              <CardContent>
                <LoginActivity initial={activity} />
              </CardContent>
            </Card>
          </ProfileBlock>

          <ProfileBlock
            id="tai-khoan"
            title="Tài khoản"
            description="Cần quyền giảng viên? Gửi mã người dùng cho quản trị viên, rồi đăng nhập lại sau khi được cấp quyền."
          >
            <Card>
              <CardContent>
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
              </CardContent>
            </Card>
          </ProfileBlock>
        </div>
      </div>
    </>
  );
}

function ProfileBlock({ id, title, description, children }: { id: string; title: string; description?: string; children: React.ReactNode }) {
  return (
    <section id={id} className="scroll-mt-24 space-y-4">
      <div className="space-y-1">
        <h2 className="text-heading">{title}</h2>
        {description && <p className="text-sm text-muted-foreground">{description}</p>}
      </div>
      {children}
    </section>
  );
}

function HeroStat({ icon: Icon, value, label, className }: { icon: LucideIcon; value: string; label: string; className?: string }) {
  return (
    <div className={cn("flex items-center gap-4 p-5 sm:p-6", className)}>
      <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-primary-soft text-primary-strong">
        <Icon className="size-5" aria-hidden />
      </span>
      <div className="min-w-0">
        <p className="text-title leading-none tabular-nums">{value}</p>
        <p className="mt-1 text-sm text-muted-foreground">{label}</p>
      </div>
    </div>
  );
}
