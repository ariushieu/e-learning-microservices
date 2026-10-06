import { LinkButton } from "@/components/ui";

export default function ForbiddenPage() {
  return (
    <div className="mx-auto max-w-md py-16 text-center">
      <div className="text-6xl font-bold text-slate-300">403</div>
      <h1 className="mt-4 text-xl font-semibold">Bạn không có quyền vào trang này</h1>
      <p className="mt-2 text-slate-500">Trang này dành cho giảng viên hoặc quản trị viên.</p>
      <LinkButton href="/" className="mt-6">Về trang chủ</LinkButton>
    </div>
  );
}
