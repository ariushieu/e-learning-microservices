# Thông báo

notification-service nhận sự kiện Kafka từ các service khác và dựng thông báo cho người
dùng, trong ứng dụng và qua email. Nó không gọi service nào và không service nào gọi nó —
chỉ nghe sự kiện.

- [Luồng đi của một thông báo](#luồng-đi-của-một-thông-báo)
- [Một sự kiện chỉ tạo đúng một thông báo](#một-sự-kiện-chỉ-tạo-đúng-một-thông-báo)
- [Khi xử lý sự kiện bị lỗi](#khi-xử-lý-sự-kiện-bị-lỗi)
- [Mẫu thông báo](#mẫu-thông-báo)
- [API](#api)
- [Thông báo tức thời](#thông-báo-tức-thời)
- [Vì sao đọc chuỗi thô thay vì để Spring chuyển đổi sẵn](#vì-sao-đọc-chuỗi-thô-thay-vì-để-spring-chuyển-đổi-sẵn)
- [Email](#email)
- [Chạy thử ở máy mình](#chạy-thử-ở-máy-mình)
- [Những chỗ còn thiếu](#những-chỗ-còn-thiếu)

## Luồng đi của một thông báo

```
quiz-service                     Kafka                    notification-service
     │                             │                              │
  chấm bài ──► publish ───────────►│                              │
                                   │──────► elearning.quiz.events ┤
                                   │                              │
                                   │            ghi processed_events (khử trùng lặp)
                                   │            tìm mẫu QUIZ_GRADED
                                   │            điền {quizTitle}, {score}
                                   │            lưu vào notifications
                                   │                              │
                                   │            sau commit: phát qua Redis ──► chuông trên web
                                   │                              │
                                   │       học viên gọi GET /api/notifications
```

Sự kiện nào sinh ra thông báo nào:

| Sự kiện | Topic | Mã mẫu | Bấm vào mở (`link_url`) |
|---|---|---|---|
| `enrollment.created` | `elearning.enrollment.events` | `ENROLLMENT_SUCCESS` | `/learn/{courseId}` |
| `enrollment.completed` | `elearning.enrollment.events` | `COURSE_COMPLETED` | `/certificates/{enrollmentId}` |
| `certificate.issued` | `elearning.enrollment.events` | `CERTIFICATE_ISSUED` | `/certificates/{enrollmentId}` |
| `quiz.graded` | `elearning.quiz.events` | `QUIZ_GRADED` | `/attempts/{attemptId}` |

`link_url` là đường dẫn trên web, luôn bắt đầu bằng `/`. Web chỉ đi theo đường dẫn nội bộ;
thiếu link thì mở trang Thông báo. `certificate.issued` có sẵn `certificateUrl` nhưng đó là
đường dẫn file PDF chưa ai phục vụ, nên không dùng — V4 xóa những link PDF đã lưu trước đó.

Loại sự kiện chưa có xử lý vẫn được ghi vào `processed_events` rồi bỏ qua. Service khác
thêm sự kiện mới không làm hỏng service này.

## Một sự kiện chỉ tạo đúng một thông báo

Kafka bảo đảm **at-least-once**: consumer xử lý xong rồi chết trước khi commit offset thì
lần sau nhận lại đúng sự kiện đó. Không xử lý gì thêm thì học viên nhận hai thông báo giống
hệt nhau cho một lần nộp bài.

Cách chặn: trước khi làm gì, chèn `event_id` vào bảng `processed_events`. Trùng khóa chính
nghĩa là đã xử lý rồi, bỏ qua. Cả hai việc nằm trong **một transaction**, nên không có
trạng thái lửng lơ kiểu "đã ghi sổ nhưng chưa tạo thông báo".

Dựa vào ràng buộc của database chứ không hỏi trước rồi mới ghi, vì hai consumer chạy song
song có thể cùng thấy "chưa có" rồi cùng tạo thông báo.

Một cái bẫy trong Spring Data đáng ghi lại: **không dùng được `save()`** cho việc này.
`save()` quyết định INSERT hay UPDATE dựa vào `@Id` có null hay không; khóa ở đây là UUID
do bên gửi sinh ra nên luôn khác null, thành ra `save()` gọi `merge()` — UPDATE đè lên dòng
cũ, không hề có lỗi trùng khóa, và cơ chế chống trùng im lặng mất tác dụng.
`ProcessedEventRepository.insertNew` dùng câu INSERT tường minh chính vì vậy.

Đã kiểm trên hệ thống thật: cho consumer đọc lại toàn bộ topic từ offset 0 bằng một consumer
group mới, số thông báo trong database không đổi.

## Khi xử lý sự kiện bị lỗi

Bản đầu tiên bắt mọi lỗi, ghi log rồi đi tiếp. Nghe an toàn, nhưng MySQL chập chờn vài giây
là mọi sự kiện tới trong khoảng đó **mất hẳn**: Kafka coi như đã nhận xong, không gửi lại,
và học viên không bao giờ nhận được thông báo. Giờ lỗi được chia làm hai loại:

| Loại lỗi | Ví dụ | Xử lý |
|---|---|---|
| Tạm thời | MySQL khởi động lại, mất kết nối | Thử lại, chờ 1s, 2s, 4s… tối đa 30s mỗi lần. Consumer đứng chờ ở message đó, không nhảy qua |
| Message hỏng | JSON sai, thiếu `eventId`/`eventType`, `userId` là chữ | Chuyển ngay sang topic `.DLT`, đi tiếp message sau |
| Trùng | Sự kiện đã có trong `processed_events` | Bỏ qua êm, không phải lỗi |

Thử lại mãi không được — tổng thời gian chờ quá 5 phút, thực tế khoảng 10–12 phút khi MySQL
tắt hẳn — thì message cũng sang `.DLT`. Không thử mãi, vì một lỗi lập trình bị đoán nhầm là
lỗi tạm thời sẽ chặn đứng consumer.

**Topic `.DLT`** (dead letter topic) giữ lại message hỏng để người xem được. Tên là topic gốc
cộng đuôi `.DLT`, ví dụ `elearning.quiz.events.DLT`. Mở Kafka UI (http://localhost:8090) →
Topics → topic `.DLT` → Messages: thấy nguyên nội dung gốc kèm header
`kafka_dlt-exception-message` (vì sao hỏng) và `kafka_dlt-original-offset` (nằm ở đâu trong
topic gốc). Mỗi lần chuyển, log có một dòng ERROR:

```
Chuyển message elearning.quiz.events-0@7 sang elearning.quiz.events.DLT: Message trên topic elearning.quiz.events thiếu eventId hoặc eventType
```

Sửa xong nguyên nhân thì gửi lại được: chép nội dung message trong Kafka UI, Produce Message
vào topic gốc. An toàn, vì `processed_events` chặn trùng nếu lỡ sự kiện đó đã được xử lý.

Bộ xử lý nằm ở `KafkaErrorHandlingConfig`, khoảng chờ chỉnh ở `elearning.kafka.retry.*`.

**Đã chạy thật** (06/10, MySQL và Kafka trong Docker, service chạy trên máy):

| Thử | Kết quả |
|---|---|
| Gửi `{not json` rồi một sự kiện hợp lệ | Message hỏng sang `.DLT`, sự kiện sau vẫn tạo thông báo — consumer không kẹt |
| Gửi lại sự kiện đã xử lý | Không thêm thông báo, không vào `.DLT` |
| `userId` là chữ | Sang `.DLT`; `processed_events` không có dòng nào cho sự kiện đó |
| Tắt MySQL, gửi sự kiện, 40 giây sau bật lại | Thông báo tạo 4 giây sau khi MySQL lên. Bản cũ mất sự kiện này |
| Tắt MySQL 3 phút 25 giây | Vẫn chờ được, cả hai sự kiện gửi trong lúc tắt đều thành thông báo |
| Giới hạn chờ 5 giây, tắt MySQL | Thử 4 lần rồi sang `.DLT` (mất 2 phút 20 giây thật); sự kiện sau tạo thông báo ngay khi MySQL lên |

## Mẫu thông báo

Nội dung nằm trong bảng `notification_templates`, nạp sẵn bằng migration, không viết trong
code — sửa câu chữ không phải build lại service.

Chỗ trống viết dạng `{tenBien}`:

```
Bài kiểm tra <b>{quizTitle}</b> của bạn đạt {score} điểm.
```

Thiếu giá trị thì thay bằng chuỗi rỗng và ghi log cảnh báo. Để nguyên `{fullName}` hiện ra
cho người dùng thì khó coi, nhưng thiếu biến gần như luôn là mẫu và sự kiện không khớp nhau,
nên vẫn phải có dấu vết trong log.

Thông báo lưu **nội dung đã điền xong**, không lưu tham chiếu tới mẫu. Sửa mẫu về sau không
làm thay đổi những thông báo đã gửi — người dùng mở lại vẫn thấy đúng câu chữ hôm trước.

## API

Tất cả đều cần đăng nhập, và đều lấy người dùng từ token. Không endpoint nào nhận `userId`
từ client nên không thể đọc hộp thư của người khác.

| Phương thức | Đường dẫn | Việc |
|---|---|---|
| GET | `/api/notifications` | Hộp thư, mới nhất trước, có phân trang |
| GET | `/api/notifications/unread-count` | Số thông báo chưa đọc |
| GET | `/api/notifications/stream` | Luồng tức thời (Server-Sent Events), xem mục dưới |
| PATCH | `/api/notifications/{id}/read` | Đánh dấu đã đọc |
| PATCH | `/api/notifications/read` | Đánh dấu đã đọc **tất cả**; `data` là số thông báo vừa đổi |
| GET | `/api/notifications/preferences` | Tùy chọn nhận thông báo: `{"inAppEnabled": true, "emailEnabled": true}` |
| PUT | `/api/notifications/preferences` | Ghi đè tùy chọn; bắt buộc đủ hai cờ, thiếu một cờ là 400 |

Tắt `inAppEnabled` thì sự kiện mới không sinh thông báo (bỏ hẳn, không để dành); thông báo cũ
vẫn còn. `emailEnabled` được lưu sẵn cho lúc có kênh email.

Đọc thông báo của người khác trả **404 chứ không phải 403**: trả 403 là gián tiếp xác nhận
id đó có tồn tại.

## Thông báo tức thời

Web mở một kết nối `GET /api/notifications/stream` và giữ nó. Có thông báo mới là
notification-service đẩy xuống ngay: chuông đổi số, hiện toast có nút "Xem". Trước đây web
cứ 20 giây hỏi lại số chưa đọc một lần.

Hai loại sự kiện, `data` đều là JSON:

| Sự kiện | `data` | Khi nào |
|---|---|---|
| `unread-count` | `{"unreadCount": 3}` | Ngay khi mở luồng, và mỗi khi số chưa đọc đổi — kể cả khi đọc ở tab khác |
| `notification` | Một phần tử giống `GET /api/notifications` | Có thông báo mới |

```bash
curl -N -H "Authorization: Bearer <token>" localhost:8080/api/notifications/stream
# event:unread-count
# data:{"unreadCount":0}
```

**Vì sao cần Redis.** Kafka giao mỗi sự kiện cho **một** bản service trong consumer group, còn
trình duyệt của người nhận có thể đang nối vào một bản khác. Bản tạo thông báo đăng lên kênh
Redis `elearning:notifications`; bản nào giữ luồng của người đó thì gửi xuống. Đã chạy thử hai
bản cùng lúc: sự kiện chỉ một bản xử lý, luồng ở cả hai bản đều nhận.

**Chỉ phát sau khi commit** (`@TransactionalEventListener`): phát sớm hơn thì trình duyệt có
thể gọi lại API mà chưa thấy thông báo, hoặc thấy một thông báo mà rollback sau đó xóa mất.

**Khi có gì hỏng:**

| Chuyện gì | Hệ quả |
|---|---|
| Redis chết | Thông báo vẫn lưu; vẫn tới người đang nối vào chính bản đã tạo. Health vẫn UP. Redis sống lại thì tự đăng ký kênh lại |
| Mất luồng (mạng, service khởi động lại) | Trình duyệt tự nối lại; trong lúc chờ, web hỏi số chưa đọc 30 giây một lần |
| Token hết hạn | Luồng tự đóng sau 15 phút (`elearning.realtime.stream-timeout`), trình duyệt nối lại bằng token mới — luồng chỉ kiểm token lúc mở |

Mỗi 25 giây service gửi một dòng rỗng để proxy ở giữa không cắt kết nối và để biết tab nào đã
đóng. Một tài khoản giữ tối đa 5 luồng, quá thì đóng luồng cũ nhất. Route `/api` của Next.js
chuyển tiếp luồng này nguyên trạng, không gom body như response JSON.

## Vì sao đọc chuỗi thô thay vì để Spring chuyển đổi sẵn

Cách thông thường là để Spring Kafka tự chuyển JSON thành đối tượng dựa vào header
`__TypeId__` mà bên gửi gắn vào. Ở đây không dùng, vì hai lý do.

**Thứ nhất, nó biến tên lớp Java thành một phần của hợp đồng.** Bên gửi đổi gói hay đổi tên
lớp là bên nhận hỏng, dù nội dung message không đổi chút nào. Ở đây định tuyến theo trường
`eventType` trong chính nội dung — thứ mà `DomainEventSerializationTest` bên shared-common
đã khóa lại bằng test.

**Thứ hai, `JsonSerializer` và `JsonDeserializer` của Spring Kafka 4 vẫn chạy trên Jackson 2**
(`com.fasterxml.jackson`), trong khi Spring Boot 4 đã chuyển sang Jackson 3
(`tools.jackson`). Bản Jackson 2 lọt vào classpath qua dependency khác lại không có module
xử lý kiểu thời gian, nên mọi sự kiện đều chết khi gặp trường `occurredAt`:

```
InvalidDefinitionException: Java 8 date/time type `java.time.Instant` not supported
by default (through reference chain: QuizGradedEvent["occurredAt"])
```

Vì vậy cả hai đầu đều dùng `StringSerializer` / `StringDeserializer` và tự chuyển đổi bằng
Jackson 3 — cùng một thư viện ở cả bên gửi lẫn bên nhận, và cũng là thư viện mà bộ test hợp
đồng đang dùng.

## Email

Sự kiện nghiệp vụ cố ý không mang email (xem [shared-contracts.md](shared-contracts.md)).
Email và tên lấy từ chính auth-service, nơi sở hữu chúng, qua topic riêng:

```
auth-service ── outbox ──► elearning.auth.events ──► notification-service
  đăng ký        user.registered                       ghi user_contacts, xếp email WELCOME
  sửa tên        user.profile.updated                  ghi đè user_contacts
```

Migration V7 của auth-service phát `user.profile.updated` cho mọi tài khoản có sẵn để nạp
`user_contacts` lần đầu (không dùng `user.registered` để không ai nhận lại email chào mừng).

Gửi email chia hai bước:

1. **Xếp hàng** khi xử lý sự kiện: mã nào có mẫu kênh EMAIL thì `NotificationService.deliver`
   lưu thêm một dòng `notifications` kênh `EMAIL`, trạng thái `PENDING`, nội dung đã điền sẵn
   `{fullName}` và `{url}` (link tuyệt đối dựng từ `WEB_BASE_URL`). Người dùng tắt email trong
   cài đặt thì bỏ qua. Dòng EMAIL không hiện trong hộp thư: mọi truy vấn hộp thư lọc `IN_APP`.
2. **Gửi** bằng `EmailDispatcher`, chạy 5 giây một lần: tra `user_contacts` lấy địa chỉ, gửi
   qua SMTP rồi chuyển `SENT`. Hỏng (máy chủ mail lỗi, chưa có địa chỉ) thì tăng `retry_count`,
   ghi `last_error` và thử lại lượt sau; hỏng 5 lần thì chuyển `FAILED`.

Tách hai bước để máy chủ mail chậm hay chết không làm hỏng việc xử lý sự kiện Kafka.

| Mã mẫu EMAIL | Khi nào |
|---|---|
| `WELCOME` | Đăng ký tài khoản |
| `ENROLLMENT_SUCCESS` | Ghi danh khóa học |
| `CERTIFICATE_ISSUED` | Hoàn thành khóa, được cấp chứng chỉ |
| `COURSE_ANNOUNCEMENT` | Giảng viên gửi thông báo cho lớp |
| `LESSON_QUESTION_ANSWERED` | Câu hỏi trong bài học có người trả lời |
| `PASSWORD_RESET` | Người dùng bấm "Quên mật khẩu" (sự kiện `user.password.reset.requested`) |

`PASSWORD_RESET` là thư bảo mật: luôn gửi kể cả khi người dùng tắt email, không tạo thông báo
trong ứng dụng, và gửi xong thì `EmailDispatcher` xóa nội dung chứa link khỏi database.

Chạy bằng Docker thì máy chủ mail là **Mailpit**: mọi thư hiện ở http://localhost:8025, không
gửi ra Internet. Dùng máy chủ thật thì đặt `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`,
`MAIL_PASSWORD`, `MAIL_FROM`.

## Chạy thử ở máy mình

```bash
docker compose up -d mysql kafka redis
./mvnw -pl notification-service -am spring-boot:run
```

Không có Kafka thì đặt `spring.kafka.enabled=false`: service vẫn khởi động, chỉ không nghe
sự kiện. Cùng công tắc với quiz-service. Không có Redis thì đặt `REALTIME_REDIS_ENABLED=false`:
luồng tức thời chỉ phát cho người nối vào chính bản này, đủ khi chạy một bản.

Thử cả chuỗi (cần thêm auth-service và quiz-service):

```bash
# nộp một bài kiểm tra bất kỳ, rồi:
curl -H "Authorization: Bearer <token>" localhost:8085/api/notifications
```

## Những chỗ còn thiếu

- **Email chỉ có bản văn bản thuần**, chưa có bản HTML có giao diện.
- **Chưa dọn `processed_events`.** Bảng này chỉ lớn thêm. Cần một job xóa bản ghi cũ hơn
  vài tháng; cột `processed_at` đã có index sẵn cho việc đó.
