# Biên bản demo-flow — 07/10/2026

Bản đã kiểm: `main 1873f06` cùng nhánh `test/gateway-notification-collections`.
Collection: [`docs/postman/demo-flow.postman_collection.json`](../../postman/demo-flow.postman_collection.json).

Một vòng xuyên 5 service đúng kịch bản demo, mỗi bước kiểm cả phần đồng bộ qua Kafka. Dùng để chạy
thử **trên máy mình** trước buổi demo, xem cả hệ thống còn thông không. Không chạy trên máy demo ngay
trước giờ demo — collection tạo khóa, quiz, chứng chỉ thật.

## Môi trường

Docker Compose trên Windows 11 (MySQL 8, Kafka KRaft, Redis 7, 5 service, gateway), JWT bật,
`RATE_LIMIT_ENABLED=false`. Newman 6.2.2: **47 request HTTP, 81/81 assertion**, 11,5 giây.

## Các bước

| Bước | Service | Kiểm | Kết quả |
|---|---|---|---|
| 0. Tài khoản | auth | Admin đăng nhập, đăng ký A/B/S, cấp vai trò bằng API, đăng nhập lại có vai trò mới | PASS |
| DEMO-01 → 02 | course | Admin tạo danh mục; giảng viên A tạo khóa, trạng thái `DRAFT`, `instructorId` = A | PASS |
| DEMO-03 | course | Khách xem khóa nháp: 404 | PASS |
| DEMO-04 → 07 | course | Thêm chương, bài học thử, bài thường; xuất bản → `PUBLISHED` | PASS |
| DEMO-08 → 09 | course | Khách thấy khóa khi lọc theo danh mục và xem được chi tiết | PASS |
| DEMO-10 | quiz | A tạo quiz 2 câu, xuất bản | PASS |
| DEMO-11 | enrollment | S ghi danh (đọc lại tới khi enrollment-service có snapshot khóa từ `course.updated`) | PASS |
| DEMO-12 | notification | Thông báo `ENROLLMENT_SUCCESS`, link `/learn/{courseId}` | PASS |
| DEMO-13 → 15 | enrollment | Khóa của tôi `ACTIVE` 0%; học xong bài 1 → 50% | PASS |
| DEMO-16 | quiz | Lấy đề: không có `isCorrect`, không có lời giải | PASS |
| DEMO-17 → 18 | quiz | Làm đúng cả hai câu: 100 điểm, đạt; xem kết quả kèm đáp án | PASS |
| DEMO-19 | notification | Thông báo `QUIZ_GRADED`, link `/attempts/{attemptId}` | PASS |
| DEMO-20 → 22 | enrollment | Học xong bài 2 → `COMPLETED` 100%, có `completedAt`; nhận chứng chỉ | PASS |
| DEMO-23 | notification | `COURSE_COMPLETED` và `CERTIFICATE_ISSUED` (chứa mã chứng chỉ), cùng link `/certificates/{enrollmentId}` | PASS |
| DEMO-24 | enrollment | Khách không đăng nhập xác minh mã: đúng mã, tên khóa, tên học viên | PASS |
| DEMO-25 | course | Khóa đếm 1 học viên (course-service nhận `enrollment.created`) | PASS |
| DEMO-26 → 27 | notification | Đọc tất cả (≥ 4 thông báo), số chưa đọc về 0 | PASS |

Sau lượt chạy, dữ liệu QA đã được xóa về trạng thái trước khi chạy.
