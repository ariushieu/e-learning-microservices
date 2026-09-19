# Phân công công việc

*Chốt ngày 19/09/2026, khi `main` ở commit `171cec4`.*

Tài liệu này ghi việc còn lại của từng người, kèm cách tự kiểm tra kết quả. Đọc phần
[Trạng thái hiện tại](#trạng-thái-hiện-tại) trước để biết mình đang đứng ở đâu.

- [Trạng thái hiện tại](#trạng-thái-hiện-tại)
- [Bảng phân công](#bảng-phân-công)
- [Việc của từng người](#việc-của-từng-người)
- [Hai cái bẫy của Spring Boot 4](#hai-cái-bẫy-của-spring-boot-4)
- [Trước khi code](#trước-khi-code)

## Trạng thái hiện tại

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

## Bảng phân công

| Người | Service | Việc | Chặn ai | Cỡ |
|---|---|---|---|---|
| Hiếu (nhóm trưởng) | shared-common | `CoursePublishedEvent` | duyd, phamquyet | 30 phút |
| quocluibotre | auth-service | API gán vai trò + admin đầu tiên | cả nhóm | ~2h |
| hiepdeptrai0111 | quiz-service | Bỏ `?userId=` ở 4 endpoint làm bài | không ai | 15 phút |
| duyd92689-debug | course-service | Phát sự kiện `course.published` | phamquyet | ~1,5h |
| phamquyet19042005-netizen | enrollment-service | Gửi outbox + nạp snapshot | không ai | ~3h |

**Thứ tự:** quocluibotre làm trước — không có giảng viên thì cả nhóm không test được phần
tạo bài kiểm tra. `CoursePublishedEvent` làm song song vì nó chặn hai người. Ba việc còn lại
chạy song song sau đó.

---

## Việc của từng người

### quocluibotre — auth-service

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

### hiepdeptrai0111 — quiz-service

**Vấn đề.** `QuizAttemptController` còn bốn endpoint nhận `?userId=` do client tự khai.

Đã thử với hai tài khoản học viên thật, A là id 3 và B là id 4. Mọi lệnh dưới đây B dùng
**token hợp lệ của chính B**, chỉ đổi tham số:

```
GET  /api/quizzes/attempts/3?userId=3          → 200, đọc nguyên bài làm của A
GET  /api/quizzes/1/attempts/history?userId=3  → 200, lịch sử của A
POST /api/quizzes/1/attempts?userId=3          → 201, mở lượt mang tên A
POST /api/quizzes/attempts/4/submit?userId=3   → 200, score 0.00, passed=false
```

Hồ sơ của A sau đó:

```
id  user_id  attempt_no  score    passed
3   3        1           100.00   1      ← A tự làm
4   3        2           0.00     0      ← B cài vào
```

B vừa phá điểm của A, vừa đốt một lượt trong `maxAttempts` của A.

**Cần làm.** Bỏ `@RequestParam Long userId`, thêm `AuthenticatedUser user`, truyền
`user.userId()` xuống service. Tầng service giữ nguyên chữ ký, không phải sửa gì.

```java
@PostMapping("/{quizId}/attempts")
public ApiResponse<QuizAttemptResponse> startAttempt(
        @PathVariable Long quizId,
        AuthenticatedUser user) {
    return ApiResponse.ok(quizAttemptService.startAttempt(quizId, user.userId()), "...");
}
```

`EnrollmentController` có bản mẫu đủ sáu endpoint. Nhớ sửa Postman collection nếu có dùng.

**Tự kiểm.** Hai tài khoản học viên, A nộp bài, B dùng token của B gọi
`GET /api/quizzes/attempts/{id}?userId=<A>` — phải trả 404, không được trả bài của A.

---

### duyd92689-debug — course-service

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
đã tới nơi. Sau khi phamquyet làm xong phần nhận thì kiểm bằng cách ghi danh khóa đó.

---

### phamquyet19042005-netizen — enrollment-service

Hai việc, làm song song được.

#### 1. Gửi outbox lên Kafka

Bảng `outbox_events` đang được ghi đúng trong cùng transaction với nghiệp vụ, nhưng không ai
đọc nó. `published_at` của mọi dòng đều là NULL.

Cần một `@Scheduled` chạy mỗi vài giây:

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

#### 2. Nạp `course_snapshots` từ Kafka

Nghe `course.published` trên topic `elearning.course.events`, ghi hoặc cập nhật một dòng
trong `course_snapshots`. Phụ thuộc vào việc của duyd92689-debug.

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

Tính tới lúc chốt tài liệu này: auth-service và course-service đang sau main 5 commit,
enrollment-service sau 5 commit và còn 2 commit cũ thừa, quiz-service đã đồng bộ.

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

### Tài liệu nên đọc

| Làm phần nào | Đọc gì |
|---|---|
| Bất cứ endpoint nào | [authentication.md](authentication.md) — lấy danh tính, chặn theo vai trò |
| Kafka, sự kiện | [notifications.md](notifications.md), [shared-contracts.md](shared-contracts.md) |
| Entity, migration | [database-design.md](database-design.md) |
| Quy ước chung | [CONTRIBUTING.md](../CONTRIBUTING.md) |
