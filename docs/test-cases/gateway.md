# Tình huống test gateway và chuẩn bị dữ liệu

**Người phụ trách:** quocluibotre. **Mốc đối chiếu:** `main d7e77e3`, phân công ngày 06/10/2026.
Đây là kế hoạch kiểm thử, **chưa phải biên bản các ca đã chạy qua Postman**.

## Tài khoản cố định

Bốn tài khoản và một ngữ cảnh không đăng nhập dưới đây được dùng trong cả sáu file.
Mật khẩu trong bảng chỉ dành cho dữ liệu dev.

| Ký hiệu | Email | Mật khẩu dev | Vai trò | Biến Postman |
|---|---|---|---|---|
| ADM | admin@elearning.hunre.edu.vn | Admin@123456 | ROLE_ADMIN | `adminId`, `adminToken`, `adminRefresh` |
| A | qa.instructor.a@example.com | Test@123456 | ROLE_STUDENT, ROLE_INSTRUCTOR | `instructorAId`, `tokenA`, `refreshA` |
| B | qa.instructor.b@example.com | Test@123456 | ROLE_STUDENT, ROLE_INSTRUCTOR | `instructorBId`, `tokenB`, `refreshB` |
| S | qa.student@example.com | Test@123456 | ROLE_STUDENT | `studentId`, `studentToken`, `studentRefresh` |
| — | Không có tài khoản | — | Không token | Chọn **No Auth**, xóa header Authorization kế thừa |

1. Đặt `baseUrl=http://localhost:8080`. Mọi URL bên dưới đều ghép với `{{baseUrl}}`.
2. Đăng nhập ADM: `POST /api/auth/login`, body `{"email":"admin@elearning.hunre.edu.vn","password":"Admin@123456"}` → **200**.
   Lưu `data.user.id`, `data.accessToken`, `data.refreshToken` vào ba biến admin.
3. Đăng ký lần lượt A, B, S: `POST /api/auth/register`, không token, ví dụ
   `{"email":"qa.instructor.a@example.com","password":"Test@123456","fullName":"Giang vien A"}` → **201**.
   Thay email/họ tên tương ứng, lưu `data.id` vào biến ID. Nếu tài khoản đã tồn tại thì đăng nhập,
   lấy `data.user.id`; không coi **409** là đăng ký thành công.
4. ADM gọi `PATCH /api/users/{{instructorAId}}/roles` và tương tự B, body
   `{"roles":["ROLE_STUDENT","ROLE_INSTRUCTOR"]}` → **200**. Với S gửi `{"roles":["ROLE_STUDENT"]}`.
5. Đăng nhập lại A, B, S, lưu token riêng cho từng người. Khi refresh, thay cả access token
   và refresh token; token refresh cũ đã bị thu hồi. Không ghi token thật vào Git.
6. ADM không tự gỡ quyền admin. Ca thay đổi quyền của S phải khôi phục S về ROLE_STUDENT
   rồi đăng nhập lại trước khi chạy file khác.

Ví dụ script sau request đăng nhập S:

```javascript
pm.test("Login succeeds", () => pm.response.to.have.status(200));
if (pm.response.code === 200) {
  const d = pm.response.json().data;
  pm.environment.set("studentId", d.user.id);
  pm.environment.set("studentToken", d.accessToken);
  pm.environment.set("studentRefresh", d.refreshToken);
}
```

## Môi trường và cách ghi kết quả

Khởi động theo [README](../../README.md), chạy `docker compose --profile app up -d --build --wait`,
rồi `bash scripts/smoke-test.sh`. Không gọi cổng 8081–8085 trong các ca nghiệm thu.
Dùng dữ liệu dev riêng, không dùng tài khoản hoặc khóa học thật.

Khi kiểm nghiệp vụ, có thể tắt rate limit bằng biến môi trường `RATE_LIMIT_ENABLED=false`
trước khi tạo lại gateway; giữ nguyên xác thực JWT. Khi chạy nhóm G-RATE, bật lại rate limit,
Redis phải khỏe và chờ ít nhất 60 giây để nạp đầy quota đăng nhập.

```powershell
$env:RATE_LIMIT_ENABLED = "false"
docker compose --profile app up -d api-gateway
# Khi chuyển sang G-RATE:
$env:RATE_LIMIT_ENABLED = "true"
docker compose --profile app up -d api-gateway
```

Mỗi request có body dùng `Content-Type: application/json`. API nghiệp vụ thành công: `success=true`;
lỗi JSON: `success=false`, có `code`, `message`, `path`, không lộ stack trace hoặc password hash.
Health và preflight CORS không dùng vỏ response nghiệp vụ này.
Danh sách phân trang dùng `data.content`, `page`, `size`, `totalElements`, `totalPages`;
danh sách nhỏ không phân trang dùng `data` là mảng.

Mỗi bảng endpoint có ít nhất sáu ca. Sáu nhóm ở mục E của
[quy ước API](../api-conventions.md#e-tự-kiểm-trước-khi-mở-pull-request) áp dụng theo hợp đồng:
endpoint công khai không bắt buộc 401; dữ liệu cá nhân kiểm chủ sở hữu thay vì mặc định 403
theo vai trò; collection rỗng có thể 200; endpoint không nhận Pageable không có ca sort.
Các ca không áp dụng được thay bằng ca nghiệp vụ cụ thể, không gửi tham số bị bỏ qua rồi kết luận đã kiểm được validation.
Khi thử lỗi vai trò/ID, giữ body hợp lệ để tránh bị chặn 400 trước khi vào kiểm quyền.

Ghi kết quả từng mã ca vào biên bản riêng:

| Mã ca | Commit/môi trường | HTTP thực tế | PASS / FAIL / BLOCKED / NOT RUN | Bằng chứng, issue |
|---|---|---|---|---|
| Ví dụ AUTH-01.1 | SHA, ngày chạy | Chưa chạy | NOT RUN | — |

**BLOCKED** khi thiếu route mới, snapshot hoặc fixture; không ghi PASS bằng đường dẫn cũ.
**FAIL** khi endpoint sẵn sàng nhưng khác kết quả mong đợi. Ghi response đã che token.
Mọi ca đọc/ghi dùng dữ liệu tồn tại; `{{missingId}}` là số dương đã xác minh không tồn tại
(ví dụ 9223372036854775807), `{{missingSlug}}` là slug mới chưa tạo.

## Thứ tự và fixture chung

Đặt `runId` thành chuỗi riêng mỗi lần chạy, ví dụ `20261006-01`; dùng nó trong slug.
Chạy các ca ghi/xóa trên bản sao riêng; không xóa fixture nền trước khi các nhóm phụ thuộc hoàn tất.
Đừng chạy tuần tự toàn bộ bảng mà không dựng lại dữ liệu: ca thành công có thể đổi trạng thái hoặc xóa tài nguyên.

| Biến | Cách tạo / tiền điều kiện |
|---|---|
| `categoryId`, `categorySlug`, `childCategoryId` | A tạo danh mục gốc và con theo COURSE-05; tên/slug có runId |
| `courseAId`, `courseASlug` | A tạo khóa DRAFT; dành cho sửa/xóa/chặn người khác |
| `publishedCourseId`, `publishedCourseSlug` | A tạo khóa khác, thêm một chương và **hai bài**, rồi PUBLISHED |
| `courseBId` | B tạo khóa DRAFT của B để kiểm cách ly quyền |
| `sectionAId`, `lessonAId`, `lesson2Id`, `resourceId` | A tạo trong publishedCourseId; hai bài có position 1/2, tổng bài = 2 |
| `draftSectionId`, `draftLessonId` | A tạo trong courseAId để kiểm ẩn nội dung DRAFT |
| `previewLessonId` | lessonAId có isPreview=true; lesson2Id là bài thường |
| `enrollmentId` | S ghi danh publishedCourseId sau khi consumer đã nạp snapshot |
| `enrollmentBId` | B ghi danh cùng khóa để kiểm dữ liệu riêng; không cần thêm tài khoản thứ sáu |
| `quizId`, `questionId`, `optionCorrectId`, `optionWrongId` | A tạo quiz DRAFT trong publishedCourseId, thêm câu hỏi theo QUIZ-08, lấy ID từ response |
| `publishedQuizId` | Quiz riêng có 2 câu SINGLE_CHOICE, mỗi câu score=1, passScore=70, maxAttempts=1; PUBLISHED |
| `question2Id`, `option2CorrectId`, `option2WrongId` | ID câu thứ hai và hai phương án của publishedQuizId; lưu cả ID câu đầu vào biến tương ứng |
| `attemptId`, `attemptBId` | S và B bắt đầu làm publishedQuizId; tạo lại quiz khi cần ca mới |
| `notificationId`, `notificationBId` | Lấy từ hộp thư S/B sau ghi danh hoặc nộp bài, chọn thông báo SENT |

`quizId` dùng sửa/xóa câu hỏi; `publishedQuizId` dùng làm bài. Không dùng lẫn questionId
giữa hai quiz: trước nhóm làm bài, lưu questionId/optionCorrectId/optionWrongId của câu đầu
**trong publishedQuizId** vào bộ biến của nhóm đó.

Trình tự: [auth](auth.md) → [course](course.md) → [enrollment](enrollment.md) →
[quiz](quiz.md) → [notification](notification.md). Các ca riêng gateway có thể chạy sau setup.
Thông báo là bất đồng bộ: đọc lại mỗi 2 giây, tối đa 30 giây làm mốc điều tra; đây là timeout
của đợt test, không phải SLA đã cam kết. Quá hạn ghi FAIL/BLOCKED và kiểm Kafka/worker.

## Những thay đổi còn chờ

Các file dùng **đường dẫn đích** trong bảng chuẩn hóa của [phân công](../phan-cong.md).
Phần mapping đầu mỗi file giữ dấu vết đường dẫn cũ; không khẳng định API mới đã chạy.
Các nhãn:
- **[CHỜ ROUTE]**: nhóm phụ trách chưa đổi controller/gateway.
- **[CHỜ SỬA]**: tiêu chí đã giao nhưng implementation còn thiếu.
- **[CẦN CHỐT]**: đề xuất hợp đồng cho chỗ tài liệu chưa quy định; ghi BLOCKED trước khi chốt.

Chặn đợt test đầy đủ hiện tại: consumer course.updated → course_snapshots; quyền sở hữu
chương/bài học; chuẩn hóa route. Nội dung bài học chưa được ghi/đọc qua DTO hiện tại.
Không sửa code nghiệp vụ trong nhiệm vụ tài liệu này.

## G-AUTH — JWT qua gateway

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Token hợp lệ | S | GET /api/auth/me, Bearer studentToken | 200; data.id=studentId |
| 2 | Không token | — | GET /api/auth/me | 401; code=UNAUTHORIZED |
| 3 | Token hỏng | — | GET /api/auth/me, Bearer abc.def.ghi | 401 |
| 4 | Token hết hạn thật | S | Giữ bản access token đã login, đợi quá expiresIn rồi GET /api/auth/me | 401; không refresh đè token đang thử |
| 5 | Tự sửa claim | S | Đổi roles hoặc sub trong payload JWT, giữ chữ ký cũ, GET /api/auth/me | 401 |
| 6 | Giả header danh tính | — | GET /api/auth/me + X-User-Id: adminId, X-User-Roles: ROLE_ADMIN | 401 |
| 7 | Header không lấn át JWT | S | GET /api/auth/me + token S và X-User-Id: adminId | 200; vẫn là S |
| 8 | Chỉ GET công khai | — | POST /api/courses + body COURSE-CREATE hợp lệ | 401 |

## G-ROUTE — Định tuyến, lỗi downstream và đường dẫn mới

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Route auth | ADM | PATCH /api/users/{{studentId}}/roles, {"roles":["ROLE_STUDENT"]} | 200 |
| 2 | Route course | — | GET /api/categories | 200 |
| 3 | Route enrollment mới [CHỜ ROUTE] | S | GET /api/enrollments | 200 |
| 4 | Tiến độ không bị gửi nhầm course-service [CHỜ ROUTE] | S | PUT /api/lessons/{{lessonAId}}/progress + ENROLL-PROGRESS | 200; dữ liệu tiến độ của S |
| 5 | Route attempts mới [CHỜ ROUTE] | S | GET /api/attempts/{{attemptId}} sau nộp bài | 200 |
| 6 | Route notification | S | GET /api/notifications | 200 |
| 7 | Downstream ngừng | S | Lần lượt stop auth/course/enrollment/quiz/notification; gọi /api/auth/me, /api/categories, /api/enrollments, /api/attempts/{{attemptId}}, /api/notifications tương ứng | 502; code=EXTERNAL_SERVICE_ERROR; các service còn lại vẫn trả kết quả bình thường |
| 8 | Hồi phục | S | Start lại đúng service vừa stop, đợi health UP, gọi lại request ca 7 | 200 |
| 9 | Route không tồn tại | S | GET /api/does-not-exist | 404; response lỗi thống nhất |

Ca 7 chỉ chạy trong môi trường test riêng. Dùng `docker compose stop <service>`, luôn
`docker compose --profile app up -d <service>` sau mỗi lượt. Cổng kết nối có timeout 3 giây;
đo thời gian để điều tra khi treo dài, không coi 3 giây là SLA toàn bộ request.
Lỗi timeout/502 cần token còn hạn để tránh nhận 401 trước khi gateway gọi downstream.

## G-RATE — Giới hạn request (Redis khỏe, rate limit bật)

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Quota login đầy | — | POST /api/auth/login + email S và mật khẩu sai, sau 60 giây không gọi nhóm login | 401; có X-RateLimit-Remaining |
| 2 | Vượt burst | — | Sau 60 giây, gửi 11 request login sai trong dưới 6 giây, cùng IP | 10 request đầu 401, request thứ 11 là 429; code=TOO_MANY_REQUESTS; Retry-After=6 |
| 3 | Phục hồi quota | — | Sau ca 2, đợi ít nhất 6 giây rồi login sai một lần | 401, không còn 429 |
| 4 | Không lách bằng header IP | — | Hết quota, đổi X-Forwarded-For liên tục rồi login sai ngay | 429; header giả không tạo quota mới |
| 5 | Nhóm auth dùng chung quota | — | 10 login sai rồi POST /api/auth/refresh-token với refresh giả trong cùng cửa sổ | 429 |
| 6 | Quota API riêng từng người | S, B | Dồn request GET /api/notifications bằng S đủ để nhận 429; ngay sau đó B gọi một lần | S có 429, B trả 200; login quota không bị dùng chung với quota API |
| 7 | Redis ngừng | S | Stop redis, GET /api/notifications, sau đó start lại redis | 200; X-RateLimit-Remaining=-1 khi Redis lỗi; gateway vẫn phục vụ |
| 8 | Health không bị trừ quota | — | GET /actuator/health nhiều lần | 200; status=UP; không bị 429 do rate limiter |

Ca burst phải đủ nhanh: gửi chậm sẽ được nạp token giữa chừng và không thể kết luận quota sai.
Tắt Runner retry tự động cho các ca này. Kết thúc phải khôi phục Redis và cấu hình rate limit.

## G-CORS — Preflight

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Origin cho phép | — | OPTIONS /api/auth/login; Origin: http://localhost:3000; Access-Control-Request-Method: POST; Access-Control-Request-Headers: content-type | 200; Access-Control-Allow-Origin đúng origin |
| 2 | Header Authorization | — | OPTIONS /api/auth/me; Origin như ca 1; request method GET; request headers authorization | 200; cho phép Authorization |
| 3 | PATCH được phép | — | OPTIONS /api/users/{{studentId}}/roles; Origin như ca 1; request method PATCH | 200; cho phép PATCH |
| 4 | Origin không được phép | — | OPTIONS /api/auth/login; Origin: https://untrusted.example; request method POST | 403; không cấp CORS cho origin đó |
| 5 | Method không được phép | — | OPTIONS /api/auth/login; Origin như ca 1; request method TRACE | 403 |
| 6 | Preflight khi hết quota | — | Lặp ca 1 sau khi quota login đã hết | 200; OPTIONS không bị rate limit |

Nếu FRONTEND_ORIGIN đã đổi, thay origin hợp lệ bằng giá trị cấu hình. Chỉ test preflight có
đủ Origin và Access-Control-Request-Method; OPTIONS thiếu các header đó không tương đương.

## Truy vết nguồn

- [Phân công](../phan-cong.md), [quy ước API](../api-conventions.md).
- [Gateway routes/CORS/quota](../../api-gateway/src/main/resources/application.properties).
- [RateLimitGatewayFilter](../../api-gateway/src/main/java/com/hunre/apigateway/ratelimit/RateLimitGatewayFilter.java).
- [GatewayErrorWebExceptionHandler](../../api-gateway/src/main/java/com/hunre/apigateway/error/GatewayErrorWebExceptionHandler.java).
