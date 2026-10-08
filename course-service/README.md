# Course service

Quản lý danh mục, khóa học, chương, bài học và tài liệu. Request của client đi qua
gateway `http://localhost:8080`; service chạy nội bộ ở cổng `8082`.

## Tài liệu đính kèm trên khu giảng dạy

Vào khóa học → menu bài học → **Sửa bài học** → **Tài liệu đính kèm**.
Mục này cũng dùng chung với lối tắt **Tài liệu** trong menu bài học.
Thêm tên và URL là lưu tài liệu ngay, độc lập với nút **Lưu bài học**.
Xóa cần xác nhận; lỗi API giữ dữ liệu nhập và cho thử lại.

`POST /api/lessons/{lessonId}/resources` chỉ chấp nhận URL tuyệt đối có host,
giao thức HTTP hoặc HTTPS (không phân biệt hoa thường), không chứa thông tin đăng nhập.
URL sai trả 400 `VALIDATION_FAILED`, kèm `fieldErrors` cho `fileUrl` để hiện dưới ô.
Không có thao tác tải tệp lên máy chủ; tài liệu là liên kết tới tệp đã có.
Các URL cũ không hợp lệ không được mở từ màn quản lý; giảng viên có thể xóa và thêm lại.

Kiểm thử trình duyệt: `scripts/check-course-resources.cjs` (Playwright), gồm luồng
giảng viên thêm/xóa, học viên mở link, lỗi mạng, quyền truy cập và sao trung bình 4,5.

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

`categoryId` của danh mục gốc lấy cả khóa trực tiếp và khóa trong các danh mục con
(cây hai cấp). Chọn ID danh mục con chỉ lấy khóa của danh mục đó; danh mục không tồn tại
trả trang rỗng. Quyền xem, các bộ lọc còn lại và tổng phân trang vẫn áp dụng cùng nhau.

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

## Đánh giá khóa học

| Endpoint | Quyền và kết quả |
|---|---|
| `GET /api/courses/{id}/reviews?page=0&size=5` | Theo quyền xem khóa học hiện có; khách chỉ thấy khóa PUBLISHED. Mới nhất trước, ID giảm dần khi cùng thời điểm. |
| `GET /api/courses/{id}/reviews/me` | Cần token hợp lệ; trả `canReview` và đánh giá của chính người gọi, không phụ thuộc trang đang đọc. |
| `PUT /api/courses/{id}/reviews/me` | Tạo/sửa đánh giá của mình, trả 200. Body `{"rating":5,"comment":"Nội dung hữu ích"}`. |
| `DELETE /api/courses/{id}/reviews/me` | Xóa đánh giá của mình, trả 200; chưa có đánh giá trả 404. |

Quyền viết yêu cầu có trong `course_learners` của đúng khóa (không gọi enrollment-service).
Chủ khóa hoặc admin cũng không được bỏ qua điều kiện này. Đây là **lịch sử từng ghi danh**:
hủy hoặc xóa ghi danh không mất quyền viết. Khóa PUBLISHED/ARCHIVED nhận đánh giá; khóa nháp
không nhận đánh giá. GET danh sách giữ nguyên quyền đọc khóa hiện tại, bao gồm quyền đọc ARCHIVED.

`rating` phải là số nguyên 1–5, `comment` tối đa 2000 ký tự. Không nhận danh tính/tên từ body:
lấy ID và tên đã xác thực trong token, lưu tên tại lần ghi gần nhất bằng migration V5.
Response không có email/userId; nhận xét là văn bản thuần, giao diện không render HTML.

Mỗi cặp khóa/người dùng chỉ có một dòng. Khóa dòng `courses` trước khi đọc/ghi đánh giá;
flush rồi tính lại trung bình (2 chữ số thập phân) và số lượt trong cùng transaction.
Hai cột thống kê không cho JPA cập nhật từ PUT khóa học. Xóa lượt cuối đưa cả hai số về 0.
Collection và kiểm thử đồng thời có trong `COURSE-25` và `CourseReviewConcurrencyTest`.

## Kiểm tra ghi danh cho quyền đọc nội dung

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

## Số học viên từng ghi danh

`studentCount` là **số người từng ghi danh duy nhất trong mỗi khóa học**, không phải
số lượt ghi danh hoặc số học viên đang ACTIVE.
Consumer riêng (`group-id=course-service`) đọc `enrollment.created` từ
`elearning.enrollment.events`. Trong cùng transaction, consumer ghi `processed_events`,
khóa hàng khóa học bằng `SELECT ... FOR UPDATE`, rồi `INSERT IGNORE` vào
`course_learners` có khóa chính `(course_id, user_id)`. Chỉ khi chèn được cặp mới mới
tăng `courses.student_count` bằng SQL nguyên tử. Lỗi ở bất kỳ bước nào rollback cả
ba thay đổi. Khóa hàng cũng ngăn khóa học bị xóa giữa lúc kiểm tra và ghi học viên.
Nhận lại cùng `eventId` hoặc nhận `eventId` mới của cùng học viên đều không tăng lần hai.
Chỉnh sửa khóa học không ghi đè số đếm do consumer cập nhật.
Khóa có số đếm lớn hơn 0 không được xóa, kể cả khi chuyển về DRAFT.
Lệnh DELETE kiểm tra lại trạng thái DRAFT và số đếm bằng 0 ngay trong database,
tránh xóa theo dữ liệu cũ nếu consumer vừa tăng số đếm sau bước đọc ban đầu.

Migration `V3__add_processed_events.sql` tạo sổ chống trùng message;
`V4__add_course_learners.sql` tạo sổ học viên duy nhất. Mỗi service có consumer
group riêng, nên notification-service vẫn nhận đủ sự kiện. Consumer đọc từ earliest
khi group chưa có offset: chỉ bù được sự kiện Kafka còn lưu. Các ghi danh cũ hơn thời
gian retention cần đối soát riêng, không tự coi số 0 là chưa từng có học viên.
Do đồng bộ bất đồng bộ, số đếm và việc chặn xóa dựa trên số đếm có thể trễ khi Kafka lỗi.

Hủy/xóa ghi danh không trừ số đếm và không xóa `course_learners`. Tái kích hoạt lượt
CANCELLED hay xóa rồi tạo lại ghi danh (đổi cả enrollmentId và eventId) vẫn chỉ tính
một người. Cùng người học hai khóa được tính một lần ở mỗi khóa. Muốn đếm người đang
học cần thống nhất thêm sự kiện hủy/xóa với enrollment-service; chưa áp dụng ở đây.

### Nâng cấp môi trường đã chạy bản đếm theo eventId

V4 chỉ tạo bảng, **không tự sửa số đếm cũ**: bảng `processed_events` cũ không lưu
userId nên không đủ thông tin suy ra các cặp học viên. Để lại số đếm cũ và một bảng
`course_learners` rỗng sẽ làm lệch số khi người cũ ghi danh lại. Trước khi phục vụ lại,
cần đối soát/nạp đầy đủ các cặp lịch sử rồi đặt `student_count` bằng số hàng tương ứng.

Nếu topic còn đủ lịch sử, có thể dựng lại từ Kafka theo quy trình bảo trì sau:

1. Sao lưu course database và ghi lại offset hiện tại. Dừng mọi instance course-service,
   tạm ngừng các thao tác ghi/xóa khóa và ghi danh; áp dụng V4 khi consumer đang tắt
   (`spring.kafka.enabled=false`), rồi dừng instance đó.
2. Trong course database, chạy một transaction để xóa dữ liệu dẫn xuất:
   `DELETE FROM course_learners;`,
   `DELETE FROM processed_events WHERE event_type = 'enrollment.created';`,
   `UPDATE courses SET student_count = 0;`, rồi commit.
   Không xóa khóa học, ghi danh hoặc outbox.
3. Khi group đã dừng, dùng công cụ Kafka reset offset group `course-service` của
   riêng topic `elearning.enrollment.events` về earliest; hoặc cấu hình một group
   mới và giữ tên đó sau triển khai. Chỉ đổi `auto-offset-reset` không reset offset
   đã có. Khởi động consumer và chờ hết lag, kiểm tra DLT và đối chiếu số hàng
   `course_learners` với `courses.student_count` trước khi mở lại thao tác ghi.

Không chạy quy trình reset nếu topic đã mất lịch sử mà chưa có bản xuất/backfill
các cặp học viên còn thiếu. Bản xuất ghi danh hiện tại không chứa người đã xóa ghi
danh, nên không đủ để khôi phục số người từng học. Replay có thể tính cả người thuộc
dữ liệu test cũ đã xóa: đó là đúng theo quy tắc lịch sử, nhưng mỗi người chỉ một lần.

Lỗi database tạm thời được thử lại (1s, 2s, 4s… tối đa 30s/lần, tổng thời gian chờ
5 phút; chỉnh bằng `elearning.kafka.retry.*`). Message sai cấu trúc hoặc không tìm
thấy khóa được giữ ở `elearning.enrollment.events.DLT`; không ghi sổ và không tăng
đếm. Hết giới hạn retry cũng chuyển DLT. Nếu gửi DLT thất bại thì chưa xác nhận message.
Đối soát nguyên nhân rồi phát lại cùng eventId; không xóa sổ chống trùng khi replay.

## Kiểm tra

### Admin gỡ đánh giá

`DELETE /api/courses/{courseId}/reviews/{reviewId}` chỉ dành cho `ROLE_ADMIN`:
không đăng nhập trả 401, học viên hoặc giảng viên (kể cả chủ khóa/người viết) trả 403.
Không tìm thấy khóa/đánh giá hoặc đánh giá thuộc khóa khác trả 404. Thành công trả 200.
Admin không cần ghi danh và có thể gỡ ở mọi trạng thái khóa học.

Giao dịch khóa hàng `courses` trước khi tìm đánh giá, xóa và tính lại
`ratingAvg`/`ratingCount`, cùng thứ tự khóa với ghi/sửa/tự xóa đánh giá.
Xóa lượt cuối đặt cả hai số liệu về 0. Gỡ không cấm tài khoản: người viết
vẫn có thể tạo đánh giá mới nếu đủ điều kiện theo sổ học viên và trạng thái khóa.

Trang chi tiết chỉ hiện nút **Gỡ** cho admin, có xác nhận và báo lỗi trong hộp thoại.
Gỡ thành công tải lại điểm/danh sách và về trang đánh giá đầu để tránh trang cuối rỗng.
Các ca API nằm ở `COURSE-27`; kiểm thử trình duyệt: `scripts/check-course-moderation.cjs`.

### Giảng viên trả lời đánh giá

Migration V6 thêm `reply` (1000 ký tự), `replied_at`, `replied_by` nullable vào `course_reviews`;
đánh giá cũ không có phản hồi. Không sửa migration đã chạy.

- `PUT /api/courses/{courseId}/reviews/{reviewId}/reply`, body `{"content":"Cảm ơn bạn"}`:
  tạo hoặc sửa một phản hồi, trả 200 với đánh giá đã cập nhật. Nội dung bắt buộc, không chỉ
  gồm khoảng trắng, tối đa 1000 ký tự; lỗi trả 400 `VALIDATION_FAILED` theo ô `content`.
- `DELETE` cùng đường dẫn: xóa riêng phản hồi, trả 200; chưa có phản hồi trả 404.
- Cả hai yêu cầu `ROLE_ADMIN` hoặc `ROLE_INSTRUCTOR` đúng chủ khóa; người khác 403,
  không token 401, không tìm thấy khóa/đánh giá hoặc sai khóa cha 404.
- `replied_by` lấy từ JWT, chỉ lưu nội bộ. Response công khai thêm `reply`, `repliedAt`,
  không lộ ID người trả lời. Quyền đọc vẫn theo trạng thái khóa và quyền học hiện có.
- Ghi/xóa phản hồi dùng khóa dòng `courses` cùng thứ tự với đánh giá; không thay số sao
  hoặc số lượt. Học viên sửa đánh giá giữ nguyên phản hồi; xóa đánh giá xóa cả phản hồi.
- Web dùng văn bản thuần, nhãn **Phản hồi của giảng viên** hoặc **Phản hồi của quản trị viên**,
  cho chủ khóa/admin Trả lời/Sửa/Xóa,
  xác nhận khi xóa, lỗi theo ô và giữ dữ liệu khi API lỗi. Giữ nguyên trang phân trang khi lưu.

Kiểm thử API trong `COURSE-28`, trình duyệt bằng `scripts/check-course-replies.cjs`.

`replyAuthorRole` do server tính: `INSTRUCTOR` nếu `repliedBy` trùng `instructorId`
của khóa, còn lại là `ADMIN`; chưa có phản hồi thì null. Không trả `repliedBy` ra API.
`updatedAt` của đánh giá đổi cả khi lưu/xóa phản hồi do `@UpdateTimestamp`, vì vậy
**không dùng trường này để kết luận học viên đã sửa đánh giá**. `repliedAt` là thời
điểm tạo/sửa phản hồi, còn `createdAt` dùng cho thứ tự danh sách đánh giá.

### Đánh giá chờ phản hồi

`GET /api/instructor/reviews?replied=false&courseId=123&page=0&size=10` đi qua
gateway 8080, bắt buộc JWT hợp lệ. INSTRUCTOR chỉ thấy đánh giá trên khóa của mình
(kể cả khóa nháp/lưu trữ); ADMIN thấy mọi khóa. Học viên trả 403, không token 401;
lọc khóa của giảng viên khác trả 403, khóa không tồn tại 404.

- `replied=false`: chưa trả lời; `true`: đã trả lời; bỏ tham số: tất cả.
- `reviews` là đối tượng phân trang; mỗi dòng `content` có `courseId`, `courseTitle`
  và `review` (đánh giá cùng phản hồi). Sắp xếp cố định `createdAt DESC, id DESC`.
- `unrepliedCount` đếm trong phạm vi người gọi và `courseId` nếu có, độc lập với
  `replied` và trang hiện tại. Sidebar/tổng quan bỏ `courseId` để đếm tất cả khóa.
- `courses` chứa ID/tên các khóa có đánh giá mà người gọi được quản lý, không phụ
  thuộc bộ lọc hiện tại; dùng cho ô chọn khóa, không làm lộ khóa của người khác.

Web `/instructor/reviews` mặc định chưa trả lời, 10 dòng/trang, dùng chung
`ReviewReply` để thêm/sửa/xóa. Lưu xong cập nhật danh sách, số đếm và sidebar;
trang cuối hết dòng thì về trang hợp lệ gần nhất, giữ bộ lọc. Gõ lại nội dung xóa
lỗi ô nhập và hiện lại bộ đếm. API lỗi hiện thông báo, không hiển thị số 0 giả.
Ca API: `COURSE-29`; trình duyệt: `scripts/check-course-inbox.cjs`.

### Chạy kiểm thử

Các thao tác sửa khóa học, đổi trạng thái và sửa chương/bài khóa hàng `courses`
trước khi ghi dữ liệu. Khi nhiều request cùng thêm/xóa/sửa bài, mỗi request đọc lại
số bài và thời lượng sau khi lấy được khóa, tránh ghi đè số liệu và tránh deadlock
do cùng chèn bài rồi mới nâng khóa ngoại lên khóa ghi. Giao dịch ghi giáo trình dùng
`READ_COMMITTED` để truy vấn bài và tính tổng không dùng snapshot cũ từ bước tìm
courseId. Sự kiện outbox được lưu trong cùng giao dịch với số liệu đã cập nhật.

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

`CurriculumConcurrencyIntegrationTest` kiểm tra tạo bài đồng thời với sửa khóa,
sửa thời lượng cùng một bài, xóa nhiều bài, xóa chương đồng thời với thêm bài ở
chương khác và nhận sự kiện ghi danh trong lúc tạo bài. Đối chiếu số liệu trên
khóa với dữ liệu bài thực tế và snapshot outbox.

`EnrollmentEventIntegrationTest` kiểm tra số đếm, rollback cả sổ học viên, gửi trùng đồng thời,
khác eventId/enrollmentId nhưng cùng người, cùng người ở hai khóa,
message sai và việc sửa khóa không ghi đè số đếm. `EnrollmentEventKafkaIntegrationTest`
dùng Kafka thật trong JVM để kiểm retry, DLT, chống trùng và việc message sau vẫn
được xử lý. `KafkaErrorHandlingConfigTest` kiểm cả trường hợp gửi DLT thất bại.

Collection cập nhật: `docs/postman/course.postman_collection.json`. Import duy nhất
file này, chọn **No environment** và chạy từ **0. Chuẩn bị**. Collection tự lưu token,
ID và dùng `baseUrl=http://localhost:8080` qua gateway. Hướng dẫn và quy tắc giữ dữ liệu
demo ở [README Postman](../docs/postman/README-course.md); biên bản ở
[kết quả course](../docs/test-cases/ket-qua/course.md).

Việc nạp `course_snapshots` là phần của enrollment-service. Nếu snapshot chưa có,
API ghi danh có thể trả 404; course-service không tự coi người chưa ghi danh là có
quyền học.
