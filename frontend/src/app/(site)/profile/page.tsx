import { BookOpenIcon, GraduationCapIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { PageHeader } from "@/components/common/page-header";
import { Stat } from "@/components/common/stat";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { formatDay, initials, label } from "@/lib/format";
import { gateway } from "@/lib/server/gateway";
import type { Enrollment, Page, User } from "@/lib/types";

export const metadata: Metadata = { title: "Hồ sơ" };

export default async function ProfilePage() {
  const [me, enrollments] = await Promise.all([
    gateway<User>("/api/auth/me"),
    gateway<Page<Enrollment>>("/api/enrollments?size=100").catch(() => null),
  ]);
  const completed = enrollments?.content.filter((e) => e.status === "COMPLETED").length ?? 0;

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <PageHeader title="Hồ sơ của tôi" />
      <Card>
        <CardContent className="flex flex-wrap items-center gap-5">
          <Avatar className="size-16">
            <AvatarFallback className="bg-primary/10 text-lg font-semibold text-primary">{initials(me.fullName)}</AvatarFallback>
          </Avatar>
          <div className="min-w-0 flex-1">
            <h2 className="text-lg font-semibold">{me.fullName}</h2>
            <p className="text-muted-foreground">{me.email}</p>
            <div className="mt-2 flex flex-wrap gap-1.5">
              {me.roles.map((r) => (
                <Badge key={r} variant="secondary">
                  {label(r)}
                </Badge>
              ))}
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-4 sm:grid-cols-2">
        <Stat label="Khóa đã ghi danh" value={enrollments?.totalElements ?? 0} icon={BookOpenIcon} />
        <Stat label="Đã hoàn thành" value={completed} icon={GraduationCapIcon} />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Thông tin tài khoản</CardTitle>
          <CardDescription>
            Cần quyền giảng viên? Gửi <b>mã người dùng</b> bên dưới cho quản trị viên, rồi đăng nhập lại sau khi được cấp quyền.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <dl className="grid gap-x-8 gap-y-4 text-sm sm:grid-cols-2">
            <Info label="Mã người dùng" value={`#${me.id}`} />
            <Info label="Số điện thoại" value={me.phone || "Chưa có"} />
            <Info label="Ngày tham gia" value={formatDay(me.createdAt)} />
            <Info label="Trạng thái" value={me.status === "ACTIVE" ? "Đang hoạt động" : me.status} />
          </dl>
        </CardContent>
      </Card>

      <div className="flex justify-end">
        <Button asChild variant="outline" size="lg">
          <Link href="/my-courses">Khóa học của tôi</Link>
        </Button>
      </div>
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4 border-b pb-3 sm:block sm:border-0 sm:pb-0">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="font-medium">{value}</dd>
    </div>
  );
}
