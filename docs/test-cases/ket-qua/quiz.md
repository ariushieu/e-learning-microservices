# Biên bản kiểm thử quiz — 07/10/2026

## Phiên bản và môi trường

- Người phụ trách: hiepdeptrai0111. Code/collection đã chạy: **`b2aceaf`**, dựa trên main `e17520b`.
- Newman **6.2.2** chạy collection Postman v2.1 qua **http://localhost:8080**.
  Không dùng giao diện Postman Desktop; đây là kết quả chạy script bằng Newman.
- Auth, course, enrollment, quiz, gateway là các ứng dụng Java thật; đăng nhập qua
  auth-service để lấy JWT, giữ xác thực ở gateway và service. H2 tạm trong bộ nhớ,
  schema Hibernate, Flyway tắt **chỉ trong tiến trình thử**. Không đổi cấu hình repo.
- Máy không có Docker; lượt này không dùng MySQL/Kafka/Redis. Rate limit, Kafka listener/outbox publisher
  tắt trong tiến trình thử; snapshot hai khóa QA được nạp trước vào H2 thay phần đồng
  bộ Kafka. Vì vậy **chưa nghiệm thu full Docker stack, migration MySQL hoặc Kafka**.
- Collection tạo khóa/câu/đề/ghi danh bằng API. Tài khoản QA đã tồn tại ở lượt chạy
  cuối: đăng ký trả 409, sau đó đăng nhập và cấp vai trò thành công. Không coi 409 là
  đăng ký mới thành công. Dữ liệu H2 mất khi dừng các tiến trình thử.
- Frontend chạy **bản production** Next.js 16.3.8, nối gateway thật. Đã đọc /design
  và dùng Callout/ErrorAlert/Button của khung chung.

## Kết quả

Các số liệu dưới đây là lượt chạy H2 ban đầu tại `b2aceaf`; kết quả review Docker
và sửa collection được ghi riêng ở mục tiếp theo, không gộp hai lượt chạy.

- Collection có **199 request**; chạy **192**, **191 request đạt toàn bộ kiểm tra**,
  **1 request FAIL**. Tổng **417 assertions: 416 PASS, 1 FAIL**; không lỗi script hoặc
  lỗi gửi HTTP. Thời gian 1 phút 55 giây (lượt H2 cũ tại `b2aceaf` đợi 92 giây;
  collection sau review #59 đã đổi thành 100 giây, không suy diễn thành kết quả chạy mới).
- 7 request thư mục Kafka bỏ qua có chủ đích (`runKafkaRecovery=false`).
  QUIZ-13.7 đã chạy với `runSlowTests=true`, không tính skip là PASS.
- **663 test Maven: 0 failure/error/skipped**. Frontend route typegen, TypeScript,
  ESLint và production build đều đạt. Kiểm cấu hình bảo mật, migration và diff: đạt.

### Review Docker của #59 và sửa ca chậm (07/10/2026)

**Nguồn: phản hồi review do người dùng cung cấp**, chạy bản gộp `main` + #59 với
MySQL/Kafka/Redis trong Docker, gateway 8080 và frontend production riêng. Không
phải lượt chạy Docker của tác giả bản sửa collection này; chưa có SHA bản gộp
hoặc báo cáo máy đọc được để lưu kèm.

- Reviewer báo **16/16 ca giao diện PASS**, gồm ghi danh/hủy/ghi danh lại,
  lỗi dịch vụ và thử lại, quyền làm thử của chủ bài/admin, nộp bài và 375 px.
- Collection mặc định: **188 request, 406/407 assertions PASS**; chỉ QUIZ-14.8 FAIL.
  Ca chậm mặc định bị bỏ qua nên số này không chứng minh QUIZ-13.7 PASS.
- QUIZ-13.7 với khoảng chờ cũ: **200, FAIL giả của collection**. Reviewer ghi DB
  `started_at 07:02:07.60` → `submitted_at 07:03:38.55` (khoảng 90,9 giây).
  Backend cắt phần lẻ rồi so sánh `elapsedSeconds > 90`, nên chưa quá giờ.
- Reviewer đổi `setTimeout` thành **100000 ms**, chạy lại trên Docker nhận **422**.
  Dữ liệu thử đã được reviewer xóa.

Bản sửa sau review tăng khoảng chờ lên **100 giây**, hướng dẫn Newman dùng
`--timeout-script 150000`, giữ kỳ vọng 422. `runSlowTests` đọc qua `pm.variables.get`
để `--env-var runSlowTests=true` có tác dụng, vẫn mặc định bỏ qua ca chậm.

Kiểm tra cục bộ sau sửa collection:

- Newman 6.2.2 chạy script trích nguyên từ thư mục ca chậm với HTTP server giả lập:
  mặc định false bỏ qua; collection true chạy; environment false ghi đè collection
  true và bỏ qua; environment true ghi đè collection false và chạy. **4/4 đạt**.
- Lượt cuối giữ nguyên timer, mất **100242 ms**, không lỗi script với timeout
  150000 ms. Server giả lập trả 422 chỉ để kiểm tra luồng script, **không phải bằng
  chứng backend từ chối quá giờ**. Ba lượt kiểm cờ đầu bỏ timer để chạy nhanh.
- JSON đọc được; **203 script Postman** biên dịch cú pháp được.
- `./mvnw -B -ntp clean verify`: **BUILD SUCCESS**, kết thúc 14:33:27 +07:00;
  **663 test, 0 failure/error/skipped**. Kiểm bảo mật, migration, tiêu đề commit
  và diff đạt. Không chạy lại frontend vì lần sửa này chỉ đổi collection/tài liệu.

**Chạy lại Docker tại máy sửa: BLOCKED** — không có Docker CLI/Desktop; WSL báo
chưa được cài. Kết quả Docker 422 ở trên là bằng chứng do reviewer cung cấp, không
được ghi thành lượt nghiệm thu Docker mới của commit này.

### Lỗi còn mở — QUIZ-14.8

**FAIL, cần sửa riêng trước khi nghiệm thu bảo mật.** Với lượt IN_PROGRESS thuộc
chính người gọi, `GET /api/attempts/{id}` trả **200** và có đáp án đúng trước khi
nộp; kỳ vọng **422**. Kiểm chứng thêm trên lượt của B: `submittedAt=null`,
`correctOptionIds=[[1],[3]]`. Không đổi kỳ vọng thành 200 để làm xanh collection.

Nguồn: `QuizAttemptServiceImpl.getAttemptResult` dựng đáp án mà chưa kiểm trạng
thái lượt làm. Phân công yêu cầu ca FAIL sửa ở PR khác; bộ thay đổi này chỉ cập nhật
web ghi danh và collection/biên bản, chưa sửa hành vi backend đó.

Reviewer còn xác nhận nộp quá giờ trả 422 nhưng trạng thái vẫn `IN_PROGRESS`:
`markAttemptExpired` lưu `EXPIRED` rồi ném `BusinessException` trong cùng transaction,
nên thay đổi bị rollback. Lần bắt đầu sau mới đánh dấu lại hết hạn. Cần PR backend
riêng cùng QUIZ-14.8, kiểm cả HTTP 422 lẫn trạng thái đã commit và lịch sử lượt làm;
chưa đánh dấu hai lỗi này đã được sửa trong #59.

### Ca cần môi trường hoặc hợp đồng bổ sung

| Mã ca | Commit/môi trường | HTTP thực tế | Kết quả | Lý do |
|---|---|---|---|---|
| QUIZ-06.8 | b2aceaf | Chưa chạy | BLOCKED | API trả List, chưa có hợp đồng hỗ trợ sort |
| QUIZ-15.8 | b2aceaf | Chưa chạy | BLOCKED | Lịch sử trả List, chưa có hợp đồng hỗ trợ sort |
| QUIZ-13.11 | H2, không Kafka | Chưa chạy | BLOCKED | Phải dừng/bật Kafka và xác minh đúng một thông báo sau khôi phục |
| Docker/MySQL | Máy hiện tại | Chưa chạy | BLOCKED | Không có Docker/MySQL; H2 không chứng minh schema/migration |

### Từng ca đã chạy

Các dòng dưới trích từ kết quả Newman cuối. Request chuẩn bị fixture có kiểm HTTP
và response contract nhưng không mang mã QUIZ, nên chỉ tính vào tổng request ở trên.
Các kỳ vọng đã cập nhật theo #42/#45/#49 được giải thích ở [quiz.md](../quiz.md).

| Mã ca / tình huống | Commit/môi trường | HTTP thực tế | PASS / FAIL | Bằng chứng |
|---|---|---|---|---|
| QUIZ-01.1 Tạo hợp lệ | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-01.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-01.3 Học viên tạo | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-01.4 Thiếu courseId | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-01.5 Sai kiểu courseId | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-01.6 Điểm quá 100 | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-01.7 Giả tác giả | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-01.8 Admin tạo | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-01.9 Khóa không tồn tại | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-01.10 Khóa người khác | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-02.1 Sửa nháp | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-02.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-02.3 Sai vai trò | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-02.4 Không tồn tại | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-02.5 Sai ID | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-02.6 Đã lưu trữ | b2aceaf / gateway + H2 | 422 | PASS | HTTP và assertions đạt |
| QUIZ-02.7 Title trống | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-02.8 Admin sửa | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-02.9 B sửa bài A | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-03.1 Xuất bản | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-03.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-03.3 Sai vai trò | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-03.4 Không tồn tại | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-03.5 Sai ID | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-03.6 Chưa có câu hỏi | b2aceaf / gateway + H2 | 422 | PASS | HTTP và assertions đạt |
| QUIZ-03.7 Lưu trữ | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-03.8 Enum sai | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-03.8b Thiếu enum | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-03.9 Admin xuất bản | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-03.10 B lưu trữ | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-04.1 Giảng viên xem | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-04.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-04.3 Học viên xem đáp án | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-04.4 Không tồn tại | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-04.5 Sai ID | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-04.6 Admin xem | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-04.7 B xem bài A | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-05.1 Đề hợp lệ | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-05.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-05.3 B lấy đề | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-05.4 Không tồn tại | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-05.5 Sai ID | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-05.6 Đề nháp | b2aceaf / gateway + H2 | 422 | PASS | HTTP và assertions đạt |
| QUIZ-05.7 Đã lưu trữ | b2aceaf / gateway + H2 | 422 | PASS | HTTP và assertions đạt |
| QUIZ-05.8 Xáo trộn | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-06.1 Danh sách theo khóa | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-06.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-06.3 A xem nháp của mình | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-06.4 Khóa không có quiz | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-06.5 Sai bộ lọc | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-06.6 Thiếu courseId | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-06.7 Không trả đáp án | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-07.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-07.3 Sai vai trò | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-07.8 B xóa bài A | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-07.1 Xóa quiz | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-07.4 Không tồn tại | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-07.5 Sai ID | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-07.6 Admin xóa | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-07.7 Xóa lại | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-08.1 Thêm hợp lệ | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-08.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-08.3 Sai vai trò | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-08.4 Không tồn tại | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-08.5 Sai ID | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-08.6 Chỉ một phương án | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-08.7 Hai đáp án đúng | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-08.8 Không đáp án đúng | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-08.9 Nhiều lựa chọn hợp lệ | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-08.10 B thêm vào bài A | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-09.1 Sửa câu | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-09.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-09.3 Sai vai trò | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-09.4 Không tồn tại | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-09.5 Sai ID | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-09.6 Sai quiz cha | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-09.7 Score 0 | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-09.8 Sai loại | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-11.1 A xem | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-11.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-11.3 Sai vai trò | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-11.4 Quiz không tồn tại | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-11.5 Sai ID | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-11.6 Admin xem | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-11.7 Quiz rỗng | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-11.8 B xem bài A | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-10.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-10.3 Sai vai trò | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-10.4 Không tồn tại | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-10.5 Sai ID | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-10.6 Sai quiz cha | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-10.1 Xóa câu | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-10.7 Admin xóa | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-12.1 Bắt đầu | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-12.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-12.3 B làm bài riêng | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-12.4 Không tồn tại | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-12.5 Sai ID | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-12.6 Đề nháp | b2aceaf / gateway + H2 | 422 | PASS | HTTP và assertions đạt |
| QUIZ-12.7 Tiếp tục | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-12.9 Giả userId | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-14.8 Không lộ đáp án khi đang làm | b2aceaf / gateway + H2 | 200 | FAIL | Kỳ vọng 422; xem lỗi lộ đáp án ở trên |
| QUIZ-13.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-13.3 B nộp bài S | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-13.4 Không tồn tại | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-13.5 Sai ID | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-13.8 Answers null | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-13.1 Đúng một trong hai câu | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-13.6 Nộp lần hai | b2aceaf / gateway + H2 | 422 | PASS | HTTP và assertions đạt |
| QUIZ-12.8 Hết số lần | b2aceaf / gateway + H2 | 422 | PASS | HTTP và assertions đạt |
| QUIZ-14.1 Kết quả đã nộp | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-14.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-14.3 B đọc bài S | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-14.4 Không tồn tại | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-14.5 Sai ID | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-14.6 Admin đọc hộ | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-14.7 Giả userId | b2aceaf / gateway + H2 | 404 | PASS | HTTP và assertions đạt |
| QUIZ-15.1 Lịch sử S | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-15.2 Không token | b2aceaf / gateway + H2 | 401 | PASS | HTTP và assertions đạt |
| QUIZ-15.3 Lịch sử B | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-15.4 Không có lịch sử | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-15.5 Sai ID | b2aceaf / gateway + H2 | 400 | PASS | HTTP và assertions đạt |
| QUIZ-15.6 Giả danh | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-15.7 Admin chưa làm | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-13.9 Bỏ trống | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-12.10 Không giới hạn lượt | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-13.10 Đúng cả hai câu | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-13.12 Giả người nộp | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-16.1 Chưa ghi danh khóa khác | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-16.2 Chưa ghi danh bị chặn | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-16.3 Không có lượt mới sau 403 | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-16.4 Hủy ghi danh | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-16.5 Hủy rồi không tiếp tục được | b2aceaf / gateway + H2 | 403 | PASS | HTTP và assertions đạt |
| QUIZ-16.6 Lượt cũ không bị đổi | b2aceaf / gateway + H2 | 200 | PASS | HTTP và assertions đạt |
| QUIZ-16.7 Ghi danh lại | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-16.8 Tiếp tục đúng lượt | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-16.9 Chủ bài được làm thử | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-16.10 Admin được làm thử | b2aceaf / gateway + H2 | 201 | PASS | HTTP và assertions đạt |
| QUIZ-13.7 Nộp quá giờ | b2aceaf / gateway + H2 | 422 | PASS | HTTP và assertions đạt |

### Kiểm tra giao diện bằng trình duyệt

| Ca | Kết quả | Quan sát |
|---|---|---|
| Học viên chưa ghi danh | PASS | Callout đúng nội dung, không có nút Bắt đầu, Ghi danh dẫn đúng /courses/2 |
| Ghi danh từ liên kết | PASS | Nút Ghi danh ngay tạo ghi danh, quay lại quiz có nút Bắt đầu |
| Hủy ghi danh sau lúc mở trang | PASS | Bấm Bắt đầu nhận 403, thay bằng Callout, không có lỗi chung |
| Tải lại sau hủy | PASS | Progress trả trạng thái CANCELLED vẫn hiện Callout |
| Ghi danh COMPLETED | PASS | Vẫn hiện nút bắt đầu khả dụng |
| Tắt enrollment-service sau mở trang | PASS | Bấm Bắt đầu nhận 502, hiện “Không kiểm tra được ghi danh, thử lại sau” |
| Mở trang khi enrollment-service dừng | PASS | Hiện ErrorAlert và Thử lại; không hiện nút Bắt đầu |
| Khôi phục dịch vụ và bấm Thử lại | PASS | Sau phục hồi/ghi danh fixture, Thử lại hiện nút Bắt đầu và vào được bài |
| Chủ bài, enrollment-service dừng | PASS | Có nút Bắt đầu và vào màn câu hỏi được |
| Admin, enrollment-service dừng | PASS | Có nút Bắt đầu và vào màn câu hỏi được |
| Học viên làm/nộp sau khôi phục | PASS | Chọn đáp án, nộp, trang kết quả đạt 100% |
| Hết lượt | PASS | Trở về bài, nút Bắt đầu bị khóa, thông báo hết số lượt |
| 375 / 768 / 1366 px | PASS | Không tràn ngang; đã xem ảnh mobile và desktop |

## Chạy lại trên máy có Docker

1. Bật cả stack và chạy smoke test theo README của repo. Các ca này chỉ dùng môi
   trường dev; tài khoản QA có thể được cấp lại vai trò trong thư mục chuẩn bị.
2. Import `docs/postman/quiz.postman_collection.json`; baseUrl mặc định 8080,
   không cần environment. Có thể đặt lại thông tin admin trong collection variables.
3. Bật `runSlowTests=true` trong collection, chạy tuần tự cả collection; ca quá giờ
   chờ 100 giây. Newman có thể dùng `--env-var runSlowTests=true --timeout-script 150000`.
   Mặc định false sẽ bỏ qua thư mục 5, không được ghi PASS.
4. Ca Kafka chạy riêng: dừng broker ở môi trường thử, bật runKafkaRecovery và chạy
   thư mục 6; bật broker lại, kiểm hộp thư đúng một thông báo của lượt vừa nộp.
   Chỉ nộp/đọc kết quả 200 chưa đủ kết luận toàn ca PASS.
5. Ghi thêm commit, môi trường, kết quả MySQL/Kafka vào biên bản; giữ nguyên ca
   QUIZ-14.8 đang FAIL cho tới khi có PR sửa và kiểm thử lại.
