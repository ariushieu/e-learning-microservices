import { redirect } from "next/navigation";

/** Quản trị chưa có trang tổng quan riêng: vào thẳng trang người dùng & quyền. */
export default function AdminIndex() {
  redirect("/admin/users");
}
