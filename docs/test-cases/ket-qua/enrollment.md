# Biên bản enrollment — 07/10/2026

Bản backend đã kiểm: **1196a26303c7796ab88bd3c8d059ed565d06bf5e** (bao gồm main e17520b).
Collection trong cùng PR, SHA-256: d7a6702d601cf2c00c6c960786b1f790cef3c6957c2b15e9ffe835ca48bbd36b. Không dùng kết quả này để xác nhận môi trường Docker/MySQL.

## Môi trường L1 và bằng chứng

- Windows, JDK 21.0.12, Maven 3.9.16; Node 24.19.0, Newman 6.2.2.
- Gateway, auth, course, enrollment, notification là các process thật, JWT bật, rate limit tắt cho bài kiểm nghiệp vụ. Tất cả request collection qua http://localhost:8080.
- Kafka KRaft thật trong JVM; H2 2.4.240 chế độ MySQL, schema do Hibernate tạo; Flyway tắt. Không có Docker/MySQL/Redis và không chạy quiz vì collection không gọi quiz.
- Seed chỉ gồm roles/admin, notification templates và khóa outbox của course. Không chèn course_snapshots, enrollment, progress hoặc certificate bằng SQL; các dữ liệu đó do API/consumer tạo.
- **Khác biệt H2:** payload của outbox enrollment dùng CLOB trong database thử để giữ nguyên JSON string. H2 JSON lưu chuỗi theo cách khác MySQL. Fixture này không sửa migration/production, và kết quả không kiểm được cột JSON hay migration MySQL.
- E1: [evidence HTTP đã bỏ token](enrollment-http-evidence.json), chỉ có tên request, mã HTTP và số assertion; không chứa header Authorization, refresh token, password hoặc response thô.
- Thời gian UTC: 2026-10-07T06:58:15.633Z → 2026-10-07T06:59:03.207Z.

## Kết quả

154 request trong collection; khi chạy có thêm các lượt polling và hai PUT đồng thời: **160 request HTTP**, **310 assertion đạt**, không request lỗi và không assertion lỗi.
Theo yêu cầu đầy đủ của từng ca: **85 PASS, 6 BLOCKED**. ENROLL-01.10 chỉ mới kiểm được HTTP 404; năm ca ENROLL-09 chưa chạy hạ tầng. Không coi folder hướng dẫn trống là PASS.

| Mã ca | Commit/môi trường | HTTP thực tế | Kết quả | Bằng chứng / giới hạn |
|---|---|---|---|---|
| ENROLL-01.1 | 1196a26 / L1 | 404, 201 | PASS | E1: 2 lượt HTTP, 2 assertion đạt. |
| ENROLL-01.2 | 1196a26 / L1 | 401 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-01.3 | 1196a26 / L1 | 201 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-01.4 | 1196a26 / L1 | 404 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-01.5 | 1196a26 / L1 | 400 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-01.6 | 1196a26 / L1 | 400 | PASS | E1: 3 lượt HTTP, 6 assertion đạt. |
| ENROLL-01.7 | 1196a26 / L1 | 409 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-01.8 | 1196a26 / L1 | 201 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-01.9 | 1196a26 / L1 | 201 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-01.10 | 1196a26 / L1 | 404 | BLOCKED | HTTP 404 đúng; chưa quan sát trực tiếp snapshot ARCHIVED. |
| ENROLL-01.11 | 1196a26 / L1 | 200 | PASS | E1: 2 lượt HTTP, 2 assertion đạt. |
| ENROLL-02.1 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-02.2 | 1196a26 / L1 | 401 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-02.3 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-02.4 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-02.5 | 1196a26 / L1 | 400 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-02.6 | 1196a26 / L1 | 400 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-02.7 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-02.8 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-03.1 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-03.2 | 1196a26 / L1 | 401 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-03.3 | 1196a26 / L1 | 403 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-03.4 | 1196a26 / L1 | 403 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-03.5 | 1196a26 / L1 | 404 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-03.6 | 1196a26 / L1 | 400 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-03.7 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-04.1 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-04.2 | 1196a26 / L1 | 401 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-04.3 | 1196a26 / L1 | 403 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-04.4 | 1196a26 / L1 | 404 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-04.5 | 1196a26 / L1 | 400 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-04.6 | 1196a26 / L1 | 422, 200 | PASS | E1: 2 lượt HTTP, 4 assertion đạt. |
| ENROLL-04.7 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-04.8 | 1196a26 / L1 | 400 | PASS | E1: 2 lượt HTTP, 4 assertion đạt. |
| ENROLL-04.9 | 1196a26 / L1 | 422 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-05.1 | 1196a26 / L1 | 200, 404 | PASS | E1: 4 lượt HTTP, 8 assertion đạt. |
| ENROLL-05.2 | 1196a26 / L1 | 401 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-05.3 | 1196a26 / L1 | 404, 200 | PASS | E1: 2 lượt HTTP, 4 assertion đạt. |
| ENROLL-05.4 | 1196a26 / L1 | 404 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-05.5 | 1196a26 / L1 | 400 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-05.6 | 1196a26 / L1 | 400 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-05.7 | 1196a26 / L1 | 200 | PASS | E1: 2 lượt HTTP, 4 assertion đạt. |
| ENROLL-05.8 | 1196a26 / L1 | 201, 200 | PASS | E1: 2 lượt HTTP, 4 assertion đạt. |
| ENROLL-06.1 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-06.2 | 1196a26 / L1 | 401 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-06.3 | 1196a26 / L1 | 403 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-06.4 | 1196a26 / L1 | 404 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-06.5 | 1196a26 / L1 | 400 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-06.6 | 1196a26 / L1 | 404 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-06.7 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-06.8 | 1196a26 / L1 | 403 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-07.1 | 1196a26 / L1 | 200 | PASS | E1: 2 lượt HTTP, 4 assertion đạt. |
| ENROLL-07.2 | 1196a26 / L1 | 401 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-07.3 | 1196a26 / L1 | 404 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-07.4 | 1196a26 / L1 | 404 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-07.5 | 1196a26 / L1 | 400 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-07.6 | 1196a26 / L1 | 400 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-07.7 | 1196a26 / L1 | 400 | PASS | E1: 2 lượt HTTP, 4 assertion đạt. |
| ENROLL-07.8 | 1196a26 / L1 | 422 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-07.9 | 1196a26 / L1 | 200 | PASS | E1: 2 lượt HTTP, 4 assertion đạt. |
| ENROLL-07.10 | 1196a26 / L1 | 200 | PASS | E1: 3 lượt HTTP, 6 assertion đạt. |
| ENROLL-07.11 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-07.12 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-07.13 | 1196a26 / L1 | 404, 200 | PASS | E1: 3 lượt HTTP, 6 assertion đạt. |
| ENROLL-07.14 | 1196a26 / L1 | 200 | PASS | E1: 3 lượt HTTP, 6 assertion đạt. |
| ENROLL-07.15 | 1196a26 / L1 | 200 | PASS | E1: 6 lượt HTTP, 12 assertion đạt. |
| ENROLL-07.16 | 1196a26 / L1 | 200 | PASS | E1: 5 lượt HTTP, 16 assertion đạt. |
| ENROLL-07.17 | 1196a26 / L1 | 200 | PASS | E1: 5 lượt HTTP, 8 assertion đạt. |
| ENROLL-08.1 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-08.2 | 1196a26 / L1 | 401 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-08.3 | 1196a26 / L1 | 404 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-08.4 | 1196a26 / L1 | 404 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-08.5 | 1196a26 / L1 | 400 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-08.6 | 1196a26 / L1 | 400 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-08.7 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-08.8 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-08.9 | 1196a26 / L1 | 200 | PASS | E1: 2 lượt HTTP, 4 assertion đạt. |
| ENROLL-10.1 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-10.2 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-10.3 | 1196a26 / L1 | 404 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-10.4 | 1196a26 / L1 | 404 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-10.5 | 1196a26 / L1 | 200 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-10.6 | 1196a26 / L1 | 401 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-10.7 | 1196a26 / L1 | 401 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-10.8 | 1196a26 / L1 | 404 | PASS | E1: 1 lượt HTTP, 2 assertion đạt. |
| ENROLL-10.9 | 1196a26 / L1 | 200 | PASS | E1: 4 lượt HTTP, 6 assertion đạt. |
| ENROLL-09.1 | 1196a26 / Docker-MySQL | N/A | BLOCKED | Chưa có Docker/MySQL; cần thao tác hạ tầng theo bảng tình huống. |
| ENROLL-09.2 | 1196a26 / Docker-MySQL | N/A | BLOCKED | Chưa có Docker/MySQL; cần thao tác hạ tầng theo bảng tình huống. |
| ENROLL-09.3 | 1196a26 / Docker-MySQL | N/A | BLOCKED | Chưa có Docker/MySQL; cần thao tác hạ tầng theo bảng tình huống. |
| ENROLL-09.4 | 1196a26 / Docker-MySQL | N/A | BLOCKED | Chưa có Docker/MySQL; cần thao tác hạ tầng theo bảng tình huống. |
| ENROLL-09.5 | 1196a26 / Docker-MySQL | N/A | BLOCKED | Chưa có Docker/MySQL; cần thao tác hạ tầng theo bảng tình huống. |

Các mã có nhiều HTTP bao gồm request thay đổi trạng thái và GET kiểm hậu điều kiện; ENROLL-01.1 có thể có 404 trong lúc chờ rồi 201. Thứ tự từng lượt nằm trong E1.

## Kiểm thử tự động bổ sung

mvn clean verify: **667 test đạt**, không failure/error/skipped. Theo module: shared-common 77, gateway 22, auth 50, course 207, enrollment 92, quiz 188, notification 31.
Test tích hợp kiểm chứng chỉ cũ thiếu tên, quyền backfill của chủ sở hữu, giữ tên khóa tại lúc cấp, 401 và rollback khi token thiếu tên, cùng retry/DLT và request đồng thời. Các test database dùng H2; test Kafka lỗi database dùng exception mô phỏng, không thay cho ENROLL-09 trên MySQL thật.

CI trên commit d52ef44 đã chạy thành công [Build & Test (JDK 17)](https://github.com/ariushieu/e-learning-microservices/actions/runs/37588146168/job/112682987910), [Schema matches entities (MySQL)](https://github.com/ariushieu/e-learning-microservices/actions/runs/37588146168/job/112682987853), [Full stack in Docker](https://github.com/ariushieu/e-learning-microservices/actions/runs/37588146168/job/112682987511) và [Frontend](https://github.com/ariushieu/e-learning-microservices/actions/runs/37588146168/job/112682987796). Flyway tạo schema và Hibernate validate cả các cột mới. Đây là bằng chứng CI/migration, không phải lượt chạy toàn bộ collection trên MySQL.

## Kiểm tra giao diện xác minh

Chạy bản production bằng `pnpm start`, kết nối gateway L1; đã xem `/design` và kiểm tra trực tiếp trên trình duyệt:

- Chưa đăng nhập: mã thật hiện đúng bốn thông tin và huy hiệu **Hợp lệ**; có quan sát trạng thái Skeleton trong lúc chờ API.
- Mã không tồn tại hiện **Không tìm thấy chứng chỉ**. Gateway không kết nối được hiện **Chưa thể xác minh chứng chỉ**, không bị báo nhầm là mã không tồn tại.
- Kiểm tra 375, 768 và 1366px; tên học viên 150 ký tự và tên khóa 200 ký tự (bao gồm chuỗi dài không có khoảng trắng) không làm tràn trang xác minh.
- Chứng chỉ cá nhân dùng tên đã lưu; liên kết có đủ origin + `/verify/CERT-…`, bấm Enter mở đúng chứng chỉ. Focus có viền nhìn thấy; không có lỗi console.
- Đã sửa thẻ chứng chỉ để tự tăng chiều cao khi nội dung dài. Ở 375/768/1366px, chiều cao nội dung bằng chiều cao thẻ và không tràn ngang; liên kết xác minh nằm trong thẻ. Tên dài dùng cỡ chữ nhỏ hơn, không cắt nội dung.
- `pnpm next typegen`, `pnpm tsc --noEmit`, `pnpm lint`, `pnpm build` đạt. Chưa xuất/in PDF thực tế; cần kiểm tra bản in A4 ngang ở môi trường demo.

## Cần chạy tiếp trên môi trường nhóm

1. Chạy lại toàn bộ collection trên Docker Compose/MySQL không thay kỳ vọng; CI đã kiểm schema và smoke-test nhưng không thay cho lượt chạy collection này.
2. Đối chiếu snapshot ARCHIVED cho ENROLL-01.10 bằng database/log; thực hiện ENROLL-09.1–09.5 và lưu offset/payload/header DLT.
3. Nếu có FAIL nghiệp vụ, mở PR sửa riêng và ghi đúng mã ca theo phan-cong.md.
