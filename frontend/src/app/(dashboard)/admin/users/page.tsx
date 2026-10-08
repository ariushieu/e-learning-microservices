import type { Metadata } from "next";
import Link from "next/link";
import { UserManagementTable } from "@/components/auth/user-management-table";
import { ErrorAlert } from "@/components/common/error-alert";
import { FormField } from "@/components/common/form-field";
import { NativeSelect } from "@/components/common/native-select";
import { Pagination } from "@/components/common/pagination";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { errorMessage } from "@/lib/errors";
import { gateway, getSession } from "@/lib/server/gateway";
import type { Page, User } from "@/lib/types";
import { DashboardPage } from "@/components/templates/dashboard-page";

export const metadata: Metadata = { title: "Người dùng & quyền" };

export default async function AdminUsersPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const value = (name: string, fallback = "") =>
    typeof params[name] === "string" ? params[name] : fallback;
  const keyword = value("keyword").trim();
  const role = value("role");
  const status = value("status");
  const sort = value("sort", "createdAt,desc");
  const query = new URLSearchParams({
    keyword,
    role,
    status,
    sort,
    page: value("page", "0"),
    size: "12",
  });
  const session = await getSession();
  let users: Page<User> | null = null;
  let error: string | null = null;
  try {
    users = await gateway<Page<User>>(`/api/users?${query}`);
  } catch (cause) {
    error = errorMessage(cause);
  }
  const hrefFor = (page: number) => {
    const next = new URLSearchParams(query);
    next.set("page", String(page));
    return `/admin/users?${next}`;
  };
  return (
    <DashboardPage
      title="Người dùng & quyền"
      description="Tìm tài khoản, quản lý vai trò và quyền truy cập."
      crumbs={[{ href: "/admin", label: "Quản trị" }, { label: "Người dùng & quyền" }]}
    >
      <div className="min-w-0 space-y-5">
        <form
          key={`${keyword}:${role}:${status}:${sort}`}
          action="/admin/users"
          className="grid items-end gap-3 sm:grid-cols-2 xl:grid-cols-[minmax(12rem,1fr)_auto_auto_auto_auto]"
        >
          <FormField id="user-keyword" label="Tìm người dùng">
            <Input
              id="user-keyword"
              name="keyword"
              placeholder="Email hoặc họ tên"
              defaultValue={keyword}
            />
          </FormField>
          <FormField id="user-role" label="Vai trò">
            <NativeSelect id="user-role" name="role" defaultValue={role}>
              <option value="">Tất cả vai trò</option>
              <option value="ROLE_STUDENT">Học viên</option>
              <option value="ROLE_INSTRUCTOR">Giảng viên</option>
              <option value="ROLE_ADMIN">Quản trị viên</option>
            </NativeSelect>
          </FormField>
          <FormField id="user-status" label="Trạng thái">
            <NativeSelect id="user-status" name="status" defaultValue={status}>
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="LOCKED">Đã khóa</option>
              <option value="PENDING">Chờ kích hoạt</option>
            </NativeSelect>
          </FormField>
          <FormField id="user-sort" label="Sắp xếp">
            <NativeSelect id="user-sort" name="sort" defaultValue={sort}>
              <option value="createdAt,desc">Mới nhất</option>
              <option value="createdAt,asc">Cũ nhất</option>
              <option value="email,asc">Email A–Z</option>
              <option value="fullName,asc">Họ tên A–Z</option>
            </NativeSelect>
          </FormField>
          <div className="flex gap-2">
            <Button type="submit">Tìm kiếm</Button>
            <Button asChild variant="ghost">
              <Link href="/admin/users">Xóa lọc</Link>
            </Button>
          </div>
        </form>
        {users && (
          <>
            <p className="text-sm text-muted-foreground" aria-live="polite">
              {users.totalElements} người dùng
              {users.totalPages > 0 && users.page < users.totalPages
                ? ` · Trang ${users.page + 1}/${users.totalPages}`
                : ""}
            </p>
            <UserManagementTable users={users.content} callerId={session?.userId ?? 0} />
            <Pagination page={users.page} totalPages={users.totalPages} hrefFor={hrefFor} />
          </>
        )}
        {error && (
          <>
            <ErrorAlert title="Không tải được người dùng" message={error} />
            <Button asChild variant="outline">
              <Link href="/admin/users">Tải lại danh sách</Link>
            </Button>
          </>
        )}
      </div>
    </DashboardPage>
  );
}
