# Kiểm thử đánh giá chờ phản hồi

Ngày 08/10/2026, mã nguồn `327ef56355323d460a902dd4d0dfc3d1a0b1e556`, dựa trên
`main` tại `53bb8f6`. Gồm nhiệm vụ inbox trong `docs/phan-cong.md` và ba góp ý của #84.

## Môi trường và kết quả

- Windows, Java 21 biên dịch release 17; MySQL 8 và Kafka 4.2.1 chạy trên máy.
  Các service chạy JAR, gọi API qua gateway **8080**, JWT bật. Course dùng Flyway
  và Hibernate validate; auth dùng DB dev có sẵn, tắt Flyway khi chạy local.
- Web **Next.js production**, `next build` rồi `next start`, trình duyệt Edge qua Playwright.
  Type generation, TypeScript, ESLint và production build đạt.
- Clean verify cả 7 module Maven: **972 test, 0 failure, 0 error, 0 skipped**.
  `InstructorReviewIntegrationTest` có 4 test tích hợp nhiều bước, gồm JWT, quyền,
  phân trang, thứ tự khi trùng thời gian, khóa DRAFT/ARCHIVED và nhãn phản hồi.
- Collection Newman 6.2.2: **298 mã ca gốc + 16 ca bổ sung**, 329 request chính,
  **1.075 HTTP, 1.028/1.028 assertion đạt**, không lỗi script.
  Chi tiết HTTP từng ca: [biên bản](course.md), [JSON đã lọc bí mật](course-evidence.json).
- Trình duyệt: **20/20 kiểm tra đạt**, không `pageerror`.
  [Danh sách kiểm tra](course-inbox/inbox-ui-results.json),
  [desktop](course-inbox/inbox-desktop.png), [375px](course-inbox/inbox-mobile.png).
- Sinh lại collection từ `scripts/build-course-collection.cjs` khớp SHA-256 file đã commit.

## Các tình huống đã thao tác trên web

| Tình huống | Kết quả thực tế |
|---|---|
| A có hai khóa, mỗi khóa một nhận xét | Hai thẻ mới nhất trước, số chờ và sidebar đều 2 |
| Lọc một khóa | Trang đếm 1, sidebar vẫn tổng 2 |
| Lỗi chỉ khoảng trắng, sau đó nhập đúng | Lỗi mất và bộ đếm ký tự hiện lại ngay, chưa cần bấm Lưu |
| Giả lập HTTP 503 khi lưu | Báo lỗi, giữ bản nháp và số chờ |
| Lưu phản hồi tại chỗ | Thẻ rời bộ lọc chưa trả lời; số chờ, sidebar và tổng quan thành 1 |
| Bộ lọc đã trả lời/tất cả | Hiện đúng các dòng tương ứng; đếm chờ độc lập với bộ lọc |
| Admin phản hồi | Inbox và trang công khai dùng nhãn quản trị viên; không lộ repliedBy |
| Phản hồi chứa HTML | Hiện chữ nguyên văn, không chạy script |
| Xóa phản hồi | Có xác nhận; Giữ lại không đổi; xác nhận xóa đưa số chờ/sidebar về 2 |
| B xem hoặc lọc khóa A | Không thấy thẻ/tùy chọn khóa A; lọc ID khóa A hiện lỗi quyền |
| Học viên/khách truy cập khu giảng dạy | Chuyển tới trang không có quyền/đăng nhập |
| Trang cuối còn một dòng, trả lời dòng đó | Tự về trang hợp lệ, giữ courseId/replied, số chờ đúng |
| 375px, 768px, 1366px | Trang và form không tràn ngang |

## Phạm vi và giới hạn

Máy này không có Docker; không coi kết quả local là kiểm thử đầy đủ stack Docker/Redis.
Rate limit tắt trong đợt chạy hàng loạt và đã khởi động lại gateway với cấu hình mặc định.
Fixture UI dùng tài khoản/khóa riêng; đã xóa các đánh giá và lưu trữ khóa do script tạo.
Tài khoản thử vẫn tồn tại; không xóa dữ liệu chung hoặc tài khoản người dùng thật.

Lần khởi động đầu, Kafka chưa gán partition nên fixture ghi danh trả 404; chạy lại sau
khi consumer ổn định. Một lần script UI đọc text khi trang còn đang stream: sửa script
chờ phần tử hiển thị. Lần chạy cuối đạt toàn bộ 20 kiểm tra nêu trên.

`unrepliedCount` trong trang theo phạm vi khóa đã chọn; sidebar/tổng quan đếm mọi khóa
được quản lý. `updatedAt` thay đổi cả khi phản hồi: không dùng để đánh dấu học viên sửa
nhận xét. Hợp đồng chi tiết ở [README course-service](../../../course-service/README.md).

## Chạy lại

```powershell
.\mvnw.cmd -pl shared-common,api-gateway,auth-service,course-service,enrollment-service,quiz-service,notification-service clean verify
pnpm dlx newman@6.2.2 run docs/postman/course.postman_collection.json --timeout-script 65000 --timeout-request 10000
$env:COURSE_PLAYWRIGHT_MODULE='<duong-dan-toi-playwright>'
$env:COURSE_BROWSER_CHANNEL='msedge'
node scripts/check-course-inbox.cjs
```

Khởi động MySQL, Kafka, các service và frontend production trước khi chạy hai lệnh
kiểm API/UI. Chỉ xuất JSON kết quả UI và response nghiệp vụ đã lọc; không commit báo
cáo Newman gốc vì nó chứa token.
