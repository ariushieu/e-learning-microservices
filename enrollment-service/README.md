# Enrollment & Progress Tracking Service

Service phụ trách ghi danh, tiến độ và chứng chỉ. Java 17 trở lên, Spring Boot 4.1.1,
MySQL/Flyway; JWT và hợp đồng sự kiện dùng từ `shared-common`.

## 1. Phần đã triển khai

- Ghi danh, hủy, tái kích hoạt, đặt lại tiến độ và xem chứng chỉ của chính người gọi.
- Worker gửi transactional outbox lên `elearning.enrollment.events`.
- `CourseSnapshotConsumer` nhận `course.updated` trên `elearning.course.events`, thêm hoặc
  thay thế toàn bộ snapshot theo `courseId`, gồm trạng thái và tổng số bài học.
- Năm đường dẫn enrollment/progress đã chuẩn hóa; gateway có route riêng cho PUT tiến độ.
- Danh tính lấy từ JWT. Endpoint ghi yêu cầu `ROLE_STUDENT`; người có nhiều vai trò vẫn
  dùng được nếu có vai trò học viên. Không cho thao tác thay học viên khác.

## 2. Luồng đồng bộ khóa học

`course-service → Kafka → CourseSnapshotConsumer → enrollment_db.course_snapshots`

Consumer dùng group `enrollment-service`, `StringDeserializer` và Jackson 3
(`tools.jackson.databind.ObjectMapper`). Không thêm dependency vào course-service và không
đọc database của service khác.

Mỗi sự kiện hợp lệ ghi đè cả dòng bằng `save()` và gán `syncedAt = Instant.now()`.
Các trường có thể null (ảnh bìa, thông tin giảng viên) được xóa đúng khi sự kiện gửi null.
`courseId`, `title`, `slug`, `totalLessons`, `status`, `eventId`, `occurredAt` phải hợp lệ.
Đặc biệt `slug` không được null vì schema MySQL hiện tại quy định NOT NULL.

- Nhận trùng: cập nhật cùng khóa chính, không tạo thêm dòng; không cần `processed_events`.
- Nhận ARCHIVED/DRAFT: vẫn cập nhật; ghi danh mới bị từ chối (404 theo cơ chế tra cứu hiện có).
- JSON sai hoặc dữ liệu bắt buộc sai: bỏ qua, ghi cảnh báo, tiếp tục message sau.
- Lỗi database: ném lỗi cho Kafka error handler retry mỗi giây, không đánh dấu offset đã xử lý.
  Retry không giới hạn để không âm thầm mất snapshot; lỗi ghi kéo dài cần kiểm tra database/log.
- `earliest` chỉ áp dụng khi group chưa có offset. Không tự đọc lại sự kiện đã hết retention.
- Thứ tự phụ thuộc producer dùng key `courseId`; không dùng `syncedAt` để so phiên bản sự kiện.
  Không thay đổi số partition hoặc phát lại sự kiện cũ sau sự kiện mới khi đang vận hành.

Cấu hình chính trong `src/main/resources/application.properties`:

```properties
spring.kafka.consumer.group-id=enrollment-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=false
spring.kafka.listener.ack-mode=record
```

`KAFKA_ENABLED=false` dùng để tắt listener khi phát triển không có Kafka.
Worker outbox có công tắc riêng `app.outbox.publisher.enabled=false`.

## 3. REST API qua gateway

Dùng `http://localhost:8080`, header `Authorization: Bearer <accessToken>`.
Cổng nội bộ enrollment là 8083; demo và kiểm tra tích hợp đi qua gateway.

| Method | URL | Body / ý nghĩa |
|---|---|---|
| POST | `/api/enrollments` | `{"courseId":10}`; tạo hoặc kích hoạt lại lượt đã hủy, trả 201 |
| GET | `/api/enrollments?page=0&size=10&sort=enrolledAt,desc` | Chỉ danh sách của tài khoản đang đăng nhập |
| GET | `/api/enrollments/{id}` | Chi tiết lượt ghi danh của mình |
| PATCH | `/api/enrollments/{id}/status` | `{"status":"CANCELLED"}` |
| DELETE | `/api/enrollments?courseId=10` | Xóa lượt ghi danh, tiến độ và chứng chỉ của mình trong khóa này |
| GET | `/api/enrollments/{id}/certificate` | Xem chứng chỉ khi hoàn thành |
| PUT | `/api/lessons/{lessonId}/progress` | `{"courseId":10,"status":"COMPLETED","watchedSeconds":300}` |
| GET | `/api/progress?courseId=10` | Tiến độ tổng và từng bài học |

PATCH chỉ chấp nhận yêu cầu chuyển sang CANCELLED. Gửi enum ACTIVE hoặc COMPLETED
nhận 422: tái kích hoạt qua POST ghi danh, hoàn thành do tiến độ tự tính. Thiếu trạng thái
hoặc enum không tồn tại trả 400. Khóa đã hoàn thành không được hủy theo nghiệp vụ hiện có.

PUT lấy `lessonId` từ URL, không lấy từ body; `userId` luôn lấy từ token.
`courseId` bắt buộc, dương; `watchedSeconds` không âm, bỏ trống mặc định 0.
Giữ nguyên kiểm tra bài học thuộc đúng khóa học qua `CourseLessonClient`:
bài sai/không tồn tại trả 404; course-service không truy cập được trả 502 và không ghi tiến độ.

### Thay đổi cần báo cho nhóm

| Cũ | Mới |
|---|---|
| `GET /api/enrollments/my-courses` | `GET /api/enrollments` |
| `DELETE /api/enrollments/course/{id}` | `DELETE /api/enrollments?courseId={id}` |
| `PATCH /api/enrollments/{id}/cancel` | `PATCH /api/enrollments/{id}/status` + body |
| `POST /api/progress/lesson` | `PUT /api/lessons/{lessonId}/progress` |
| `GET /api/progress/course/{id}` | `GET /api/progress?courseId={id}` |

Không giữ alias đường dẫn cũ. Frontend/collection phải chuyển sang URL mới.

Gateway thêm route `enrollment-lesson-progress`, `order=-10`, chỉ khớp PUT và
`/api/lessons/{lessonId}/progress`, đích là enrollment-service. GET bài học và các đường
dẫn course khác giữ route course-service. Không mở công khai PUT.
Phải triển khai gateway cùng thay đổi API này. Nhóm trưởng cần review route trước khi merge.
Phân công yêu cầu phần API mở PR riêng với phần consumer; code hiện tại chuẩn bị tại local,
chưa có xác nhận review hoặc triển khai chung.

## 4. Chạy và kiểm tra thủ công

Từ thư mục gốc repo:

```bash
docker compose --profile app up -d --build --wait
bash scripts/smoke-test.sh
```

1. Đăng nhập giảng viên, tạo khóa học và bài học rồi xuất bản qua gateway.
2. Kafka UI `http://localhost:8090`: topic `elearning.course.events` có `course.updated`,
   key bằng `courseId`.
3. Trong `enrollment_db`, kiểm tra:

```sql
SELECT course_id, title, slug, total_lessons, status, synced_at
FROM course_snapshots WHERE course_id = 3;
```

4. Đăng nhập học viên; POST ghi danh khóa vừa xuất bản: 201.
5. Gửi lại cùng message: vẫn một dòng snapshot, `synced_at` được cập nhật.
6. Sửa tên/số bài của khóa đang xuất bản: snapshot cập nhật đúng.
7. Chuyển khóa sang ARCHIVED; dùng học viên khác ghi danh: bị từ chối.
8. Khóa PUBLISHED: gọi PUT tiến độ, GET tiến độ và GET chứng chỉ sau khi học đủ bài.
9. Kiểm tra `outbox_events.published_at` và thông báo của học viên để xác nhận chuỗi liên service.

Có thể produce message mẫu khi chưa chạy course-service (đổi ID phù hợp môi trường):

```json
{
  "eventId": "3b2e8f10-7c4d-4a1e-9f6b-2d5c8e7a1b90",
  "eventType": "course.updated",
  "occurredAt": "2026-10-06T07:00:00Z",
  "courseId": 3,
  "title": "Microservices",
  "slug": "microservices",
  "thumbnailUrl": null,
  "instructorId": 7,
  "instructorName": "Giang vien",
  "totalLessons": 2,
  "status": "PUBLISHED"
}
```

Snapshot mẫu chỉ đủ test ghi danh; test tiến độ vẫn cần bài học thật ở course-service.

## 5. Postman

Import `docs/postman/enrollment-service.postman_collection.json`.

- Điền `email`, `password` của học viên và `courseId`, `lessonId` có thật; giữ
  `baseUrl=http://localhost:8080`.
- Request đăng nhập tự lưu `accessToken`; ghi danh tự lưu `enrollmentId`.
- Chạy từng thư mục theo trạng thái. Chứng chỉ chỉ trả 200 sau khi hoàn thành tất cả bài.
- Thư mục hủy dùng lượt chưa COMPLETED. Thư mục xóa đặt riêng vì xóa cả tiến độ/chứng chỉ
  của tài khoản đang đăng nhập trong khóa đã chọn.
- Thử quyền sở hữu bằng học viên thứ hai; thử sai vai trò bằng tài khoản chỉ có
  `ROLE_INSTRUCTOR`. Không lưu mật khẩu/token thật vào file collection trong Git.

## 6. Kiểm thử tự động

Từ thư mục gốc:

```bash
./mvnw -pl enrollment-service,api-gateway -am test
./mvnw clean verify
```

Trên Windows dùng `mvnw.cmd` thay cho `./mvnw`.

- `CourseSnapshotConsumerTest`: mapping đủ trường, thiếu trường, message sai, event khác,
  trường tương lai và lỗi database phải được ném ra để retry.
- `CourseSnapshotKafkaIntegrationTest`: broker Kafka thực trong JVM + H2; nhận message,
  nhận trùng, ghi đè cả trường null, archive chặn ghi danh, message hỏng không chặn message
  sau, lỗi tạm thời được retry trước snapshot tiếp theo.
- `EnrollmentApiIntegrationTest`: JWT thật + MVC + service + H2; danh tính từ token,
  ID bài từ URL, quyền sở hữu, đầu vào lỗi, không tự hoàn thành, vai trò học viên.
- `EnrollmentRoutingIntegrationTest`: gọi HTTP qua gateway thật ở cổng ngẫu nhiên;
  backend giả lập xác nhận đúng service, URL, query, body và token. PUT thiếu token trả 401;
  GET bài học công khai vẫn đến course-service.
- Test cũ của nghiệp vụ, client kiểm bài học và outbox tiếp tục chạy.

Test tự động không thay thế kiểm thử toàn bộ Docker/MySQL/notification. Không sửa entity
hay migration đã merge trong nhiệm vụ này; bước MySQL thật ở trên vẫn cần trước buổi demo.

### Kết quả kiểm tra local ngày 06/10/2026

Maven 3.9.16 / JDK 21 chạy `clean verify`: **BUILD SUCCESS**, 316 test, 0 failure,
0 error, 0 skipped. Đã build cả 8 module của reactor.

| Module | Test đạt |
|---|---:|
| shared-common | 77 |
| api-gateway | 21 |
| auth-service | 26 |
| course-service | 73 |
| enrollment-service | 63 |
| quiz-service | 34 |
| notification-service | 22 |

Collection Postman có 17 request, đã kiểm tra JSON và đường dẫn; chưa chạy Postman Runner
trên hệ thống Docker. Môi trường thực hiện không có lệnh Docker nên chưa xác nhận toàn
chuỗi với MySQL/Kafka/notification chạy qua Docker Compose. Test Kafka dùng broker thật
trong JVM; test gateway dùng HTTP thật với backend giả lập; test database dùng H2.
