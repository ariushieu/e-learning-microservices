# Tình huống test notification-service

Đọc [gateway.md](gateway.md) để có token/fixture, cách đợi sự kiện và ghi kết quả.
Tất cả request qua `{{baseUrl}}`. Collection:
[`docs/postman/notification.postman_collection.json`](../postman/notification.postman_collection.json);
đã chạy trên Docker ngày 07/10/2026: [biên bản](ket-qua/notification.md).
Sáu endpoint giữ nguyên đường dẫn. Mọi tài khoản đã đăng nhập đọc hộp thư của chính mình,
không có vai trò riêng được đọc hộp thư người khác; ADM cũng không có ngoại lệ.
NotificationResponse trả `read` (boolean), không trả status hay userId.

## Dữ liệu trước khi thử

S và B mỗi người ghi danh hoặc nộp quiz để có thông báo riêng. S cần ít nhất hai thông báo,
trong đó một cái chưa đọc; lưu ID của S vào notificationId, của B vào notificationBId.
Lấy ID từ GET /api/notifications bằng đúng token, nhận dạng theo type/title của dữ liệu runId.
Nếu enrollment còn chờ snapshot, dùng quiz. Không đoán ID và không thêm SQL vào bảng.
Trước ca mark-read/unread-count, ghi lại số chưa đọc; dừng các hoạt động tạo sự kiện khác
để biến động của bài test không bị lẫn với thông báo mới.

## NOTIFY-01 — GET /api/notifications

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Danh sách hợp lệ | S | GET /api/notifications?page=0&size=2 | 200; data.content tối đa 2; metadata đúng; mới nhất trước |
| 2 | Không token | — | GET /api/notifications | 401 |
| 3 | Hộp thư người khác | B | GET /api/notifications | 200; không có notificationId của S |
| 4 | Trang rỗng | S | GET /api/notifications?page=999999&size=2 | 200; content=[] |
| 5 | Sort bịa | S | GET /api/notifications?sort=abcxyz | 400; không 500 |
| 6 | Sort đường dẫn thuộc tính bịa | S | GET /api/notifications?sort=user.nonexistent | 400 |
| 7 | Giả userId | B | GET /api/notifications?userId={{studentId}} | 200; vẫn hộp thư B, không có notificationId của S |
| 8 | Admin chỉ xem của mình | ADM | GET /api/notifications | 200; không có thông báo của S/B |
| 9 | Không lộ chi tiết vận hành | S | GET /api/notifications | 200; không retryCount/lastError/tokenHash; read và readAt đúng |

Đây là collection nên không có ca ID không tồn tại=404. Pageable có thể chuẩn hóa page
không hợp lệ; dùng thuộc tính sort bịa để kiểm lỗi truy vấn thay vì giả định page=abc luôn 400.

## NOTIFY-02 — GET /api/notifications/unread-count

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Có thông báo chưa đọc | S | GET /api/notifications/unread-count | 200; data là số nguyên >=0, khớp tổng chưa đọc qua các trang |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Đếm riêng theo người | B | Cùng URL | 200; số của B, không bị gộp S |
| 4 | Không có thông báo | ADM (chưa phát sinh sự kiện) | Cùng URL | 200; data=0 |
| 5 | Token hỏng | — | Cùng URL, Bearer invalid | 401 |
| 6 | Giả danh bằng query | B | GET /api/notifications/unread-count?userId={{studentId}} | 200; vẫn số của B |
| 7 | Sau đọc một thông báo mới | S | Ghi N trước; PATCH notificationId/read; GET lại unread-count | PATCH 200, GET 200; data=N-1 |
| 8 | Đánh dấu lại đã đọc | S | PATCH lại cùng ID; GET unread-count | 200 cho cả hai; số không giảm thêm |

Endpoint không có tham số nghiệp vụ hay Pageable; ca ID sai/404/sort không áp dụng.
Không kết luận S và B dùng chung dữ liệu chỉ vì hai số đếm tình cờ bằng nhau; đối chiếu cả ID hộp thư.

## NOTIFY-03 — PATCH /api/notifications/{id}/read

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Đọc thông báo của mình | S | PATCH /api/notifications/{{notificationId}}/read, không body | 200; data.read=true; readAt có giá trị |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Đọc thông báo của S | B | Cùng URL | 404, không 403; không tiết lộ thông báo có tồn tại |
| 4 | ID không tồn tại | S | PATCH /api/notifications/{{missingId}}/read | 404 |
| 5 | Sai kiểu ID | S | PATCH /api/notifications/abc/read | 400 |
| 6 | Admin đọc hộ | ADM | PATCH /api/notifications/{{notificationId}}/read | 404 |
| 7 | Gọi lặp | S | PATCH lần hai cùng thông báo | 200; readAt giữ thời điểm lần đầu, không tạo bản ghi mới |
| 8 | S dùng ID của B | S | PATCH /api/notifications/{{notificationBId}}/read | 404; B đọc hộp thư vẫn thấy trạng thái cũ |

## NOTIFY-04 — PATCH /api/notifications/read (đọc tất cả)

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Còn N chưa đọc | S | PATCH /api/notifications/read, không body | 200; data=N; message "Đã đánh dấu đã đọc N thông báo"; unread-count sau đó = 0 |
| 2 | Gọi lại khi đã đọc hết | S | PATCH lần hai | 200; data=0 |
| 3 | Không đụng hộp thư người khác | S rồi B | S PATCH /read; B GET unread-count | 200; số của B không đổi |
| 4 | Giả userId | S | PATCH /api/notifications/read?userId={{instructorBId}} | 200; chỉ hộp thư S đổi |
| 5 | Không token | — | Cùng URL | 401 |
| 6 | Giữ readAt cũ | S | Đọc một thông báo, ghi readAt; PATCH /read; GET hộp thư | readAt của thông báo đó không đổi |

## NOTIFY-05 — GET /api/notifications/stream (tức thời)

Postman không đọc được luồng Server-Sent Events; ca 2–5 chạy bằng `curl -N` hoặc trên web.

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Không token / token hỏng | — | GET /api/notifications/stream | 401 |
| 2 | Mở luồng | S | `curl -N -H "Authorization: Bearer {{studentToken}}" {{baseUrl}}/api/notifications/stream` | 200 `text/event-stream`; sự kiện đầu `unread-count` đúng số hiện tại |
| 3 | Có thông báo mới | S | Giữ ca 2, S nộp quiz | Trong vài giây có `notification` (type QUIZ_GRADED, linkUrl `/attempts/{id}`) rồi `unread-count` tăng 1 |
| 4 | Không lọt sang người khác | B | Mở luồng của B, S ghi danh | Luồng B không có `notification` |
| 5 | Đọc ở nơi khác | S | Giữ ca 2, PATCH /read | Luồng nhận `unread-count` 0 |

## NOTIFY-06 — /api/notifications/preferences

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Chưa từng lưu | B | GET /api/notifications/preferences | 200; inAppEnabled=true, emailEnabled=true |
| 2 | Tắt trong ứng dụng | B | PUT body `{"inAppEnabled":false,"emailEnabled":true}` | 200; data đúng body; message "Đã lưu cài đặt thông báo" |
| 3 | Thiếu một cờ | B | PUT body `{"inAppEnabled":false}` | 400 VALIDATION_FAILED |
| 4 | Sai kiểu | B | PUT body `{"inAppEnabled":"maybe","emailEnabled":true}` | 400 |
| 5 | Không token | — | GET và PUT | 401 |
| 6 | Đang tắt thì không tạo | B | Sau ca 2, B ghi danh hoặc nộp quiz; chờ; GET unread-count | 200; số không tăng |
| 7 | Không ảnh hưởng người khác | S | GET preferences của S | 200; vẫn true/true |
| 8 | Bật lại | B | PUT `{"inAppEnabled":true,"emailEnabled":true}` | 200; sự kiện sau đó lại tạo thông báo |

## NOTIFY-EVENT — Kiểm tra luồng sự kiện qua API

Đây là các kịch bản liên service bổ sung, không phải API tạo thông báo mới.
Mọi GET kiểm chứng đều là GET /api/notifications bằng token người nhận, phải trả **200**.
Chờ tối đa theo quy trình gateway.md, đối chiếu type và title chứa tên dữ liệu riêng runId.
Khi đếm, phân trang đến hết hoặc dùng title riêng không trùng; không chỉ nhìn trang đầu.

| # | Tình huống | Tài khoản | Request / thao tác | Mong đợi |
|---|---|---|---|---|
| 1 | enrollment.created | S | POST /api/enrollments 201; GET hộp thư | GET 200; thêm đúng một type=ENROLLMENT_SUCCESS với tên khóa đúng |
| 2 | enrollment.completed | S | Hoàn thành đủ 2 bài bằng PUT progress 200; GET hộp thư | GET 200; thêm đúng một type=COURSE_COMPLETED |
| 3 | certificate.issued | S | Sau ca 2, GET certificate 200 và GET hộp thư | GET 200; type=CERTIFICATE_ISSUED chứa cùng certificateCode |
| 4 | quiz.graded | S | Nộp QUIZ-SUBMIT-50 nhận 200; GET hộp thư | GET 200; đúng một type=QUIZ_GRADED, tên quiz và điểm 50 đúng |
| 5 | Giữ hai số lẻ | S | Nộp quiz đúng 1/2 câu; GET hộp thư | GET 200; content hiển thị 50.00 điểm |
| 6 | Không gửi nhầm người | B | B GET hộp thư sau S ghi danh/nộp quiz riêng | 200; không có thông báo vừa tạo cho S |
| 7 | Replay cùng eventId | S | Trong Kafka UI dev, copy nguyên message quiz.graded đã xử lý, gửi lại đúng topic và payload; GET hộp thư | 200; số thông báo của sự kiện đó không tăng; giữ nguyên eventId, không phát event mới |
| 8 | Loại không tạo thông báo | S | A xuất bản/cập nhật khóa để phát course.updated; GET hộp thư S | 200; không có thông báo mới chỉ vì course.updated |
| 9 | Kafka ngừng rồi hồi phục | S | Stop Kafka trong môi trường riêng, nộp quiz nhận 200; start Kafka; GET hộp thư | 200; thông báo tới một lần khi worker gửi được outbox |
| 10 | Đọc thông báo không tạo sự kiện mới | S | PATCH notificationId/read 200 rồi GET hộp thư | 200; cùng tổng số bản ghi, chỉ read/readAt đổi |
| 11 | Link mở đúng trang | S | Sau ca 1–4, GET hộp thư | linkUrl: ENROLLMENT_SUCCESS `/learn/{courseId}`; COURSE_COMPLETED và CERTIFICATE_ISSUED `/certificates/{enrollmentId}`; QUIZ_GRADED `/attempts/{attemptId}` |

Replay là thao tác qua Kafka UI, **không thể làm bằng endpoint Postman notification**.
Nếu người test không có Kafka UI/quyền vào môi trường dev, đánh dấu ca 7 BLOCKED, không giả lập
bằng hai lần nộp quiz (hai lần nộp là hai nghiệp vụ, không phải cùng eventId).
Tắt Kafka chỉ trong môi trường test riêng, luôn bật lại trước khi rời ca. Không tự sửa
processed_events để tạo trạng thái thử.

## Truy vết nguồn

- [NotificationController](../../notification-service/src/main/java/com/hunre/notificationservice/controller/NotificationController.java).
- [NotificationService](../../notification-service/src/main/java/com/hunre/notificationservice/service/NotificationService.java).
- [NotificationResponse](../../notification-service/src/main/java/com/hunre/notificationservice/dto/NotificationResponse.java).
- [EventProcessor](../../notification-service/src/main/java/com/hunre/notificationservice/consumer/EventProcessor.java),
  [cơ chế thông báo](../notifications.md), [phân công lỗi định dạng điểm](../phan-cong.md).
