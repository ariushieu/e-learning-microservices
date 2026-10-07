# Biên bản kiểm thử auth-service

## N1 — Tự chạy Newman sau sửa định dạng số điện thoại

- **Thời gian:** 2026-10-07T15:48:39.257018+07:00 đến 2026-10-07T15:49:49.252195+07:00.
- **Code đã chạy:** `ee88921b9e40083ef05c29138da39a78f118dcdd`, từ `main 1873f06`. Gateway/auth/course/quiz được build cùng lượt bằng `mvnw.cmd -B -ntp -pl auth-service,api-gateway,course-service,quiz-service -am verify`: **565 test PASS**, không lỗi hoặc bỏ qua (auth: 67, trong đó quản lý hồ sơ: 35).
- **Nguồn kết quả:** file JSON do Newman 6.2.2 xuất ra trong lượt chạy này. HTTP và số assertion ở từng dòng dưới được trích trực tiếp từ `run.executions`; không suy từ kỳ vọng hoặc kết quả reviewer.
- **Kết quả:** **153/153 request, 358/358 assertion PASS**; đủ **76/76 mã ca PASS, 0 FAIL, 0 BLOCKED, 0 request bị bỏ qua**. Ngoài các mã ca có 18 request chuẩn bị/khôi phục, cũng đều đạt.
- **Môi trường:** Windows, Java 17, MySQL 8.0.43 thật trên `127.0.0.1:13317`, datadir và ba schema auth/course/quiz tạo mới riêng. Flyway dựng schema, Hibernate `ddl-auto=validate` thành công. Các JAR chạy native vì máy không có Docker.
- **Gateway:** `http://127.0.0.1:18080` (8080 đã được tiến trình khác sử dụng); JWT vẫn bật ở gateway và service. Toàn bộ request Newman đi qua gateway, không gọi thẳng service. Chỉ bản collection dùng cho lượt chạy đổi `baseUrl` và điền ba fixture; collection trong Git giữ `http://localhost:8080` và không chứa token.
- **Phạm vi môi trường:** chạy gateway, auth, course và quiz là các service collection auth gọi. Kafka consumer/outbox worker tắt trong tiến trình test; Redis, enrollment và notification không khởi động. Lượt này kiểm nghiệp vụ auth qua MySQL/gateway, không thay cho full-stack Docker/smoke test hoặc kiểm thử giao diện.
- **Rate limit:** chỉ tắt bằng tham số `--elearning.rate-limit.enabled=false` của gateway test riêng. Sau lượt chạy mọi tiến trình test đã dừng; không để lại gateway tắt rate limit, không đổi cấu hình đã commit hoặc tiến trình ứng dụng có sẵn. Database dev cổng 3306 không bị tác động.
- **Collection SHA-256 trước khi điền fixture/đổi cổng:** `6355608c76819e1976f5b7dfd1b0ef5ae5b250c41c90fb266e058fbce4faa1bf`.

### Fixture thật cho ba ca trước đây BLOCKED

1. Khởi động auth-service trên MySQL mới với `jwt.access-token-expiration-ms=1000` và `jwt.refresh-token-expiration-ms=1000`. Đăng ký/đăng nhập tài khoản fixture qua gateway, giữ nguyên access/refresh token nhận được rồi chờ quá TTL.
2. Dừng auth instance TTL ngắn và khởi động lại với TTL mặc định trên cùng database. Điền hai token đã hết hạn vào `expiredAccessToken` và `expiredRefreshToken`; AUTH-05.5/AUTH-03.8 đều trả 401.
3. Đăng ký/đăng nhập tài khoản fixture khác bằng API, giữ JWT còn hạn, rồi xóa đúng user fixture đó bằng SQL trong database riêng (khóa ngoại xóa các bản ghi liên quan). Điền JWT vào `deletedUserToken`; AUTH-07.10 trả 404. Không xóa tài khoản trên database dùng chung.

### Lệnh và bằng chứng

Newman được cài riêng bằng pnpm trong `target/phone-tools` (phiên bản 6.2.2). Bản runtime của collection chỉ đổi bốn biến đã nêu, không đổi request, kỳ vọng hoặc test script:

```bash
pnpm --dir target/phone-tools exec newman run   target/phone-acceptance/20261007-154839/runtime-collection.json   --reporters cli,json   --reporter-json-export target/phone-acceptance/20261007-154839/newman-report.json   --timeout-request 15000
```

Báo cáo gốc và bản tóm tắt đã bỏ payload nằm ở thư mục `target/phone-acceptance/20261007-154839/`
trên máy chạy. Không commit report gốc hoặc runtime collection vì có token. Cách chạy trên Docker
với collection gốc: [hướng dẫn auth](../../postman/auth.md#chạy-bằng-newman); phải cung cấp ba fixture
trên nếu muốn chạy đủ mọi ca. Không có fixture thì ghi BLOCKED, không tính PASS.

## HTTP thực tế theo mã ca

Dấu `→` thể hiện thứ tự các request mang cùng mã ca, gồm cả chuẩn bị/đọc lại/dọn dữ liệu nếu có.
Số assertion tính tất cả request của mã đó. Kế hoạch: [auth.md](../auth.md).

| Mã ca | Commit/môi trường | HTTP thực tế | Trạng thái | Bằng chứng |
|---|---|---|---|---|
| AUTH-01.1 | `ee88921` / N1 | 201 | PASS | 3 assertion đạt; Đăng ký hợp lệ, không token |
| AUTH-01.2 | `ee88921` / N1 | 201 | PASS | 3 assertion đạt; Token admin không cấp quyền cho người đăng ký |
| AUTH-01.3 | `ee88921` / N1 | 409 | PASS | 3 assertion đạt; Trùng email |
| AUTH-01.4 | `ee88921` / N1 | 400 | PASS | 2 assertion đạt; Email sai định dạng |
| AUTH-01.5 | `ee88921` / N1 | 400 | PASS | 2 assertion đạt; Mật khẩu ngắn |
| AUTH-01.6 | `ee88921` / N1 | 400 | PASS | 2 assertion đạt; Thiếu tên |
| AUTH-01.7 | `ee88921` / N1 | 409 | PASS | 2 assertion đạt; Chuẩn hóa email |
| AUTH-01.8 | `ee88921` / N1 | 400 | PASS | 2 assertion đạt; JSON hỏng |
| AUTH-02.1 | `ee88921` / N1 | 200 | PASS | 3 assertion đạt; Login không token |
| AUTH-02.2 | `ee88921` / N1 | 200 | PASS | 3 assertion đạt; Login admin seed |
| AUTH-02.3 | `ee88921` / N1 | 401 | PASS | 2 assertion đạt; Không tồn tại tài khoản |
| AUTH-02.4 | `ee88921` / N1 | 401 | PASS | 3 assertion đạt; Sai mật khẩu |
| AUTH-02.5 | `ee88921` / N1 | 400 | PASS | 2 assertion đạt; Thiếu mật khẩu |
| AUTH-02.6 | `ee88921` / N1 | 400 | PASS | 2 assertion đạt; Email sai định dạng |
| AUTH-02.7 | `ee88921` / N1 | 200 | PASS | 3 assertion đạt; Vai trò không bị token gửi kèm chi phối |
| AUTH-02.8 | `ee88921` / N1 | 200 → 200 → 200 → 200 | PASS | 9 assertion đạt; Sau cấp quyền |
| AUTH-03.1 | `ee88921` / N1 | 200 → 200 | PASS | 5 assertion đạt; Refresh hợp lệ, không access token |
| AUTH-03.2 | `ee88921` / N1 | 401 | PASS | 2 assertion đạt; Dùng lại refresh đã rotate |
| AUTH-03.3 | `ee88921` / N1 | 401 | PASS | 2 assertion đạt; Refresh không tồn tại |
| AUTH-03.4 | `ee88921` / N1 | 200 → 200 → 401 | PASS | 6 assertion đạt; Refresh bị thu hồi |
| AUTH-03.5 | `ee88921` / N1 | 400 | PASS | 2 assertion đạt; Body thiếu trường |
| AUTH-03.6 | `ee88921` / N1 | 400 | PASS | 2 assertion đạt; Chuỗi trống |
| AUTH-03.7 | `ee88921` / N1 | 200 → 200 → 200 → 200 → 200 | PASS | 11 assertion đạt; Quyền trong DB thay đổi |
| AUTH-03.8 | `ee88921` / N1 | 401 | PASS | 2 assertion đạt; Refresh token thật đã quá TTL 1 giây, không sửa chuỗi token. |
| AUTH-03.9 | `ee88921` / N1 | 401 | PASS | 2 assertion đạt; Access token không thay thế refresh token |
| AUTH-04.1 | `ee88921` / N1 | 200 → 200 → 401 | PASS | 6 assertion đạt; Logout bằng refresh hợp lệ |
| AUTH-04.2 | `ee88921` / N1 | 200 | PASS | 2 assertion đạt; Gọi lại logout |
| AUTH-04.3 | `ee88921` / N1 | 200 | PASS | 2 assertion đạt; Refresh không tồn tại |
| AUTH-04.4 | `ee88921` / N1 | 200 | PASS | 2 assertion đạt; Không body |
| AUTH-04.5 | `ee88921` / N1 | 200 | PASS | 2 assertion đạt; Refresh trống |
| AUTH-04.6 | `ee88921` / N1 | 400 | PASS | 2 assertion đạt; JSON sai cú pháp |
| AUTH-04.7 | `ee88921` / N1 | 200 | PASS | 2 assertion đạt; Access token còn hạn sau logout |
| AUTH-04.8 | `ee88921` / N1 | 200 → 200 → 200 → 200 | PASS | 8 assertion đạt; Phiên khác vẫn hoạt động |
| AUTH-05.1 | `ee88921` / N1 | 200 | PASS | 3 assertion đạt; Đúng danh tính |
| AUTH-05.2 | `ee88921` / N1 | 401 | PASS | 2 assertion đạt; Không token |
| AUTH-05.3 | `ee88921` / N1 | 200 | PASS | 3 assertion đạt; Giảng viên cũng được đọc chính mình |
| AUTH-05.4 | `ee88921` / N1 | 401 | PASS | 2 assertion đạt; Token không hợp lệ |
| AUTH-05.5 | `ee88921` / N1 | 401 | PASS | 2 assertion đạt; Access token thật đã quá exp, phát bởi auth-service TTL 1 giây. |
| AUTH-05.6 | `ee88921` / N1 | 200 | PASS | 3 assertion đạt; Giả danh qua query |
| AUTH-05.7 | `ee88921` / N1 | 200 | PASS | 3 assertion đạt; Không lộ dữ liệu nhạy cảm |
| AUTH-06.1 | `ee88921` / N1 | 200 → 200 | PASS | 5 assertion đạt; Cấp giảng viên |
| AUTH-06.2 | `ee88921` / N1 | 401 | PASS | 2 assertion đạt; Không token |
| AUTH-06.3 | `ee88921` / N1 | 403 | PASS | 2 assertion đạt; Học viên tự nâng quyền |
| AUTH-06.4 | `ee88921` / N1 | 403 | PASS | 2 assertion đạt; Giảng viên cấp quyền |
| AUTH-06.5 | `ee88921` / N1 | 404 | PASS | 2 assertion đạt; User không tồn tại |
| AUTH-06.6 | `ee88921` / N1 | 400 | PASS | 2 assertion đạt; ID sai kiểu |
| AUTH-06.7 | `ee88921` / N1 | 400 → 400 → 400 → 400 | PASS | 8 assertion đạt; Roles rỗng/thiếu/null |
| AUTH-06.8 | `ee88921` / N1 | 400 | PASS | 2 assertion đạt; Mã vai trò không tồn tại |
| AUTH-06.9 | `ee88921` / N1 | 200 → 200 → 403 | PASS | 7 assertion đạt; Gỡ instructor |
| AUTH-06.10 | `ee88921` / N1 | 422 | PASS | 2 assertion đạt; Admin tự hạ quyền |
| AUTH-06.11 | `ee88921` / N1 | 200 | PASS | 2 assertion đạt; Admin cập nhật chính mình, giữ ADMIN |
| AUTH-06.12 | `ee88921` / N1 | 201 → 201 → 201 → 200 → 200 → 200 | PASS | 12 assertion đạt; JWT cũ sau gỡ quyền |
| AUTH-07.1 | `ee88921` / N1 | 200 → 200 | PASS | 6 assertion đạt; Sửa hồ sơ hợp lệ |
| AUTH-07.2 | `ee88921` / N1 | 401 | PASS | 2 assertion đạt; Chưa đăng nhập |
| AUTH-07.3 | `ee88921` / N1 | 401 | PASS | 2 assertion đạt; Token hỏng |
| AUTH-07.4 | `ee88921` / N1 | 400 → 400 | PASS | 6 assertion đạt; Thiếu hoặc trắng họ tên |
| AUTH-07.5 | `ee88921` / N1 | 400 | PASS | 3 assertion đạt; Họ tên quá dài |
| AUTH-07.6 | `ee88921` / N1 | 400 | PASS | 3 assertion đạt; Số điện thoại quá dài |
| AUTH-07.7 | `ee88921` / N1 | 200 → 200 → 200 → 200 → 200 → 200 → 200 → 200 | PASS | 24 assertion đạt; Xóa số điện thoại |
| AUTH-07.8 | `ee88921` / N1 | 200 | PASS | 3 assertion đạt; Giả danh và nâng quyền qua body |
| AUTH-07.9 | `ee88921` / N1 | 200 | PASS | 3 assertion đạt; Chuẩn hóa khoảng trắng |
| AUTH-07.10 | `ee88921` / N1 | 404 | PASS | 3 assertion đạt; JWT còn hạn của user fixture đã bị xóa khỏi MySQL riêng. |
| AUTH-07.11 | `ee88921` / N1 | 200 → 200 → 200 → 200 → 200 | PASS | 15 assertion đạt; Số điện thoại hợp lệ, kiểm biên |
| AUTH-07.12 | `ee88921` / N1 | 200 → 400 → 400 → 400 → 400 → 400 → 400 → 200 | PASS | 24 assertion đạt; Ký tự sai định dạng |
| AUTH-07.13 | `ee88921` / N1 | 400 → 200 | PASS | 6 assertion đạt; Số điện thoại quá ngắn |
| AUTH-08.1 | `ee88921` / N1 | 200 → 401 → 200 | PASS | 6 assertion đạt; Đổi mật khẩu hợp lệ |
| AUTH-08.2 | `ee88921` / N1 | 401 → 401 → 200 | PASS | 6 assertion đạt; Thu hồi tất cả phiên |
| AUTH-08.3 | `ee88921` / N1 | 200 → 200 | PASS | 4 assertion đạt; Không ảnh hưởng người khác |
| AUTH-08.4 | `ee88921` / N1 | 400 → 200 → 200 | PASS | 7 assertion đạt; Sai mật khẩu hiện tại |
| AUTH-08.5 | `ee88921` / N1 | 400 → 400 | PASS | 6 assertion đạt; Thiếu/trắng mật khẩu hiện tại |
| AUTH-08.6 | `ee88921` / N1 | 400 → 400 | PASS | 6 assertion đạt; Thiếu/trắng mật khẩu mới |
| AUTH-08.7 | `ee88921` / N1 | 400 → 400 | PASS | 6 assertion đạt; Mật khẩu mới ngoài giới hạn |
| AUTH-08.8 | `ee88921` / N1 | 401 | PASS | 2 assertion đạt; Chưa đăng nhập |
| AUTH-08.9 | `ee88921` / N1 | 401 | PASS | 2 assertion đạt; Token hỏng |
| AUTH-08.10 | `ee88921` / N1 | 200 → 200 → 200 | PASS | 6 assertion đạt; Giả danh qua userId |
| AUTH-08.11 | `ee88921` / N1 | 200 | PASS | 2 assertion đạt; Access token đã phát |

## Lịch sử trước lượt N1

Lượt review PR #57 tại `4e6ee7d` trên bản gộp `main + #56 + #57` đã chạy 130/133 request,
294/294 assertion PASS, tương ứng 70 PASS và 3 BLOCKED do thiếu fixture. Nguồn:
[review](https://github.com/ariushieu/e-learning-microservices/pull/57#pullrequestreview-5438638977)
và [comment chạy độc lập](https://github.com/ariushieu/e-learning-microservices/pull/57#issuecomment-6033297978).
Ba ca đó đã được chạy thật trong N1 ở trên. Kết quả giao diện 13/13 trong review là kết quả
của PR #57; lượt N1 không chạy lại trình duyệt.
