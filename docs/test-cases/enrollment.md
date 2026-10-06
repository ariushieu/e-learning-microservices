# Tình huống test enrollment-service

Đọc [gateway.md](gateway.md) để chuẩn bị tài khoản, token, ID và biên bản.
Mọi request qua `{{baseUrl}}`. **Kế hoạch chưa chạy Postman.**

## Điều kiện và đường dẫn

Consumer `course.updated` nạp `course_snapshots` còn **[CHỜ SỬA]**. Xuất bản khóa có hai bài,
đợi snapshot đồng bộ trước ca ghi danh thành công. Nếu khóa tồn tại bên course nhưng chưa
có snapshot, ghi BLOCKED cho ca ghi danh/tiến độ/chứng chỉ; không coi 404 đó là PASS.
Không tự ghi SQL vào snapshot để che thiếu consumer.

| Hiện tại | Đường dẫn đích dùng trong bảng |
|---|---|
| GET /api/enrollments/my-courses | GET /api/enrollments |
| PATCH /api/enrollments/{id}/cancel | PATCH /api/enrollments/{id}/status + {"status":"CANCELLED"} |
| DELETE /api/enrollments/course/{id} | DELETE /api/enrollments?courseId={id} |
| POST /api/progress/lesson | PUT /api/lessons/{lessonId}/progress |
| GET /api/progress/course/{id} | GET /api/progress?courseId={id} |

Các route trên **[CHỜ ROUTE]**. Route PUT tiến độ phải đến enrollment-service, không bị
`/api/lessons/**` của course-service bắt trước. ID lesson chuyển từ body sang path; body vẫn
có courseId cho đến khi nhóm chốt hợp đồng khác.

Các API này cho phép **mọi người đã đăng nhập thao tác dữ liệu của chính mình**; không tự
đặt yêu cầu chỉ ROLE_STUDENT khi code/hợp đồng không có. Dùng B/ADM để kiểm truy cập dữ liệu
của S. Chỉ GET danh sách ghi danh có Pageable; các API khác không nhận sort.

## Body mẫu

**ENROLL-CREATE**: `{"courseId":{{publishedCourseId}}}`.

**ENROLL-PROGRESS** cho URL chứa lessonAId:

```json
{"courseId":{{publishedCourseId}},"status":"COMPLETED","watchedSeconds":60}
```

Biến enrollmentId là của S, enrollmentBId là của B. Mỗi ca hủy/xóa phải có lượt ghi danh
riêng hoặc tạo lại fixture. Để kiểm hoàn thành: khóa có đúng hai bài, S hoàn thành bài 1 → 50,
hoàn thành bài 2 → 100, trạng thái COMPLETED và có chứng chỉ.

## ENROLL-01 — POST /api/enrollments

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Ghi danh khóa đã đồng bộ | S | POST /api/enrollments + ENROLL-CREATE, chưa từng ghi danh | 201; userId=studentId, status=ACTIVE, progressPercent=0 |
| 2 | Không token | — | Cùng URL/body hợp lệ | 401 |
| 3 | Giảng viên ghi danh cho chính mình | B | ENROLL-CREATE, B chưa ghi danh | 201; userId=instructorBId, không tác động lượt của S |
| 4 | Khóa không tồn tại | S | {"courseId":{{missingId}}} | 404 |
| 5 | Sai kiểu ID | S | {"courseId":"abc"} | 400 |
| 6 | ID không dương/thiếu | S | Lần lượt {"courseId":0}, {"courseId":-1}, {} | 400 mỗi request |
| 7 | Ghi danh lần hai | S | POST cùng khóa khi lượt đã ACTIVE | 409 |
| 8 | Đăng ký lại sau hủy | S | Hủy lượt ACTIVE rồi POST lại cùng khóa | 201; dùng lại ID cũ, status=ACTIVE; không tạo hai lượt |
| 9 | Giả người học | S | Body thêm "userId":{{instructorBId}} trên khóa S chưa ghi danh | 201; vẫn thuộc S |
| 10 | Khóa bỏ xuất bản đã đồng bộ | S | Với khóa chưa ghi danh, A xuất bản rồi ARCHIVED; đợi snapshot cập nhật; POST khóa đó | 404; DefaultCourseClient lọc bỏ snapshot không PUBLISHED trước khi service kiểm trạng thái; chưa đồng bộ thì BLOCKED |
| 11 | Thông báo ghi danh | S | Sau ca 1, GET /api/notifications theo thời hạn gateway.md | GET 200; đúng một thông báo ghi danh khóa tương ứng |

## ENROLL-02 — GET /api/enrollments [CHỜ ROUTE]

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Danh sách của mình | S | GET /api/enrollments?page=0&size=2&sort=enrolledAt,desc | 200; data.content chỉ các lượt của S, metadata phân trang |
| 2 | Không token | — | GET /api/enrollments | 401 |
| 3 | Tài khoản khác | B | GET /api/enrollments | 200; không có enrollmentId của S |
| 4 | Trang không có dữ liệu | S | GET /api/enrollments?page=999999&size=2 | 200; content=[] |
| 5 | Sort bịa | S | GET /api/enrollments?sort=abcxyz | 400 |
| 6 | Sort thuộc tính con bịa | S | GET /api/enrollments?sort=course.nonexistent | 400; không 500 |
| 7 | Không cho đọc hộ | B | GET /api/enrollments?userId={{studentId}} | 200; vẫn chỉ dữ liệu B |
| 8 | Dữ liệu đã hủy | S | Sau hủy một lượt, GET /api/enrollments | 200; lượt đó có status=CANCELLED (không bị xóa) |

Không có ID đích để thử 404 trên collection; dùng trang rỗng. Không dùng page=abc làm ca 400:
Pageable có thể tự dùng mặc định. Hai ca sort kiểm nhánh sai thuộc tính thực sự.

## ENROLL-03 — GET /api/enrollments/{id}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Đọc lượt của mình | S | GET /api/enrollments/{{enrollmentId}} | 200; userId=studentId |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Đọc lượt của S | B | Cùng URL | 403 |
| 4 | Admin không có ngoại lệ đọc hộ | ADM | Cùng URL | 403 |
| 5 | Không tồn tại | S | GET /api/enrollments/{{missingId}} | 404 |
| 6 | ID sai kiểu | S | GET /api/enrollments/abc | 400 |
| 7 | Đọc lượt đã hủy của mình | S | GET ID của S có status=CANCELLED | 200; status=CANCELLED |

## ENROLL-04 — PATCH /api/enrollments/{id}/status [CHỜ ROUTE]

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hủy lượt ACTIVE | S | PATCH /api/enrollments/{{enrollmentId}}/status, {"status":"CANCELLED"} | 200; status=CANCELLED |
| 2 | Không token | — | Cùng URL/body | 401 |
| 3 | Hủy lượt của S | B | Cùng URL/body | 403 |
| 4 | Không tồn tại | S | PATCH /api/enrollments/{{missingId}}/status + body hợp lệ | 404 |
| 5 | ID sai kiểu | S | PATCH /api/enrollments/abc/status + body hợp lệ | 400 |
| 6 | Hủy khóa đã hoàn thành | S | PATCH lượt COMPLETED của S, status=CANCELLED | 422; giữ trạng thái COMPLETED |
| 7 | Hủy lại | S | Gửi lại cho lượt đã CANCELLED | 200; vẫn CANCELLED |
| 8 | Enum sai/thiếu [CẦN CHỐT DTO mới] | S | Lần lượt {"status":"UNKNOWN"}, {} | 400 theo quy ước validation |
| 9 | Tự cấp hoàn thành [CẦN CHỐT] | S | PATCH lượt ACTIVE, {"status":"COMPLETED"} | 422 đề xuất; không cho tự nhận chứng chỉ |

Đích chuẩn hóa chỉ thay thao tác cancel hiện có; không mặc nhiên mở mọi chuyển trạng thái.
Ca 9 yêu cầu nhóm enrollment chốt mã lỗi trước đợt chạy.

## ENROLL-05 — DELETE /api/enrollments?courseId={id} [CHỜ ROUTE]

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Xóa lượt của mình | S | DELETE /api/enrollments?courseId={{publishedCourseId}} | 200; lượt, tiến độ và chứng chỉ của S bị xóa; GET ID cũ 404 |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Người khác chưa ghi danh | A | Cùng URL khi A chưa ghi danh | 404; lượt của S không bị xóa |
| 4 | Khóa không có lượt của S | S | DELETE /api/enrollments?courseId={{missingId}} | 404 |
| 5 | Sai kiểu | S | DELETE /api/enrollments?courseId=abc | 400 |
| 6 | Thiếu bộ lọc bắt buộc | S | DELETE /api/enrollments | 400; không xóa hàng loạt |
| 7 | Người khác cũng ghi danh | B | DELETE cùng courseId, khi cả S và B có lượt riêng | 200; chỉ xóa lượt B; S đọc lượt mình vẫn 200 |
| 8 | Ghi danh lại sau reset | S | Sau ca 1, POST /api/enrollments + ENROLL-CREATE | 201; ID mới, progressPercent=0 |

## ENROLL-06 — GET /api/enrollments/{id}/certificate

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hoàn thành đủ hai bài | S | GET /api/enrollments/{{enrollmentId}}/certificate | 200; certificateCode có giá trị; userId=studentId; courseId đúng |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Chứng chỉ của người khác | B | Cùng URL | 403 |
| 4 | Lượt không tồn tại | S | GET /api/enrollments/{{missingId}}/certificate | 404 |
| 5 | ID sai kiểu | S | GET /api/enrollments/abc/certificate | 400 |
| 6 | Chưa hoàn thành | S | GET certificate của lượt ACTIVE riêng mới ghi danh | 404 |
| 7 | Đọc nhiều lần | S | Gọi lại ca 1 | 200; cùng certificateCode và id, không cấp trùng |
| 8 | Admin đọc hộ | ADM | GET chứng chỉ của S | 403 |

Không yêu cầu fileUrl luôn có PDF: hiện hệ thống có mã chứng chỉ, chưa có luồng dựng/tải PDF.

## ENROLL-07 — PUT /api/lessons/{lessonId}/progress [CHỜ ROUTE]

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hoàn thành bài đầu | S | PUT /api/lessons/{{lessonAId}}/progress + ENROLL-PROGRESS | 200; bài COMPLETED; GET /api/progress?courseId=publishedCourseId trả 200 và progressPercent=50 |
| 2 | Không token | — | Cùng URL/body | 401 |
| 3 | Người chưa ghi danh | A | Cùng URL/body, A chưa ghi danh | 404; không cập nhật thay S |
| 4 | Không có lượt cho khóa | S | Body courseId={{missingId}} | 404 |
| 5 | ID bài sai kiểu | S | PUT /api/lessons/abc/progress + body hợp lệ | 400 |
| 6 | Giây xem âm | S | ENROLL-PROGRESS với watchedSeconds=-1 | 400 |
| 7 | Thiếu/sai trạng thái | S | Lần lượt bỏ status, status="INVALID" | 400 |
| 8 | Lượt đã hủy | S | PUT bài của lượt CANCELLED | 422 |
| 9 | Gửi hoàn thành lần hai | S | Lặp ca 1 | 200; vẫn 50%, không đếm bài hai lần |
| 10 | Hoàn thành bài cuối | S | PUT /api/lessons/{{lesson2Id}}/progress + ENROLL-PROGRESS | 200; GET tiến độ 200, 100%, COMPLETED; GET certificate 200 |
| 11 | Giây xem không giảm | S | Đã xem 60, PUT watchedSeconds=10, cùng status | 200; watchedSeconds vẫn 60 |
| 12 | Giả danh trong body | B (đã ghi danh) | ENROLL-PROGRESS thêm userId=studentId | 200; chỉ tiến độ B thay đổi |
| 13 | Bài không tồn tại hoặc khác khóa [CẦN CHỐT] | S | PUT missingId hoặc bài khóa B với courseId khóa A | 404 đề xuất; không tăng tiến độ; code hiện chưa kiểm lesson thuộc course |

Ca 13 là lỗ hổng phát hiện khi đọc service, chưa có hợp đồng client kiểm lesson; ghi BLOCKED
cho nghiệm thu cho đến khi chủ service chốt/sửa, không coi việc tăng điểm bằng ID bịa là đúng.

## ENROLL-08 — GET /api/progress?courseId={id} [CHỜ ROUTE]

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Đọc tiến độ | S | GET /api/progress?courseId={{publishedCourseId}} | 200; enrollmentId đúng; totalLessonsCount=2; lessons chỉ của S |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Người chưa ghi danh | A | Cùng URL | 404 |
| 4 | Khóa không có lượt | S | GET /api/progress?courseId={{missingId}} | 404 |
| 5 | Sai kiểu | S | GET /api/progress?courseId=abc | 400 |
| 6 | Thiếu courseId | S | GET /api/progress | 400 |
| 7 | Giả userId | B (đã ghi danh) | GET /api/progress?courseId={{publishedCourseId}}&userId={{studentId}} | 200; enrollmentId=enrollmentBId |
| 8 | Chưa học bài nào | S | GET tiến độ lượt mới chưa có lesson_progress | 200; progressPercent=0, completedLessonsCount=0 |
| 9 | Đọc sau hoàn thành | S | GET sau hoàn thành 2/2 bài, gọi hai lần | 200; progressPercent=100, certificateCode không đổi |

## Truy vết nguồn

- [EnrollmentController](../../enrollment-service/src/main/java/com/hunre/enrollmentservice/controller/EnrollmentController.java),
  [ProgressController](../../enrollment-service/src/main/java/com/hunre/enrollmentservice/controller/ProgressController.java).
- [EnrollmentServiceImpl](../../enrollment-service/src/main/java/com/hunre/enrollmentservice/service/impl/EnrollmentServiceImpl.java),
  [ProgressServiceImpl](../../enrollment-service/src/main/java/com/hunre/enrollmentservice/service/impl/ProgressServiceImpl.java).
- [DTO](../../enrollment-service/src/main/java/com/hunre/enrollmentservice/dto/request),
  [phân công consumer và route](../phan-cong.md).
