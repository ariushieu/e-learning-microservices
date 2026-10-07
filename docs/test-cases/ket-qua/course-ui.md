# Kiểm tra trang chủ và hồi quy course

Ngày 07/10/2026, commit `b8ce705b0ff953484387568df2a034fd657398e2`.
Frontend production Next.js 16.3.8, Edge headless qua Playwright; dữ liệu tạo qua gateway 8080.

**21 kiểm tra đạt**, không có lỗi JavaScript. Script tạo 16 khóa trong 2 danh mục gốc và 1 danh mục con riêng, có
khác biệt về giá/trình độ và đủ dữ liệu cho 2 trang; dọn đúng ID đã tạo sau khi chạy.

- Chip danh mục giữ từ khóa, trình độ, sort; đặt lại trang đầu và đồng bộ select.
- Giá tăng/giảm, mới nhất, số học viên: đúng thứ tự; dùng `id,desc` làm tiêu chí phụ khi bằng nhau.
  Collection Postman kiểm riêng số học viên với fixture có giá trị khác nhau, không chỉ toàn số 0.
- Trang 2 giữ bộ lọc và sort; đổi sort trở về trang đầu.
- Tìm kiếm vẫn giữ danh mục, trình độ, sort.
- Danh mục cha lấy cả khóa con trên hai trang; chọn danh mục con chỉ trả khóa con, tô chip cha và giữ trạng thái sau tải lại.
- Test JPA tái hiện trước sửa: 2/4 ca thất bại (thiếu khóa con, sai tổng phân trang). Sau sửa: 4/4 đạt.
- 375px, 768px, 1366px: trang không tràn ngang, hàng chip cuộn ngang.
- Chip “Tất cả” cập nhật select sau điều hướng client; Tab đi tiếp được tới chip kế bên.
- Có trạng thái không tìm thấy kết quả; sort lạ trên URL giao diện trở về Mới nhất.
  API nhận trường sort lạ vẫn phải 400 (đã kiểm trong collection).

[Danh sách kiểm tra máy ghi](course-ui/catalog-ui-results.json).
Ảnh: [375px](course-ui/catalog-375.png), [768px](course-ui/catalog-768.png),
[1366px](course-ui/catalog-1366.png).

## Kiểm tra build

- Next route typegen, TypeScript `--noEmit`, ESLint và production build: đạt.
- Maven `clean verify` cho toàn bộ 7 module: **667 test, 0 failure, 0 error, 0 skipped**.
  Chạy rõ danh sách module để giữ báo cáo/runtime đang mở trong `target/` ở root:

```bash
./mvnw -pl shared-common,api-gateway,auth-service,course-service,enrollment-service,quiz-service,notification-service clean verify
```

Test Maven có H2 và Kafka nhúng; kiểm API có MySQL/Kafka thật trên máy, chi tiết ở
[biên bản API](course.md). Không gọi kết quả local này là kiểm thử Docker.

## Chạy lại giao diện

Backend phải đang chạy; import collection và chạy “0. Chuẩn bị” để có tài khoản QA.
Trong `frontend/` chạy `pnpm next typegen`, `pnpm tsc --noEmit`, `pnpm lint`, `pnpm build`,
rồi `pnpm start`. Mở `/design` để đối chiếu khung chung.

Script trình duyệt: `node scripts/check-course-catalog.cjs`, cần Playwright và Chromium/Edge.
Có thể cài Playwright ở thư mục công cụ riêng rồi đặt `COURSE_PLAYWRIGHT_MODULE` bằng đường dẫn
module tuyệt đối. `COURSE_BROWSER_CHANNEL=msedge` dùng Edge đã cài; nếu không đặt thì dùng Chromium
của Playwright. Đổi `COURSE_GATEWAY_URL`, `COURSE_WEB_URL`, `COURSE_UI_OUTPUT`, `COURSE_QA_PASSWORD`
nếu môi trường khác. Không chạy đồng thời script UI với collection vì cả hai tạo dữ liệu QA.

Chọn giá trị trong ô Sắp xếp rồi bấm **Lọc**; form tìm kiếm cũng gửi toàn bộ bộ lọc đang chọn.
Bản sửa bổ sung lọc danh mục cha/con trong course-service; không đổi schema.
