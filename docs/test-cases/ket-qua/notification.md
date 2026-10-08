# Biên bản notification — 07/10/2026

Bản đã kiểm: `main 1873f06` cùng bản sửa `readAt` trong nhánh `test/gateway-notification-collections`
(xem [Lỗi tìm thấy](#lỗi-tìm-thấy-và-đã-sửa)).
Collection: [`docs/postman/notification.postman_collection.json`](../../postman/notification.postman_collection.json).

## Môi trường

- Docker Compose trên Windows 11, đủ MySQL 8, Kafka KRaft, Redis 7, 5 service, gateway. JWT bật,
  `RATE_LIMIT_ENABLED=false` trong lúc chạy collection. Mọi request đi qua `http://localhost:8080`.
- Newman 6.2.2 (`pnpm dlx newman@6.2.2`): **120 request HTTP** (109 request trong file + các lượt đọc
  lại chờ Kafka), **212/212 assertion**, khoảng 25 giây.
- Thông báo do Kafka tạo bất đồng bộ: các request "Chờ ..." đọc lại hộp thư mỗi 2 giây, tối đa 30 giây.
  Không chèn thông báo bằng SQL — mọi thông báo sinh từ ghi danh, nộp bài, hoàn thành khóa thật.
- NOTIFY-05 ca 2–5 (luồng SSE) và NOTIFY-EVENT-7, 9 (Kafka) chạy tay bằng `curl` và lệnh Kafka trong
  container, ghi ở cột bằng chứng.

## Kết quả

**55 ca: 55 PASS.** Không FAIL, không BLOCKED.

| Mã ca | HTTP thực tế | Kết quả | Bằng chứng |
|---|---|---|---|
| NOTIFY-01.1 | 200 | PASS | `size=2`: tối đa 2 phần tử, `page`/`size`/`totalPages` khớp, mới nhất trước |
| NOTIFY-01.2 | 401 | PASS | |
| NOTIFY-01.3 | 200 | PASS | Hộp thư B có `notificationBId`, không có thông báo nào của S |
| NOTIFY-01.4 | 200 | PASS | `content=[]` |
| NOTIFY-01.5 | 400 | PASS | `sort=abcxyz` |
| NOTIFY-01.6 | 400 | PASS | `sort=user.nonexistent` |
| NOTIFY-01.7 | 200 | PASS | `?userId=studentId` bằng token B: vẫn hộp thư B |
| NOTIFY-01.8 | 200 | PASS | Admin không thấy thông báo của S/B |
| NOTIFY-01.9 | 200 | PASS | Mỗi phần tử đúng 8 khóa `id, type, title, content, linkUrl, read, createdAt, readAt`; `readAt` null ⇔ chưa đọc |
| NOTIFY-02.1 | 200 | PASS | Số nguyên ≥ 1, bằng số chưa đọc đếm qua mọi trang |
| NOTIFY-02.2 | 401 | PASS | |
| NOTIFY-02.3 | 200 | PASS | Số của B, khớp hộp thư B |
| NOTIFY-02.4 | 200 | PASS | Khớp hộp thư của chính admin |
| NOTIFY-02.5 | 401 | PASS | |
| NOTIFY-02.6 | 200 | PASS | `?userId=studentId` bằng token B: vẫn số của B |
| NOTIFY-02.7 | 200, 200 | PASS | N → N−1 |
| NOTIFY-02.8 | 200, 200 | PASS | Đọc lại không giảm thêm |
| NOTIFY-03.1 | 200 | PASS | `read=true`, có `readAt` |
| NOTIFY-03.2 | 401 | PASS | |
| NOTIFY-03.3 | 404 | PASS | B đọc thông báo của S |
| NOTIFY-03.4 | 404 | PASS | `missingId` |
| NOTIFY-03.5 | 400 | PASS | `abc` |
| NOTIFY-03.6 | 404 | PASS | Admin đọc hộ |
| NOTIFY-03.7 | 200, 200 | PASS | `readAt` lần hai đúng bằng lần đầu; tổng số thông báo không đổi. Lần chạy đầu FAIL — xem dưới |
| NOTIFY-03.8 | 404, 200 | PASS | Thông báo của B vẫn `read=false`, `readAt=null` |
| NOTIFY-04.1 | 200, 200 | PASS | `data`=N, message "Đã đánh dấu đã đọc N thông báo", số chưa đọc sau đó = 0 |
| NOTIFY-04.2 | 200 | PASS | `data=0` |
| NOTIFY-04.3 | 200 | PASS | Số của B không đổi |
| NOTIFY-04.4 | 200, 200 | PASS | `?userId=instructorBId`: chỉ hộp thư S, số của B không đổi |
| NOTIFY-04.5 | 401 | PASS | |
| NOTIFY-04.6 | 200 | PASS | `readAt` của thông báo đã đọc từ trước giữ nguyên. Lần chạy đầu FAIL — xem dưới |
| NOTIFY-05.1 | 401, 401 | PASS | Không token và token hỏng |
| NOTIFY-05.2 | 200 | PASS | `curl -N`: `Content-Type: text/event-stream`, sự kiện đầu `unread-count` = 3 (đúng số trước đó) |
| NOTIFY-05.3 | — | PASS | S nộp bài: `notification` (QUIZ_GRADED, `linkUrl=/attempts/88`) rồi `unread-count` 4 |
| NOTIFY-05.4 | — | PASS | Luồng B mở cùng lúc chỉ có `unread-count` của B và `:ping`, không có `notification` |
| NOTIFY-05.5 | — | PASS | `PATCH /read` từ nơi khác: luồng S nhận `unread-count` 0 |
| NOTIFY-06.1 | 200 | PASS | `true/true` |
| NOTIFY-06.2 | 200 | PASS | Đúng body, message "Đã lưu cài đặt thông báo" |
| NOTIFY-06.3 | 400 | PASS | `VALIDATION_FAILED` |
| NOTIFY-06.4 | 400 | PASS | `"maybe"` |
| NOTIFY-06.5 | 401, 401 | PASS | GET và PUT |
| NOTIFY-06.6 | 200 | PASS | B nộp bài khi đang tắt, S nộp ngay sau làm mốc; thông báo của S tới rồi chờ thêm 3 giây: tổng của B không đổi |
| NOTIFY-06.7 | 200 | PASS | S vẫn `true/true` |
| NOTIFY-06.8 | 200 | PASS | Bật lại, B nộp bài: đúng một thông báo mới cho lượt đó |
| NOTIFY-EVENT-1 | 200 | PASS | Đúng một `ENROLLMENT_SUCCESS` có tên khóa |
| NOTIFY-EVENT-2 | 200 | PASS | Học xong 2/2 bài: đúng một `COURSE_COMPLETED` |
| NOTIFY-EVENT-3 | 200 | PASS | Đúng một `CERTIFICATE_ISSUED`, nội dung chứa đúng `certificateCode` |
| NOTIFY-EVENT-4 | 200 | PASS | Đúng một `QUIZ_GRADED` cho lượt, có tên quiz |
| NOTIFY-EVENT-5 | 200 | PASS | Nội dung có "50.00 điểm" — nhãn [CHỜ SỬA payload quiz] đã hết |
| NOTIFY-EVENT-6 | 200 | PASS | Hộp thư B không có link lượt làm bài / chứng chỉ của S; B có đúng một thông báo ghi danh của chính mình |
| NOTIFY-EVENT-7 | — | PASS | Chạy tay: gửi lại nguyên văn message `elearning.quiz.events` của lượt 86 (cùng key, cùng eventId): số thông báo `/attempts/86` vẫn 1; log báo trùng khóa `processed_events` |
| NOTIFY-EVENT-8 | 200, 200 | PASS | A sửa khóa (`course.updated`), chờ 5 giây: tổng thông báo của S không đổi |
| NOTIFY-EVENT-9 | 200 | PASS | Chạy tay: dừng Kafka, S nộp lượt 87 (200), 5 giây sau chưa có thông báo; bật Kafka: thông báo tới sau ~2 giây, 10 giây sau vẫn đúng 1 |
| NOTIFY-EVENT-10 | 200, 200 | PASS | Đọc một thông báo, chờ 3 giây: tổng không đổi |
| NOTIFY-EVENT-11 | 200 | PASS | `/learn/{courseId}`, `/certificates/{enrollmentId}` (hoàn thành và chứng chỉ), `/attempts/{attemptId}` |

## Lỗi tìm thấy và đã sửa

**NOTIFY-03.7 và NOTIFY-04.6 FAIL ở lần chạy đầu:** `PATCH /{id}/read` trả `readAt` dạng
`...31.803925661Z` (nano giây), còn mọi lần đọc sau trả `...31.803926Z`. Cột `read_at` là
`DATETIME(6)`, MySQL **làm tròn** phần nano giây khi lưu, nên giá trị trả lần đầu khác giá trị
đã lưu. Sửa trong cùng pull request: `Notification.now()` cắt thời điểm còn micro giây, dùng cho
`markRead()` và `markAllRead`; thêm `NotificationTest`. Dựng lại image, chạy lại cả collection:
212/212.

`createdAt` không bị: Hibernate tự cắt `@CreationTimestamp` theo độ chính xác của cột (đã đối chiếu
`createdAt` trong luồng SSE với `GET /api/notifications`: trùng nhau).

## Ghi chú

- Hộp thư admin không phải lúc nào cũng trống (collection quiz cho admin làm thử bài, sinh
  `QUIZ_GRADED` cho admin), nên NOTIFY-02.4 kiểm số đếm khớp hộp thư của chính admin thay vì cố định 0.
- Sau lượt chạy, dữ liệu QA đã được xóa về trạng thái trước khi chạy; cài đặt thông báo của B được
  bật lại ở NOTIFY-06.8 (và ở SETUP-09 nếu lượt trước dừng giữa chừng).
