# Enrollment & Progress Tracking Service

Service quản lý đăng ký khóa học (Enrollment) và theo dõi tiến độ học tập (Progress Tracking) cho nền tảng E-Learning HUNRE, thuộc kiến trúc Microservices.

---

## 1. Công nghệ sử dụng
- **Ngôn ngữ & Framework:** Java 21, Spring Boot 4.1.1 (Spring MVC, Spring Data JPA, Spring Validation).
- **Cơ sở dữ liệu:** MySQL 8.4 (`enrollment_db`), Flyway Migration, H2 In-Memory DB (chạy kiểm thử tự động).
- **Giao tiếp liên dịch vụ:** 
  - RESTful API (Client stub gọi `auth-service` và `course-service`).
  - Transactional Outbox Pattern (`outbox_events`) sẵn sàng phát sự kiện Kafka (`enrollment.created`, `enrollment.completed`).
- **Kiến trúc:** Layered Architecture chuẩn:
  - `controller`: Tiếp nhận HTTP request, phân luồng và trả về `ApiResponse<T>`.
  - `service`: Chứa logic nghiệp vụ, tính toán % tiến độ bài học, transactional outbox.
  - `repository`: Tầng truy cập dữ liệu Spring Data JPA.
  - `entity`: Định nghĩa các thực thể JPA (`Enrollment`, `LessonProgress`, `CourseSnapshot`, `OutboxEvent`).
  - `dto`: Request & Response DTOs với Bean Validation.
  - `client`: Giao tiếp hoặc giả lập dữ liệu từ các service khác (`AuthClient`, `CourseClient`).

---

## 2. Thiết kế Cơ sở dữ liệu (`enrollment_db`)

Mã nguồn migration nằm tại: `src/main/resources/db/migration/V1__init_enrollment_schema.sql`

1. **`enrollments`**: Quản lý lượt đăng ký khóa học của học viên.
   - Khóa chính `id`, `user_id`, `course_id`, `status` (`ACTIVE`, `COMPLETED`, `CANCELLED`), `progress_percent`, `enrolled_at`, `completed_at`, `last_accessed_at`.
   - Ràng buộc `UNIQUE(user_id, course_id)` chống đăng ký trùng lặp.
2. **`lesson_progress`**: Theo dõi trạng thái từng bài học.
   - Khóa chính `id`, `enrollment_id` (FK), `lesson_id`, `status` (`IN_PROGRESS`, `COMPLETED`), `watched_seconds`, `completed_at`.
   - Ràng buộc `UNIQUE(enrollment_id, lesson_id)`.
3. **`course_snapshots`**: Lưu bản sao thông tin khóa học (tên, tổng số bài học `total_lessons`) giảm thiểu phụ thuộc mạng sang `course-service`.
4. **`outbox_events`**: Bảng outbox lưu sự kiện để phát sang Apache Kafka một cách an toàn.
5. **`course_progress`**: View tiến độ khóa học tổng hợp từ `enrollments`.

---

## 3. Danh sách REST API & Hướng dẫn Test bằng cURL

Cổng mặc định: `http://localhost:8083`

### Module 1: Đăng ký khóa học (Enrollment)

#### 1. Đăng ký khóa học mới (`POST /api/enrollments`)
```bash
curl -X POST http://localhost:8083/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": 1,
    "userId": 1
  }'
```
*Phản hồi mẫu (HTTP 201):*
```json
{
  "success": true,
  "message": "Đăng ký khóa học thành công",
  "data": {
    "id": 1,
    "userId": 1,
    "courseId": 1,
    "courseTitle": "Khóa học Lập trình Microservices #1",
    "status": "ACTIVE",
    "progressPercent": 0.00,
    "enrolledAt": "2026-09-19T06:00:00Z",
    "completedAt": null,
    "lastAccessedAt": "2026-09-19T06:00:00Z"
  },
  "timestamp": "2026-09-19T06:00:00Z"
}
```

#### 2. Lấy danh sách khóa học của học viên (`GET /api/enrollments/my-courses`)
```bash
curl -X GET "http://localhost:8083/api/enrollments/my-courses?userId=1&page=0&size=10"
```

#### 3. Lấy chi tiết lượt ghi danh theo ID (`GET /api/enrollments/{id}`)
```bash
curl -X GET http://localhost:8083/api/enrollments/1
```

---

### Module 2: Theo dõi tiến độ học (Progress Tracking)

#### 1. Cập nhật tiến độ bài học (`POST /api/progress/lesson`)
```bash
# Đánh dấu đang học bài học số 1, đã xem 180 giây:
curl -X POST http://localhost:8083/api/progress/lesson \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": 1,
    "lessonId": 1,
    "status": "IN_PROGRESS",
    "watchedSeconds": 180,
    "userId": 1
  }'

# Đánh dấu hoàn thành bài học số 1 (hệ thống sẽ tự động tính lại % của khóa học):
curl -X POST http://localhost:8083/api/progress/lesson \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": 1,
    "lessonId": 1,
    "status": "COMPLETED",
    "watchedSeconds": 450,
    "userId": 1
  }'
```

#### 2. Lấy tiến độ chi tiết khóa học (`GET /api/progress/course/{courseId}`)
```bash
curl -X GET "http://localhost:8083/api/progress/course/1?userId=1"
```
*Phản hồi mẫu:*
```json
{
  "success": true,
  "data": {
    "courseId": 1,
    "enrollmentId": 1,
    "courseTitle": "Khóa học Lập trình Microservices #1",
    "status": "ACTIVE",
    "progressPercent": 20.00,
    "completedLessonsCount": 1,
    "totalLessonsCount": 5,
    "lastAccessedAt": "2026-09-19T06:05:00Z",
    "lessons": [
      {
        "lessonId": 1,
        "status": "COMPLETED",
        "watchedSeconds": 450,
        "completedAt": "2026-09-19T06:05:00Z"
      }
    ]
  },
  "timestamp": "2026-09-19T06:05:00Z"
}
```

---

## 4. Hướng dẫn chạy

### Cách 1: Chạy trực tiếp trên máy cục bộ (Local)
1. Khởi động hạ tầng MySQL:
   ```bash
   docker compose up -d mysql
   ```
2. Chạy service từ thư mục gốc của dự án:
   ```bash
   ./mvnw -pl enrollment-service -am spring-boot:run
   ```

### Cách 2: Chạy kiểm thử tự động (Unit / Integration Tests)
```bash
./mvnw clean test -pl enrollment-service -am
```

### Cách 3: Chạy qua Docker
1. Build Docker image từ thư mục gốc:
   ```bash
   docker build -t e-learning-enrollment-service -f enrollment-service/Dockerfile .
   ```
2. Khởi chạy container:
   ```bash
   docker run -d -p 8083:8083 \
     --name enrollment-service \
     --env-file enrollment-service/.env.example \
     e-learning-enrollment-service
   ```

---

## 5. Hướng dẫn kiểm thử toàn diện với Postman (`postman_collection.json`)

Tất cả các API và kịch bản test đã được cấu hình sẵn trong file: `enrollment-service/postman_collection.json`.

### Các bước nhập (Import) và thực thi:
1. Mở ứng dụng **Postman**.
2. Nhấn nút **Import** (góc trên bên trái) $\rightarrow$ Chọn tab **Files** $\rightarrow$ Trỏ tới file:
   ```
   d:\download\BTL_T6\e-learning-microservices\enrollment-service\postman_collection.json
   ```
3. Sau khi Import thành công, bạn sẽ thấy collection **"HUNRE E-Learning - Enrollment & Progress Tracking Service"** với 4 nhóm kiểm thử:
   - **🔥 1. Kịch bản kiểm thử luồng thực tế (Khóa 3 bài đạt 100%)**:
     - *Bước 1:* Đăng ký Khóa học 3 bài (Khóa #3: Docker & Microservices) $\rightarrow$ Status `ACTIVE`, Tiến độ `0%`.
     - *Bước 2:* Tra cứu `my-courses` của học viên.
     - *Bước 3:* Học bài 1 (`IN_PROGRESS`, đã xem 120s).
     - *Bước 4:* Hoàn thành bài 1 (`COMPLETED`).
     - *Bước 5:* Kiểm tra tiến độ khóa học đạt **$33.33\%$** ($1/3$ bài).
     - *Bước 6:* Hoàn thành bài 2 (`COMPLETED`) $\rightarrow$ Tiến độ đạt **$66.67\%$** ($2/3$ bài).
     - *Bước 7:* Hoàn thành bài 3 (`COMPLETED` - Bài cuối).
     - *Bước 8:* Kiểm tra chi tiết tiến độ đạt đủ **$3/3$ bài ($100.00\%$)** và trạng thái tự động chuyển thành **`COMPLETED`**.
   - **📚 2. Module 1: Đăng ký khóa học (Enrollment APIs)**:
     - Đăng ký thành công (`HTTP 201`).
     - Bắt lỗi đăng ký trùng lặp (`HTTP 409 DUPLICATE_RESOURCE`).
     - Bắt lỗi thiếu trường bắt buộc (`HTTP 400 VALIDATION_FAILED`).
     - Lấy danh sách khóa học có phân trang (`GET /api/enrollments/my-courses`).
     - Tra cứu chi tiết lượt ghi danh theo ID.
   - **📈 3. Module 2: Theo dõi tiến độ học (Progress Tracking APIs)**:
     - Cập nhật đang học bài (`IN_PROGRESS`).
     - Cập nhật hoàn thành bài (`COMPLETED`).
     - Lấy chi tiết tiến độ (`GET /api/progress/course/{id}`).
     - Bắt lỗi cập nhật khi chưa đăng ký (`HTTP 404 RESOURCE_NOT_FOUND`).
   - **⚙️ 4. Giám sát & Vận hành**:
     - Kiểm tra sức khỏe dịch vụ qua Spring Actuator (`GET /actuator/health`).

### Chạy tự động (Runner):
- Bạn có thể nhấn chuột phải vào Collection $\rightarrow$ Chọn **Run collection** $\rightarrow$ Nhấn **Run** để Postman tự động kiểm tra toàn bộ assertions (`pm.test`) xanh 100%.

