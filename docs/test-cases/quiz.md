# Tình huống test quiz-service

Dùng [gateway.md](gateway.md) cho tài khoản, token, fixture và biên bản.
Mọi request qua `{{baseUrl}}`. Collection: `docs/postman/quiz.postman_collection.json`.
Biên bản thực chạy và giới hạn môi trường: [ket-qua/quiz.md](ket-qua/quiz.md).

**Đối chiếu ngày 07/10/2026 với main `e17520b`:** #42 chuẩn hóa route, #45 kiểm
quyền chủ bài/ẩn bài nháp, #49 kiểm ghi danh. Những kỳ vọng cập nhật dưới đây dựa
trên các thay đổi đã merge, không dựa vào việc muốn làm xanh bộ kiểm thử.
Các API quản lý quiz/câu hỏi chỉ cho chủ bài hoặc ADM; S và B thao tác bài A đều 403 (#45).
Bắt đầu/tiếp tục lượt làm cần ghi danh ACTIVE hoặc COMPLETED đúng khóa; chủ bài/ADM được miễn (#49).
Làm bài và đọc kết quả kiểm danh tính người làm, không bắt buộc chỉ ROLE_STUDENT.

## Đường dẫn đích

| Hiện tại | Đích dùng trong bảng |
|---|---|
| GET /api/quizzes/course/{id} | GET /api/quizzes?courseId={id} |
| PATCH /api/quizzes/{id}/publish và /archive | PATCH /api/quizzes/{id}/status + body status |
| GET /api/quizzes/{id}/attempts/history | GET /api/quizzes/{id}/attempts |
| GET /api/quizzes/attempts/{attemptId} | GET /api/attempts/{attemptId} |
| POST /api/quizzes/attempts/{attemptId}/submit | POST /api/attempts/{attemptId}/submit |

Các route đích đã có ở #42, gồm POST /api/attempts/{attemptId}/submit và route gateway /api/attempts/**.

Danh sách quiz theo course và lịch sử attempt hiện trả List, chưa Pageable (cần chuẩn hóa C1).
Các ca sort của hai endpoint được đánh dấu CẦN CHỐT; không giả định sort đang được xử lý.
Các endpoint khác không nhận Pageable/sort.

## Body mẫu và dữ liệu

**QUIZ-CREATE**:

```json
{"courseId":{{publishedCourseId}},"title":"Quiz QA {{runId}}","timeLimitMinutes":10,"passScore":70,"maxAttempts":1,"shuffleQuestions":false}
```

**QUIZ-UPDATE**: cùng các trường của QUIZ-CREATE, bỏ courseId.
**QUIZ-QUESTION** (dùng cả tạo/sửa):

```json
{"content":"1 + 1 = ?","type":"SINGLE_CHOICE","score":1,"position":1,"explanation":"1 + 1 = 2","options":[{"content":"2","isCorrect":true,"position":1},{"content":"3","isCorrect":false,"position":2}]}
```

**QUIZ-SUBMIT-50** cho quiz đã xuất bản có đúng hai câu, mỗi câu score=1:

```json
{"answers":[{"questionId":{{questionId}},"selectedOptionIds":[{{optionCorrectId}}]},{"questionId":{{question2Id}},"selectedOptionIds":[{{option2WrongId}}]}]}
```

Dùng QUIZ-QUESTION để tạo câu 1; câu 2 đổi content/position, giữ một phương án đúng và một
phương án sai. Tất cả option ID phải lấy từ response của **đúng quiz**, không đoán ID.
Lưu ID quiz/câu/option khi tạo. Cho nhóm quản lý dùng quizId; cho nhóm làm bài dùng
publishedQuizId và bộ questionId/optionId của quiz đó. PUT question thay phương án nên phải
lấy lại ID option, không tái dùng ID trước khi sửa.

Mỗi ca nộp bài phải có attempt IN_PROGRESS mới và còn hạn; mỗi ca giới hạn số lượt có quiz
riêng maxAttempts=1. Sau một ca nộp thành công, phải dựng lại trước ca nộp thành công khác.
Quiz kiểm timeout đặt timeLimitMinutes=1, đợi **hơn 90 giây** từ startedAt (có 30 giây ân hạn).

## QUIZ-01 — POST /api/quizzes

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Tạo hợp lệ | A | POST /api/quizzes + QUIZ-CREATE | 201; status=DRAFT, createdBy=instructorAId |
| 2 | Không token | — | Cùng URL/body | 401 |
| 3 | Học viên tạo | S | Cùng URL/body | 403 |
| 4 | Thiếu courseId | A | QUIZ-CREATE bỏ courseId | 400 |
| 5 | Sai kiểu courseId | A | Body courseId="abc" | 400 |
| 6 | Điểm đạt quá 100 | A | Body passScore=101 | 400 |
| 7 | Giả tác giả | A | Body thêm createdBy={{instructorBId}} | 201; createdBy vẫn instructorAId |
| 8 | Admin tạo | ADM | Body hợp lệ | 201; createdBy=adminId |
| 9 | Course không tồn tại | A | Body courseId={{missingId}} | 404; kiểm course đã có ở #45 |

## QUIZ-02 — PUT /api/quizzes/{id}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Sửa quiz DRAFT | A | PUT /api/quizzes/{{quizId}} + QUIZ-UPDATE | 200; nội dung mới đúng |
| 2 | Không token | — | Cùng URL/body | 401 |
| 3 | Sai vai trò | S | Cùng URL/body | 403 |
| 4 | Quiz không tồn tại | A | PUT /api/quizzes/{{missingId}} + body hợp lệ | 404 |
| 5 | Sai ID | A | PUT /api/quizzes/abc + body hợp lệ | 400 |
| 6 | Đã ARCHIVED | A | PUT quiz lưu trữ riêng + body hợp lệ | 422 |
| 7 | Title trống | A | QUIZ-UPDATE title="" | 400 |
| 8 | Admin sửa | ADM | PUT quiz của A + body hợp lệ | 200 |

## QUIZ-03 — PATCH /api/quizzes/{id}/status

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Xuất bản có câu hỏi | A | PATCH /api/quizzes/{{quizId}}/status, {"status":"PUBLISHED"} | 200; status=PUBLISHED |
| 2 | Không token | — | Cùng URL/body | 401 |
| 3 | Sai vai trò | S | Cùng URL/body | 403 |
| 4 | Không tồn tại | A | PATCH /api/quizzes/{{missingId}}/status + {"status":"PUBLISHED"} | 404 |
| 5 | Sai ID | A | PATCH /api/quizzes/abc/status + {"status":"PUBLISHED"} | 400 |
| 6 | Quiz chưa có câu hỏi | A | Xuất bản quiz DRAFT rỗng riêng | 422 |
| 7 | Lưu trữ | A | PATCH quiz riêng + {"status":"ARCHIVED"} | 200; status=ARCHIVED |
| 8 | Sai/thiếu enum | A | Lần lượt {"status":"UNKNOWN"}, {} | 400 |
| 9 | Admin xuất bản | ADM | Xuất bản quiz có câu hỏi | 200 |

## QUIZ-04 — GET /api/quizzes/{id}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Giảng viên xem đáp án | A | GET /api/quizzes/{{quizId}} | 200; có explanation và isCorrect của phương án |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Học viên xem đáp án | S | Cùng URL | 403 |
| 4 | Không tồn tại | A | GET /api/quizzes/{{missingId}} | 404 |
| 5 | Sai ID | A | GET /api/quizzes/abc | 400 |
| 6 | Admin xem | ADM | GET /api/quizzes/{{quizId}} | 200 |
| 7 | Giảng viên B xem bài A | B | GET quiz của A | 403 theo quyền chủ bài (#45) |

## QUIZ-05 — GET /api/quizzes/{id}/take

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Đề hợp lệ | S | GET /api/quizzes/{{publishedQuizId}}/take | 200; có câu hỏi/phương án, không explanation, không isCorrect |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Giảng viên lấy đề | B | Cùng URL | 200; cũng không lộ đáp án |
| 4 | Không tồn tại | S | GET /api/quizzes/{{missingId}}/take | 404 |
| 5 | Sai ID | S | GET /api/quizzes/abc/take | 400 |
| 6 | Đề chưa xuất bản | S | GET /api/quizzes/{{quizId}}/take khi DRAFT | 422 |
| 7 | Đề đã lưu trữ | S | GET quiz ARCHIVED/take | 422 |
| 8 | Xáo trộn | S | Với shuffleQuestions=true, gọi nhiều lần | 200; cùng tập ID/câu hỏi, vẫn không đáp án; không bắt buộc mỗi lần thứ tự phải khác |

## QUIZ-06 — GET /api/quizzes?courseId={id}

Theo #45: học viên/B chỉ thấy bài PUBLISHED của A; A thấy cả bài nháp của mình. Mảng không chứa câu hỏi/đáp án.

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Danh sách theo khóa | S | GET /api/quizzes?courseId={{publishedCourseId}} | 200; mọi quiz có courseId đúng |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Giảng viên xem | A | Cùng URL | 200 |
| 4 | Khóa không có quiz | S | GET /api/quizzes?courseId={{missingId}} | 200; danh sách rỗng, không 404 |
| 5 | Sai kiểu bộ lọc | S | GET /api/quizzes?courseId=abc | 400 |
| 6 | Thiếu courseId bắt buộc | S | GET /api/quizzes | 400 theo chuyển đổi từ path bắt buộc sang query |
| 7 | Không trả đáp án | S | GET /api/quizzes?courseId={{publishedCourseId}} | 200; không có options/isCorrect/explanation |
| 8 | Sort bịa sau C1 [CẦN CHỐT] | S | GET /api/quizzes?courseId={{publishedCourseId}}&sort=abcxyz | 400 sau khi có Pageable; hiện List bỏ qua sort nên BLOCKED |

## QUIZ-07 — DELETE /api/quizzes/{id}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Xóa quiz riêng | A | DELETE /api/quizzes/{{quizId}} | 200; A GET lại 404 |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Sai vai trò | S | Cùng URL | 403 |
| 4 | Không tồn tại | A | DELETE /api/quizzes/{{missingId}} | 404 |
| 5 | Sai ID | A | DELETE /api/quizzes/abc | 400 |
| 6 | Admin xóa | ADM | DELETE quiz riêng | 200 |
| 7 | Xóa lại | A | DELETE ID đã xóa | 404 |

## QUIZ-08 — POST /api/quizzes/{quizId}/questions

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Thêm câu hỏi | A | POST /api/quizzes/{{quizId}}/questions + QUIZ-QUESTION | 201; hai options, đúng một isCorrect=true; lưu các ID |
| 2 | Không token | — | Cùng URL/body | 401 |
| 3 | Sai vai trò | S | Cùng URL/body | 403 |
| 4 | Quiz không tồn tại | A | POST /api/quizzes/{{missingId}}/questions + body hợp lệ | 404 |
| 5 | Sai ID | A | POST /api/quizzes/abc/questions + body hợp lệ | 400 |
| 6 | Chỉ một phương án | A | QUIZ-QUESTION giữ một option | 400 |
| 7 | SINGLE_CHOICE có hai đáp án đúng | A | Đặt cả hai isCorrect=true | 400 |
| 8 | MULTIPLE_CHOICE không đáp án đúng | A | Đặt type="MULTIPLE_CHOICE", tất cả isCorrect=false | 400 |
| 9 | Đúng nhiều lựa chọn | A | type="MULTIPLE_CHOICE", có ít nhất một đáp án đúng | 201 |

## QUIZ-09 — PUT /api/quizzes/{quizId}/questions/{questionId}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Sửa câu hỏi | A | PUT /api/quizzes/{{quizId}}/questions/{{questionId}} + QUIZ-QUESTION | 200; cập nhật nội dung/options; lưu ID options mới |
| 2 | Không token | — | Cùng URL/body | 401 |
| 3 | Sai vai trò | S | Cùng URL/body | 403 |
| 4 | Câu không tồn tại | A | PUT /api/quizzes/{{quizId}}/questions/{{missingId}} + body hợp lệ | 404 |
| 5 | Sai ID | A | PUT /api/quizzes/{{quizId}}/questions/abc + body hợp lệ | 400 |
| 6 | Sai quiz cha | A | PUT câu questionId nhưng quizId là quiz khác có thật | 400; câu hỏi không thuộc bài kiểm tra |
| 7 | Score bằng 0 | A | Body score=0 | 400 |
| 8 | Sai loại câu | A | Body type="UNKNOWN" | 400 |

## QUIZ-10 — DELETE /api/quizzes/{quizId}/questions/{questionId}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Xóa câu riêng | A | DELETE /api/quizzes/{{quizId}}/questions/{{questionId}} | 200; GET questions không còn câu |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Sai vai trò | S | Cùng URL | 403 |
| 4 | Câu không tồn tại | A | DELETE /api/quizzes/{{quizId}}/questions/{{missingId}} | 404 |
| 5 | Sai ID | A | DELETE /api/quizzes/{{quizId}}/questions/abc | 400 |
| 6 | Sai quiz cha | A | DELETE questionId trong quizId khác có thật | 400; câu không bị xóa |
| 7 | Admin xóa | ADM | DELETE câu test riêng | 200 |

## QUIZ-11 — GET /api/quizzes/{quizId}/questions

Danh sách câu hỏi không Pageable. #45 kiểm quiz cha và quyền chủ bài trước khi trả danh sách.

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Giảng viên xem | A | GET /api/quizzes/{{quizId}}/questions | 200; theo position; có isCorrect/explanation |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Sai vai trò | S | Cùng URL | 403 |
| 4 | Quiz không tồn tại | A | GET /api/quizzes/{{missingId}}/questions | 404; quiz cha không tồn tại (#45) |
| 5 | Sai ID | A | GET /api/quizzes/abc/questions | 400 |
| 6 | Admin xem | ADM | GET /api/quizzes/{{quizId}}/questions | 200 |
| 7 | Quiz rỗng có thật | A | GET questions của quiz mới chưa thêm câu | 200; data=[] |

## QUIZ-12 — POST /api/quizzes/{quizId}/attempts

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Bắt đầu | S | POST /api/quizzes/{{publishedQuizId}}/attempts, không body | 201; userId=studentId, status=IN_PROGRESS, attemptNo=1; lưu attemptId |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Người khác làm bài riêng | B | Cùng URL | 201; userId=instructorBId; attemptBId khác attemptId |
| 4 | Quiz không tồn tại | S | POST /api/quizzes/{{missingId}}/attempts | 404 |
| 5 | Sai ID | S | POST /api/quizzes/abc/attempts | 400 |
| 6 | Quiz DRAFT | S | POST /api/quizzes/{{quizId}}/attempts khi DRAFT | 422 |
| 7 | Tiếp tục đang làm | S | POST lại khi attempt IN_PROGRESS còn giờ | 201 theo controller hiện tại; cùng attemptId, không tạo bản ghi thứ hai |
| 8 | Hết số lần | S | Nộp bài maxAttempts=1 rồi POST lại | 422 |
| 9 | Giả userId | S | POST URL thêm ?userId={{instructorBId}} | 201; vẫn thuộc S |
| 10 | Không giới hạn lượt | S | Quiz riêng maxAttempts=0; nộp và bắt đầu lượt mới | 201; attemptNo tăng |

## QUIZ-13 — POST /api/attempts/{attemptId}/submit

Ca Kafka dùng quy trình khôi phục trong gateway.md; không có Kafka thì vẫn phải lưu bài và outbox. #42 đã sửa giữ nguyên payload điểm; vẫn cần kiểm thực tế khôi phục Kafka để nghiệm thu cả ca.

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Nộp đúng một trong hai câu | S | POST /api/attempts/{{attemptId}}/submit + QUIZ-SUBMIT-50 | 200; score=50, passScore=70, passed=false; attemptId đúng |
| 2 | Không token | — | Cùng URL/body | 401 |
| 3 | Nộp bài của S | B | Cùng URL/body | 404; không tiết lộ attempt thuộc người khác |
| 4 | Attempt không tồn tại | S | POST /api/attempts/{{missingId}}/submit + body hợp lệ | 404 |
| 5 | Sai ID | S | POST /api/attempts/abc/submit + body hợp lệ | 400 |
| 6 | Nộp lần hai | S | Nộp lại attempt đã SUBMITTED | 422 |
| 7 | Quá giờ | S | Quiz timeLimitMinutes=1; đợi 100 giây rồi nộp | 422; không chấm bài; lưu EXPIRED, điểm 0; lịch sử vẫn EXPIRED ở request sau, không tạo quiz.graded |
| 8 | Answers null | S | Body {"answers":null} | 400 |
| 9 | Bỏ trống tất cả đáp án | S | Body {"answers":[]} | 200; score=0; không nhầm với answers=null |
| 10 | Đúng cả hai câu | S | Chọn optionCorrectId và option2CorrectId trên attempt mới | 200; score=100, passed=true |
| 11 | Kafka ngừng | S | Trong môi trường riêng, stop Kafka, nộp attempt hợp lệ mới rồi start lại Kafka | Nộp 200; kết quả vẫn đọc 200; sau Kafka hồi phục, hộp thư có đúng một thông báo |
| 12 | Giả người nộp | S | QUIZ-SUBMIT-50 thêm userId=instructorBId | 200; kết quả vẫn userId=studentId |

## QUIZ-14 — GET /api/attempts/{attemptId}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Kết quả đã nộp | S | GET /api/attempts/{{attemptId}} sau QUIZ-SUBMIT-50 | 200; score=50; có kết quả từng câu và đáp án |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Người khác đọc kết quả S | B | Cùng URL | 404 |
| 4 | Không tồn tại | S | GET /api/attempts/{{missingId}} | 404 |
| 5 | Sai ID | S | GET /api/attempts/abc | 400 |
| 6 | Admin đọc hộ | ADM | GET kết quả của S | 404; API hiện chỉ dành cho người làm |
| 7 | Giả userId | B | GET /api/attempts/{{attemptId}}?userId={{studentId}} | 404 |
| 8 | Không lộ đáp án trước khi nộp | S | GET attempt IN_PROGRESS vừa tạo; thử lại với lượt EXPIRED | 422; không có data, correctOptionIds hoặc questionResults. Quy tắc đã chốt trong review #59: chỉ SUBMITTED mới được xem kết quả |

## QUIZ-15 — GET /api/quizzes/{quizId}/attempts

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Lịch sử chính mình | S | GET /api/quizzes/{{publishedQuizId}}/attempts | 200; chỉ userId=studentId, attemptNo giảm dần |
| 2 | Không token | — | Cùng URL | 401 |
| 3 | Người khác xem lịch sử riêng | B | Cùng URL | 200; chỉ attempt của B, không có attemptId của S |
| 4 | Quiz không có lịch sử | S | GET /api/quizzes/{{missingId}}/attempts | 200; data=[] theo truy vấn hiện tại |
| 5 | Sai ID | S | GET /api/quizzes/abc/attempts | 400 |
| 6 | Giả danh | B | GET /api/quizzes/{{publishedQuizId}}/attempts?userId={{studentId}} | 200; vẫn chỉ B |
| 7 | Chưa từng làm quiz có thật | ADM | GET /api/quizzes/{{publishedQuizId}}/attempts | 200; data=[] nếu ADM chưa làm |
| 8 | Sort bịa sau C1 [CẦN CHỐT] | S | GET /api/quizzes/{{publishedQuizId}}/attempts?sort=abcxyz | 400 sau khi có Pageable; hiện không hỗ trợ sort nên BLOCKED |

## QUIZ-16 — ghi danh và quyền sở hữu bổ sung

Collection thêm các ca #45/#49: B tạo bài trong khóa A, B sửa/thêm/xem/lưu trữ/xóa
bài A bị 403; học viên chưa ghi danh bị 403 và không tạo lượt; hủy ghi danh chặn
resume mà giữ nguyên lượt; ghi danh lại tiếp tục đúng lượt; chủ bài/admin làm thử.
Các request chuẩn bị mang tên riêng, không tính là ca nghiệm thu.

QUIZ-06.8 và QUIZ-15.8 vẫn BLOCKED vì List chưa hỗ trợ sort; collection không tự
coi việc bỏ qua sort là PASS. QUIZ-13.7 bật `runSlowTests=true` trong collection hoặc
Newman `--env-var runSlowTests=true`, chờ **100 giây**, với `--timeout-script 150000`.
Sau review #59, tăng khoảng chờ vì giới hạn 60 giây + ân hạn 30 giây và phép so sánh
`elapsedSeconds > 90` cắt phần lẻ; chờ 92 giây trên host có thể chưa đủ trong Docker.
Giữ nguyên kỳ vọng HTTP 422, không đổi backend hoặc coi ca bỏ qua là PASS.
QUIZ-13.11 phải chủ động dừng/bật Kafka và xác minh thông báo; mặc định bỏ qua.

## QUIZ-17 — GET /api/quizzes/{quizId}/results

Theo phân công 08/10: chủ đề A hoặc ADMIN xem kết quả toàn lớp. API lịch sử riêng
`/attempts` giữ nguyên. Đề mẫu có 2 câu bằng điểm, điểm đạt 75, tối đa 5 lượt;
S nộp 50 điểm, B nộp 100 điểm. Collection dùng đề riêng `statsQuizId`.

| # | Tình huống | Mong đợi |
|---|---|---|
| 1 | Chưa có bài nộp | 200; số người, điểm trung bình, tỉ lệ đạt đều 0; learners rỗng |
| 2 | S đang làm | Không tăng số lượt nộp hoặc số người |
| 3 | A đọc sau S và B nộp | 2 người, 2 lượt, trung bình 75, đạt 50%; câu 1 đúng 100%, câu 2 đúng 50%; có tên, không email/đáp án |
| 4 | ADMIN đọc | 200, cùng số liệu A |
| 5 | Giảng viên B đọc | 403, không data |
| 6 | S giả userId/isAdmin | 403, không data |
| 7 | Không token | 401 |
| 8 | Quiz không tồn tại | 404 |
| 9 | size=1 | 1 dòng, tổng 2 người, 2 trang; summary vẫn toàn lớp |
| 10 | page=1&size=1 | Người còn lại, không trùng trang đầu |
| 11 | sort=secret,desc | 400 |
| 12 | A và ADMIN làm thử rồi nộp | Số liệu vẫn như ca 3 |
| 13 | S nộp thêm lượt 0 điểm | 2 người, 3 lượt, trung bình vẫn 75, đạt 50%; câu 1 đúng 66.67%, câu 2 đúng 33.33% |
| 14 | page=999 | learners rỗng; summary vẫn toàn lớp |
| 15 | Đề quá giờ của QUIZ-13.7 | 1 lượt EXPIRED, không có lượt SUBMITTED, trung bình 0; cần runSlowTests=true |

Integration test bổ sung: lưu tên từ JWT, chặn giả tên/cờ làm thử, không đổi phân loại khi
resume bằng vai trò mới, lượt quiz khác không ảnh hưởng, tên trống hiện `Học viên #<id>`,
lượt cũ thiếu phân loại bị loại và có `unclassifiedAttempts`, ID sai kiểu trả 400.

Định nghĩa thống kê: trung bình lấy điểm cao nhất mỗi người, tỉ lệ đạt tính người có ít nhất
một lượt đạt. Tỉ lệ đúng từng câu dùng **tất cả lượt SUBMITTED đã chấm câu đó**, không chỉ lượt
cao nhất; câu chưa được chấm có `gradedAnswers=0`, web hiện "Chưa có dữ liệu". EXPIRED chỉ
tăng bộ đếm hết giờ. Lượt thử và lượt cũ chưa phân loại không tham gia bất cứ thống kê nào.

## QUIZ-18 — tải kết quả CSV

Chạy sau QUIZ-17. Quyền, tập học viên và cách tính điểm giống `/results`.

| # | Tình huống | Mong đợi |
|---|---|---|
| 1 | A tải, kể cả page=99&size=1 | 200, đủ 2 học viên, S có 2 lượt/50 điểm và B 1 lượt/100 điểm; không có A/admin làm thử; text/csv UTF-8, đúng tên attachment |
| 2 | Admin tải | Nội dung giống A |
| 3 | B không phải tác giả tải | 403 JSON, không có header attachment |
| 4 | S gửi userId của A, isAdmin=true | Vẫn 403, không file |
| 5 | Không token | 401, không file |
| 6 | Quiz không tồn tại | 404 RESOURCE_NOT_FOUND |
| 7 | S đổi tên =1+1, đăng nhập lại, làm/nộp bài | Ô tên là `'=1+1`, 3 lượt và điểm cao nhất vẫn 50; không công thức |
| 8 | S đổi tên `Nguyễn, "Ánh"`, đăng nhập lại, làm/nộp bài | Ô CSV là `"Nguyễn, ""Ánh"""`, tiếng Việt giữ nguyên, 4 lượt/50 điểm |
| 9 | Đề chưa có lượt nộp | File chỉ có header tiếng Việt |

Integration test bổ sung: BOM byte EF BB BF, đủ 22 học viên ngoài giới hạn 20 của web,
giờ UTC+7 chính xác, tên bắt đầu bằng + / - / @ / tab / CR / LF, phẩy/kép/xuống dòng,
fallback tên trống, không sửa dữ liệu gốc, loại preview/legacy/expired/ongoing và ID sai kiểu.
Kiểm web: bấm **Tải CSV** tải đúng tên `ket-qua-quiz-<id>.csv`, byte BOM và nội dung
giữ nguyên qua cầu nối; trang 375/768/1366px không tràn ngang. Không coi kiểm byte BOM
là đã mở file bằng Excel; ghi riêng việc đó trong biên bản nếu đã thực hiện.

## Truy vết nguồn

- [Controllers](../../quiz-service/src/main/java/com/hunre/quizservice/controller),
  [DTO](../../quiz-service/src/main/java/com/hunre/quizservice/dto).
- [QuizServiceImpl](../../quiz-service/src/main/java/com/hunre/quizservice/service/impl/QuizServiceImpl.java).
- [QuestionServiceImpl](../../quiz-service/src/main/java/com/hunre/quizservice/service/impl/QuestionServiceImpl.java).
- [QuizAttemptServiceImpl](../../quiz-service/src/main/java/com/hunre/quizservice/service/impl/QuizAttemptServiceImpl.java).
- [Phân công outbox/đường dẫn](../phan-cong.md), [quy ước B7/C1](../api-conventions.md).

## QUIZ-19 — nhập câu hỏi CSV

Chạy nhóm 9 của collection sau setup. Multipart `file` được tạo ngay trong collection,
không phụ thuộc đường dẫn trên máy người chạy. Câu nhập thành công được thêm cuối đề.

| # | Tình huống | Mong đợi |
|---|---|---|
| 1 | A tải mẫu | 200, CSV UTF-8, attachment mau-cau-hoi.csv |
| 2 | A nhập file mẫu | 201, imported=3; đủ SINGLE/MULTIPLE/TRUE_FALSE |
| 3 | Dòng 2 không có đáp án đúng, dòng 3 hợp lệ | 400, lỗi dòng 2; đề vẫn 3 câu, không lưu dòng 3 |
| 4–6 | B, học viên, khách nhập | 403, 403, 401 |
| 7 | File 2 MB | 400 VALIDATION_FAILED, lỗi toàn file dòng 1 |
| 8 | File 201 câu | 400 dòng 202; không lưu một phần |
| 9 | Admin nhập BOM, phẩy/kép/xuống dòng trong ô | 201, lưu nguyên tiếng Việt/ký tự đặc biệt |
| 10 | Lỗi sau ô nhiều dòng | 400, đúng dòng vật lý 4 |
| 11 | File dùng dấu chấm phẩy | 201 |
| 12 | Ngoặc kép chưa đóng | 400 dòng 2 |
| 13 | Ô đáp án dài 1001 ký tự | 400 dòng 2 |
| 14 | B tải mẫu đề của A | 403 |
| 15 | Đề không tồn tại | 404 |
| 16 | Xuất bản, làm thử và nộp các câu vừa nhập | Đúng 5 câu sau các lần nhập hợp lệ, không lộ isCorrect khi làm; nộp đúng đạt 100 |

Integration bổ sung: rollback khi lưu câu thứ hai lỗi; đủ 200 câu; mọi loại câu thiếu/thừa
đáp án đúng, ô trống, điểm không hợp lệ, nội dung/giải thích quá dài, UTF-8 lỗi, file rỗng,
thiếu part, header/cột sai, admin, JWT thật. Kiểm web: chọn file, tải mẫu, bảng lỗi và sửa
file rồi nhập, danh sách cập nhật, thông báo file lớn/rỗng, 375/768/1366px không tràn.

## QUIZ-20 — xáo trộn theo lượt làm

Nhóm 10 của collection: tạo đề 5 câu (3 SINGLE_CHOICE, 1 MULTIPLE_CHOICE, 1 TRUE_FALSE),
bật cả hai cài đặt, dùng S đã ghi danh trong setup. Thứ tự so theo ID câu và ID đáp án.

| # | Tình huống | Mong đợi |
|---|---|---|
| 1 | A xem đề gốc | Câu theo position 1–5, ghi lại thứ tự gốc và ID đáp án đúng |
| 2 | S chưa bắt đầu gọi take | Thứ tự gốc, không tự tạo lượt |
| 3 | S bắt đầu rồi gọi take 5 lần | Cùng thứ tự câu và đáp án; không có isCorrect/giải thích; TRUE_FALSE Đúng trước Sai |
| 4 | Làm tiếp, gửi userId/attemptId/seed giả qua query | Cùng ID lượt và cùng thứ tự của S |
| 5 | A chưa làm, trong khi S đang làm | A thấy thứ tự gốc, không mượn seed của S |
| 6 | A mở trang soạn đề sau nhiều lần take | Thứ tự gốc không đổi |
| 7 | S nộp đúng bằng ID đáp án | 100 điểm dù vị trí đã xáo |
| 8 | Xem kết quả | Câu và đáp án theo thứ tự gốc |
| 9 | A xem thống kê | Câu theo thứ tự gốc, điểm trung bình 100 |
| 10 | A xuất CSV | Có lượt nộp 100 điểm, Đạt |
| 11 | S làm lượt mới, tải 2 lần | ID mới, thứ tự mới trên fixture 5 câu này và ổn định giữa 2 lần |
| 12 | A tắt cả hai cài đặt | take trả thứ tự gốc |

Integration bổ sung: 4 tổ hợp bật/tắt độc lập; mặc định false; round-trip tạo/sửa; B không
đổi được cài đặt; khách 401; đề DRAFT 422; lượt đã nộp/hết hạn không làm seed; position
trùng vẫn ổn định nhờ id; take không thay dữ liệu soạn đề/thống kê/CSV.
Web: bật/lưu hai cài đặt, bắt đầu phải lấy đề sau khi tạo lượt, tải lại rồi làm tiếp không
đổi thứ tự, nộp đúng đạt 100%, kết quả thứ tự gốc; thử 375/768/1366px.
