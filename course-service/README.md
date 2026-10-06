# Course service

Quản lý danh mục, khóa học, chương, bài học và tài liệu. Request của client đi qua
gateway `http://localhost:8080`; service chạy nội bộ ở cổng `8082`.

## Quyền truy cập

- Các thao tác ghi khóa học, chương, bài học và tài liệu yêu cầu token có
  `ROLE_INSTRUCTOR` hoặc `ROLE_ADMIN`.
- Giảng viên chỉ được ghi trong khóa học do mình sở hữu. Admin được quản lý mọi khóa.
  Service kiểm tra quyền trước khi cập nhật dữ liệu hoặc phát sự kiện; user ID null
  không được bỏ qua bước kiểm tra.
- Khóa `DRAFT`, kể cả bài preview bên trong, chỉ chủ khóa học và admin đọc được.
  Người khác nhận `404`, dù đã từng ghi danh.
- Khóa `ARCHIVED`: chủ khóa, admin và học viên có ghi danh `ACTIVE`/`COMPLETED`
  được đọc chi tiết khóa (theo ID hoặc slug), đề cương và nội dung/tài liệu bài học.
  Khách, người chưa ghi danh hoặc đã hủy ghi danh nhận `404`, kể cả bài preview.
  Danh sách công khai vẫn chỉ trả khóa `PUBLISHED`; quyền đọc không cấp quyền sửa.
- Bài preview thuộc khóa `PUBLISHED`: khách được đọc nội dung và tài liệu.
- Bài thường thuộc khóa `PUBLISHED`: chỉ chủ khóa học, admin hoặc người có ghi danh `ACTIVE`/`COMPLETED`
  được nhận `content`, `contentUrl` và `resources`. Người chưa có quyền vẫn nhận
  metadata bài học với nội dung null và danh sách tài liệu rỗng. Quy tắc áp dụng
  cả `GET /api/lessons/{id}` và `GET /api/courses/{courseId}/curriculum`.

## Đường dẫn đã chuẩn hóa

| Thao tác | Method và đường dẫn |
|---|---|
| Lọc khóa học của giảng viên | `GET /api/courses?instructorId={id}` |
| Tạo chương | `POST /api/courses/{courseId}/sections` |
| Tạo bài học | `POST /api/sections/{sectionId}/lessons` |
| Xóa tài liệu | `DELETE /api/lessons/{lessonId}/resources/{resourceId}` |

Các đường dẫn cũ tương ứng đã được bỏ. Gateway hiện bao phủ cả bốn đường dẫn mới.
`instructorId` là bộ lọc, không phải danh tính người gọi. Bộ lọc kết hợp được với
`categoryId`, `level`, `keyword`, `page`, `size`, `sort`. Khách/người khác chỉ thấy
khóa đã xuất bản; chủ giảng viên hoặc admin mới thấy khóa nháp trong kết quả lọc.

Body tạo chương không chứa `courseId`:

```json
{"title": "Chương 1", "position": 1}
```

Body tạo/sửa bài học không chứa `sectionId`; ví dụ:

```json
{
  "title": "Bài 1",
  "type": "ARTICLE",
  "content": "Nội dung bài đọc",
  "contentUrl": null,
  "durationSeconds": 0,
  "position": 1,
  "isPreview": false
}
```

`content` giới hạn 1.000.000 ký tự, `contentUrl` giới hạn 500 ký tự. `PUT` thay thế
nội dung: bỏ hai trường này hoặc gửi null sẽ xóa giá trị cũ. Hai cột đã có trong
migration V1; thay đổi này bổ sung mapping entity, không sửa migration đã merge.

Khi xóa tài liệu, `resourceId` phải thuộc đúng `lessonId` trong URL; sai quan hệ trả
`404`, người không phải chủ/admin trả `403`.

## Kiểm tra ghi danh

Course-service sử dụng API đọc hiện có của enrollment-service:
`GET /api/enrollments?page=0&size=100&sort=id,asc`.
Nó chuyển tiếp Bearer token của người gọi, đối chiếu cả `userId`, `courseId` và
trạng thái, đọc tiếp trang sau khi cần. Không nhận user ID do client tự truyền,
không truy cập database của enrollment-service, không dùng API tiến độ có tác dụng
cập nhật dữ liệu. Một lần đọc curriculum chỉ kiểm tra ghi danh tối đa một lượt
(lượt này có thể cần nhiều trang).

| Biến môi trường | Mặc định chạy local |
|---|---|
| `ENROLLMENT_SERVICE_URL` | `http://localhost:8083` |
| `ENROLLMENT_LIST_PATH` | `/api/enrollments` |
| `ENROLLMENT_TIMEOUT_MS` | `2000` |

Docker Compose đặt URL thành `http://enrollment-service:8083`. Deadline 2 giây áp
dụng cho toàn bộ lượt kiểm tra, gồm các trang; không retry, không theo redirect
và không cache quyền. Ghi danh bị hủy sẽ không còn được mở nội dung ở lần đọc sau.
Không đăng nhập/không ghi danh/token bị enrollment-service từ chối: không mở nội
dung. Nếu service đích lỗi, phản hồi sai cấu trúc hoặc timeout:

- Với khóa `ARCHIVED`: không xác minh được ghi danh thì trả `404` cho học viên,
  không tiết lộ cả metadata. Chủ khóa và admin vẫn đọc được mà không cần enrollment-service.
- Đọc đề cương khóa `PUBLISHED` tại `GET /api/courses/{courseId}/curriculum`: ghi log cảnh báo và vẫn
  trả `200`, giữ metadata của tất cả bài học và nội dung/tài liệu bài preview;
  bài thường có `content`, `contentUrl` null và `resources` rỗng.
- Mở trực tiếp bài thường của khóa `PUBLISHED` tại `GET /api/lessons/{id}`: vẫn trả
  `502 EXTERNAL_SERVICE_ERROR` để người học biết chưa thể kiểm tra quyền truy cập.

Chỉ lỗi `EXTERNAL_SERVICE_ERROR` được xử lý theo các quy tắc trên. Các lỗi nghiệp vụ
khác vẫn được trả về; chủ khóa học và admin không cần gọi kiểm tra ghi danh.

Triển khai course-service cùng enrollment-service và gateway có API chuẩn hóa.
Nếu môi trường đang ghi đè đường dẫn cũ, đổi `ENROLLMENT_LIST_PATH=/api/enrollments`
hoặc bỏ biến này để dùng mặc định mới.
Không tự chuyển sang đường dẫn khác khi gặp 404 để tránh che lỗi cấu hình.

### Lưu ý khi cập nhật tiến độ khóa lưu trữ

Quyền đọc khóa `ARCHIVED` cần token của học viên đã ghi danh.
`CourseLessonClient.validateLesson` bên enrollment-service chuyển tiếp nguyên
`Authorization` khi gọi `GET /api/lessons/{id}`. Nhờ đó học viên còn quyền học
cập nhật được tiến độ; không mở quyền đọc khóa lưu trữ cho khách.

## Gửi sự kiện khi Kafka gián đoạn

Migration `V2__add_course_outbox.sql` thêm `outbox_events` và hàng khóa worker.
Thay đổi khóa học và ghi sự kiện chạy trong cùng giao dịch: ghi outbox thất bại
thì thay đổi khóa cũng rollback. Kafka không tham gia giao dịch ghi khóa.
Worker đọc hàng đã commit theo ID, gửi nguyên payload với key `courseId`, chờ
broker xác nhận rồi mới ghi `published_at`. Gặp lỗi thì dừng lượt gửi và thử lại
sau 3 giây. Khóa database ngăn nhiều instance gửi cùng lúc. Khi mất xác nhận hoặc
process chết, sự kiện có thể được gửi lại; consumer snapshot phải chịu được bản trùng.

`COURSE_OUTBOX_ENABLED=false` chỉ tắt lịch gửi, không tắt việc lưu sự kiện.
Không xóa hàng chưa có `published_at`. Sự kiện đã mất trước khi triển khai outbox
không tự phục hồi; cần phát lại snapshot từ nguồn nếu có dữ liệu lệch cũ.

Trong lúc snapshot đang chờ đồng bộ, enrollment-service kiểm tra trực tiếp
`GET /api/courses/{id}` trước khi tạo hoặc kích hoạt lại ghi danh. Khóa không
`PUBLISHED` trả 404; không kiểm tra được nguồn thì 502, không ghi thêm dữ liệu.
Đây là kiểm tra tại thời điểm gọi, không phải giao dịch phân tán khóa cả hai database.

## Kiểm tra

```powershell
.\mvnw.cmd -pl course-service -am test
.\mvnw.cmd clean verify
bash scripts/verify-schema.sh
```

`CurriculumAuthorizationTest` chạy controller, service và repository thật trên H2,
bật xác thực JWT để kiểm tra tám thao tác ghi, quyền đọc, ID cha/con, bộ lọc và dữ
liệu sau khi request bị từ chối. `EnrollmentAccessClientTest` dùng HTTP server cục
bộ để kiểm tra phân trang, token, ghi danh hủy, timeout, redirect và lỗi phản hồi.
`EnrollmentContentAccessIntegrationTest` dùng JWT, MVC, H2 và client HTTP thật với
enrollment backend giả lập để kiểm tra đường dẫn cấu hình, chuyển tiếp token và
quyền đọc bài học/đề cương của ghi danh ACTIVE, COMPLETED, CANCELLED hoặc chưa ghi danh.
Test cũng đối chiếu đường dẫn mặc định production với cấu hình đã kiểm thử.

Collection cập nhật: `docs/postman/course-service-v2.postman_collection.json` cùng
environment V2. Import lại collection, chọn environment, nhập mật khẩu rồi chạy
theo thứ tự. URL trong collection đã điền trực tiếp `http://127.0.0.1:8080`.
`runCleanup=true` chỉ xóa dữ liệu thử do collection tạo.

Việc nạp `course_snapshots` là phần của enrollment-service. Nếu snapshot chưa có,
API ghi danh có thể trả 404; course-service không tự coi người chưa ghi danh là có
quyền học.
