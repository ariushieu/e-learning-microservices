# Thông báo

notification-service nhận sự kiện Kafka từ các service khác và dựng thông báo cho người
dùng. Nó không gọi service nào và không service nào gọi nó — chỉ nghe sự kiện.

- [Luồng đi của một thông báo](#luồng-đi-của-một-thông-báo)
- [Một sự kiện chỉ tạo đúng một thông báo](#một-sự-kiện-chỉ-tạo-đúng-một-thông-báo)
- [Mẫu thông báo](#mẫu-thông-báo)
- [API](#api)
- [Vì sao đọc chuỗi thô thay vì để Spring chuyển đổi sẵn](#vì-sao-đọc-chuỗi-thô-thay-vì-để-spring-chuyển-đổi-sẵn)
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
                                   │       học viên gọi GET /api/notifications
```

Sự kiện nào sinh ra thông báo nào:

| Sự kiện | Topic | Mã mẫu |
|---|---|---|
| `enrollment.created` | `elearning.enrollment.events` | `ENROLLMENT_SUCCESS` |
| `enrollment.completed` | `elearning.enrollment.events` | `COURSE_COMPLETED` |
| `certificate.issued` | `elearning.enrollment.events` | `CERTIFICATE_ISSUED` |
| `quiz.graded` | `elearning.quiz.events` | `QUIZ_GRADED` |

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
| PATCH | `/api/notifications/{id}/read` | Đánh dấu đã đọc |

Đọc thông báo của người khác trả **404 chứ không phải 403**: trả 403 là gián tiếp xác nhận
id đó có tồn tại.

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

## Chạy thử ở máy mình

```bash
docker compose up -d mysql kafka
./mvnw -pl notification-service -am spring-boot:run
```

Không có Kafka thì đặt `spring.kafka.enabled=false`: service vẫn khởi động, chỉ không nghe
sự kiện. Cùng công tắc với quiz-service.

Thử cả chuỗi (cần thêm auth-service và quiz-service):

```bash
# nộp một bài kiểm tra bất kỳ, rồi:
curl -H "Authorization: Bearer <token>" localhost:8085/api/notifications
```

## Những chỗ còn thiếu

- **Kênh EMAIL chưa gửi gì.** Chưa có máy chủ mail, và các mẫu EMAIL cần `{fullName}` mà sự
  kiện cố ý không mang theo thông tin cá nhân (xem [shared-contracts.md](shared-contracts.md)).
  Muốn gửi email thì phải hỏi auth-service để lấy tên và địa chỉ.
- **Chưa có API sửa tùy chọn nhận thông báo.** Bảng `notification_preferences` đã được tôn
  trọng khi tạo thông báo, nhưng chưa có endpoint để người dùng tự bật tắt.
- **Chưa dọn `processed_events`.** Bảng này chỉ lớn thêm. Cần một job xóa bản ghi cũ hơn
  vài tháng; cột `processed_at` đã có index sẵn cho việc đó.
