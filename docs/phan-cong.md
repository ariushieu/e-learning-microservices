# Bảng theo dõi công việc

> **Cập nhật lần cuối:** 06/10/2026 — `main` ở `2287546`
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
| phamquyet19042005-netizen | enrollment-service | [Nạp `course_snapshots`](#phamquyet19042005-netizen--nạp-course_snapshots) | **Cao nhất** — mắt xích cuối của chuỗi ghi danh | ~1h |
| hiepdeptrai0111 | quiz-service | [Giữ nguyên điểm số trong sự kiện](#hiepdeptrai0111--giữ-nguyên-điểm-số-trong-sự-kiện) | Thấp | ~30 phút |
| hiepdeptrai0111, phamquyet | quiz, enrollment | [Chuẩn hóa đường dẫn API](#cả-nhóm--chuẩn-hóa-đường-dẫn-api) — duyd đã xong phần course (#37) | Trung bình — trước đợt test Postman | ~1h/người |

**duyd và quocluibotre đã xong hết việc trong bảng** (#37, #36) — chờ việc mới.

**Việc gấp nhất là của phamquyet.** course-service đã phát `course.updated` (#31), enrollment
đã gửi outbox lên Kafka (#29). Chỉ còn một mắt xích: nạp `course_snapshots` từ sự kiện đó là
chuỗi đăng ký → ghi danh → thông báo chạy thông trọn vẹn lần đầu tiên.

**Tài khoản giảng viên và admin giờ tạo được bằng API** (#27), không cần SQL nữa:

```
Admin có sẵn: admin@elearning.hunre.edu.vn / Admin@123456   (tài khoản dev — đổi khi triển khai thật)
Cấp quyền:    PATCH /api/users/{id}/roles   {"roles": ["ROLE_STUDENT", "ROLE_INSTRUCTOR"]}
```

Người được cấp quyền phải **đăng nhập lại** mới nhận vai trò mới — token cũ vẫn mang vai trò
cũ tới khi hết hạn.

Việc chuẩn hóa đường dẫn để cuối cùng, nhưng **phải xong trước đợt test Postman và trước khi
bắt đầu frontend** — đổi đường dẫn sau khi đã viết collection hay đã có frontend gọi là phải
làm lại hết.

## Khi nào test toàn bộ API bằng Postman

**Chưa sẵn sàng — xong 2/4.** Môi trường test thì đã có (xem dưới).
Đủ bốn điều kiện sau thì nhóm trưởng báo cả nhóm vào test:

| # | Điều kiện | Nếu test trước khi có | Tình trạng |
|---|---|---|---|
| 1 | API gán vai trò | Người test phải sửa database bằng SQL mới có tài khoản giảng viên | **Xong** (#27) |
| 2 | Phân quyền course-service, lọc khóa `DRAFT` | "Học viên tạo được khóa học" sẽ bị ghi nhận là chạy đúng | **Xong** (#31, #37) |
| 3 | Ghi danh chạy thông (`course.updated` + nạp snapshot + gửi outbox) | Ghi danh, tiến độ, chứng chỉ, thông báo ghi danh đều 404 — nửa hệ thống không test được | Phát sự kiện xong (#31), gửi outbox xong (#29) — chỉ còn nạp snapshot |
| 4 | Chuẩn hóa đường dẫn | Viết collection xong, đổi đường dẫn là viết lại | course xong (#37) — còn quiz (hiep), enrollment (phamquyet) |

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

**Collection Postman:** `docs/postman/course-service-v2.*` (#37) đi qua gateway, dùng đường dẫn
mới, đã tự lưu token — dùng được ngay cho course-service. Hai collection cũ của course-service
và enrollment-service **không dùng lại được**: bản cũ gọi thẳng `localhost:8082` không kèm token,
bản kia dùng đường dẫn sẽ đổi ở điều kiện 4. Đủ điều kiện thì nhóm trưởng gộp thành một
collection chung cho cả 5 service.

**Danh sách tình huống test đã có** (#36): `docs/test-cases/` — mỗi service một file, đọc
[gateway.md](test-cases/gateway.md) trước để tạo bốn tài khoản cố định. Đây là kế hoạch, chưa
phải biên bản; đến đợt test thì ghi kết quả từng mã ca theo bảng ở đầu `gateway.md`.

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
gateway lẫn từng service. Gateway giới hạn số request bằng Redis. Toàn bộ 359 test xanh. Cả hệ thống chạy được bằng một lệnh
`docker compose --profile app up -d --build --wait`, xem
[README](../README.md#cách-nhanh-nhất-chạy-cả-hệ-thống-bằng-docker).

**Chuỗi đã chạy thông:**

```
đăng nhập → làm bài kiểm tra → nộp bài → nhận thông báo trong ứng dụng
admin cấp quyền giảng viên → tạo khóa học → xuất bản → sự kiện course.updated lên Kafka
```

Nộp bài giờ đi qua outbox (#32): tắt Kafka rồi nộp bài thì bài làm vẫn lưu, sự kiện nằm chờ
trong `outbox_events`, bật Kafka lại là thông báo tới — đã chạy thật ngày 03/10.

Phía nhận cũng không còn mất sự kiện (#38): MySQL của notification-service tắt giữa chừng thì
consumer thử lại tới khi MySQL lên; message hỏng chuyển sang topic `.DLT` thay vì bị bỏ đi.

Nội dung bài học giờ xem được (#37): bài xem thử ai cũng đọc, bài thường chỉ chủ khóa, admin và
người đã ghi danh — course-service hỏi enrollment-service bằng chính token của người gọi.

**Chuỗi chưa chạy, và vì sao:**

| Không làm được | Nguyên nhân | Ai sửa |
|---|---|---|
| Ghi danh khóa học | Sự kiện `course.updated` đã lên Kafka nhưng chưa ai nạp vào `course_snapshots` | phamquyet |

**Lỗ hổng đang mở:** không còn. Lỗ hổng chương/bài học của giảng viên khác đã đóng ở #37 —
chạy thật qua gateway, giảng viên B tạo/sửa/xóa trong khóa của A đều nhận 403.

Việc đó của phamquyet xong là demo chạy trọn vẹn: đăng ký → ghi danh → học → làm bài → nhận
thông báo → chứng chỉ.

---

## Chi tiết từng việc

### phamquyet19042005-netizen — nạp `course_snapshots`

**Sự kiện đã có sẵn:** `CourseUpdatedEvent` (loại `course.updated`) trong shared-common.
Tên cũ trong bảng này là `course.published` — đã đổi, lý do ở
[shared-contracts.md](shared-contracts.md#courseupdatedevent-khác-các-sự-kiện-còn-lại).

Dependency Kafka (`spring-kafka` + `spring-boot-kafka`) đã có trong `enrollment-service/pom.xml`
từ #29 — không phải thêm gì, chỉ việc viết consumer.

**Cần làm.** Nghe topic `KafkaTopics.COURSE_EVENTS`, lọc `eventType` bằng
`EventTypes.COURSE_UPDATED`, rồi **ghi đè cả dòng** trong `course_snapshots` theo `courseId`.
Mỗi trường của sự kiện khớp đúng một cột của bảng.

course-service đã phát sự kiện thật từ #31: xuất bản một khóa học là có message trên topic.
Vẫn có thể tự tạo message mẫu bằng Kafka UI như phần Tự kiểm bên dưới.

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

### hiepdeptrai0111 — giữ nguyên điểm số trong sự kiện

> Việc nhỏ phát hiện khi chạy thử #32. Không chặn demo.

**Vấn đề.** Cột `payload` của `quiz_db.outbox_events` có kiểu `JSON`. MySQL không lưu nguyên
chuỗi mà phân tích rồi viết lại, nên số thập phân bị đổi dạng trước khi lên Kafka:

```
quiz-service tạo:   {"eventId": "...", "score": 100.00, ...}
MySQL lưu và trả:   {"score": 100.0, "passed": true, ...}      ← mất số 0, đảo thứ tự khóa
thông báo hiện:     "Bài kiểm tra ... của bạn đạt 100.0 điểm."
```

Trước #32 thông báo hiện `100.00`. Điểm 85.50 giờ thành 85.5. Không sai dữ liệu, nhưng sự kiện
lên Kafka không còn là thứ quiz-service đã tạo ra — đúng điều outbox phải đảm bảo.

**Cần làm.**

1. Migration mới `V3__store_outbox_payload_as_text.sql` đổi cột sang `LONGTEXT NOT NULL`.
   **Không sửa V2** — V2 đã merge, CI sẽ chặn (xem [Nếu có sửa entity hoặc migration](#nếu-có-sửa-entity-hoặc-migration)).
2. `OutboxEvent.payload` đổi `columnDefinition` cho khớp, nếu không job "Schema matches
   entities" sẽ đỏ.
3. Bỏ đoạn xử lý H2 trong `QuizOutboxIntegrationTest` (`if (payload.isTextual())`) — đổi kiểu
   cột rồi thì không cần nữa.
4. `OutboxPublisherWorker`: truyền cả `ex` vào `log.error` thay vì `ex.getMessage()`, để log
   giữ được nguyên nhân gốc.

enrollment-service cũng dùng cột `JSON` nhưng các sự kiện của nó không có số thập phân, nên
chưa bị. Không cần sửa bên đó.

**Tự kiểm.** Tạo bài 2 câu, nộp đúng 1 câu. Thông báo phải hiện `50.00 điểm`, và
`SELECT payload FROM quiz_db.outbox_events` phải giữ nguyên thứ tự khóa như lúc tạo.

---

### Cả nhóm — chuẩn hóa đường dẫn API

> **Có hạn chót**: phải xong trước [đợt test Postman](#khi-nào-test-toàn-bộ-api-bằng-postman)
> và trước khi ai đó bắt đầu viết frontend. Đổi đường dẫn sau khi đã có collection hay
> frontend gọi thì gãy hết và không ai muốn sửa nữa.

**Vấn đề.** Năm service đang đặt đường dẫn theo năm kiểu khác nhau. Không sai về chức năng,
nhưng người viết frontend sẽ phải nhớ mỗi service một quy ước, và đây là thứ dễ mất điểm
nhất khi chấm.

Luật đã viết ở [api-conventions.md](api-conventions.md), phần B. Mỗi người sửa service của
mình. **duyd đã xong phần course-service (#37)**, còn lại:

| Người | Đang là | Đổi thành | Quy tắc |
|---|---|---|---|
| hiepdeptrai | `GET /api/quizzes/course/{id}` | `GET /api/quizzes?courseId={id}` | B2 |
| hiepdeptrai | `PATCH /api/quizzes/{id}/publish` và `/archive` | `PATCH /api/quizzes/{id}/status` + body | B4 |
| hiepdeptrai | `GET /api/quizzes/{id}/attempts/history` | `GET /api/quizzes/{id}/attempts` | B5 |
| hiepdeptrai | `GET /api/quizzes/attempts/{attemptId}` | `GET /api/attempts/{attemptId}` | B7 |
| phamquyet | `GET /api/enrollments/my-courses` | `GET /api/enrollments` — **đổi cùng lúc** `course.enrollment.list-path` trong course-service | B6 |
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
