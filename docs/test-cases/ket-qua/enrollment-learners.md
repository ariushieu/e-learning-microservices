# Biên bản ENROLL-11 — 08/10/2026

Code được kiểm: **733a24000e67bef0ba7bea1ecdb28cb081fbdc22**, đã ghép main **a16f740** (#69).
Phạm vi: danh sách học viên của giảng viên, tên từ JWT, route gateway và component quản lý khóa.

## Kiểm thử tự động

| Kiểm tra | Kết quả | Bằng chứng / giới hạn |
|---|---|---|
| Maven `clean verify` toàn reactor | PASS — 818 test, 0 failure/error/skip | JDK 21.0.12, Maven 3.9.16; hoàn tất 09:26:15 UTC+7; database test H2 |
| `CourseLearnerApiIntegrationTest` | PASS — 20 test | JWT thật + MVC + JPA + progress service; chỉ giả lập HTTP sang course-service |
| `EnrollmentRoutingIntegrationTest` | PASS — 7 test | HTTP qua gateway thật, backend stub xác nhận đích/path/query/token; kiểm các route course lân cận |
| Frontend typegen, tsc, lint, production build | PASS | Chạy lại sau khi ghép #69; Next 16.3.8, Node 24.19.0 |
| Migration cũ không bị sửa, xác thực production bật, Git whitespace | PASS | `check-migrations.sh origin/main HEAD`, `check-security-config.sh`, `git diff --check` |
| Cú pháp Postman + mọi URL đi qua gateway | PASS | 182 request định nghĩa, 28 request ENROLL-11 |

## HTTP qua gateway và các service thật

- Thời gian: **09:29:11–09:30:22 ngày 08/10/2026 (UTC+7)**.
- Newman 6.2.2; **187 request HTTP** (gồm polling và request đồng thời), **366/366 assertion PASS**;
  không lỗi request/assertion. ENROLL-11: **28 request**, tất cả đạt.
- [Bằng chứng đã loại token/body](enrollment-learners-http-evidence.json) lưu tên request, mã HTTP,
  số assertion và lỗi nếu có. Không chứa token, cookie, email hoặc response thô.
- SHA-256 collection: `6206407b1197ac20eda8bc669d8bb98f69a7e073c71534ebf242100d9c91d3c9`.
- Gateway, auth, course, enrollment, notification chạy bằng JAR mới; Kafka KRaft thật. JWT bật,
  rate limit tắt trên stack QA. Mọi request dùng gateway `http://localhost:8080`.
- Database **H2 chế độ MySQL**, Hibernate tạo schema, Flyway tắt; không có Docker/MySQL/Redis.
  Outbox payload của enrollment được đổi sang CLOB **chỉ trong DB QA** để tránh khác biệt H2 JSON.
  Không sửa production, không nạp snapshot/enrollment/progress/certificate bằng SQL; fixture do API
  và Kafka consumer tạo. Không dùng kết quả này để kết luận migration MySQL đã chạy thành công.

| Ca ENROLL-11 | Kết quả | Bằng chứng |
|---|---|---|
| 1–10 | PASS | HTTP: A/admin 200, S/B 403, thiếu token 401 qua gateway, thiếu snapshot 404, ID/query sai 400; phân trang và total đúng |
| 11–12 | PASS | HTTP lọc CANCELLED/ACTIVE, tên không nhận từ body; test tích hợp kiểm token mang tên mới khi kích hoạt lại và đọc không backfill |
| 13–16 | PASS | HTTP một trong hai bài → 50%, hoàn thành → 100% và đúng mã/ngày chứng chỉ; sort hai chiều; xóa lượt → danh sách rỗng |
| 17 — hành vi lượt cũ NULL | PASS trên H2 | Test tích hợp xác nhận NULL và userId dự phòng, đọc bằng token A không ghi tên A |
| 17 — Flyway V3 trên MySQL có dữ liệu cũ | BLOCKED tại máy | Máy không có Docker/MySQL. Cần chạy migration với DB cũ và `ddl-auto=validate`; CI schema chỉ kiểm database mới |
| 18 | PASS | Test tích hợp: JWT thiếu tên/trắng/>150 ký tự → 401; không ghi enrollment/outbox |
| 19–20 — giao diện và responsive | BLOCKED tại máy | CUA và node_repl đều không khởi động: `windows sandbox failed: helper_unknown_error: setup refresh had errors`. Không có screenshot hoặc kết luận PASS về 375/768/1366px, bàn phím và đổi bộ lọc nhanh |

Ca 19–20 cần mở `/instructor/courses/{id}` bằng A, thử lọc/sort/phân trang/làm mới, tên dài và tên
NULL, khóa chưa có ai, trang vượt cuối, lỗi API; kiểm đủ 375/768/1366px và thao tác Tab.
Phần frontend đã build nhưng chưa được kiểm bằng trình duyệt. PR giữ nháp cho tới khi xác nhận các
mục còn thiếu. Các ca hạ tầng ENROLL-09 không được Newman thực hiện, không ghi là PASS.

Biên bản này bổ sung cho [kết quả trước](enrollment.md), không thay thế bằng chứng Docker/MySQL của
review #60 hoặc suy diễn rằng lần chạy H2 này đã kiểm hạ tầng MySQL/Kafka outage.
