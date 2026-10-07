import { Brand } from "@/components/layout/brand";
import { StatusPage } from "@/components/common/status-page";

// notFound() ở bất kỳ trang nào cũng rơi về đây, ngoài layout (site) nên không có header: tự đặt logo để quay về.
export default function NotFound() {
  return (
    <div className="flex min-h-svh flex-col p-6 sm:p-10">
      <Brand />
      <div className="flex flex-1 items-center justify-center">
        <StatusPage code="404" title="Không tìm thấy trang" description="Khóa học có thể chưa được xuất bản hoặc đã bị gỡ." />
      </div>
    </div>
  );
}
