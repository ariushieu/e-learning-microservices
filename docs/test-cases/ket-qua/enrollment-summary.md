# Biên bản ENROLL-12 — 08/10/2026

Phần triển khai: `0fcfdfd`, ghép main `94bb008` tại `8e757f7`.
Phạm vi: số liệu toàn khóa/từng bài, route gateway, quyền snapshot và giao diện giảng viên.
Không đổi entity/migration. Trang của course-service chỉ đổi một dòng truyền đề cương.

## Kiểm thử cục bộ

- Bản trước khi ghép main (`0fcfdfd`, main `7a3cc03`): `mvn clean verify` đạt **888 test**,
  không failure/error/skip, JDK 21.0.12 và Maven 3.9.16.
- Chạy lại sau khi ghép main `94bb008` (`8e757f7`): **914 test đạt**, không failure/error/skip,
  hoàn tất 13:15:25 UTC+7. Lần đầu bị khóa JAR bởi Kafka QA; dừng stack QA rồi `clean verify` thành công.
- `CourseLearnerSummaryIntegrationTest`: **11 test đạt**. Mẫu S/B/C/D đúng số liệu, loại tiến độ
  đã hủy và dữ liệu khóa khác; quyền/token/ID, khóa rỗng, IN_PROGRESS, làm tròn và chứng chỉ thực tế.
  Kiểm Hibernate statistics: **4 query**, không tải entity Enrollment/LessonProgress/Certificate.
- `EnrollmentRoutingIntegrationTest`: **8 test đạt**, GET summary tới enrollment; GET chi tiết
  khóa và đề cương vẫn tới course-service. JWT tại enrollment có kiểm thử MVC riêng.
- `node --test scripts/check-enrollment-summary.test.mjs`: **6 test đạt**. Ghép đề cương, bài
  chưa học 0%, bài xóa ẩn, thứ tự chương/bài, không sửa props, ngưỡng giảm 20 điểm phần trăm.
- Frontend typegen, TypeScript, lint và production build đạt cả trước và sau khi ghép main.
- Migration cũ, cấu hình JWT production và `git diff --check` đạt.

## Collection qua các service thật

Lượt cục bộ **13:08:22–13:09:20 UTC+7**, code `0fcfdfd` trên main `7a3cc03`:

- **223 lượt HTTP, 432/432 assertion đạt**, không lỗi request/assertion; 215 request định nghĩa,
  số thực tế gồm polling và hai PUT đồng thời. ENROLL-12 có 14 request định nghĩa, 15 lượt gồm polling.
- Gateway, auth, course, enrollment, notification chạy bằng JAR; Kafka KRaft thật. JWT bật,
  rate limit tắt trên stack QA. Fixture tạo qua API và consumer, không chèn snapshot bằng SQL.
- Database **H2 chế độ MySQL**, Flyway tắt; outbox payload CLOB chỉ trong DB QA để tránh khác biệt
  JSON của H2. Không coi lượt này là kiểm thử Docker/MySQL.
- [Bằng chứng HTTP](enrollment-summary-http-evidence.json) chỉ lưu tên request, mã HTTP, số
  assertion và tên assertion lỗi; không lưu token, cookie, body hoặc header.
- SHA-256 collection: `688facc1b18c296a30a90b6f12f55d4f45c5414aa7af7197b021abacd335f915`.

Mẫu nghiệm thu: active=2, completed=1, cancelled=1, averageProgress=50,
completionRate=33.33, certificatesIssued=1; bài 1=66.67%, bài 2=33.33%.
D hoàn thành bài 1 trước khi hủy vẫn không được cộng vào tỉ lệ; C chỉ IN_PROGRESS vẫn 0%.

## Docker và trình duyệt trong CI

Máy cục bộ không có Docker; công cụ trình duyệt không khởi động được do lỗi sandbox.
Workflow `Full stack in Docker` đã được mở rộng để chạy toàn bộ collection trên MySQL/Kafka/Redis
và kiểm giao diện bằng Chromium sau smoke test và collection auth có sẵn. Kết quả CI sẽ được
ghi trong PR; không coi việc thêm script là bằng chứng đã chạy thành công.

- `scripts/report-enrollment-newman.cjs` loại dữ liệu nhạy cảm trước khi lưu artifact.
- `scripts/check-enrollment-summary-ui.cjs` dùng fixture ENROLL-12: bốn ô số liệu, phần trăm hai chữ số,
  cảnh báo giảm, bộ lọc không đổi thống kê, Skeleton, API lỗi không che bảng học viên và làm mới phục hồi.
  Bổ sung bài tên dài để kiểm 0%, kiểm 375/768/1366px và Tab; xóa bài fixture để kiểm ẩn dữ liệu lịch sử.
- Artifact `enrollment-summary-ui` chỉ chứa `docker-http.json`, `results.json`, screenshot;
  collection export chứa token và report Newman thô chỉ ở thư mục tạm của runner, không upload.
- Các ca outage/retry thủ công **ENROLL-09.1–09.5 không được chạy** trong collection hoặc browser test.

PR được mở ở trạng thái **Ready for review**, theo yêu cầu của người phụ trách enrollment.
