# Biên bản QUIZ-19 — nhập câu hỏi CSV

Ngày 08/10/2026. Nhánh `quiz-service`, đồng bộ `main` tới `371b613` (#80).
Kiểm phần code trong cùng commit với biên bản này, trước khi commit/push.

## Môi trường

- Java 17; MySQL Community 8.0.46 cài trực tiếp trên Windows, instance riêng cổng 13306.
  Auth/course/enrollment/quiz chạy thật, Flyway bật và Hibernate validate; gateway 8080
  xác thực JWT. Tài khoản QA đăng nhập qua auth API thật.
- Kafka listener/outbox worker và rate limit tắt bằng tham số tiến trình kiểm thử.
  Nạp course snapshot cho các khóa QA thay phần đồng bộ Kafka. Bảy request Kafka thủ công
  của collection bỏ qua, không tính là PASS.
- Frontend production build, `next start` cổng 3001; thao tác bằng Codex in-app browser.
- Máy không có Docker. Chưa chạy đầy đủ Docker/Kafka/Redis ở máy này, chưa mở/lưu file bằng
  ứng dụng Excel. Đã thử BOM UTF-8, CRLF, dấu chấm phẩy, ô có phẩy/kép/xuống dòng và byte tải
  qua cầu nối web. Không coi các kiểm tra đó là đã thao tác trong Excel.

## Kết quả

| Kiểm tra | Kết quả |
|---|---|
| Toàn bộ `clean verify` | **930 test**, 0 failure/error/skip; BUILD SUCCESS, 5 phút 14 giây |
| Integration mới | **28/28**, gồm phân quyền, CSV, giới hạn và rollback khi câu thứ hai lỗi |
| Frontend typegen, tsc, lint, production build | PASS |
| Collection qua gateway/MySQL | **263 request, 561/561 assertions**, 0 lỗi script; 2 phút 9.5 giây |
| Nhóm QUIZ-19 | **22 request** gồm chuẩn bị/lấy đề/nộp; mọi kỳ vọng PASS |
| QUIZ-13.7 và QUIZ-14.8 hồi quy | Nộp quá giờ chờ 100 giây → 422; lượt đang làm không lấy được đáp án → 422 |
| Biên dung lượng/số câu trên MySQL | **5/5 PASS**, [limits.json](quiz-import/limits.json) |
| Luồng giao diện | **14/14 PASS**, [ui-checks.json](quiz-import/ui-checks.json) |
| Quy ước | Cấu hình bảo mật, migration đã merge và diff đều PASS |

Lệnh collection: `pnpm dlx newman@6.2.2 run docs/postman/quiz.postman_collection.json --env-var runSlowTests=true --timeout-script 150000`.
Bản [api-checks.json](quiz-import/api-checks.json) chỉ giữ tên ca, status, kết quả assertion;
không chứa token, cookie hay response cá nhân.

Lượt đầu có một assertion mới sai vì đọc `questions` từ response bắt đầu lượt làm. Đã sửa
ca kiểm lấy câu hỏi qua `/quizzes/{id}/take`, giữ kiểm trạng thái `IN_PROGRESS` của lượt,
rồi chạy lại **toàn collection** đạt số liệu trên. Không hạ kỳ vọng để đổi FAIL thành PASS.

## Những điểm đã xác minh

- Tải mẫu qua web có byte giống hệt resource, BOM EF BB BF. Nhập mẫu có đủ 3 loại câu,
  đúng đáp án, tổng 4 điểm. Xuất bản và làm/nộp bằng giao diện đạt **100% (3/3 câu)**.
- File có dòng 2 thiếu đáp án đúng và dòng sau hợp lệ: 400, bảng lỗi dòng 2; không thêm
  câu nào. Sửa bằng mẫu hợp lệ thì dialog đóng, toast báo 3 câu, danh sách tự cập nhật.
- B/học viên 403, khách 401, đề không tồn tại 404; ADMIN nhập được đề của A.
- File **1048576 byte** hợp lệ (105 câu) nhập được; **1048577 byte** trả 400, dữ liệu không
  đổi. File 2 MiB qua gateway trả JSON 400 đúng contract. File 201 câu trả lỗi dòng 202;
  đủ 200 câu nhập được trên MySQL và nối position liên tiếp sau các câu đang có.
- CSV có BOM, quoted comma, escaped quote và xuống dòng lưu nguyên nội dung; lỗi ở bản
  ghi tiếp theo báo dòng vật lý bắt đầu. CSV chấm phẩy nhập được.
- Integration cố tình gây lỗi khi lưu câu thứ hai: HTTP 500 và transaction rollback,
  không còn câu đầu tiên. Các ca lỗi validation kiểm trước khi có bất kỳ lần lưu nào.
- Hộp thoại lỗi 375px và trang soạn đề 375/768/1366px không tràn ngang. File lớn/rỗng báo
  lỗi dễ đọc; mở lại hộp thoại xóa file/lỗi cũ; Hủy đóng hộp thoại. Không có console error
  trong lượt đọc log cuối luồng làm bài.

## Ảnh kiểm chứng

- [Lỗi dòng 2 ở 375px](quiz-import/error-375.png)
- [Danh sách sau nhập 3 câu](quiz-import/success-1366.png)
- [Làm bài từ mẫu đạt 100%](quiz-import/result-100.png)
