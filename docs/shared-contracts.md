# Hợp đồng dùng chung (shared-common)

Module `shared-common` chứa những gì **nhiều service cùng phải đồng ý với nhau**. DTO
riêng của từng service không thuộc về đây.

- [Nguyên tắc: cái gì vào, cái gì không](#nguyên-tắc-cái-gì-vào-cái-gì-không)
- [Vỏ response và phân trang](#vỏ-response-và-phân-trang)
- [Xử lý lỗi](#xử-lý-lỗi)
- [Vì sao cần auto-configuration](#vì-sao-cần-auto-configuration)
- [Sự kiện Kafka](#sự-kiện-kafka)
- [Quy tắc thay đổi hợp đồng](#quy-tắc-thay-đổi-hợp-đồng)

## Nguyên tắc: cái gì vào, cái gì không

| Được đưa vào shared-common | Không được đưa vào |
|-----------------------------|---------------------|
| Vỏ response, phân trang, hình dạng lỗi | DTO request/response riêng của một service |
| Mã lỗi và handler dựng response lỗi | Logic nghiệp vụ |
| Payload sự kiện Kafka, tên topic | JPA entity, repository |
| Cách đọc và kiểm JWT, mã vai trò | Cấu hình riêng của một service |
| Hằng số dùng chung giữa các service | |

Phần xác thực nằm ở đây vì nó vượt qua đúng bài kiểm tra bên dưới: định dạng token và tên
vai trò là thứ cả 5 service phải hiểu **giống hệt nhau**, lệch một chữ là phân quyền sai.
Chi tiết ở [docs/authentication.md](authentication.md).

Hai lý do cho cột bên phải:

**JPA entity.** Entity là chi tiết bên trong của service sở hữu database. Dùng chung entity
nghĩa là 5 service chung một mô hình dữ liệu, phá vỡ nguyên tắc database-per-service mà cả
[thiết kế database](database-design.md) dựng lên để bảo vệ.

**Mọi thứ khác.** Sửa một dòng trong `shared-common` là phải build và triển khai lại cả 5
service. Module này càng phình to thì hệ thống càng giống một monolith bị chia nhỏ — có đủ
cái phiền của microservices mà không được cái lợi nào.

Khi phân vân: *thứ này có phải hai service khác nhau cùng phải hiểu giống nhau không?*
Không thì để trong service.

## Vỏ response và phân trang

```java
@GetMapping("/{id}")
public ApiResponse<CourseResponse> findById(@PathVariable Long id) {
    return ApiResponse.ok(courseService.findById(id));
}

@PostMapping
public ApiResponse<CourseResponse> create(@Valid @RequestBody CreateCourseRequest request) {
    return ApiResponse.ok(courseService.create(request), "Tạo khóa học thành công");
}

@DeleteMapping("/{id}")
public ApiResponse<Void> delete(@PathVariable Long id) {
    courseService.delete(id);
    return ApiResponse.message("Đã xóa khóa học");
}
```

Kết quả:

```json
{ "success": true, "message": null, "data": { "id": 1, "title": "..." }, "timestamp": "2026-09-18T07:15:00Z" }
```

Danh sách có phân trang dùng `PageResponse`, tự chuyển từ `Page` của Spring Data:

```java
Page<Course> page = courseRepository.findAll(pageable);
return ApiResponse.ok(PageResponse.of(
        page.getContent().stream().map(CourseResponse::from).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements()));
```

`PageResponse` cố ý không phụ thuộc Spring Data, để service không dùng database (như
notification-service ở giai đoạn đầu) vẫn dùng được, và để `shared-common` không kéo theo
tầng lưu trữ.

## Xử lý lỗi

Không tự dựng response lỗi và không `try/catch` trong controller. Ném exception, phần còn
lại `GlobalExceptionHandler` lo.

```java
Course course = courseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", id));

if (userRepository.existsByEmail(email)) {
    throw new DuplicateResourceException("người dùng", "email", email);
}

if (course.getStatus() != CourseStatus.PUBLISHED) {
    throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
            "Khóa học chưa được xuất bản nên không thể ghi danh");
}
```

### Bảng mã lỗi

| `ErrorCode` | HTTP | Dùng khi |
|-------------|------|-----------|
| `VALIDATION_FAILED` | 400 | Dữ liệu không qua được ràng buộc, tự sinh từ `@Valid` |
| `BAD_REQUEST` | 400 | Thiếu tham số, sai kiểu, JSON hỏng |
| `UNAUTHORIZED` | 401 | Chưa đăng nhập, token sai hoặc hết hạn |
| `FORBIDDEN` | 403 | Đã đăng nhập nhưng không đủ quyền |
| `RESOURCE_NOT_FOUND` | 404 | Không tìm thấy tài nguyên |
| `METHOD_NOT_ALLOWED` | 405 | Đúng đường dẫn nhưng sai phương thức HTTP |
| `DUPLICATE_RESOURCE` | 409 | Email trùng, slug trùng, ghi danh hai lần |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | `Content-Type` không được hỗ trợ |
| `BUSINESS_RULE_VIOLATED` | 422 | Hợp lệ về hình thức nhưng sai quy tắc nghiệp vụ |
| `EXTERNAL_SERVICE_ERROR` | 502 | Gọi sang service khác bị lỗi |
| `INTERNAL_ERROR` | 500 | Lỗi ngoài dự kiến |

### Hình dạng response lỗi

```json
{
  "success": false,
  "code": "VALIDATION_FAILED",
  "message": "Dữ liệu gửi lên không hợp lệ",
  "path": "/api/courses",
  "timestamp": "2026-09-18T07:15:00Z",
  "fieldErrors": [
    { "field": "durationMinutes", "message": "thời lượng phải lớn hơn 0" },
    { "field": "title", "message": "tiêu đề không được để trống" }
  ]
}
```

`fieldErrors` bị lược khỏi JSON khi rỗng. Frontend nên xử lý theo `code` chứ đừng so sánh
chuỗi `message`, vì `message` có thể sửa bất cứ lúc nào.

Ba điểm đã được kiểm chứng bằng test:

- **Lỗi 500 không lộ chi tiết nội bộ.** Thông báo thật được ghi vào log phía server, client
  chỉ nhận "Hệ thống gặp sự cố, vui lòng thử lại sau". Stack trace hay câu SQL không bao
  giờ đi ra ngoài.
- **`fieldErrors` không chứa giá trị bị từ chối.** Giá trị đó có thể là mật khẩu.
- **Exception chuẩn của Spring giữ nguyên mã trạng thái.** Gọi sai đường dẫn vẫn là 404,
  không bị lưới bắt-tất-cả biến thành 500 và che mất lỗi thật.
- **Thông báo nội bộ của Spring được thay bằng tiếng Việt.** Mặc định Spring trả về
  `"No static resource api/... for request ..."`, vừa là tiếng Anh vừa lộ chi tiết khung
  ứng dụng. Nội dung gốc chỉ nằm trong log phía server.

Muốn hiển thị thông báo riêng cho một tình huống nghiệp vụ thì ném `BusinessException`.
Đừng dùng `ResponseStatusException`: thông báo truyền vào đó sẽ bị thay bằng nội dung
chung theo mã trạng thái.

## Vì sao cần auto-configuration

Spring chỉ quét component trong gói của lớp `@SpringBootApplication` trở xuống. Gói của các
service là `com.hunre.authservice`, `com.hunre.courseservice`..., còn `GlobalExceptionHandler`
nằm ở `com.hunre.sharedcommon` nên **không** được quét tới.

Đây là kiểu hỏng nguy hiểm vì nó im lặng: mọi thứ vẫn biên dịch, service vẫn chạy, chỉ có
điều lỗi trả về sai định dạng và không ai biết cho tới khi frontend gọi thử.

Cách vá hay gặp là thêm `@ComponentScan("com.hunre")` vào từng service, nhưng service nào
quên là hỏng. Thay vào đó `shared-common` khai báo lớp auto-configuration trong file
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, và
Spring Boot tự nạp. **Service không phải khai báo gì cả.**

Service muốn xử lý lỗi theo cách riêng thì chỉ cần tự tạo bean cùng kiểu, bản mặc định sẽ
tự nhường chỗ nhờ `@ConditionalOnMissingBean`:

```java
@Bean
GlobalExceptionHandler authExceptionHandler() {
    return new AuthExceptionHandler();
}
```

## Sự kiện Kafka

### Topic

| Topic | Service phát | Sự kiện |
|-------|--------------|---------|
| `elearning.enrollment.events` | enrollment-service | `enrollment.created`, `enrollment.completed`, `certificate.issued` |
| `elearning.course.events` | course-service | sự kiện về khóa học |
| `elearning.quiz.events` | quiz-service | `quiz.graded` |

Gom theo service phát chứ không tách mỗi loại sự kiện một topic, vì Kafka chỉ bảo đảm thứ
tự trong phạm vi một partition của một topic. Ghi danh phải đến trước hoàn thành khóa học,
tách topic là mất bảo đảm đó.

Khi gửi, đặt khóa message là id thực thể gốc (ví dụ `userId`) để mọi sự kiện của cùng một
người rơi vào cùng partition.

### Danh mục sự kiện

| Lớp | `eventType` | Ai phát | Ai nghe | Mẫu thông báo |
|-----|-------------|---------|---------|----------------|
| `EnrollmentCreatedEvent` | `enrollment.created` | enrollment | notification, course | `ENROLLMENT_SUCCESS` |
| `EnrollmentCompletedEvent` | `enrollment.completed` | enrollment | notification | `COURSE_COMPLETED` |
| `CertificateIssuedEvent` | `certificate.issued` | enrollment | notification | `CERTIFICATE_ISSUED` |
| `QuizGradedEvent` | `quiz.graded` | quiz | notification, enrollment | `QUIZ_GRADED` |

Cột cuối trỏ tới `notification_templates.code` đã nạp sẵn trong migration của
notification-service.

Luôn dùng hằng số `EventTypes.*` và `KafkaTopics.*`, đừng gõ chuỗi trực tiếp. Gõ tay thì
một bên viết `"enrollment.created"` còn bên kia viết `"enrollmentCreated"` là message không
bao giờ tới nơi, mà chẳng có lỗi nào hiện ra.

### Ba trường bắt buộc

Mọi sự kiện đều cài `DomainEvent` với `eventId`, `eventType`, `occurredAt`.

`eventId` là mã UUID sinh một lần tại nơi phát. Consumer ghi mã này vào
`processed_events` **trước khi** xử lý; trùng khóa chính nghĩa là đã xử lý rồi, bỏ qua.
Kafka bảo đảm at-least-once nên một sự kiện hoàn toàn có thể tới hai lần — không khử trùng
lặp thì người học nhận hai email cho cùng một lần ghi danh.

`occurredAt` là lúc nghiệp vụ xảy ra, không phải lúc message lên Kafka. Hai mốc này lệch
nhau vì outbox gửi sau khi transaction đã commit.

### Sự kiện mang gì

Chỉ mang id và dữ liệu mà **service phát thật sự sở hữu**. Ví dụ `EnrollmentCreatedEvent`
có `courseTitle` (lấy từ bảng `course_snapshots` mà enrollment-service giữ) nhưng **không**
có email hay họ tên người dùng — những thứ đó thuộc auth-service.

Nhét email vào sự kiện đồng nghĩa enrollment-service phải gọi sang auth-service ngay lúc
ghi danh, biến một luồng bất đồng bộ thành phụ thuộc đồng bộ: auth-service sập là không ai
ghi danh được nữa. Consumer nào cần thông tin người dùng thì tự lấy.

### Ví dụ JSON

```json
{
  "eventId": "9f1c2f3e-1b2a-4c3d-8e5f-6a7b8c9d0e1f",
  "eventType": "enrollment.created",
  "occurredAt": "2026-09-18T07:15:00Z",
  "enrollmentId": 42,
  "userId": 7,
  "courseId": 3,
  "courseTitle": "Kiến trúc Microservices"
}
```

## Quy tắc thay đổi hợp đồng

Các lớp trong `shared-common` là hợp đồng giữa **hai service do hai người khác nhau làm**,
không phải DTO nội bộ muốn sửa lúc nào cũng được.

| Thay đổi | An toàn? | Vì sao |
|----------|----------|--------|
| Thêm trường mới vào sự kiện | Có | Consumer cũ bỏ qua trường lạ, đã có test bảo đảm |
| Thêm giá trị mới vào `ErrorCode` | Có | Không ảnh hưởng mã đang dùng |
| Thêm lớp sự kiện mới | Có | Không ai đang nghe |
| **Đổi tên trường** | **Không** | Consumer đọc ra null trong im lặng, không lỗi biên dịch |
| **Xóa trường** | **Không** | Như trên |
| **Đổi giá trị `eventType` hoặc tên topic** | **Không** | Message không bao giờ tới nơi |
| **Đổi HTTP status của một `ErrorCode`** | **Không** | Frontend đang xử lý theo status cũ |

Cần làm một thay đổi ở cột "Không" thì **báo cả nhóm trước**, thống nhất rồi sửa đồng thời
cả hai phía trong cùng một pull request.

Bộ test trong `shared-common` khóa chặt tên trường JSON của cả 4 sự kiện, nên đổi tên sẽ
làm đỏ CI ngay thay vì âm thầm hỏng lúc chạy thật.
