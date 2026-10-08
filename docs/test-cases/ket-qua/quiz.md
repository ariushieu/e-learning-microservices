# Biên bản kiểm thử quiz — 07/10/2026

> Biên bản mới nhất: [QUIZ-18 — tải kết quả CSV, 08/10/2026](quiz-csv.md).
> Trước đó: [QUIZ-17 — kết quả dành cho giảng viên, 08/10/2026](quiz-results.md).

## Bản sửa bảo vệ kết quả và lưu EXPIRED — `194f5ef`

Code và collection: **`194f5ef305d5bcc2fe54fbcdd8e0aa41986e4347`**, đồng bộ
`main` **`1873f06`**. Các mục phía dưới phần này giữ lịch sử #59 để đối chiếu.

- `./mvnw -B -ntp clean verify`: **695 test, 0 failure/error/skipped**, BUILD SUCCESS,
  kết thúc 16:05:23 +07:00. Có **8 ca tích hợp mới** trong `QuizAttemptResultIntegrationTest`.
- Đã chạy test mới trước khi sửa: **3 FAIL**, đúng hai trạng thái trả đáp án 200
  (IN_PROGRESS/EXPIRED) và EXPIRED bị rollback thành IN_PROGRESS. Sau sửa **8/8 PASS**.
  Test không có transaction bao ngoài, đọc lại DB sau HTTP 422 để xác minh commit.
  Có kiểm mất xác thực, người khác/admin đọc hộ, kết quả đã nộp, nộp lặp, không
  tạo answers/outbox khi hết giờ, tạo lượt tiếp theo và rollback khi lưu outbox lỗi.
- Frontend: `pnpm next typegen`, `pnpm tsc --noEmit`, `pnpm lint`, `pnpm build`: PASS.
- Kiểm cấu hình bảo mật, migration, tiêu đề commit và diff: PASS.

### Chạy thật qua gateway tại máy sửa

Auth/course/enrollment/quiz/gateway là **5 tiến trình Java thật**, xác thực bật,
JWT lấy qua API đăng nhập; frontend dùng build production riêng ở cổng 3001.
Database **H2 tạm**, Hibernate dựng schema, Flyway tắt trong tiến trình thử.
Máy chưa có Docker/WSL; không chạy MySQL/Kafka/Redis. Snapshot khóa 1/2 được nạp
trước, Kafka listener/outbox publisher và rate limit tắt chỉ trong môi trường thử.
Dữ liệu khởi tạo H2 và khóa ký JWT được cấu hình nhất quán ở tiến trình thử,
không sửa cấu hình hoặc migration đã commit.

Newman **6.2.2**, `runSlowTests=true` qua environment override, timeout script
150000 ms: **192 request, 417/417 assertions PASS**, 0 lỗi request/script,
thời gian **2 phút 40,2 giây**, gồm ca đợi 100 giây. 7 request Kafka bỏ qua có chủ
đích, không tính PASS. Số request khác lượt Docker #59 do môi trường đồng bộ khác;
không gộp số liệu hai lượt.

| Kiểm tra | HTTP / dữ liệu quan sát | Kết quả |
|---|---|---|
| QUIZ-14.8 — lượt đang làm | 422 BUSINESS_RULE_VIOLATED; không có data/correctOptionIds/questionResults | PASS |
| QUIZ-13.7 — nộp sau 100 giây | 422; timeoutAttempt=9, quiz=14 | PASS |
| Lịch sử ngay sau lỗi quá giờ | Lượt 9 EXPIRED, score=0, passed=false, submittedAt đã lưu | PASS |
| Đọc kết quả lượt EXPIRED | 422, không trả đáp án | PASS |
| Nộp lại lượt EXPIRED | 422; lịch sử vẫn EXPIRED, submittedAt không đổi | PASS |
| Lượt hợp lệ mới để kiểm web | Lượt 10: trước nộp GET 422, nộp 200 với score=100 | PASS |

### Kiểm tra trình duyệt với frontend production

- Đăng nhập học viên bằng form; mở trực tiếp `/attempts/9` khi còn IN_PROGRESS
  và sau khi EXPIRED: hiện "Chưa có kết quả bài kiểm tra", không có đáp án.
- `/quizzes/14`: lịch sử hiện **Hết giờ**, không có "Xem kết quả"; hết 1/1 lượt
  thì khóa nút bắt đầu. Không tràn ngang tại **375 / 768 / 1366 px**; đã xem ảnh
  desktop và mobile.
- `/quizzes/15`: bắt đầu từ UI, nộp lượt 10 bằng API từ phiên khác, rồi nộp lại
  trên UI → 422, khóa câu trả lời, hiện nút **Về bài kiểm tra**. Bấm nút tải lại
  lịch sử thành SUBMITTED, 100%, khóa bắt đầu vì hết lượt; liên kết kết quả hoạt động.
- `/attempts/10`: vẫn có 100%, đáp án đúng và giải thích. Không có lỗi console
  ở phiên kiểm giao diện cuối.

**Giới hạn:** bản backend `194f5ef` chưa được chạy trên Docker/MySQL/Kafka tại máy
này. Kết quả Docker `c55a9f2` bên dưới chỉ nghiệm thu collection/giao diện của #59.
Kafka recovery và hai ca sort vẫn chưa nghiệm thu. Các tiến trình thử được dừng
sau kiểm tra, database H2 tạm không được giữ lại.

## Lịch sử #59 — phiên bản và môi trường

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

**Nghiệm thu Docker của bản sửa collection: đã có kết quả từ reviewer.**
Theo [review phê duyệt #59](https://github.com/ariushieu/e-learning-microservices/pull/59#pullrequestreview-5439270481),
ariushieu chạy tại **`c55a9f2`**, MySQL/Kafka/Redis, gateway 8080, tạm tắt rate limit,
bật ca chậm: **193 request, 416/417 assertions PASS**, QUIZ-13.7 nhận **422** và
`--env-var runSlowTests=true` hoạt động. Chỉ QUIZ-14.8 còn FAIL tại phiên bản đó.
Kết quả này thay trạng thái BLOCKED cũ của lượt nghiệm thu #59; không phải Docker
do tác giả chạy tại máy này, cũng không chứng minh bản sửa backend sau #59.

### Lỗi tái hiện tại #59 — QUIZ-14.8 (lịch sử)

**FAIL tại bản #59, yêu cầu sửa riêng trước khi nghiệm thu bảo mật.** Với lượt IN_PROGRESS thuộc
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
| Docker/MySQL của #59 | c55a9f2 / reviewer ariushieu | QUIZ-13.7: 422 | Đã chạy | 193 request, 416/417; chỉ QUIZ-14.8 FAIL, nguồn review phê duyệt ở trên |

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
5. Ghi thêm commit, môi trường, kết quả MySQL/Kafka vào biên bản; giữ nguyên kỳ vọng
   422 của QUIZ-14.8 và xác nhận PASS sau bản sửa bảo vệ kết quả.
