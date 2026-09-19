# Bảng theo dõi công việc

> **Cập nhật lần cuối:** 19/09/2026 22:05 — `main` ở `caffb94`
>
> File này là nơi duy nhất ghi ai đang làm gì. Xong một việc thì nhóm trưởng cập nhật ngay
> tại đây, nên **cứ `git pull` là biết việc tiếp theo của mình**, không phải hỏi ai.

- [Việc của bạn](#việc-của-bạn)
- [Trạng thái hệ thống](#trạng-thái-hệ-thống)
- [Chi tiết từng việc](#chi-tiết-từng-việc)
- [Đã xong](#đã-xong)
- [Hai cái bẫy của Spring Boot 4](#hai-cái-bẫy-của-spring-boot-4)
- [Trước khi code](#trước-khi-code)

## Việc của bạn

| Người | Service | Việc đang mở | Ưu tiên | Cỡ |
|---|---|---|---|---|
| quocluibotre | auth-service | [API gán vai trò + admin đầu tiên](#quocluibotre--api-gán-vai-trò) | **Cao nhất** — chặn cả nhóm | ~2h |
| duyd92689-debug | course-service | [Phát `course.published`](#duyd92689-debug--phát-sự-kiện-coursepublished) + [bỏ `instructorId` khỏi body](#duyd92689-debug--bỏ-instructorid-khỏi-request-body) | Cao — chặn phamquyet | ~2h |
| phamquyet19042005-netizen | enrollment-service | [Gửi outbox](#phamquyet19042005-netizen--gửi-outbox-lên-kafka) + [nạp snapshot](#phamquyet19042005-netizen--nạp-course_snapshots) | Cao | ~3h |
| hiepdeptrai0111 | quiz-service | [Chuyển phát sự kiện sang outbox](#hiepdeptrai0111--chuyển-phát-sự-kiện-sang-outbox) | Thấp — làm sau cùng | ~2h |
| Hiếu (nhóm trưởng) | shared-common | `CoursePublishedEvent` | Cao — chặn 2 người | 30 phút |

**Thứ tự.** quocluibotre làm trước vì không có tài khoản giảng viên thì cả nhóm không test
được phần tạo bài kiểm tra. `CoursePublishedEvent` làm song song vì nó chặn hai người. Ba
việc còn lại chạy song song sau đó.

## Trạng thái hệ thống

Năm service đã có code, database chạy tự động bằng Flyway, xác thực JWT hoạt động ở cả
gateway lẫn từng service.

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

Ba việc đó xong là demo chạy trọn vẹn: đăng ký → ghi danh → học → làm bài → nhận thông báo
→ chứng chỉ.

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

**Tự kiểm.** Đăng nhập bằng admin, gán `ROLE_INSTRUCTOR` cho một tài khoản khác, rồi tài
khoản đó gọi `POST /api/quizzes` phải thành công. Học viên thường gọi vẫn phải nhận 403.

**Tiện thể.** `/api/auth/me` đang tự đọc và kiểm token thủ công ngay trong controller. Filter
của shared-common đã làm việc đó trước khi request tới nơi rồi, nên đổi sang nhận
`AuthenticatedUser user` là bỏ được cả đoạn.

---

### duyd92689-debug — phát sự kiện `course.published`

**Vấn đề.** enrollment-service cần biết khóa học nào tồn tại và đã xuất bản, nhưng nó không
được phép đọc `course_db` — đó là nguyên tắc database-per-service. Nó giữ một bản sao trong
bảng `course_snapshots`, và bản sao đó phải do course-service báo sang qua Kafka.

Hiện bảng đó rỗng và không có gì đổ vào, nên `POST /api/enrollments` luôn trả 404.

**Cần làm.** Khi `changeCourseStatus` chuyển khóa học sang `PUBLISHED`, và khi `updateCourse`
sửa một khóa đã xuất bản, phát sự kiện `course.published` lên topic
`elearning.course.events`.

Nhóm trưởng sẽ thêm `CoursePublishedEvent` vào shared-common trước, bạn chỉ việc dùng.

Đọc [Hai cái bẫy của Spring Boot 4](#hai-cái-bẫy-của-spring-boot-4) trước khi bắt đầu —
đã có hai người vấp rồi.

**Tự kiểm.** Chạy kèm notification-service (nó nghe cả ba topic), xuất bản một khóa học, xem
log notification-service thấy dòng `Bỏ qua sự kiện loại course.published` — nghĩa là message
đã tới nơi.

---

### duyd92689-debug — bỏ `instructorId` khỏi request body

**Vấn đề.** `CreateCourseRequest` có trường `instructorId` do client tự khai, và
`CourseServiceImpl` ghi thẳng vào khóa học:

```java
.instructorId(request.getInstructorId())
```

Nên ai cũng tạo được khóa học đứng tên giảng viên khác.

Cùng họ với lỗi `?userId=` vừa vá ở quiz-service và enrollment-service, chỉ khác là nó nằm
trong body chứ không phải query param.

**Cần làm.** Bỏ `instructorId` khỏi `CreateCourseRequest`, thêm `AuthenticatedUser user` vào
controller, dùng `user.userId()`. Nhân tiện chặn luôn theo vai trò: chỉ `ROLE_INSTRUCTOR`
hoặc `ROLE_ADMIN` mới được tạo, sửa, xóa khóa học và danh mục.

`QuizController.requireQuizManager` là bản mẫu gọn nhất.

**Tự kiểm.** Học viên thường gọi `POST /api/courses` phải nhận 403. Giảng viên tạo khóa học
thì `instructor_id` trong database phải là id của chính họ, kể cả khi body có gửi kèm id
khác.

---

### phamquyet19042005-netizen — gửi outbox lên Kafka

**Vấn đề.** Bảng `outbox_events` đang được ghi đúng trong cùng transaction với nghiệp vụ,
nhưng không ai đọc nó. `published_at` của mọi dòng đều là NULL.

**Cần làm.** Một `@Scheduled` chạy mỗi vài giây:

```java
List<OutboxEvent> chuaGui = outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
// gửi lên KafkaTopics.ENROLLMENT_EVENTS, gửi xong thì set published_at
```

Trường `payload` đã là chuỗi JSON sẵn nên gửi thẳng được, không phải chuyển đổi lại.

**Tự kiểm — lần đầu tiên nhìn thấy kết quả thật.** Chạy kèm notification-service, ghi danh
một khóa học, rồi gọi bằng token của chính học viên đó:

```bash
curl -H "Authorization: Bearer <token>" localhost:8085/api/notifications
```

Phải thấy *"Bạn đã ghi danh khóa học ..."*. Đây là chuỗi hoàn chỉnh đầu tiên đi qua ba
service của hệ thống.

---

### phamquyet19042005-netizen — nạp `course_snapshots`

**Cần làm.** Nghe `course.published` trên topic `elearning.course.events`, ghi hoặc cập nhật
một dòng trong `course_snapshots`. Phụ thuộc vào việc của duyd92689-debug.

**Phải khử trùng lặp.** Kafka bảo đảm at-least-once nên một sự kiện có thể tới nhiều lần.
notification-service đã làm sẵn bằng bảng `processed_events` với khóa chính là `event_id`,
copy cách đó là được.

Có một bẫy JPA ghi rõ trong [docs/notifications.md](notifications.md): **không dùng `save()`**
để ghi sổ khử trùng lặp. `save()` chọn INSERT hay UPDATE dựa vào `@Id` có null không, mà
khóa ở đây là UUID do bên gửi sinh ra nên luôn khác null — thành ra nó UPDATE đè lên dòng cũ,
không hề có lỗi trùng khóa, và cơ chế chống trùng im lặng mất tác dụng. Dùng câu INSERT
tường minh.

**Tự kiểm.** Xuất bản một khóa học bên course-service, kiểm tra
`SELECT * FROM course_snapshots` trong `enrollment_db` thấy dòng tương ứng, rồi ghi danh
khóa đó phải thành công thay vì 404.

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

## Đã xong

| Ngày | PR | Việc | Người |
|---|---|---|---|
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

Thiếu module này thì `@KafkaListener` không được đăng ký, hoặc migration không chạy — và
**log không có lấy một dòng nào nhắc tới Kafka hay Flyway**. Service khởi động sạch sẽ, health
báo UP, mọi thứ trông bình thường.

Ở Spring Boot 3 thì chỉ cần thư viện gốc, nên mọi hướng dẫn trên mạng đều thiếu dòng này.

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
merge. Chạy một lần trước khi bắt đầu:

```bash
git fetch origin
git reset --hard origin/main
git push --force-with-lease
```

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

### Lấy danh tính người gọi

Không controller nào được nhận `userId` hay `instructorId` từ client, dù qua query param hay
qua body. Nhận `AuthenticatedUser user` rồi dùng `user.userId()`. Chi tiết ở
[authentication.md](authentication.md).

### Tài liệu nên đọc

| Làm phần nào | Đọc gì |
|---|---|
| Bất cứ endpoint nào | [authentication.md](authentication.md) — lấy danh tính, chặn theo vai trò |
| Kafka, sự kiện | [notifications.md](notifications.md), [shared-contracts.md](shared-contracts.md) |
| Entity, migration | [database-design.md](database-design.md) |
| Quy ước chung | [CONTRIBUTING.md](../CONTRIBUTING.md) |
