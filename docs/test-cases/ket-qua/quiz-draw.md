# Biên bản QUIZ-21 — rút câu theo lượt

Ngày chạy: 08/10/2026. Nhánh `quiz-service`, nền `origin/main` tại `7da5a12`.

## Kết quả

| Kiểm tra | Kết quả |
|---|---|
| Toàn bộ Maven reactor, clean verify | 1.023 test, không fail/error/skip |
| `QuizDrawIntegrationTest` (nằm trong tổng trên) | 14 test, PASS |
| Frontend typegen, TypeScript, lint, production build | PASS |
| Collection đầy đủ qua gateway, có slow tests | 331 request, 683/683 assertion PASS |
| Nhóm QUIZ-21 trong collection | 40 request, PASS |
| Giao diện production trong trình duyệt | 17 kiểm tra PASS; ba lần reload/resume giữ nguyên đề |
| Nâng cấp MySQL thật V5 → V6 | PASS; giữ 49 quiz cũ với cài đặt null |
| Kiểm tra cấu hình bảo mật, migration | PASS |

## Môi trường và cách chạy

- Java 17; auth, course, enrollment, quiz và gateway chạy bằng các class đã build từ
  source hiện tại. Gateway cổng 8080; frontend build production rồi chạy cổng 3001.
- MySQL **8.0.46 chạy native**, instance riêng cổng 13306, các database QA riêng.
  Flyway bật và Hibernate `validate`; xác thực JWT vẫn bật.
- Máy không có Docker. Đây **không phải** biên bản kiểm thử Docker/MySQL/Kafka đầy đủ.
  Kafka listener/worker và rate limit được tắt trong tiến trình QA; course snapshot phục vụ
  ghi danh được seed tương ứng. Bảy mục Kafka thủ công trong collection không gửi request.
- Integration test dùng H2 riêng, tách biệt với lượt chạy collection trên MySQL thật.
- Backend: `./mvnw clean verify`. Frontend: `pnpm exec next typegen`,
  `pnpm exec tsc --noEmit`, `pnpm lint`, `pnpm build`.
- Collection `docs/postman/quiz.postman_collection.json`: chạy setup và toàn bộ nhóm,
  bật `--env-var runSlowTests=true`, `--timeout-script 130000` (lớn hơn chờ 100 giây).
  Tài khoản/biến môi trường QA không được đưa vào báo cáo.

## API, dữ liệu và hồi quy

- Bộ 12 câu chọn 5, gồm SINGLE/MULTI/TRUE_FALSE. Lưu đúng 5 ID cùng lượt trong transaction;
  tải lại, resume, đổi cài đặt hoặc thêm câu không rút lại. Khi tắt xáo, bộ con theo thứ tự soạn.
- Đúng cả 5 đạt 100%; kết quả chỉ có bộ đã chấm. ID câu ngoài bộ trả 400, không ghi đáp án,
  điểm hay outbox; nộp lại đúng bộ vẫn thành công. Null, N = M và N > M lấy hết; 0/-1/201 bị 400.
- **Quy tắc xóa đã được người phụ trách xác nhận:** giữ ID đã chọn, bỏ câu bị xóa, không rút bù;
  chấm trên tổng điểm các câu còn lại. Request cũ chứa câu vừa xóa vẫn hợp lệ. Xóa cả bộ trả 422,
  không chấm và không phát sự kiện. Sửa nội dung không đổi thành viên bộ; chưa có snapshot nội dung.
- Xóa mềm giữ câu từng có đáp án đã chấm để không lỗi khóa ngoại và không làm mất kết quả cũ.
  Ngân hàng, đề đang làm và thống kê không hiện câu bị xóa. Sửa/xóa lại câu đã xóa trả 404.
- Thống kê dùng số lần thực sự gặp từng câu, không dùng tổng lượt của quiz. Fixture học viên
  sau xóa có 4 câu được chấm và 7 câu chưa gặp; web hiển thị đúng “Chưa có lượt nào”.
- V6 chạy trên dữ liệu V5: 49/49 quiz cũ có `questions_per_attempt = NULL`; backfill 34 dòng
  thành viên cho lượt IN_PROGRESS và 94 dòng từ đáp án SUBMITTED. Flyway V1–V6 đều thành công.
  [Kết quả truy vấn migration](quiz-draw/migration.txt).
- Bộ test bảo vệ quyền sở hữu, ghi danh, kết quả chưa nộp, hết giờ/rollback/outbox vẫn đạt.
  [Kết quả collection đã loại bỏ thông tin xác thực](quiz-draw/api-checks.json).

## Giao diện đã thao tác thật

- Ô mặc định trống; 0 và 201 báo lỗi; lưu 5 cùng hai cờ xáo rồi reload vẫn giữ cài đặt.
- Landing hiện 5 câu; bắt đầu có đúng “Câu 1/5” đến “Câu 5/5”. Ba lần reload/Làm tiếp giữ nguyên
  câu và thứ tự đáp án. Nộp đúng được 100%, kết quả chỉ có 5 câu. Lượt tiếp theo trong fixture
  có bộ khác, không khẳng định mọi lượt ngẫu nhiên luôn khác nhau.
- Xóa trống cài đặt rồi lưu/reload; lượt mới đủ 12 câu. Khi trang đang mở và đã chọn đáp án,
  xóa một câu qua API (câu từng xuất hiện trong kết quả trước); bấm Nộp trên trang cũ vẫn được
  100%, kết quả đúng 11/11 câu còn lại.
- Lượt làm thử của tác giả không tính vào thống kê. Trang thống kê học viên có mẫu số theo câu.
- Bề ngang 375/768/1366 không tràn ngang; console không có lỗi.
- [Dữ liệu kiểm tra UI](quiz-draw/ui-checks.json),
  [375px](quiz-draw/settings-375.png), [768px](quiz-draw/settings-768.png),
  [1366px](quiz-draw/settings-1366.png), [làm 5 câu](quiz-draw/take-five.png),
  [kết quả 5 câu](quiz-draw/result-five.png),
  [nộp sau xóa câu](quiz-draw/deleted-question-result.png),
  [thống kê](quiz-draw/statistics.png).

## Ghi nhận trong quá trình kiểm tra

- Lượt Maven đầu phát hiện hai assertion cũ yêu cầu xóa vật lý. Đã sửa kiểm tra sang xóa mềm,
  đồng thời giữ kiểm tra quyền và câu không còn trong ngân hàng. Lượt clean verify cuối đạt toàn bộ.
- Lượt collection MySQL đầu chạy đồng thời với full Maven gặp một timeout enrollment (502),
  gây các lỗi dây chuyền ở fixture thống kê. QUIZ-21 vẫn đạt. Sau khi Maven xong, chạy lại **toàn bộ**
  collection, không hạ kỳ vọng: 331 request và 683/683 assertion đạt, thời gian 2 phút 12 giây.
- Chưa chạy Docker/Kafka thực trên máy này; không suy diễn kết quả native MySQL thành kết quả Docker.
