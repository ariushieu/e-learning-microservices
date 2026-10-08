# Biên bản QUIZ-18 — tải kết quả CSV

Ngày 08/10/2026. Code **`3243d5abe84f42781023d3f317188e4dea1c583d`**, đã đồng bộ
`main` tới `3b175a5` (#74). Các kết quả dưới đây do người triển khai chạy trên máy này.

## Môi trường

- MySQL Community 8.0.46 cài trực tiếp trên Windows, instance kiểm thử riêng cổng 13306,
  database riêng. Auth/course/enrollment/quiz chạy với Flyway bật và Hibernate validate;
  gateway 8080 giữ xác thực JWT, tài khoản đăng nhập qua auth API thật.
- Kafka listener/outbox worker và rate limit tắt bằng tham số tiến trình kiểm thử.
  Nạp hai course snapshot để thay phần đồng bộ Kafka. Bảy request Kafka thủ công bỏ qua.
- Frontend production build chạy Next start cổng 3001. UI dùng dữ liệu tạm 21 học viên
  để kiểm tải từ trang thứ hai; collection tạo đề và lượt làm qua API thật.
- Không có Docker trên máy. Chưa chạy Docker/Kafka/Redis end-to-end, chưa mở file bằng
  ứng dụng Excel. Đã kiểm byte BOM, nội dung UTF-8, escape công thức và parse CSV độc lập.

## Kết quả

| Kiểm tra | Kết quả |
|---|---|
| Toàn bộ `clean verify` | **876 test**, 0 failure/error/skip, BUILD SUCCESS; 3 phút 44 giây |
| `QuizResultsIntegrationTest` | **24 ca**, gồm **13 ca CSV mới**, tất cả PASS |
| Frontend typegen, tsc, lint, build | PASS |
| Toàn collection qua gateway/MySQL | **241 request, 525/525 assertions**, 0 lỗi script, 2 phút 7.5 giây |
| QUIZ-18 | **20 request** gồm chuẩn bị/khôi phục, cả 9 tình huống đều PASS |
| QUIZ-13.7/14.8 hồi quy | Chờ 100 giây → 422; xem kết quả khi đang làm → 422 |
| Kiểm giao diện | **12/12 PASS**, không có console error trong lượt kiểm |
| Đối chiếu bổ sung | **3/3 PASS**: BOM trên MySQL, CSV parse khớp JSON và UTC+7, byte tải qua web giống gateway |
| Quy ước | Cấu hình bảo mật, migration đã merge và diff đều PASS |

Newman chạy với `--env-var runSlowTests=true --timeout-script 150000`. Không tính các
request Kafka bị bỏ qua là PASS. Collection giữ nguyên các kỳ vọng cũ.

## Các ca quan trọng

- A/admin tải cùng dữ liệu; B/học viên 403, khách 401, đề không tồn tại 404. Lỗi là JSON,
  không mang header attachment. `Accept: application/json` vẫn tải CSV được.
- S đổi tên `=1+1` qua profile, đăng nhập lại, bắt đầu và nộp bài: CSV có `'=1+1`.
  Đổi tiếp thành `Nguyễn, "Ánh"`: CSV có `"Nguyễn, ""Ánh"""`; parse lại giữ đúng tên.
  Đã khôi phục tên tài khoản QA sau ca thử. Điểm cao nhất vẫn 50 khi làm lại được 0.
- Integration kiểm đủ tiền tố `= + - @ tab CR LF`, tên có phẩy/kép/xuống dòng, tên trống,
  không sửa tên lưu trong DB; bỏ lượt preview/legacy/expired/ongoing.
- File xuất không phân trang: integration có 22 học viên; web đứng ở trang 2 (1 dòng)
  nhưng tải đủ 21 học viên. Đề rỗng tải header. File có BOM EF BB BF, CRLF, tiếng Việt,
  giờ nộp theo Asia/Ho_Chi_Minh. Parser độc lập đối chiếu từng cột với JSON thật.
- Bấm link trên web tải đúng `ket-qua-quiz-9000.csv`; nội dung từng byte giống tải thẳng
  qua gateway. B không thấy link tải trên trang không có quyền. Nút hiện và trang không
  tràn ngang ở 375/768/1366px. Đã xem ảnh desktop/mobile.

## Bằng chứng

- [API, collection và đối chiếu CSV](quiz-csv/api-checks.json)
- [12 kiểm tra giao diện](quiz-csv/ui-checks.json)
- [Desktop](quiz-csv/results-1366.png), [tablet](quiz-csv/results-768.png),
  [mobile](quiz-csv/results-375.png), [không có quyền](quiz-csv/forbidden.png)
