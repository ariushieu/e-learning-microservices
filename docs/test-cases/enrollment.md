# Tình huống test enrollment-service

Đọc [gateway.md](gateway.md) để chuẩn bị tài khoản, token, ID và biên bản.
Mọi request qua `{{baseUrl}}`. Collection và cách chạy ở [Postman enrollment](../postman/enrollment.md);
kết quả thực tế và giới hạn môi trường ở [biên bản](ket-qua/enrollment.md).

## Điều kiện và đường dẫn

Consumer `course.updated` nạp `course_snapshots` đã được triển khai trong nhánh này. Xuất bản khóa có hai bài,
đợi snapshot đồng bộ trước ca ghi danh thành công. Nếu khóa tồn tại bên course nhưng chưa
có snapshot, ghi BLOCKED cho ca ghi danh/tiến độ/chứng chỉ; không coi 404 đó là PASS.
Không tự ghi SQL vào snapshot để che thiếu consumer.

| Đường dẫn cũ đã bỏ | Đường dẫn hiện tại dùng trong bảng |
|---|---|
| GET /api/enrollments/my-courses | GET /api/enrollments |
| PATCH /api/enrollments/{id}/cancel | PATCH /api/enrollments/{id}/status + {"status":"CANCELLED"} |
| DELETE /api/enrollments/course/{id} | DELETE /api/enrollments?courseId={id} |
| POST /api/progress/lesson | PUT /api/lessons/{lessonId}/progress |
| GET /api/progress/course/{id} | GET /api/progress?courseId={id} |

Các route trên đã có trong nhánh này. Route PUT tiến độ phải đến enrollment-service, không bị
`/api/lessons/**` của course-service bắt trước. ID lesson chuyển từ body sang path; body vẫn
có courseId cho đến khi nhóm chốt hợp đồng khác.

Các API này cho phép **mọi người đã đăng nhập thao tác dữ liệu của chính mình**; không tự
đặt yêu cầu chỉ ROLE_STUDENT khi code/hợp đồng không có. Dùng B/ADM để kiểm truy cập dữ liệu
của S. GET danh sách ghi danh và GET học viên của khóa (ENROLL-11) có phân trang/sort.

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

## ENROLL-02 — GET /api/enrollments

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

## ENROLL-04 — PATCH /api/enrollments/{id}/status

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hủy lượt ACTIVE | S | PATCH /api/enrollments/{{enrollmentId}}/status, {"status":"CANCELLED"} | 200; status=CANCELLED |
| 2 | Không token | — | Cùng URL/body | 401 |
| 3 | Hủy lượt của S | B | Cùng URL/body | 403 |
| 4 | Không tồn tại | S | PATCH /api/enrollments/{{missingId}}/status + body hợp lệ | 404 |
| 5 | ID sai kiểu | S | PATCH /api/enrollments/abc/status + body hợp lệ | 400 |
| 6 | Hủy khóa đã hoàn thành | S | PATCH lượt COMPLETED của S, status=CANCELLED | 422; giữ trạng thái COMPLETED |
| 7 | Hủy lại | S | Gửi lại cho lượt đã CANCELLED | 200; vẫn CANCELLED |
| 8 | Enum sai/thiếu | S | Lần lượt {"status":"UNKNOWN"}, {} | 400 theo quy ước validation |
| 9 | Tự cấp hoàn thành | S | PATCH lượt ACTIVE, {"status":"COMPLETED"} | 422; không cho tự nhận chứng chỉ |

Đích chuẩn hóa chỉ thay thao tác cancel hiện có; không mặc nhiên mở mọi chuyển trạng thái.
Ca 9 đã chốt 422 trong API và kiểm thử tự động; ACTIVE cũng bị từ chối qua PATCH.

## ENROLL-05 — DELETE /api/enrollments?courseId={id}

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

## ENROLL-07 — PUT /api/lessons/{lessonId}/progress

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
| 13 | Bài không tồn tại hoặc khác khóa | S | PUT missingId hoặc bài khóa B với courseId khóa A | 404; không tăng tiến độ; CourseLessonClient kiểm bài học thuộc đúng khóa |
| 14 | Không hạ trạng thái bài đã hoàn thành | S | Sau ca 1, gửi IN_PROGRESS với watchedSeconds lớn hơn rồi nhỏ hơn | 200; bài vẫn COMPLETED, completedAt không đổi, watchedSeconds chỉ tăng; tiến độ khóa vẫn 50% |
| 15 | Không thu hồi hoàn thành/chứng chỉ | S | Sau ca 10, lưu certificateCode rồi gửi IN_PROGRESS cho bài đã hoàn thành; GET tiến độ và chứng chỉ | 200; khóa vẫn COMPLETED, 100%, completedAt và certificateCode không đổi; không thêm thông báo hoàn thành/cấp chứng chỉ |
| 16 | Hai request tiến độ đồng thời | S | Trên lượt mới, gửi COMPLETED và IN_PROGRESS gần đồng thời cho cùng bài | Cả hai 200; chỉ một dòng tiến độ, trạng thái cuối COMPLETED, watchedSeconds bằng giá trị lớn nhất; không cấp trùng chứng chỉ |
| 17 | Bổ sung bài sau khi đã cấp chứng chỉ | A rồi S | Sau ca 10, A thêm bài vào khóa; đợi snapshot; S gửi IN_PROGRESS rồi GET tiến độ | 200; totalLessonsCount tăng nhưng lượt đã hoàn thành vẫn COMPLETED, 100%, mã chứng chỉ giữ nguyên |

Ca 13 đã có client kiểm bài học qua course-service và test tự động. Khi course-service
không truy cập được, API trả 502 và không ghi tiến độ. Vẫn cần chạy ca này qua gateway thật.

## ENROLL-08 — GET /api/progress?courseId={id}

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

## ENROLL-09 — Retry Kafka và DLT

Chạy trên môi trường thử riêng; khôi phục MySQL và cấu hình retry sau khi kiểm tra.

Có [bộ kiểm thử Docker tự động](enrollment-resilience.md) cho cả năm ca, chạy trong workflow
`Enrollment resilience`. Ca 1 dùng MySQL riêng của enrollment để course-service vẫn xuất bản
được trong lúc database enrollment dừng. Kết quả của bộ này tách khỏi collection HTTP.
Lượt nghiệm thu 08/10: [5/5 ca đạt trên MySQL/Kafka thật](ket-qua/enrollment-resilience.md).

| # | Tình huống | Thao tác | Mong đợi |
|---|---|---|---|
| 1 | Mất MySQL tạm thời | Tắt MySQL, xuất bản khóa, bật MySQL sau 40 giây | Snapshot cuối cùng cập nhật sau khi database hoạt động; không mất message |
| 2 | Tiêu đề vượt độ dài | Kafka UI gửi course.updated hợp lệ trừ title dài 300 ký tự; gửi khóa hợp lệ ngay sau trên cùng partition | Message lỗi giữ trong elearning.course.events.DLT với key/payload/header nguồn; khóa sau vẫn đồng bộ |
| 3 | JSON hỏng | Gửi not-json rồi sự kiện khóa hợp lệ | JSON hỏng vào DLT; consumer vẫn xử lý khóa sau |
| 4 | Hết ngân sách retry | Giảm max-elapsed-time trong môi trường test, giữ database lỗi quá ngân sách | Message được retry rồi chuyển DLT; khi database phục hồi, sự kiện sau xử lý được |
| 5 | Gửi DLT thất bại | Làm DLT không ghi được, gửi message hỏng; khôi phục quyền/kết nối DLT | Không commit bỏ qua offset lỗi trước khi lưu được DLT; sau khôi phục lưu được payload gốc và xử lý tiếp |

## ENROLL-10 — GET /api/certificates/verify/{code}

Sau ENROLL-07.10/ENROLL-06.1, lưu certificateCode. Mọi ca dưới đi qua gateway.

| # | Tình huống | Tài khoản | Mong đợi |
|---|---|---|---|
| 1 | Mã được cấp sau khi hoàn thành | — | 200; đúng bốn trường learnerName, courseTitle, issuedAt, certificateCode; không email/ID/fileUrl; Cache-Control no-store |
| 2 | Người khác xác minh cùng mã | B | 200; cùng thông tin công khai với ca 1 |
| 3 | Mã không tồn tại | — | 404 |
| 4 | Mã sai định dạng | — | 404 |
| 5 | Query thêm learnerName=Forged và userId của B | — | 200; dữ liệu không đổi |
| 6 | POST vào URL xác minh, không token | — | 401; chỉ GET được công khai |
| 7 | GET /api/certificates không token | — | 401; không mở danh sách |
| 8 | Xóa lượt S rồi xác minh mã cũ | — | 404 |
| 9 | A đổi tên khóa, chờ snapshot đổi; xác minh và đọc chứng chỉ riêng | — và S | 200; tên trên chứng chỉ vẫn là tên đã lưu lúc cấp |

Hồi quy migration (test tích hợp và kiểm thủ công trên database có chứng chỉ cũ):
chứng chỉ thiếu tên sau V2 trả 422 khi xác minh; B/admin không được backfill hộ; S mở chứng chỉ
riêng thì tên được bổ sung từ JWT của S và snapshot, mã cũ/ngày cấp giữ nguyên.
Không tạo dữ liệu migration giả bằng endpoint nhận tên từ người xác minh.

Web: mở link ở cửa sổ chưa đăng nhập thấy Hợp lệ; mã lạ hiển thị EmptyState; gateway lỗi hiển thị
ErrorAlert, không nhầm với mã không tồn tại. Kiểm 375/768/1366px và liên kết trên bản in chứng chỉ.


## ENROLL-11 — GET /api/courses/{courseId}/learners

Route gateway `enrollment-course-learners`, order -10, GET → enrollment-service. Service tự kiểm
JWT vì gateway public `GET:/api/courses/**`. A là INSTRUCTOR sở hữu snapshot, B là giảng viên khác
đã ghi danh, S là STUDENT. ADMIN được đọc; chỉ có userId trùng chủ khóa nhưng đã mất vai trò
INSTRUCTOR vẫn bị từ chối. Khóa ARCHIVED còn snapshot thì chủ khóa vẫn xem được.

Response `ApiResponse<PageResponse<...>>`: `enrollmentId`, `userId` (để web hiển thị tên dự phòng),
`learnerName`, `status`, `progressPercent`, `enrolledAt`, `lastAccessedAt`, `completedAt`,
`certificateCode` nullable. Không email, token hoặc thông tin tài khoản khác; `Cache-Control: no-store`.
Mặc định page=0, size=10, sort=enrolledAt,desc; size 1–100; chỉ sort enrolledAt/progressPercent,
có thể gửi nhiều tham số sort. ID giảm dần là thứ tự phụ khi giá trị bằng nhau.
Offset page × size vượt giới hạn số nguyên của JPA trả 400 thay vì lỗi 500.

| # | Tình huống | Tài khoản | Mong đợi |
|---|---|---|---|
| 1 | S/B ghi danh, khóa khác cũng có lượt | A | 200; chỉ S/B của khóa này; đúng tên token, tiến độ 0, chưa có chứng chỉ; đúng 9 trường |
| 2 | Không token hoặc token sai | — | 401 ở enrollment-service, dù gateway cho GET đi qua |
| 3 | B đã ghi danh, giả instructorId=A | B | 403 |
| 4 | S giả userId=A; A mất vai trò INSTRUCTOR | S/A | 403 |
| 5 | ADMIN không phải chủ khóa | ADMIN | 200 |
| 6 | Khóa chưa có snapshot | A | 404 |
| 7 | courseId=0/-1/abc | A | 400 |
| 8 | size=1, trang 0/1; trang vượt cuối; giá trị sort bằng nhau | A | 200; không trùng lượt giữa trang, total đúng; trang vượt cuối rỗng, total vẫn giữ |
| 9 | page=-1/abc, size=0/101/abc, status lạ, sort ngoài whitelist hoặc nhiều sort có trường lạ | A | 400, không 500 |
| 10 | Khóa đã có snapshot nhưng chưa có ai; khóa ARCHIVED | A | 200; danh sách rỗng hoặc giữ danh sách hiện tại |
| 11 | Lọc ACTIVE/CANCELLED/COMPLETED | A | 200; content và total chỉ tính đúng trạng thái |
| 12 | Giả tên trong body/query; hủy rồi kích hoạt bằng token mang tên mới | S/A | Tên từ JWT của S; cùng enrollmentId, cập nhật tên khi kích hoạt; đọc bằng token A không ghi tên A vào lượt |
| 13 | S hoàn thành một trong hai bài | A | 200; S 50%, B 0% |
| 14 | S hoàn thành bài cuối | A | 200; 100%, COMPLETED, completedAt và mã đúng với chứng chỉ đã cấp |
| 15 | sort=progressPercent asc/desc và enrolledAt asc/desc | A | 200; đúng thứ tự, cùng giá trị thì id desc |
| 16 | S/B xóa lượt ghi danh | A | 200; không còn lượt hoặc chứng chỉ trong danh sách |
| 17 | Migration V3 trên database có lượt cũ | A | Cột learner_name VARCHAR(150) NULL; dữ liệu cũ giữ NULL; web hiện Học viên #userId; đọc không backfill |
| 18 | Token ghi danh thiếu tên, tên trắng hoặc dài >150 | S | 401; không ghi enrollment/outbox, cần đăng nhập lại |
| 19 | Web tại /instructor/courses/{id}, 375/768/1366px | A | Tên dài không tràn; đủ tên, trạng thái, ProgressMeter, ngày ghi danh/hoàn thành; lọc/sort về trang đầu; phân trang/back giữ query |
| 20 | Web đang tải, khóa rỗng, trang vượt cuối, API lỗi, đổi bộ lọc nhanh | A | Skeleton/EmptyState/ErrorAlert phù hợp; có làm mới; không hiển thị response cũ sai bộ lọc; Tab tới được điều khiển |

ENROLL-11 trong collection được đặt tại đúng thời điểm trước/sau hủy, học 50%, hoàn thành và xóa.
Ca token đặc biệt, dữ liệu cũ và sort bằng nhau được kiểm trong `CourseLearnerApiIntegrationTest`.
Ca migration MySQL và kiểm web phải ghi bằng chứng riêng; không tính PASS chỉ từ Newman/H2.

## ENROLL-12 — GET /api/courses/{courseId}/learners/summary

Cùng quyền theo snapshot và JWT với ENROLL-11, `Cache-Control: no-store`. Các số liệu là của
toàn khóa, không phụ thuộc page/size/status của bảng học viên. `averageProgress` tính trung bình
tiến độ của ACTIVE + COMPLETED; `completionRate` = COMPLETED / (ACTIVE + COMPLETED) × 100.
Tỉ lệ từng bài = số lượt chưa hủy đã hoàn thành bài / (ACTIVE + COMPLETED) × 100.
Mọi tỉ lệ và trung bình làm tròn HALF_UP đến hai chữ số thập phân; mẫu số 0 trả 0.
`certificatesIssued` đếm chứng chỉ thực tế còn lưu, không suy ra từ số lượt COMPLETED.

API `lessons` chỉ có bài đã có tiến độ thuộc lượt chưa hủy, kể cả IN_PROGRESS với completedCount=0.
Snapshot không chứa danh sách ID bài; web ghép với đề cương hiện tại, bổ sung bài chưa ai học = 0,
ẩn bài đã xóa và sắp xếp theo vị trí chương/bài. Không gọi course-service để tổng hợp số liệu.

| # | Tình huống | Tài khoản | Mong đợi |
|---|---|---|---|
| 1 | Khóa đã đồng bộ nhưng chưa ai ghi danh | A | 200; toàn bộ số liệu 0, lessons=[] |
| 2 | Chỉ D ghi danh, xong bài 1 rồi hủy | A | cancelled=1; các số liệu khác 0, lessons=[]; không NaN/chia cho 0 |
| 3 | Khóa 2 bài: S xong cả hai; B xong bài 1; C chưa hoàn thành bài; D hủy | A | active=2, completed=1, cancelled=1, averageProgress=50, completionRate=33.33, certificatesIssued=1; bài 1 count=2/rate=66.67, bài 2 count=1/rate=33.33 |
| 4 | Query status=CANCELLED&page=99&size=1 | A | Không thay đổi số liệu toàn khóa |
| 5 | Admin không phải chủ khóa | ADMIN | 200, cùng số liệu với A |
| 6 | Không token hoặc token sai | — | 401 do service kiểm JWT, dù đường dẫn GET ở gateway public |
| 7 | B đã ghi danh, thêm instructorId=A | B | 403 |
| 8 | S thêm userId=A; A mất vai trò giảng viên | S/A | 403; không dùng danh tính từ query |
| 9 | Khóa không có snapshot | A | 404 |
| 10 | courseId=0/-1/abc | A | 400 |
| 11 | GET chi tiết khóa/đề cương; POST summary; GET summary/unknown | A/— | GET chi tiết/đề cương vẫn tới course-service; route enrollment chỉ nhận hai đường dẫn GET chính xác |
| 12 | IN_PROGRESS, lượt khóa khác, tiến độ của D trước khi hủy | A | Không cộng nhầm vào số hoàn thành; loại lượt hủy khỏi cả trung bình và mẫu số |
| 13 | 30 học viên, có tiến độ | A | Một query snapshot + ba query tổng hợp; không materialize Enrollment/LessonProgress/Certificate |
| 14 | Khóa ARCHIVED; snapshot đổi chủ | A/B | ARCHIVED vẫn xem được; chủ cũ 403, chủ mới INSTRUCTOR 200 |
| 15 | Web với đề cương đảo thứ tự, bài mới và bài đã xóa | A | Thứ tự theo đề cương, bài mới 0%, không hiện bài đã xóa; không sửa mảng props |
| 16 | Tỉ lệ bài sau giảm ≥20 điểm phần trăm | A | Nền cảnh báo + icon + chữ nêu mức giảm; đúng tại ngưỡng 20, không đánh dấu 19.99, tăng hoặc tất cả 0 |
| 17 | Web lọc/phân trang/làm mới; API summary lỗi hoặc chậm | A | Thống kê không đổi theo lọc/trang; làm mới tải cả hai; Skeleton/ErrorAlert riêng, bảng học viên không bị ẩn do summary lỗi |
| 18 | Web không có bài hoặc lỗi tải đề cương | A | EmptyState khi đề cương rỗng; lỗi đề cương có hướng dẫn tải lại, vẫn hiện số liệu toàn khóa |
| 19 | Web 375/768/1366px, tên bài dài, Tab | A | Không tràn ngang; hiển thị đúng 66,67%/33,33%; điều khiển dùng được bằng bàn phím |

Collection có thư mục 7 tạo khóa riêng và kiểm ca 1–11 (thêm IN_PROGRESS cho C nhưng tiến độ vẫn 0).
`CourseLearnerSummaryIntegrationTest` bổ sung các ca quyền/token đặc biệt, làm tròn và truy vấn tổng hợp.
`node --test scripts/check-enrollment-summary.test.mjs` kiểm quy tắc ghép/đánh dấu đề cương.
CI Full stack in Docker chạy collection sau smoke test, tạm tắt rate limit trên stack CI dùng một lần.
Kiểm tra bằng trình duyệt và các ca outage ENROLL-09 cần bằng chứng riêng, không suy ra từ unit test.

## Truy vết nguồn

- [EnrollmentController](../../enrollment-service/src/main/java/com/hunre/enrollmentservice/controller/EnrollmentController.java),
  [ProgressController](../../enrollment-service/src/main/java/com/hunre/enrollmentservice/controller/ProgressController.java).
- [CourseLearnerController](../../enrollment-service/src/main/java/com/hunre/enrollmentservice/controller/CourseLearnerController.java),
  [CourseLearnerService](../../enrollment-service/src/main/java/com/hunre/enrollmentservice/service/CourseLearnerService.java).
- [EnrollmentServiceImpl](../../enrollment-service/src/main/java/com/hunre/enrollmentservice/service/impl/EnrollmentServiceImpl.java),
  [ProgressServiceImpl](../../enrollment-service/src/main/java/com/hunre/enrollmentservice/service/impl/ProgressServiceImpl.java).
- [DTO](../../enrollment-service/src/main/java/com/hunre/enrollmentservice/dto/request),
  [phân công consumer và route](../phan-cong.md).
