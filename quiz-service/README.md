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

Các test H2 không thay thế kiểm chứng schema MySQL hoặc chạy toàn bộ Docker stack.
Các ca Postman chung nằm ở [docs/test-cases/quiz.md](../docs/test-cases/quiz.md).

### Kết quả kiểm chứng ngày 06/10/2026

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
