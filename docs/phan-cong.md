# Bảng theo dõi công việc

> **Cập nhật lần cuối:** 06/10/2026 — `main` ở `6e19e13`
>
> File này là nơi duy nhất ghi ai đang làm gì. Xong một việc thì nhóm trưởng cập nhật ngay
> tại đây, nên **cứ `git pull` là biết việc tiếp theo của mình**, không phải hỏi ai.

- [Việc của bạn](#việc-của-bạn)
- [Khi nào test toàn bộ API bằng Postman](#khi-nào-test-toàn-bộ-api-bằng-postman)
- [Quy tắc viết API](#quy-tắc-viết-api)
- [Trạng thái hệ thống](#trạng-thái-hệ-thống)
- [Chi tiết từng việc](#chi-tiết-từng-việc)
- [Đã xong](#đã-xong)
- [Hai cái bẫy của Spring Boot 4](#hai-cái-bẫy-của-spring-boot-4)
- [Trước khi code](#trước-khi-code)

## Việc của bạn

| Người | Service | Việc đang mở | Ưu tiên | Cỡ |
|---|---|---|---|---|
| hiepdeptrai0111 | quiz-service | [Chủ sở hữu của bài kiểm tra](#hiepdeptrai0111--chủ-sở-hữu-của-bài-kiểm-tra) | **Cao** — lỗ hổng | ~1h30 |
| duyd92689-debug | course-service | [Học viên giữ quyền học khi khóa bị lưu trữ](#duyd92689-debug--học-viên-giữ-quyền-học-khi-khóa-bị-lưu-trữ) | Trung bình | ~1h |
| phamquyet19042005-netizen | enrollment-service | [Thử lại có giới hạn, message hỏng sang `.DLT`](#phamquyet19042005-netizen--thử-lại-có-giới-hạn-message-hỏng-sang-dlt) | Thấp | ~45 phút |
| Hiếu | cả hệ thống | [Collection Postman chung](#hiếu--collection-postman-chung) | **Cao** — đợt test chờ việc này | ~2h |
| **Cả nhóm** | service của mình | [Đợt test Postman](#cả-nhóm--đợt-test-postman) | Sau khi có collection chung | ~2h/người |

**Đã đủ bốn điều kiện để test toàn bộ API** (#41, #42 vừa merge). Thứ tự:

1. Hiếu dựng collection chung — báo trong nhóm khi xong.
2. Mỗi người chạy file tình huống test của **service mình** (thầy yêu cầu mỗi người tự kiểm
   service mình) và ghi biên bản.
3. Ca nào FAIL thì người phụ trách service sửa trong PR riêng.

Việc của hiep nên xong **trước** khi chạy `quiz.md`, nếu không các ca phân quyền bài kiểm tra
chắc chắn FAIL — đã biết trước, không cần test để phát hiện.

**Tài khoản giảng viên và admin tạo bằng API** (#27), không cần SQL:

```
Admin có sẵn: admin@elearning.hunre.edu.vn / Admin@123456   (tài khoản dev — đổi khi triển khai thật)
Cấp quyền:    PATCH /api/users/{id}/roles   {"roles": ["ROLE_STUDENT", "ROLE_INSTRUCTOR"]}
```

Người được cấp quyền phải **đăng nhập lại** mới nhận vai trò mới — token cũ vẫn mang vai trò
cũ tới khi hết hạn.

## Khi nào test toàn bộ API bằng Postman

**Sẵn sàng — đủ 4/4.** Bắt đầu khi có collection chung, cách làm ở
[Đợt test Postman](#cả-nhóm--đợt-test-postman).

| # | Điều kiện | Xong ở |
|---|---|---|
| 1 | API gán vai trò | #27 |
| 2 | Phân quyền course-service, lọc khóa `DRAFT` | #31, #37 |
| 3 | Ghi danh chạy thông (`course.updated` + nạp snapshot + gửi outbox) | #29, #31, #41 |
| 4 | Chuẩn hóa đường dẫn | #37 (course), #41 (enrollment), #42 (quiz) |

**Môi trường test đã sẵn.** Không ai phải tự bật 6 service trong IntelliJ:

```bash
git pull
docker compose --profile app up -d --build --wait
bash scripts/smoke-test.sh        # 12 dòng OK là cả 6 service đã lên, thông database và Redis
```

Mọi request trong Postman đi qua **gateway `http://localhost:8080`**. Đừng gọi thẳng cổng
8081–8085: trong Docker các cổng đó không mở ra ngoài, và gọi thẳng thì bỏ qua đúng hai thứ
hay hỏng nhất là định tuyến và kiểm token ở vòng ngoài.

**Gateway giới hạn số lần đăng nhập theo địa chỉ IP** (#34): 10 lần liền, sau đó 6 giây mới
được thêm một lần. Mọi request từ máy mình vào Docker đều mang chung một IP, nên khi chạy
Postman Runner hay nhiều người đăng nhập liên tục trên cùng một máy sẽ nhận **429**. Đó là
gateway chặn đúng, không phải lỗi của service. Chờ một phút, hoặc tắt hẳn trong lúc test:

```bash
RATE_LIMIT_ENABLED=false docker compose --profile app up -d api-gateway
```

Bật lại bằng cùng lệnh, bỏ `RATE_LIMIT_ENABLED=false`. Chi tiết ở
[README](../README.md#giới-hạn-request).

**Collection Postman:** đang dựng một collection chung cho cả 5 service (việc của Hiếu). Trong
lúc chờ, `docs/postman/course-service-v2.*` (#37) dùng được cho course-service. Hai collection
cũ `course-service.*` và `enrollment-service.*` **không dùng lại được** — gọi thẳng cổng service
hoặc dùng đường dẫn cũ.

**Danh sách tình huống test** (#36): `docs/test-cases/` — mỗi service một file, đọc
[gateway.md](test-cases/gateway.md) trước để tạo bốn tài khoản cố định.

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
gateway lẫn từng service. Gateway giới hạn số request bằng Redis. Toàn bộ 419 test xanh. Cả hệ thống chạy được bằng một lệnh
`docker compose --profile app up -d --build --wait`, xem
[README](../README.md#cách-nhanh-nhất-chạy-cả-hệ-thống-bằng-docker).

**Cả chuỗi đã chạy thông** (lần đầu, khi review #41 ngày 06/10):

```
admin cấp quyền giảng viên → tạo khóa, chương, bài → xuất bản
  → course.updated lên Kafka → enrollment-service tự nạp course_snapshots
học viên ghi danh → xem nội dung bài → cập nhật tiến độ tới 100%
  → nhận đủ 3 thông báo: ghi danh thành công, hoàn thành khóa, cấp chứng chỉ có mã
học viên làm bài kiểm tra → nộp → thông báo "đạt 50.00 điểm"
```

**Không mất sự kiện khi một phần hệ thống chết:**

- Nộp bài đi qua outbox (#32): tắt Kafka thì bài làm vẫn lưu, bật lại là thông báo tới.
- notification-service (#38) và enrollment-service (#41): MySQL tắt giữa chừng thì consumer
  thử lại tới khi MySQL lên. notification-service chuyển message hỏng sang topic `.DLT`.
- course-service: enrollment-service chết thì đề cương vẫn xem được, chỉ ẩn nội dung bài
  thường (#37).

**Lỗ hổng đang mở:**

| Lỗ hổng | Mức độ | Ai sửa |
|---|---|---|
| Giảng viên tạo/sửa/xóa bài kiểm tra trong khóa của người khác; học viên thấy bài DRAFT | Cao | hiep |

---

## Chi tiết từng việc

### hiepdeptrai0111 — chủ sở hữu của bài kiểm tra

> Cùng loại lỗ hổng duyd đã đóng ở course-service (#37). Nhóm trưởng tìm ra khi rà code
> ngày 06/10.

**Vấn đề.** Mọi endpoint ghi của quiz-service chỉ kiểm vai trò (`requireQuizManager`), không
kiểm ai là chủ. Chạy thật qua gateway với hai giảng viên A và B, khóa 6 không phải của B:

```
B tạo bài kiểm tra trong khóa 6                      → 201
B tạo bài kiểm tra cho khóa 999999 (không tồn tại)   → 201
B sửa bài kiểm tra của A                             → 200
B thêm câu hỏi vào bài của A                         → 201
B xóa bài kiểm tra của A                             → 200
Học viên GET /api/quizzes?courseId=6                 → 200, thấy cả bài DRAFT
```

**Cần làm.**

1. **Tạo bài:** hỏi course-service `GET /api/courses/{courseId}`, chuyển tiếp nguyên header
   `Authorization` của người gọi (khóa DRAFT chỉ chủ khóa xem được). Không tìm thấy → 404.
   `data.instructorId` khác người gọi và người gọi không phải admin → 403. course-service không
   trả lời → 502 `EXTERNAL_SERVICE_ERROR`. Có hai mẫu để chép: `EnrollmentAccessClient` bên
   course-service và `CourseLessonClient` bên enrollment-service.
2. **Sửa, đổi trạng thái, xóa bài; thêm/sửa/xóa câu hỏi:** so `quiz.getCreatedBy()` với
   `user.userId()`, admin được qua. Không cần gọi course-service — bước 1 đã bảo đảm người tạo
   là chủ khóa. Viết như duyd: `if (currentUserId == null || (!isAdmin && !currentUserId.equals(...)))`.
3. **`GET /api/quizzes?courseId=`:** học viên chỉ thấy bài `PUBLISHED` (quy tắc A3). Người
   tạo và admin thấy cả DRAFT.
4. `docker-compose.yml`: thêm `COURSE_SERVICE_URL: http://course-service:8082` cho quiz-service,
   giống phamquyet đã làm cho enrollment-service.

**Tự kiểm.** Đúng sáu dòng ở trên phải thành 403, 404, 403, 403, 403 và "không thấy DRAFT". A làm
những việc đó với bài của mình vẫn 201/200; admin làm được với bài của bất kỳ ai.

---

### duyd92689-debug — học viên giữ quyền học khi khóa bị lưu trữ

**Vấn đề.** Giảng viên chuyển khóa sang `ARCHIVED` thì học viên **đã ghi danh** mất luôn khóa
học. Chạy thật khi review #41:

```
S đã ghi danh, khóa đang PUBLISHED   GET /api/courses/{id}/curriculum   → 200
giảng viên chuyển khóa sang ARCHIVED
S                                    GET /api/courses/{id}/curriculum   → 404
S                                    GET /api/lessons/{id}              → 404
```

`canViewCourse` chỉ cho chủ khóa và admin xem khóa không `PUBLISHED`. Đúng với `DRAFT`, nhưng
`ARCHIVED` nghĩa là "ngừng nhận học viên mới", không phải "lấy lại khóa của người đã học".

**Cần làm.** Trong `canViewCourse`, khóa `ARCHIVED` còn cho xem nếu người gọi đã ghi danh —
dùng lại `enrollmentAccessClient.hasEnrollment(...)`. Giữ nguyên:

- `DRAFT` vẫn chỉ chủ khóa và admin.
- Danh sách công khai `GET /api/courses` vẫn chỉ trả `PUBLISHED`.
- Ghi danh mới vào khóa `ARCHIVED` vẫn bị enrollment-service từ chối (đã chạy đúng).
- enrollment-service chết → khóa `ARCHIVED` trả 404 cho học viên là chấp nhận được; đừng để
  lỗi đó làm hỏng khóa `PUBLISHED`.

**Tự kiểm.** S ghi danh, giảng viên lưu trữ khóa: S xem đề cương và bài học vẫn 200, có nội
dung. T chưa ghi danh → 404. Khách → 404.

---

### phamquyet19042005-netizen — thử lại có giới hạn, message hỏng sang `.DLT`

> Ghi chú không chặn merge từ review #41.

**Vấn đề.** `KafkaConsumerConfig` thử lại **vô hạn** với mọi lỗi. Message hỏng thì
`CourseSnapshotConsumer` đã tự bắt và bỏ qua nên không sao, nhưng một lỗi không phải tạm thời
lọt qua `validate()` — ví dụ vi phạm ràng buộc cột — sẽ làm consumer đứng mãi ở message đó, và
mọi khóa học sau nó không đồng bộ được nữa.

**Cần làm.** Chép cách của notification-service (#38): `KafkaErrorHandlingConfig` và
`KafkaRetryProperties`. Lỗi tạm thời thử lại với khoảng chờ tăng dần tới giới hạn, rồi chuyển
message sang `elearning.course.events.DLT`. Tài liệu ở
[notifications.md](notifications.md#khi-xử-lý-sự-kiện-bị-lỗi).

**Tự kiểm.** Tắt MySQL 40 giây rồi xuất bản một khóa: snapshot vẫn cập nhật khi MySQL lên
(như lúc review #41). Gửi một `course.updated` có `title` dài 300 ký tự bằng Kafka UI: không
kẹt consumer, khóa xuất bản ngay sau đó vẫn có snapshot.

---

### Hiếu — collection Postman chung

Một collection duy nhất cho cả 5 service, đi qua gateway `http://localhost:8080`:

- Thư mục "0. Chuẩn bị": đăng nhập admin, đăng ký bốn tài khoản cố định theo
  [gateway.md](test-cases/gateway.md), admin cấp quyền, đăng nhập lại — token tự lưu vào biến.
- Mỗi service một thư mục, đặt tên request theo mã ca trong `docs/test-cases/`, mỗi request có
  `pm.test` kiểm mã HTTP mong đợi.
- Gộp `course-service-v2` của duyd vào; bỏ hai collection cũ.

Xong thì báo trong nhóm và cập nhật bảng này.

---

### Cả nhóm — đợt test Postman

Thầy yêu cầu mỗi người tự kiểm service của mình:

| Người | Chạy file |
|---|---|
| quocluibotre | [auth.md](test-cases/auth.md) |
| duyd92689-debug | [course.md](test-cases/course.md) |
| phamquyet19042005-netizen | [enrollment.md](test-cases/enrollment.md) |
| hiepdeptrai0111 | [quiz.md](test-cases/quiz.md) — sau khi xong việc chủ sở hữu |
| Hiếu | [gateway.md](test-cases/gateway.md), [notification.md](test-cases/notification.md) |

**Cách làm.**

```bash
git pull
docker compose --profile app up -d --build --wait
bash scripts/smoke-test.sh        # 12 dòng OK mới bắt đầu
```

Import collection chung, chạy thư mục "0. Chuẩn bị" trước, rồi chạy thư mục service của mình.

**Ghi biên bản** vào `docs/test-cases/ket-qua/<service>.md` theo bảng mẫu ở đầu
[gateway.md](test-cases/gateway.md#môi-trường-và-cách-ghi-kết-quả): mã ca, commit đã chạy, mã
HTTP thực tế, PASS / FAIL / BLOCKED, bằng chứng. Mở pull request riêng cho biên bản.

**Ca FAIL** thì sửa trong pull request khác, ghi mã ca trong mô tả (ví dụ "sửa QUIZ-03.4").
Đừng sửa mong đợi trong file tình huống cho khớp với kết quả — trừ khi chắc chắn tình huống
viết sai, và khi đó ghi lý do.

**Khóa id 1 ("Kien truc Microservices") chưa ghi danh được:** khóa này xuất bản trước khi có
sự kiện đồng bộ nên enrollment-service chưa biết nó. Giảng viên của khóa hoặc admin gọi
`PUT /api/courses/1` với nguyên dữ liệu cũ là khóa được đồng bộ (sửa khóa đang `PUBLISHED`
luôn phát `course.updated`). Hoặc dùng khóa mới tạo trong thư mục "0. Chuẩn bị".

---


## Đã xong

| Ngày | PR | Việc | Người |
|---|---|---|---|
| 06/10 | #41 | enrollment-service tự nạp `course_snapshots` từ Kafka, kiểm bài học trước khi ghi tiến độ, chuẩn hóa đường dẫn | phamquyet19042005-netizen |
| 06/10 | #42 | Outbox quiz giữ nguyên điểm số (`50.00`), chuẩn hóa đường dẫn quiz | hiepdeptrai0111 |
| 06/10 | #39 | Sửa tiêu đề pull request là check tự chạy lại | Hiếu |
| 06/10 | #37 | Chủ sở hữu chương/bài học, trả nội dung bài học theo quyền, chuẩn hóa đường dẫn course, collection Postman qua gateway | duyd92689-debug |
| 06/10 | #36 | Tình huống test cho cả 5 service (`docs/test-cases/`), chặn vai trò `null` | quocluibotre |
| 06/10 | #38 | notification-service thử lại khi lỗi tạm thời, message hỏng sang `.DLT` | Hiếu |
| 03/10 | #34 | Gateway giới hạn số request bằng Redis: chống dò mật khẩu, Redis chết thì vẫn cho qua | Hiếu |
| 03/10 | #32 | quiz-service gửi sự kiện chấm điểm qua outbox | hiepdeptrai0111 |
| 25/09 | #29 | Gửi outbox của enrollment-service lên Kafka | phamquyet19042005-netizen |
| 25/09 | #31 | Phân quyền course-service, ẩn khóa DRAFT, phát `course.updated` | duyd92689-debug |
| 25/09 | #27 | API gán vai trò, admin đầu tiên, `/me` dùng `AuthenticatedUser` | quocluibotre |
| 25/09 | #30 | CI chặn cấu hình tắt xác thực; sửa hướng dẫn tắt xác thực trên máy (thiếu bước bật profile) | Hiếu |
| 25/09 | #28 | Chạy cả hệ thống bằng Docker; gateway trả 502 sau tối đa 3 giây khi một service chết | Hiếu |
| 25/09 | #26 | Lệnh đồng bộ nhánh ghi rõ tên nhánh | Hiếu |
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
