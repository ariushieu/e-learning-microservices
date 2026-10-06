import type { Metadata } from "next";
import { Badge, Card, LinkButton, PageTitle, formatDate } from "@/components/ui";
import { gateway } from "@/lib/server/gateway";
import type { Enrollment, Page, User } from "@/lib/types";

export const metadata: Metadata = { title: "Hồ sơ" };

const ROLE_LABELS: Record<string, string> = {
  ROLE_STUDENT: "Học viên",
  ROLE_INSTRUCTOR: "Giảng viên",
  ROLE_ADMIN: "Quản trị viên",
};

export default async function ProfilePage() {
  const [me, enrollments] = await Promise.all([
    gateway<User>("/api/auth/me"),
    gateway<Page<Enrollment>>("/api/enrollments?size=100").catch(() => null),
  ]);
  const completed = enrollments?.content.filter((e) => e.status === "COMPLETED").length ?? 0;

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <PageTitle title="Hồ sơ của tôi" />
      <Card>
        <div className="flex items-center gap-5">
          <div className="grid h-16 w-16 place-items-center rounded-full bg-indigo-600 text-2xl font-bold text-white">
            {me.fullName.charAt(0).toUpperCase()}
          </div>
          <div>
            <h2 className="text-xl font-semibold">{me.fullName}</h2>
            <p className="text-slate-500">{me.email}</p>
            <div className="mt-2 flex flex-wrap gap-2">
              {me.roles.map((r) => (
                <Badge key={r} value={ROLE_LABELS[r] ?? r} />
              ))}
            </div>
          </div>
        </div>
        <dl className="mt-6 grid grid-cols-2 gap-4 text-sm sm:grid-cols-4">
          <Info label="Mã người dùng" value={`#${me.id}`} />
          <Info label="Điện thoại" value={me.phone || "—"} />
          <Info label="Tham gia" value={formatDate(me.createdAt)} />
          <Info label="Trạng thái" value={me.status === "ACTIVE" ? "Đang hoạt động" : me.status} />
        </dl>
        <p className="mt-4 text-xs text-slate-500">
          Cần quyền giảng viên? Gửi mã người dùng <b>#{me.id}</b> cho quản trị viên, rồi đăng nhập lại sau khi được cấp quyền.
        </p>
      </Card>
      <Card>
        <h3 className="font-semibold">Học tập</h3>
        <div className="mt-3 flex items-center gap-8 text-sm">
          <Info label="Khóa đã ghi danh" value={String(enrollments?.totalElements ?? 0)} />
          <Info label="Đã hoàn thành" value={String(completed)} />
          <LinkButton href="/my-courses" variant="secondary" className="ml-auto">
            Khóa của tôi
          </LinkButton>
        </div>
      </Card>
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-slate-500">{label}</dt>
      <dd className="font-medium text-slate-900">{value}</dd>
    </div>
  );
}
