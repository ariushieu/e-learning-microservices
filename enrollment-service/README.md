# Enrollment & Progress Tracking Service

Service quản lý đăng ký khóa học (Enrollment), theo dõi tiến độ học tập (Progress Tracking) và cấp chứng chỉ hoàn thành (Certificate) cho nền tảng E-Learning HUNRE, thuộc kiến trúc Microservices.

---

## 1. Công nghệ sử dụng
- **Ngôn ngữ & Framework:** Java 21, Spring Boot 4.1.1 (Spring MVC, Spring Data JPA, Spring Validation).
- **Cơ sở dữ liệu:** MySQL 8.4 (`enrollment_db`), Flyway Migration, H2 In-Memory DB (chạy kiểm thử tự động).
- **Xác thực:** JWT thông qua `AuthenticatedUser` (từ `shared-common`).
- **Giao tiếp liên dịch vụ:** 
  - Đọc bản sao chỉ đọc `course_snapshots` (nhất quán cuối đồng bộ từ `course-service`).
  - Transactional Outbox Pattern (`outbox_events`) lưu các sự kiện `enrollment.created`, `enrollment.completed`, `certificate.issued`.
- **Kiến trúc:** Layered Architecture:
  - `controller`: Tiếp nhận HTTP request, phân luồng và trả về `ApiResponse<T>`.
  - `service`: Chứa logic nghiệp vụ, tính toán % tiến độ bài học, transactional outbox.
  - `repository`: Tầng truy cập dữ liệu Spring Data JPA.
  - `entity`: Định nghĩa các thực thể JPA (`Enrollment`, `LessonProgress`, `Certificate`, `CourseSnapshot`, `OutboxEvent`).
  - `dto`: Request & Response DTOs với Bean Validation.
  - `client`: Tra cứu dữ liệu từ `course_snapshots`.

---

## 2. Thiết kế Cơ sở dữ liệu (`enrollment_db`)

Mã nguồn migration nằm tại: `src/main/resources/db/migration/V1__init_enrollment_schema.sql`

1. **`enrollments`**: Quản lý lượt đăng ký khóa học của học viên.
   - Khóa chính `id`, `user_id`, `course_id`, `status` (`ACTIVE`, `COMPLETED`, `CANCELLED`), `progress_percent`, `enrolled_at`, `completed_at`, `last_accessed_at`.
   - Ràng buộc `UNIQUE(user_id, course_id)` chống đăng ký trùng lặp.
   - Ràng buộc `CHECK (progress_percent BETWEEN 0 AND 100)`.
2. **`lesson_progress`**: Theo dõi trạng thái từng bài học.
   - Khóa chính `id`, `enrollment_id` (FK), `lesson_id`, `status` (`IN_PROGRESS`, `COMPLETED`), `watched_seconds`, `completed_at`.
   - Ràng buộc `UNIQUE(enrollment_id, lesson_id)`.
3. **`certificates`**: Lưu chứng chỉ tốt nghiệp cấp khi hoàn thành 100% khóa học.
   - Khóa chính `id`, `enrollment_id` (FK-UK), `certificate_code` (UK), `file_url`, `issued_at`.
4. **`course_snapshots`**: Lưu bản sao thông tin khóa học đồng bộ qua Kafka (`title`, `slug`, `total_lessons`, `status`).
5. **`outbox_events`**: Bảng outbox lưu sự kiện để phát sang Apache Kafka theo Transactional Outbox Pattern.

---

## 3. Danh sách REST API

Cổng mặc định: `http://localhost:8083`

> **Lưu ý xác thực:** Các endpoint nghiệp vụ yêu cầu header `Authorization: Bearer <jwt-token>`. Thông tin `userId` được tự động trích xuất từ token qua `AuthenticatedUser`. Khi chạy kiểm thử local không cần token, bật `elearning.security.enabled=false` trong `application-local.properties`.

### Module 1: Đăng ký khóa học (Enrollment)

| Phương thức | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/enrollments` | Đăng ký khóa học mới (Body: `{"courseId": 1}`) |
| `GET` | `/api/enrollments/my-courses` | Danh sách khóa học của học viên (hỗ trợ phân trang `page`, `size`) |
| `GET` | `/api/enrollments/{id}` | Lấy chi tiết lượt ghi danh theo ID |
| `PATCH` | `/api/enrollments/{id}/cancel` | Hủy đăng ký khóa học (chuyển sang `CANCELLED`) |
| `DELETE` | `/api/enrollments/course/{courseId}` | Đặt lại / Hủy ghi danh để học lại từ đầu |
| `GET` | `/api/enrollments/{id}/certificate` | Xem thông tin chứng chỉ hoàn thành khóa học |

### Module 2: Theo dõi tiến độ học (Progress Tracking)

| Phương thức | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/progress/lesson` | Cập nhật tiến độ bài học (thời gian xem, trạng thái `IN_PROGRESS`/`COMPLETED`) |
| `GET` | `/api/progress/course/{courseId}` | Lấy chi tiết tiến độ khóa học, % hoàn thành và danh sách bài học |

---

## 4. Kế hoạch phát triển tiếp theo (Next Steps / PRs)
- **Outbox Publisher Worker:** Triển khai một `@Scheduled` job định kỳ đọc các dòng chưa gửi (`published_at IS NULL`) trong bảng `outbox_events`, gửi thông điệp lên topic Kafka `KafkaTopics.ENROLLMENT_EVENTS`, và đánh dấu thời điểm `published_at`.
- **Course Kafka Consumer:** Lắng nghe topic `course.*` từ `course-service` để tự động cập nhật bản ghi trong `course_snapshots`.

---

## 5. Hướng dẫn kiểm thử với Postman

File bộ sưu tập kiểm thử: `docs/postman/enrollment-service.postman_collection.json`.

Import vào Postman và chạy với biến môi trường `baseUrl = http://localhost:8083`. Khi chạy với cấu hình `elearning.security.enabled=false`, hệ thống tự động gán danh tính học viên `id=1` cho toàn bộ các request.
