import { LinkButton } from "@/components/ui";

export default function NotFound() {
  return (
    <div className="mx-auto max-w-md py-16 text-center">
      <div className="text-6xl font-bold text-slate-300">404</div>
      <h1 className="mt-4 text-xl font-semibold">Không tìm thấy trang</h1>
      <p className="mt-2 text-slate-500">Khóa học có thể chưa được xuất bản hoặc đã bị gỡ.</p>
      <LinkButton href="/" className="mt-6">Về trang chủ</LinkButton>
    </div>
  );
}
