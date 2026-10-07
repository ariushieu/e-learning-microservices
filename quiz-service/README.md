# Quiz service

## Quyền sở hữu bài kiểm tra

- Tạo bài (`POST /api/quizzes`): chỉ giảng viên hoặc admin. Service gọi
  `GET /api/courses/{courseId}` bằng nguyên header `Authorization` của người gọi.
  Giảng viên phải là `data.instructorId` của khóa; admin có thể tạo ở mọi khóa tồn tại.
  `createdBy` luôn lấy từ token, không lấy từ body.
- Sửa, xuất bản, lưu trữ, xóa bài và thêm/sửa/xóa câu hỏi: chỉ người tạo bài hoặc admin.
  Kiểm tra ở tầng service trước khi thay đổi dữ liệu. Không gọi course-service lại cho
  các thao tác này.
- Xem chi tiết có đáp án (`GET /api/quizzes/{id}` và `GET /api/quizzes/{id}/questions`):
  cũng chỉ người tạo bài có vai trò giảng viên hoặc admin.
- `GET /api/quizzes?courseId=...`: học viên chỉ thấy `PUBLISHED`; giảng viên thấy thêm
  bài do mình tạo ở mọi trạng thái; admin thấy tất cả bài trong khóa được hỏi.
  Không nhận danh tính/quyền từ query param. Người tạo đã mất vai trò giảng viên cũng
  chỉ thấy bài đã xuất bản.
- Học viên vẫn dùng `/api/quizzes/{id}/take` để xem đề đã xuất bản, không có đáp án
  hoặc giải thích. Bài nháp không thể bắt đầu làm.

## Kết nối course-service

`COURSE_SERVICE_URL` mặc định là `http://localhost:8082`; Docker Compose đặt thành
`http://course-service:8082`. Thời gian chờ kết nối tối đa 2 giây, chờ phản hồi 3 giây.

| Tình huống tạo bài | HTTP |
|---|---|
| Khóa công khai thuộc giảng viên khác | 403 |
| Khóa không tồn tại hoặc khóa nháp bị course-service ẩn với người gọi | 404 |
| course-service không truy cập được, hết thời gian chờ hoặc trả dữ liệu không hợp lệ | 502 `EXTERNAL_SERVICE_ERROR` |
| course-service từ chối token/quyền | Giữ 401/403 |

Không trường hợp xác minh thất bại nào được lưu bài vào database. Với bài hiện có,
quyền dựa trên `createdBy`; admin tạo bài trong khóa của người khác thì admin vẫn là
người tạo bài đó.

## Ghi danh trước khi làm bài

`POST /api/quizzes/{quizId}/attempts` kiểm tra ghi danh trước khi tạo hoặc trả lại
lượt làm dở. Quiz-service gọi `GET /api/enrollments` bằng nguyên token của người gọi,
duyệt các trang theo `id` và chỉ chấp nhận dòng có đúng `userId`, `courseId` cùng
trạng thái `ACTIVE` hoặc `COMPLETED`.

- Chưa ghi danh, ghi danh đã `CANCELLED`, hoặc chỉ có ghi danh của người/khóa khác:
  trả 403 `FORBIDDEN`.
- Người tạo bài và admin được làm thử mà không gọi enrollment-service. Điều kiện
  bài đã xuất bản, có câu hỏi và giới hạn lượt làm vẫn áp dụng.
- Lỗi kết nối, timeout, phản hồi không hợp lệ hoặc lỗi server: trả 502
  `EXTERNAL_SERVICE_ERROR`. Không tạo lượt, không đổi trạng thái lượt cũ sang hết giờ.
- Nếu enrollment-service từ chối token với 401/403, quiz-service từ chối làm bài
  bằng 403. Không chuyển token theo HTTP redirect.
- Lượt làm dở cũng phải kiểm lại ghi danh: sau khi hủy ghi danh, gọi lại endpoint
  bắt đầu không được dùng lượt cũ để vượt kiểm tra.

Cấu hình `ENROLLMENT_SERVICE_URL` mặc định `http://localhost:8083`; Compose dùng
`http://enrollment-service:8083`. `ENROLLMENT_TIMEOUT_MS` mặc định 2000 ms là hạn chờ
chung cho toàn bộ các trang, không phải 2000 ms cho từng trang. Không nhận quyền admin
hay danh tính người làm từ query/body.

Phạm vi thay đổi này là endpoint bắt đầu/tiếp tục lượt làm; các API xem đề, nộp bài
và đọc kết quả giữ nguyên hành vi hiện có.

## Kiểm thử

Chạy từ thư mục gốc repository:

```bash
./mvnw -B -ntp -pl quiz-service -am test
./mvnw -B -ntp clean verify
```

`CourseOwnershipClientTest` kiểm tra chuyển tiếp token, chủ khóa/admin, lỗi HTTP,
phản hồi hỏng và timeout. `QuizOwnershipIntegrationTest` chạy Spring với JWT thật,
controller, service và database H2: kiểm từng thao tác với chủ bài, giảng viên khác,
học viên và admin; xác nhận request bị chặn không sửa dữ liệu; kiểm lọc trạng thái,
giả danh qua body/query, ghép sai bài/câu hỏi và ẩn đáp án.

`EnrollmentAccessClientTest` dùng HTTP server cục bộ để kiểm token, phân trang,
trạng thái ghi danh, dữ liệu sai, lỗi mạng và hạn chờ tổng. `QuizEnrollmentIntegrationTest`
chạy JWT, controller, service, HTTP client và database thật trong Spring (H2, server
ghi danh giả lập), kiểm cả số dòng và trạng thái đã commit khi bị từ chối hoặc lỗi.

Các test H2 không thay thế kiểm chứng schema MySQL hoặc chạy toàn bộ Docker stack.
Các ca Postman chung nằm ở [docs/test-cases/quiz.md](../docs/test-cases/quiz.md).

### Collection qua gateway và giao diện ghi danh (07/10/2026)

Import [quiz.postman_collection.json](../docs/postman/quiz.postman_collection.json),
chạy tuần tự từ **0. Chuẩn bị**. Collection tự đăng nhập/cấp vai trò cho tài khoản QA,
tạo khóa và đề riêng theo `runId`, lưu token/ID bằng collection variables. Không cần
environment; `baseUrl` mặc định là `http://localhost:8080`. File này thay collection
`quiz-service.postman_collection.json` cũ gọi trực tiếp cổng 8084.

- Chỉ dùng trên dữ liệu dev; cần auth, course, enrollment, quiz, gateway và Kafka để
  đồng bộ snapshot khóa mới. Tài khoản QA cố định theo `docs/test-cases/gateway.md`.
- Bật `runSlowTests=true` trong tab Variables của collection để chạy ca nộp quá giờ
  (chờ **100 giây**: giới hạn 60 giây + ân hạn 30 giây + khoảng đệm cho Docker).
  Cờ này đọc bằng `pm.variables.get`, nên Newman cũng nhận `--env-var runSlowTests=true`
  (ưu tiên hơn giá trị trong collection). Đặt `--timeout-script 150000`, lớn hơn thời
  gian chờ. Không tính ca đã bỏ qua là PASS.
- `runKafkaRecovery` mặc định `false`: ca Kafka yêu cầu dừng/bật broker và kiểm
  thông báo riêng theo hướng dẫn thư mục 6. Hai ca sort vẫn BLOCKED vì endpoint
  trả List chưa hỗ trợ sort.
- Dữ liệu thử được giữ để đối chiếu, mỗi lần chạy tạo bộ mới. Không export token
  thật hoặc thông tin đăng nhập cá nhân vào Git.

Chạy từ thư mục gốc repo, bật cả ca chậm:

```bash
pnpm dlx newman@6.2.2 run docs/postman/quiz.postman_collection.json --env-var runSlowTests=true --timeout-script 150000
```

`runKafkaRecovery` vẫn đọc collection variable; muốn chạy ca Kafka phải sửa biến
trong collection và thực hiện dừng/bật broker theo hướng dẫn, không chỉ dùng `--env-var`.

Trang `/quizzes/{id}` kiểm ghi danh trước khi hiện nút bắt đầu: chưa ghi danh hoặc
đã hủy thì hiện thông báo và liên kết về khóa học. Chủ bài/admin được miễn kiểm tra.
403 khi bắt đầu được chuyển thành thông báo ghi danh; 502/503/504 hiện lỗi kiểm tra
ghi danh. Lỗi kiểm tra khi mở trang có nút thử lại. Backend vẫn quyết định quyền cuối cùng.

Biên bản, ca lỗi còn mở và giới hạn môi trường nằm tại
[ket-qua/quiz.md](../docs/test-cases/ket-qua/quiz.md).

### Bảo vệ kết quả và lưu lượt hết giờ

`GET /api/attempts/{id}` chỉ trả đáp án cho lượt `SUBMITTED` của chính người gọi;
`IN_PROGRESS` và `EXPIRED` trả 422, không có dữ liệu đáp án. Nộp quá giờ vẫn trả
422 nhưng lưu `EXPIRED`, điểm 0 và thời điểm kết thúc; không tạo sự kiện chấm điểm.
Chỉ `AttemptExpiredException` được miễn rollback, các lỗi lưu bài/sự kiện khác
vẫn rollback toàn bộ. Web chỉ mở kết quả lượt đã nộp, lượt hết giờ hiện trong lịch sử.

`QuizAttemptResultIntegrationTest` kiểm HTTP với JWT và đọc lại dữ liệu đã commit
sau lỗi (không bọc test trong transaction), gồm ẩn đáp án, quyền sở hữu, nộp lặp,
lịch sử EXPIRED, và rollback khi lưu outbox thất bại.

### Kết quả kiểm chứng ngày 06/10/2026

Phần kiểm ghi danh (sau thay đổi quyền sở hữu):

- `clean verify`: 594 test toàn repository, gồm 188 test quiz-service; không lỗi hoặc
  bỏ qua. Phần ghi danh bổ sung 56 ca tự động.
- 34 request qua gateway đạt khi chạy course, enrollment, quiz và gateway thật với
  H2 tạm và JWT có kiểm chữ ký. Ghi danh và hủy ghi danh qua API thật; dữ liệu mẫu
  cho trường hợp `COMPLETED` và phân trang được nạp vào H2 trước khi chạy.
- Kiểm tra được cả ghi danh ở trang thứ hai, ghi danh lại sau khi hủy, không thêm
  lượt khi bị từ chối/502, giữ nguyên lượt đang làm khi mất kết nối, và chủ bài/admin
  vẫn tạo lượt làm thử mới khi enrollment-service đã dừng.
- Máy không có Docker: chưa kiểm phần ghi danh với MySQL/Docker; lượt HTTP này tắt
  Kafka và giới hạn request bằng cấu hình tiến trình tạm. Không thay cấu hình bảo
  mật mặc định và chưa thay thế đợt Postman chung.

Phần quyền sở hữu đã kiểm trước đó:

- `clean verify` trên Java 17: 503 test toàn repository, trong đó 132 test quiz-service;
  không lỗi, không bỏ qua. Phần quyền sở hữu bổ sung 84 ca tự động.
- Chạy gateway, course-service và quiz-service thật với H2 tạm, giữ kiểm chữ ký JWT
  ở cả gateway và service: 73 request qua `http://localhost:8080` đều đúng mã HTTP.
  Có kiểm nội dung response, danh sách bị lọc và dữ liệu không đổi sau khi bị từ chối.
- Tạo bài trong khóa `DRAFT` của mình thành công, chứng minh token được chuyển tới
  course-service. Giảng viên khác tạo trong khóa `PUBLISHED` bị 403, khóa không tồn tại
  trả 404. Sau khi dừng course-service, tạo bài trả 502 và không thêm bản ghi; đọc bài
  hiện có vẫn trả 200.
- Kiểm tra cấu hình bảo mật, migration đã merge và định dạng diff đều đạt.
- Máy kiểm thử không có Docker: chưa chạy Docker Compose/MySQL thật. Lượt HTTP dùng
  H2, token ký riêng cho kiểm thử, tắt Kafka và giới hạn request bằng cấu hình tiến
  trình tạm; không thay cấu hình xác thực mặc định. Đây là kiểm chứng phần quyền sở
  hữu, chưa phải biên bản hoàn tất đợt Postman chung của nhóm.
