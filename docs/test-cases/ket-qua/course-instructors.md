# Kiểm thử hồ sơ giảng viên công khai

Ngày 08/10/2026. Backend/collection tại `389fe4c`; giao diện cuối tại `4b6716c`
(chỉ chuyển khối sao khỏi vùng ép kích thước icon và thêm kiểm tra nửa sao).
Nền `main`: `7da5a12`, phân công mục trang giảng viên công khai.

## Kết quả

| Phần | Kết quả | Bằng chứng |
|---|---|---|
| Clean verify 7 module Maven | **1.014 test, 0 lỗi, 0 bỏ qua** | Bao gồm 5 test mới trong `InstructorProfileIntegrationTest` |
| Next.js production | **PASS** typegen, TypeScript, ESLint, build | Build lại sau chỉnh kích thước sao |
| Collection course qua gateway 8080 | **1.078/1.078 assertion**, 1.154 HTTP, không lỗi script | [Biên bản](course.md), [JSON](course-evidence.json) |
| Hồ sơ giảng viên trên Edge | **18/18 PASS** | [JSON](course-instructors/instructor-ui-results.json) |
| Tắt course-service | **2/2 PASS** | [JSON](course-instructors/instructor-outage-results.json) |
| Hồi quy trang chủ sau sửa thẻ khóa | **21/21 PASS** | [JSON](course-instructors/catalog-ui-results.json) |

Collection chạy 317 mã ca gốc + 16 ca bổ sung, 348 request chính. Sinh lại bằng
`scripts/build-course-collection.cjs` khớp SHA-256 file đã commit. Không sửa kỳ vọng
để chấp nhận khóa nháp hoặc điểm trung bình cộng sai.

## Các điểm đã kiểm tra

- Fixture A có hai khóa PUBLISHED (2 lượt 5 sao và 1 lượt 2 sao), một DRAFT và một
  ARCHIVED có đánh giá. Hồ sơ trả **2 khóa, 3 học viên, 3 lượt, điểm 4.00**.
  Người học ở hai khóa được tính ở từng khóa theo định nghĩa `totalStudents`.
- Chủ khóa, admin và học viên xem trang hồ sơ đều chỉ thấy hai khóa PUBLISHED.
  Frontend gọi API danh sách bằng quyền khách; không làm mất quyền xem khóa nháp
  của khu quản lý dùng token.
- Không có khóa công khai, ID không tồn tại: 404; ID sai kiểu: 400 ở API, trang
  404 trên web. Xuất bản khóa đầu tiên không có đánh giá: 0 học viên, điểm/lượt 0.
- Thêm khóa chưa đánh giá không kéo điểm xuống. Sửa/xóa đánh giá hoặc lưu trữ khóa
  cập nhật thống kê; không còn khóa công khai trả 404 cả khi chủ khóa gọi API.
- Tổng học viên trên 32 bit, điểm có phần thập phân, tên null/trắng được kiểm bằng
  test tích hợp. Tên lấy từ snapshot khóa công khai có ID lớn nhất.
- Tên giảng viên trên thẻ và trang chi tiết mở đúng hồ sơ; không có `<a>` lồng nhau.
  Bấm ảnh bìa vẫn mở khóa học. Bộ lọc/sắp xếp/chip danh mục trang chủ vẫn hoạt động.
- Với 13 khóa: trang đầu 12, trang sau 1; không trùng, tải lại giữ trang, vượt tổng
  quay về trang cuối hợp lệ. Số liệu giảng viên không phụ thuộc trang đang xem.
- Không tràn ngang ở 375, 768, 1366px; [ảnh desktop](course-instructors/instructor-1366.png),
  [ảnh 375px](course-instructors/instructor-375.png).
- Điểm 4,5 có nửa sao thật: đo chiều rộng phần tô bằng 50% sao; SVG và khung có cùng
  kích thước. Nhãn đọc màn hình và số hiển thị đều dùng dấu phẩy.
- Tắt course-service thật: gateway 502, trang báo “Không tải được hồ sơ”, không
  chuyển thành 404/đăng nhập. Đã khởi động lại service sau ca thử.

## Môi trường và giới hạn

Windows, Java 21 biên dịch release 17, MySQL 8, Kafka 4.2.1 trên máy; sáu service
chạy JAR, frontend `next start`, Edge qua Playwright. Course bật Flyway và Hibernate
validate, JWT bật. Auth dùng DB dev sẵn có, tắt Flyway local. Không có Docker trên
máy này, không coi đây là kết quả tự chạy full stack Docker/Redis.

Rate limit tắt trong lượt thử hàng loạt, đã restart gateway với mặc định sau đó.
Fixture UI riêng: đánh giá đã xóa, khóa đã lưu trữ; tài khoản thử vẫn tồn tại.
Collection giữ fixture để demo theo quy ước. Không xóa dữ liệu người dùng thật.
Không commit báo cáo Newman gốc có token; JSON bằng chứng chỉ chứa response nghiệp vụ.

Điểm tổng tính từ `rating_avg` (đã lưu hai chữ số) và `rating_count` của từng khóa;
tên là snapshot course-service, không đồng bộ tên mới từ auth. Hợp đồng ở
[README course-service](../../../course-service/README.md).

## Chạy lại

```powershell
.\mvnw.cmd -pl shared-common,api-gateway,auth-service,course-service,enrollment-service,quiz-service,notification-service clean verify
pnpm dlx newman@6.2.2 run docs/postman/course.postman_collection.json --timeout-script 65000 --timeout-request 10000
$env:COURSE_PLAYWRIGHT_MODULE='<duong-dan-toi-playwright>'
$env:COURSE_BROWSER_CHANNEL='msedge'
node scripts/check-course-instructors.cjs
node scripts/check-course-catalog.cjs
```

Chạy backend/frontend production trước các script API/UI. Ca outage: dừng riêng
course-service trong môi trường thử, đặt `COURSE_PROFILE_OUTAGE_ID` bằng ID số rồi
chạy lại script hồ sơ; khởi động lại service và bỏ biến này sau khi thử.
