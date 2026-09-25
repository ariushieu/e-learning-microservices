# Bảng theo dõi công việc

> **Cập nhật lần cuối:** 25/09/2026 — `main` ở `9b305a7`
>
> File này là nơi duy nhất ghi ai đang làm gì. Xong một việc thì nhóm trưởng cập nhật ngay
> tại đây, nên **cứ `git pull` là biết việc tiếp theo của mình**, không phải hỏi ai.

- [Việc của bạn](#việc-của-bạn)
- [Quy tắc viết API](#quy-tắc-viết-api)
- [Trạng thái hệ thống](#trạng-thái-hệ-thống)
- [Chi tiết từng việc](#chi-tiết-từng-việc)
- [Đã xong](#đã-xong)
- [Hai cái bẫy của Spring Boot 4](#hai-cái-bẫy-của-spring-boot-4)
- [Trước khi code](#trước-khi-code)

## Việc của bạn

| Người | Service | Việc đang mở | Ưu tiên | Cỡ |
|---|---|---|---|---|
| quocluibotre | auth-service | [API gán vai trò + admin đầu tiên](#quocluibotre--api-gán-vai-trò) | **Cao nhất** — chặn cả nhóm | ~2h |
| duyd92689-debug | course-service | [Phân quyền và lọc trạng thái](#duyd92689-debug--phân-quyền-và-lọc-trạng-thái-cho-course-service) | **Cao nhất** — 3 lỗ hổng | ~3h |
| duyd92689-debug | course-service | [Phát `course.updated`](#duyd92689-debug--phát-sự-kiện-courseupdated) | Cao | ~1h |
| duyd92689-debug | course-service | [Trả nội dung bài học](#duyd92689-debug--trả-nội-dung-bài-học) | Trung bình | ~1h |
| phamquyet19042005-netizen | enrollment-service | [Gửi outbox](#phamquyet19042005-netizen--gửi-outbox-lên-kafka) + [nạp snapshot](#phamquyet19042005-netizen--nạp-course_snapshots) | Cao | ~3h |
| hiepdeptrai0111 | quiz-service | [Chuyển phát sự kiện sang outbox](#hiepdeptrai0111--chuyển-phát-sự-kiện-sang-outbox) | Thấp — làm sau cùng | ~2h |
| Cả nhóm | mọi service | [Chuẩn hóa đường dẫn API](#cả-nhóm--chuẩn-hóa-đường-dẫn-api) | Thấp — sau khi demo chạy | ~1h/người |

**Không ai phải chờ ai.** Sự kiện `CourseUpdatedEvent` đã có trong shared-common, nên phía
phát (duyd) và phía nhận (phamquyet) làm song song được — phamquyet tự tạo message mẫu bằng
Kafka UI để test, không cần đợi course-service.

**Thứ tự ưu tiên.** quocluibotre làm trước vì không có tài khoản giảng viên thì cả nhóm
không test được phần tạo bài kiểm tra. Việc phân quyền của duyd92689-debug ngang hàng về độ
gấp: hiện course-service không kiểm quyền ở bất kỳ đâu. Việc chuẩn hóa đường dẫn để cuối
cùng, nhưng **phải xong trước khi bắt đầu frontend** — đổi đường dẫn sau khi frontend đã
gọi là gãy hết.

## Quy tắc viết API

Mới thêm: **[docs/api-conventions.md](api-conventions.md)** — luật chung cho cả 5 service,
mỗi quy tắc có số hiệu để review chỉ cần ghi *"vi phạm A2"*.

Đọc trước khi viết endpoint mới. Tóm tắt phần bắt buộc:

| Mã | Quy tắc |
|---|---|
| A1 | Danh tính lấy từ token, không nhận `userId`/`instructorId`/`createdBy` từ client |
| A2 | Mọi endpoint ghi phải kiểm vai trò |
| A3 | Endpoint công khai phải tự lọc trạng thái, không trả dữ liệu chưa xuất bản |
| A4 | Thêm controller mới thì khai route ở gateway (CI kiểm) |
| A5 | Dùng `ApiResponse` và `ErrorCode`, không tự chế hình dạng response |

Bốn trong năm quy tắc này sinh ra từ lỗi có thật trong repo, ghi rõ trong tài liệu.

## Trạng thái hệ thống

Năm service đã có code, database chạy tự động bằng Flyway, xác thực JWT hoạt động ở cả
gateway lẫn từng service. Toàn bộ 216 test xanh.

**Chuỗi đã chạy thông:**

```
đăng nhập → làm bài kiểm tra → nộp bài → nhận thông báo trong ứng dụng
```

**Chuỗi chưa chạy, và vì sao:**

| Không làm được | Nguyên nhân | Ai sửa |
|---|---|---|
| Tạo bài kiểm tra, thêm câu hỏi | Không tài khoản nào có `ROLE_INSTRUCTOR` được | quocluibotre |
| Ghi danh khóa học | `course_snapshots` rỗng, không ai đổ dữ liệu vào | duyd + phamquyet |
| Thông báo khi ghi danh | Outbox ghi rồi nhưng không ai gửi lên Kafka | phamquyet |
| Xem nội dung bài học | `LessonResponse` không có trường `content` | duyd |

**Lỗ hổng đang mở:**

| Lỗ hổng | Mức độ | Ai sửa |
|---|---|---|
| course-service không kiểm quyền ở bất kỳ endpoint nào | Nặng | duyd |
| Khóa học `DRAFT` đọc được không cần đăng nhập | Nặng | duyd |
| `instructorId` do client tự khai | Vừa | duyd |

Ba việc ở bảng trên xong là demo chạy trọn vẹn: đăng ký → ghi danh → học → làm bài → nhận
thông báo → chứng chỉ.

---

## Chi tiết từng việc

### quocluibotre — API gán vai trò

**Vấn đề.** Hiện không tài khoản nào có thể trở thành giảng viên.
`AuthServiceImpl.register()` gán cứng `ROLE_STUDENT`, và auth-service chỉ có bốn endpoint:
register, login, refresh-token, logout. Không có chỗ nào đổi vai trò.

`ROLE_INSTRUCTOR` và `ROLE_ADMIN` có trong enum `RoleCode`, có sẵn trong bảng `roles`
(migration `V2__seed_roles.sql` đã nạp), nhưng không ai giữ được.

Hệ quả: chín chỗ kiểm quyền `requireQuizManager` trong quiz-service **không ai vượt qua
được**. Không thể tạo bài kiểm tra, thêm câu hỏi, xuất bản hay xem đáp án.

**Cần làm.**

1. `PATCH /api/users/{id}/roles` — gán và gỡ vai trò cho một tài khoản. Chỉ `ROLE_ADMIN`
   gọi được:

   ```java
   if (!user.hasRole(Roles.ADMIN)) {
       throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ quản trị viên mới được đổi vai trò");
   }
   ```

   Xem `QuizController.requireQuizManager` làm mẫu.

2. **Tài khoản admin đầu tiên.** Đây là bài toán con gà quả trứng: cần admin mới gán được
   admin. Gợi ý một migration `V3__seed_admin.sql` tạo sẵn một tài khoản admin với mật khẩu
   đã băm BCrypt, ghi rõ trong tài liệu rằng đây là tài khoản dev và phải đổi khi triển khai
   thật.

   Đừng làm kiểu "email này trong biến môi trường thì tự lên admin lúc khởi động" — dễ quên
   tắt, và quên thì ai biết email đó đều thành admin.

3. **Đường dẫn mới `/api/users/**` phải khai route ở gateway** (quy tắc A4), nếu không thì
   gọi qua cổng 8080 nhận 404 dù service chạy đúng. `GatewayRouteCoverageTest` sẽ làm CI đỏ
   nếu quên — sửa trong `api-gateway/src/main/resources/application.properties`, route
   `[0]` của auth-service.

**Tự kiểm.** Đăng nhập bằng admin, gán `ROLE_INSTRUCTOR` cho một tài khoản khác, rồi tài
khoản đó gọi `POST /api/quizzes` phải thành công. Học viên thường gọi vẫn phải nhận 403.
Gọi **qua gateway** chứ không gọi thẳng cổng 8081.

**Tiện thể.** `/api/auth/me` đang tự đọc và kiểm token thủ công ngay trong controller. Filter
của shared-common đã làm việc đó trước khi request tới nơi rồi, nên đổi sang nhận
`AuthenticatedUser user` là bỏ được cả đoạn.

---

### duyd92689-debug — phân quyền và lọc trạng thái cho course-service

> Việc gấp nhất của bạn. Ba lỗ hổng, cùng nằm trong course-service.

**1. Không có lấy một dòng kiểm quyền.** Đếm `hasRole|hasAnyRole|AuthenticatedUser` trong
cả 4 controller ra đúng 0:

```
CategoryController.java:0   CourseController.java:0
LessonController.java:0     SectionController.java:0
```

Đã thử bằng tài khoản chỉ có `ROLE_STUDENT`: `POST /api/courses` tạo khóa học thành công.
Bất kỳ ai đăng nhập cũng tạo, sửa, xóa được khóa học, danh mục, chương và bài học.

**2. `instructorId` do client tự khai.** `CreateCourseRequest` có trường đó và
`CourseServiceImpl` ghi thẳng `.instructorId(request.getInstructorId())`. Gửi
`{"instructorId": 999}` thì database ghi đúng 999 — tạo khóa học đứng tên người khác.
Cùng họ với lỗi `?userId=` đã vá ở quiz-service và enrollment-service, chỉ khác là nó nằm
trong body.

**3. Khóa học chưa xuất bản đọc được không cần đăng nhập.** `public-paths` mở
`GET:/api/courses/**` và `GET:/api/lessons/**`, nhưng chỉ `getPublishedCourses` lọc trạng
thái. Ba hàm còn lại thì không, nên gọi **không kèm token** vẫn ra dữ liệu `DRAFT`:

```
GET /api/courses/{id}               → 200, đủ cả summary và description
GET /api/courses/slug/{slug}        → 200
GET /api/courses/instructor/{id}    → 200, liệt kê khóa DRAFT của giảng viên đó
GET /api/courses/{id}/curriculum    → 200, cả cây chương
```

**Cần làm.**

1. Bỏ `instructorId` khỏi `CreateCourseRequest`, thêm `AuthenticatedUser user` vào
   controller, dùng `user.userId()` (quy tắc A1).
2. Chặn theo vai trò: chỉ `ROLE_INSTRUCTOR` hoặc `ROLE_ADMIN` mới được tạo, sửa, xóa khóa
   học, danh mục, chương, bài học (A2). `QuizController.requireQuizManager` là bản mẫu gọn
   nhất — copy sang cả 4 controller.
3. Sửa được thì chặn luôn ở tầng dữ liệu: giảng viên chỉ sửa và xóa được khóa học của
   chính mình, trừ admin.
4. Lọc trạng thái cho ba hàm đọc công khai và cho `getCurriculumByCourseId` (A3). Cách
   làm: khách và học viên chỉ thấy `PUBLISHED`; chủ khóa học và admin thấy tất cả.

**Tự kiểm.**

```bash
# Học viên thường tạo khóa học: phải 403
curl -i -X POST localhost:8080/api/courses -H "Authorization: Bearer $TOKEN_HOC_VIEN" ...

# Giảng viên tạo, body cố tình gửi instructorId của người khác:
#   instructor_id trong database phải là id của chính người gọi

# Khóa học DRAFT, gọi KHÔNG token: phải 404 (đừng trả 403 — 403 là xác nhận nó có tồn tại)
curl -i localhost:8080/api/courses/<id-draft>
```

---

### duyd92689-debug — phát sự kiện `course.updated`

> Tên cũ trong bảng này là `course.published`. Đã đổi — lý do ở dưới.

**Vấn đề.** enrollment-service cần biết khóa học nào tồn tại và đang mở, nhưng nó không
được phép đọc `course_db` — đó là nguyên tắc database-per-service. Nó giữ một bản sao trong
bảng `course_snapshots`, và bản sao đó phải do course-service báo sang qua Kafka.

Hiện bảng đó rỗng và không có gì đổ vào, nên `POST /api/enrollments` luôn trả 404.

**Sự kiện đã có sẵn:** `CourseUpdatedEvent` trong shared-common. Nó mang **ảnh chụp toàn
bộ** khóa học, kể cả `status`, chứ không chỉ báo "vừa xuất bản". Nếu chỉ báo lúc xuất bản
thì lưu trữ khóa học sẽ không phát gì, và học viên vẫn ghi danh được vào khóa đã đóng.
Chi tiết ở [shared-contracts.md](shared-contracts.md#courseupdatedevent-khác-các-sự-kiện-còn-lại).

**Cần làm.**

1. Thêm **hai** dependency vào `course-service/pom.xml` — hiện chưa có dòng Kafka nào:

   ```xml
   <dependency>
       <groupId>org.springframework.kafka</groupId>
       <artifactId>spring-kafka</artifactId>
   </dependency>
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-kafka</artifactId>
   </dependency>
   ```

   Thiếu dòng thứ hai là service chạy êm mà không gửi gì, log không nhắc tới Kafka lấy một
   lần. Xem [bẫy số 1](#1-auto-configuration-nằm-ở-module-riêng).

2. Phát `CourseUpdatedEvent` lên `KafkaTopics.COURSE_EVENTS` trong ba trường hợp:

   | Khi nào | Ở đâu |
   |---|---|
   | Khóa học chuyển sang `PUBLISHED` | `changeCourseStatus` |
   | Sửa một khóa đang `PUBLISHED` | `updateCourse`, và khi thêm/xóa bài học (đổi `totalLessons`) |
   | Khóa đang `PUBLISHED` chuyển sang trạng thái khác | `changeCourseStatus` |

   Bản nháp chưa từng xuất bản thì **không** phát.

3. Khóa message là **`courseId`**, không phải `userId` như các sự kiện khác — để mọi phiên
   bản của cùng một khóa học tới đúng thứ tự.

4. Gửi bằng `KafkaTemplate<String, String>` + `tools.jackson.databind.ObjectMapper`, đừng
   dùng `JsonSerializer`. Chép `QuizEventPublisher` trong quiz-service là nhanh nhất — xem
   [bẫy số 2](#2-spring-kafka-vẫn-dùng-jackson-2-boot-4-đã-sang-jackson-3).

```java
CourseUpdatedEvent event = CourseUpdatedEvent.of(
        course.getId(), course.getTitle(), course.getSlug(), course.getThumbnailUrl(),
        course.getInstructorId(), course.getInstructorName(), course.getTotalLessons(),
        course.getStatus().name());
```

**Tự kiểm.** Bật Kafka và Kafka UI:

```bash
docker compose up -d kafka kafka-ui
```

Xuất bản một khóa học, mở http://localhost:8090 → Topics → `elearning.course.events` →
Messages. Phải thấy một message có key là id khóa học, `eventType` là `course.updated`,
`status` là `PUBLISHED`. Lưu trữ khóa đó, phải thấy message thứ hai với `status` là
`ARCHIVED`.

Đừng tìm trong log của notification-service: nó có nhận, nhưng dòng "bỏ qua" ghi ở mức
`DEBUG` nên cấu hình mặc định không in ra.

---

### duyd92689-debug — trả nội dung bài học

**Vấn đề.** `LessonResponse` có `title`, `type`, `durationSeconds`, `position`,
`isPreview`, `resources` — nhưng **không có `content` lẫn `contentUrl`**. Hai cột đó có
trong bảng `lessons`, có trong entity, và không một chỗ nào trong course-service map chúng
ra response.

Nghĩa là một website học trực tuyến hiện không có đường nào để xem bài học.

**Cần làm.** Thêm `content` và `contentUrl` vào `LessonResponse`, nhưng **không trả cho
mọi người** — đây là phần đáng tiền của khóa học:

- Bài có `isPreview = true`: ai cũng xem được, kể cả khách chưa đăng nhập.
- Bài thường: chỉ trả khi người gọi là chủ khóa học, là admin, hoặc đã ghi danh.

Phần "đã ghi danh" phải hỏi enrollment-service, mà course-service chưa gọi sang service nào
bao giờ. Làm sau cùng và hỏi nhóm trưởng trước khi bắt đầu — có thể để tạm mức "đã đăng
nhập" rồi siết sau, miễn là ghi rõ `// TODO` kèm lý do.

**Tự kiểm.** Bài `isPreview = true` gọi không token phải thấy `content`. Bài thường gọi
bằng token người lạ không được thấy.

---

### phamquyet19042005-netizen — gửi outbox lên Kafka

**Vấn đề.** Bảng `outbox_events` đang được ghi đúng trong cùng transaction với nghiệp vụ,
nhưng không ai đọc nó. `published_at` của mọi dòng đều là NULL.

**Trước tiên:** `enrollment-service/pom.xml` chưa có dòng Kafka nào. Thêm cả `spring-kafka`
lẫn `spring-boot-kafka` — thiếu cái thứ hai là service chạy êm mà không gửi gì. Xem
[bẫy số 1](#1-auto-configuration-nằm-ở-module-riêng). Việc nạp snapshot bên dưới cũng cần
hai dòng này, nên làm một lần cho cả hai.

**Cần làm.** Một `@Scheduled` chạy mỗi vài giây:

```java
List<OutboxEvent> chuaGui = outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
// gửi lên KafkaTopics.ENROLLMENT_EVENTS, gửi xong thì set published_at
```

Trường `payload` đã là chuỗi JSON sẵn nên gửi thẳng được, không phải chuyển đổi lại.

**Tự kiểm — lần đầu tiên nhìn thấy kết quả thật.** Chạy kèm notification-service, ghi danh
một khóa học, rồi gọi bằng token của chính học viên đó:

```bash
curl -H "Authorization: Bearer <token>" localhost:8080/api/notifications
```

Phải thấy *"Bạn đã ghi danh khóa học ..."*. Đây là chuỗi hoàn chỉnh đầu tiên đi qua ba
service của hệ thống.

---

### phamquyet19042005-netizen — nạp `course_snapshots`

**Sự kiện đã có sẵn:** `CourseUpdatedEvent` (loại `course.updated`) trong shared-common.
Tên cũ trong bảng này là `course.published` — đã đổi, lý do ở
[shared-contracts.md](shared-contracts.md#courseupdatedevent-khác-các-sự-kiện-còn-lại).

**Cần làm.** Nghe topic `KafkaTopics.COURSE_EVENTS`, lọc `eventType` bằng
`EventTypes.COURSE_UPDATED`, rồi **ghi đè cả dòng** trong `course_snapshots` theo `courseId`.
Mỗi trường của sự kiện khớp đúng một cột của bảng.

Không cần viết code chờ duyd: sự kiện đã có, bạn tự tạo message mẫu bằng Kafka UI để test
(xem phần Tự kiểm).

Đọc String rồi tự phân tích bằng `tools.jackson.databind.ObjectMapper`, đừng dùng
`JsonDeserializer`. Chép `KafkaEventConsumer` trong notification-service — xem
[bẫy số 2](#2-spring-kafka-vẫn-dùng-jackson-2-boot-4-đã-sang-jackson-3).

**Không cần bảng `processed_events` ở đây** — khác với phần outbox và khác với
notification-service. Sự kiện này là ảnh chụp, không phải "một việc vừa xảy ra": nhận trùng
hai lần thì ghi đè hai lần cùng một giá trị, kết quả vẫn đúng. Khử trùng lặp chỉ cần khi xử
lý hai lần gây ra hậu quả hai lần, như gửi hai email.

Cũng vì thế mà ở đây **dùng `save()` là đúng**, dù [notifications.md](notifications.md) dặn
đừng dùng cho bảng khử trùng lặp. `save()` với `@Id` khác null sẽ tìm dòng cũ, có thì UPDATE,
không có thì INSERT — đúng thứ cần cho một bản sao. Cái bẫy bên kia là do ở đó cần *lỗi* khi
trùng; ở đây thì không.

Nhớ gán `syncedAt = Instant.now()` mỗi lần ghi đè. `CourseSnapshot` chỉ điền trường này
trong `@PrePersist`, nên lúc cập nhật Hibernate ghi lại giá trị cũ, và
`ON UPDATE CURRENT_TIMESTAMP` của MySQL không chạy vì cột đã được gán tường minh.

**Tự kiểm.** Không cần đợi course-service. Bật Kafka và Kafka UI:

```bash
docker compose up -d kafka kafka-ui
```

Mở http://localhost:8090 → Topics → `elearning.course.events` → Produce Message, key là
`3`, value dán ví dụ JSON trong
[shared-contracts.md](shared-contracts.md#courseupdatedevent-khác-các-sự-kiện-còn-lại).
Rồi:

1. `SELECT * FROM course_snapshots` trong `enrollment_db` thấy dòng `course_id = 3`.
2. Ghi danh khóa 3 qua gateway phải thành công thay vì 404.
3. Gửi lại đúng message đó lần nữa: vẫn một dòng, không lỗi.
4. Gửi bản có `"status": "ARCHIVED"`: ghi danh khóa 3 phải bị từ chối.

---

### hiepdeptrai0111 — chuyển phát sự kiện sang outbox

> Ưu tiên thấp. Ba việc ở trên chặn demo, việc này thì không — cứ làm sau khi nhóm thông
> được chuỗi ghi danh. Nếu rảnh sớm thì báo nhóm trưởng.

**Vấn đề.** `QuizAttemptServiceImpl.submitAttempt` gửi Kafka **bên trong transaction**, ngay
sau khi lưu bài làm:

```java
QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);   // dòng 204
...
quizEventPublisher.publishQuizGraded(event);                       // dòng 216, vẫn trong transaction
```

Nếu transaction rollback sau dòng 216 thì sự kiện đã bay đi rồi, và học viên nhận thông báo
về một điểm số không tồn tại trong database. Ngược lại, gửi Kafka hỏng thì bài làm vẫn được
lưu nhưng không ai biết để gửi lại.

Đây là bài toán ghi hai nơi (dual write) mà mẫu outbox sinh ra để giải.

**Cần làm.** Giống enrollment-service: thêm bảng `outbox_events` cho `quiz_db` bằng một
migration mới, ghi sự kiện vào bảng đó trong cùng transaction với bài làm, rồi một
`@Scheduled` riêng đọc và gửi lên Kafka.

Đợi phamquyet19042005-netizen làm xong phần gửi bên enrollment-service rồi copy cách làm,
để hai service không mỗi bên một kiểu.

**Tự kiểm.** Nộp bài khi **tắt Kafka** (`spring.kafka.enabled=false`): bài làm vẫn lưu, và
có một dòng trong `outbox_events` với `published_at` là NULL. Bật Kafka lại thì dòng đó được
gửi đi và thông báo xuất hiện.

---

### Cả nhóm — chuẩn hóa đường dẫn API

> Ưu tiên thấp nhưng **có hạn chót**: phải xong trước khi ai đó bắt đầu viết frontend.
> Đổi đường dẫn sau khi frontend đã gọi thì gãy hết và không ai muốn sửa nữa.

**Vấn đề.** Năm service đang đặt đường dẫn theo năm kiểu khác nhau. Không sai về chức năng,
nhưng người viết frontend sẽ phải nhớ mỗi service một quy ước, và đây là thứ dễ mất điểm
nhất khi chấm.

Luật đã viết ở [api-conventions.md](api-conventions.md), phần B. Mỗi người sửa service của
mình:

| Người | Đang là | Đổi thành | Quy tắc |
|---|---|---|---|
| duyd | `GET /api/courses/instructor/{id}` | `GET /api/courses?instructorId={id}` | B2 |
| duyd | `DELETE /api/lessons/resources/{id}` | `DELETE /api/lessons/{lessonId}/resources/{id}` | B3 |
| duyd | `POST /api/sections` (courseId trong body) | `POST /api/courses/{courseId}/sections` | B3 |
| duyd | `POST /api/lessons` (sectionId trong body) | `POST /api/sections/{sectionId}/lessons` | B3 |
| hiepdeptrai | `GET /api/quizzes/course/{id}` | `GET /api/quizzes?courseId={id}` | B2 |
| hiepdeptrai | `PATCH /api/quizzes/{id}/publish` và `/archive` | `PATCH /api/quizzes/{id}/status` + body | B4 |
| hiepdeptrai | `GET /api/quizzes/{id}/attempts/history` | `GET /api/quizzes/{id}/attempts` | B5 |
| hiepdeptrai | `GET /api/quizzes/attempts/{attemptId}` | `GET /api/attempts/{attemptId}` | B7 |
| phamquyet | `GET /api/enrollments/my-courses` | `GET /api/enrollments` | B6 |
| phamquyet | `DELETE /api/enrollments/course/{id}` | `DELETE /api/enrollments?courseId={id}` | B2 |
| phamquyet | `PATCH /api/enrollments/{id}/cancel` | `PATCH /api/enrollments/{id}/status` + body | B4 |
| phamquyet | `POST /api/progress/lesson` | `PUT /api/lessons/{lessonId}/progress` | B1, B2 |
| phamquyet | `GET /api/progress/course/{id}` | `GET /api/progress?courseId={id}` | B2 |

**Lưu ý.** Đổi đường dẫn là đổi cả route ở gateway (A4) và `public-paths` nếu endpoint đó
công khai. Đổi `/api/progress` thành `/api/lessons/{id}/progress` thì đường dẫn đó rơi sang
route của course-service — báo nhóm trưởng trước, đừng tự đổi.

Ai làm xong phần của mình thì mở một pull request riêng, đừng gộp chung với việc khác:
đường dẫn đổi là frontend phải sửa theo, cần nhìn thấy rõ trong lịch sử.

---

## Đã xong

| Ngày | PR | Việc | Người |
|---|---|---|---|
| 25/09 | #25 | Bỏ `createdBy` khỏi body, người tạo bài kiểm tra lấy từ token | hiepdeptrai0111 |
| 25/09 | #24 | `CourseUpdatedEvent`: hợp đồng sự kiện khóa học cho `course_snapshots` | Hiếu |
| 25/09 | #23 | Sửa route `/api/progress` bị sót ở gateway, `?sort=` sai trả 400, quy tắc viết API | Hiếu |
| 19/09 | #22 | Biến danh sách phân công thành bảng theo dõi sống | Hiếu |
| 19/09 | #21 | Bỏ `?userId=` ở 4 endpoint làm bài | hiepdeptrai0111 |
| 19/09 | #20 | Bảng theo dõi công việc | Hiếu |
| 19/09 | #13 | Chặn xem đáp án và quản lý bài kiểm tra theo vai trò, nối Flyway | hiepdeptrai0111 |
| 19/09 | #15 | enrollment-service: ghi danh, tiến độ, chứng chỉ | phamquyet19042005-netizen |
| 19/09 | #18 | notification-service: dựng thông báo từ sự kiện Kafka | Hiếu |
| 19/09 | #17 | CI chặn sửa migration đã merge | Hiếu |
| 19/09 | #16 | CI đối chiếu entity với schema bằng MySQL thật | Hiếu |
| 19/09 | #14 | Nối auto-configuration của Flyway | Hiếu |
| 19/09 | #11 | course-service dùng `Instant` thay `LocalDateTime` | duyd92689-debug |
| 18/09 | #12 | Xác thực JWT ở gateway và từng service | Hiếu |
| 18/09 | #9 | course-service: danh mục, khóa học, chương trình học | duyd92689-debug |
| 18/09 | #8 | auth-service: đăng ký, đăng nhập, JWT | quocluibotre |
| 18/09 | #7 | quiz-service: bài kiểm tra, câu hỏi, chấm điểm | hiepdeptrai0111 |

---

## Hai cái bẫy của Spring Boot 4

Cả hai đều khiến service chạy bình thường mà **không báo lỗi gì**, nên rất tốn thời gian nếu
không biết trước. Ai làm phần Kafka đều sẽ gặp.

### 1. Auto-configuration nằm ở module riêng

Spring Boot 4 tách auto-configuration ra khỏi thư viện gốc. Khai thư viện thôi là không đủ:

| Muốn dùng | Phải khai thêm |
|---|---|
| Flyway | `org.springframework.boot:spring-boot-flyway` |
| Kafka | `org.springframework.boot:spring-boot-kafka` |
| MockMvc trong test | `org.springframework.boot:spring-boot-starter-webmvc-test` |

Thiếu module này thì `@KafkaListener` không được đăng ký, hoặc migration không chạy — và
**log không có lấy một dòng nào nhắc tới Kafka hay Flyway**. Service khởi động sạch sẽ, health
báo UP, mọi thứ trông bình thường.

Ở Spring Boot 3 thì chỉ cần thư viện gốc, nên mọi hướng dẫn trên mạng đều thiếu dòng này.

Kèm theo đó là vài lớp bị đổi gói, tìm theo tên cũ sẽ không ra:

| Lớp | Gói cũ (Boot 3) | Gói mới (Boot 4) |
|---|---|---|
| `AutoConfigureMockMvc` | `...boot.test.autoconfigure.web.servlet` | `...boot.webmvc.test.autoconfigure` |
| `ErrorWebExceptionHandler` | `...boot.web.reactive.error` | `...boot.webflux.error` |
| `PropertyReferenceException` | `...data.mapping` | `...data.core` |

### 2. Spring Kafka vẫn dùng Jackson 2, Boot 4 đã sang Jackson 3

`JsonSerializer` và `JsonDeserializer` của Spring Kafka 4 chạy trên
`com.fasterxml.jackson` (Jackson 2), trong khi Spring Boot 4 dùng `tools.jackson`
(Jackson 3). Bản Jackson 2 lọt vào classpath qua thư viện khác lại không có module xử lý
kiểu thời gian, nên mọi sự kiện đều chết ở trường `occurredAt`:

```
InvalidDefinitionException: Java 8 date/time type `java.time.Instant` not supported
by default (through reference chain: QuizGradedEvent["occurredAt"])
```

**Cách làm đúng:** dùng `StringSerializer` / `StringDeserializer`, rồi tự chuyển JSON bằng
`tools.jackson.databind.ObjectMapper` (tiêm thẳng vào constructor, Spring có sẵn bean này).
Xem `QuizEventPublisher` trong quiz-service và `KafkaEventConsumer` trong
notification-service.

Thêm một lợi ích: message không còn header `__TypeId__` chứa tên lớp Java, nên đổi tên lớp
bên gửi không làm hỏng bên nhận. Consumer định tuyến theo trường `eventType` trong nội dung.

---

## Trước khi code

### Đồng bộ nhánh

Tất cả các PR đều merge kiểu squash, nên nhánh cũ sẽ xung đột với chính code mình vừa được
merge. Chạy một lần trước khi bắt đầu, **thay `quiz-service` bằng tên nhánh của bạn ở cả ba
chỗ**:

```bash
git fetch origin
git checkout quiz-service
git log --oneline origin/main..quiz-service     # in ra dòng nào là còn việc chưa merge: DỪNG, hỏi nhóm trưởng
git reset --hard origin/main
git push --force-with-lease origin quiz-service
```

**Lệnh push phải ghi tên nhánh.** Bản cũ trong file này viết `git push --force-with-lease`
trống trơn và thiếu cả `git checkout`, nên đang đứng ở nhánh nào là đè lên nhánh đó — ngày
25/09 nhánh `auth-service` đã bị đẩy từ máy của một người không phụ trách nó. Lần đó may là
nhánh không có gì chưa merge nên không mất code, nhưng lần sau thì chưa chắc. Ghi rõ
`origin quiz-service` thì dù lỡ đứng nhầm nhánh, lệnh cũng chỉ đụng đúng nhánh của bạn.

### Nếu có sửa entity hoặc migration

```bash
docker compose up -d mysql
./mvnw -DskipTests package
bash scripts/verify-schema.sh
```

`./mvnw verify` **không** bắt được lệch giữa entity và migration, vì test chạy trên H2 với
`ddl-auto=create-drop` — schema dựng từ chính entity nên hai bên không bao giờ gặp nhau.

Và **đừng sửa file migration đã vào main**. Flyway lưu checksum, sửa lại thì máy nào đã chạy
sẽ không khởi động được, máy nào chưa chạy thì nhận schema khác. Đổi schema thì thêm file mới
`V<n>__*.sql` với câu `ALTER TABLE`. CI có job kiểm việc này.

### Nếu có thêm hoặc sửa endpoint

Đọc [api-conventions.md](api-conventions.md) và chạy phần tự kiểm ở cuối tài liệu đó. Hai
việc hay quên nhất:

- Khai route ở gateway cho tiền tố mới (A4) — CI có `GatewayRouteCoverageTest` kiểm.
- Gọi thử **qua gateway cổng 8080**, không gọi thẳng cổng của service. Gọi thẳng thì bỏ qua
  cả định tuyến lẫn lớp kiểm token vòng ngoài, đúng hai thứ hay hỏng nhất.

### Lấy danh tính người gọi

Không controller nào được nhận `userId`, `instructorId` hay `createdBy` từ client, dù qua
query param hay qua body. Nhận `AuthenticatedUser user` rồi dùng `user.userId()`. Chi tiết ở
[authentication.md](authentication.md).

### Tài liệu nên đọc

| Làm phần nào | Đọc gì |
|---|---|
| Bất cứ endpoint nào | [api-conventions.md](api-conventions.md) — luật đặt đường dẫn, mã lỗi, phân quyền |
| Bất cứ endpoint nào | [authentication.md](authentication.md) — lấy danh tính, chặn theo vai trò |
| Kafka, sự kiện | [notifications.md](notifications.md), [shared-contracts.md](shared-contracts.md) |
| Entity, migration | [database-design.md](database-design.md) |
| Quy ước chung | [CONTRIBUTING.md](../CONTRIBUTING.md) |
