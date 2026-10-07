# Kiểm chứng đánh giá khóa học

Ngày 07/10/2026, mã nguồn `5c31231d7d85f828d05b098cf3b475bd397fb352`.
Backend JAR qua gateway 8080, MySQL 8 và Kafka 4.2.1 thật trên Windows;
frontend Next.js production, Edge headless qua Playwright. JWT bật. Gateway tắt rate limit
trong phiên QA, auth dùng DB dev đã khởi tạo và tắt Flyway local. Máy không có Docker.

## Kết quả

- Maven `clean verify` toàn bộ 7 module: **708 test, 0 failure/error/skipped**.
  Riêng tính năng mới: 18 ca API tích hợp và 3 ca đồng thời.
- Collection: **220 mã ca trong course.md + 16 ca hồi quy**, 854 HTTP request tính cả bước phụ,
  **778/778 assertion đạt**. Xem [biên bản API](course.md) và [JSON](course-evidence.json).
- 25 ca `COURSE-25.1`–`.25`: tạo 5 sao → 5/1; thêm 3 sao → 4/2; sửa/xóa tính lại đúng;
  bỏ lượt cuối → 0/0; không lộ email; người chưa ghi danh và giả danh bị chặn;
  sai dữ liệu không làm đổi thống kê. Hai người ghi đồng thời và sáu PUT cùng người qua
  gateway trên MySQL đều trả thành công, không nhân đôi bản ghi hoặc sai trung bình.
- Test H2 thêm tình huống 12 người đánh giá cùng lúc với PUT khóa học, cùng người upsert 12 lần,
  xóa/sửa xen kẽ, làm tròn 14/3 → 4.67, giới hạn 2000 ký tự và quyền trên khóa nháp.
- Flyway áp dụng **V5 thành công** trên MySQL; Hibernate `ddl-auto=validate` khởi động thành công.
  Cột `author_name VARCHAR(150) NULL`; `rating TINYINT`, `comment VARCHAR(2000)`, thời gian DATETIME(6).
  Các migration V1–V4 không thay đổi.
- Giao diện đánh giá: **16/16 kiểm tra đạt**, không lỗi JavaScript hoặc lỗi dọn fixture.
- Chạy lại script trang chủ `check-course-catalog.cjs`: **21/21 đạt**.
- Next typegen, TypeScript, ESLint và production build: đạt.

## Giao diện đã thử

Khách thấy danh sách/trạng thái rỗng và lời mời đăng nhập. Chủ khóa chưa ghi danh không có form.
Học viên chọn sao, gửi nhận xét, sửa điểm và xóa bằng hộp xác nhận. Form của mình vẫn tải đúng
khi nhận xét đã nằm ở trang hai. Xóa lượt cuối trên trang hai đưa về trang đầu, không bị kẹt ở trang rỗng.
Lỗi API giữ nội dung đang viết. Nhận xét chứa thẻ script hiển thị như văn bản, không thực thi.
Kiểm tra 375/768/1366px không tràn ngang; thẻ khóa học công khai cập nhật điểm và số lượt.

- [Danh sách kiểm tra tự động](course-reviews/review-ui-results.json)
- [Điện thoại 375px](course-reviews/reviews-375.png)
- [Máy tính bảng 768px](course-reviews/reviews-768.png)
- [Máy tính 1366px](course-reviews/reviews-1366.png)

## Chạy lại

```bash
./mvnw -pl shared-common,api-gateway,auth-service,course-service,enrollment-service,quiz-service,notification-service clean verify
node scripts/build-course-collection.cjs
pnpm dlx newman@6.2.2 run docs/postman/course.postman_collection.json --timeout-script 65000 --timeout-request 10000
node scripts/check-course-reviews.cjs
node scripts/check-course-catalog.cjs
```

Script trình duyệt cần Playwright; `COURSE_PLAYWRIGHT_MODULE` cho phép chỉ định module đã cài
ở thư mục công cụ riêng, `COURSE_BROWSER_CHANNEL=msedge` dùng Edge có sẵn. Có thể đổi
`COURSE_GATEWAY_URL`, `COURSE_WEB_URL`, `COURSE_UI_OUTPUT`, `COURSE_QA_PASSWORD`.
Chạy tuần tự collection và các script UI vì cùng tạo dữ liệu QA.

Script review tạo khóa, tài khoản và ghi danh qua API; xóa các đánh giá do mình tạo rồi lưu trữ
khóa ở cuối. Khóa đã có học viên lịch sử không xóa bằng SQL để vượt ràng buộc nghiệp vụ.
Fixture Postman giữ để demo. Bằng chứng không chứa mật khẩu, token hay response đăng nhập.

## Lỗi tìm được trong lúc phát triển

Ca điểm lẻ 2.5 ban đầu trả 200 vì JSON tự ép sang Integer. Đã đổi DTO sang kiểm tra
BigDecimal với 0 chữ số thập phân; kiểm lại trả 400, không ghi dữ liệu. Luồng xóa cũng đặt lại
trang nhận xét để tránh trang cuối trở thành trang rỗng sau khi xóa.
