# Biên bản kiểm thử auth-service

## Tổng quan quản trị và hoàn thiện #70 — 08/10/2026

Xem [biên bản tổng quan quản trị](auth-overview.md): HTTP thực tế của toàn bộ collection,
kết quả Docker CI, 54 kiểm tra trình duyệt ở 1366/768/375px và ảnh giao diện.
Kết quả native: 291 request, 714/714 assertion PASS với ba fixture thật; tách riêng kết quả
Docker không có fixture đặc biệt để không ghi nhầm các ca BLOCKED thành PASS.

## N3 — Lỗi phone trên form đăng ký và giới hạn 30 ký tự

- **Thời gian:** 2026-10-07 22:02:15–22:02:44 (UTC+7).
- **Frontend đã chạy:** `3f01ef9`, đã đồng bộ `main e92c686`; `pnpm --dir frontend build` và `pnpm --dir frontend lint` đều thành công. Bản production chạy tại `http://127.0.0.1:3100`.
- **Backend thật:** dùng JAR auth/gateway đã build ở N2 (mã nguồn hai module không đổi), MySQL 8.0.43 với datadir mới riêng trên 13317, Flyway V1–V4; frontend gọi gateway 18080, xác thực bật. Rate limit chỉ tắt ở gateway test; tất cả tiến trình test đã dừng. Đây là lượt native, không phải Docker.
- **Trình duyệt:** Playwright + Microsoft Edge, headless, viewport 1366×900 và 375×900. Không mock API. **10/10 ca PASS** (5 ca dưới đây × 2 viewport), không lỗi JavaScript, không tràn ngang ở các trạng thái được kiểm tra.
- **Bằng chứng:** [kết quả từng ca](auth-ui/phone-results.json), [ảnh desktop](auth-ui/register-phone-error-1366.png), [ảnh mobile](auth-ui/register-phone-error-375.png).

| Ca giao diện | Thao tác | Kết quả thực tế |
|---|---|---|
| Đăng ký với `abc` | Điền đủ form và bấm Tạo tài khoản | Vẫn ở `/register`; lỗi tiếng Việt hiện ngay dưới ô phone; `aria-invalid=true` |
| Đăng ký với `+++++++++` | Gửi lại form đủ trường | Hiện cùng lỗi dưới ô phone |
| Đăng ký với `  12345678` | Gửi lại form đủ trường | Hiện cùng lỗi dưới ô phone |
| Đăng ký với số dài 30 ký tự | Gõ `+1 2 3 4 5 6 7 8 9 0 1 2 3 4 5` bằng bàn phím, sửa form lỗi rồi gửi | Không bị cắt ở ký tự 20; đăng ký/đăng nhập thành công, chuyển `/profile`; số đã lưu đúng |
| Cập nhật hồ sơ với số dài 30 ký tự | Gõ `+9 8 7 6 5 4 3 2 1 0 9 8 7 6 5`, Lưu thông tin rồi tải lại | `maxLength=30`, có toast thành công; tải lại vẫn giữ đủ số |

Thông báo quan sát được: “Số điện thoại phải có 9–15 chữ số, có thể bắt đầu bằng + và cách nhau bằng một khoảng trắng”.
N3 bổ sung kiểm thử web cho lỗi reviewer phát hiện ở `5e64e99`; không coi collection N2 là bằng chứng kiểm thử giao diện.

## N2 — Đếm chữ số và dùng chung ràng buộc đăng ký/hồ sơ sau review PR #65

- **Thời gian:** 2026-10-07T16:46:35.789343+07:00 đến 2026-10-07T16:47:48.754234+07:00.
- **Code đã chạy:** `4005b8f5254a9def2b601cf73984bf07c438b1bd`, đã đồng bộ `main 34baba3`. Lượt N2 kiểm lại quy tắc mới; kết quả N1 bên dưới chỉ là lịch sử của regex cũ.
- **Maven:** `mvnw.cmd -B -ntp verify` toàn dự án thành công: 766 test, 0 failure, 0 error, 0 skipped. Riêng auth-service: 108 test, không bỏ qua; controller integration hồ sơ/đăng ký/mật khẩu: 76 test. Các JAR gateway/auth/course/quiz được build từ lượt này.
- **Newman 6.2.2:** **205/205 request, 504/504 assertion PASS**, **80/80 mã ca PASS**, 0 FAIL, 0 BLOCKED, không request bị bỏ qua. Có 18 request chuẩn bị/khôi phục ngoài các mã ca.
- **Môi trường:** Windows/Java 17, MySQL 8.0.43 thật ở `127.0.0.1:13317`, datadir riêng mới. Flyway chạy đủ V1–V4 của auth, Hibernate `ddl-auto=validate` thành công. V4 mở rộng `users.phone` từ 20 lên 30 ký tự; số 15 chữ số có `+` và 14 dấu cách được lưu/đọc lại qua cả hai API.
- **Đường đi:** mọi request Newman qua gateway `http://127.0.0.1:18080`, JWT bật. Dùng cổng 18080 vì 8080 có tiến trình khác; MySQL dev 3306 không bị tác động. Máy không có Docker, nên chạy các JAR native; không ghi nhận lượt này là Docker, smoke test hoặc kiểm thử giao diện.
- **Phạm vi:** gateway/auth/course/quiz; Kafka consumer/outbox worker tắt cho tiến trình test, không khởi động Redis/enrollment/notification. Rate limit chỉ tắt bằng tham số của gateway test; toàn bộ tiến trình test đã dừng sau lượt chạy, cấu hình rate limit trong Git vẫn bật.
- **Nguồn kết quả:** HTTP/assertion lấy trực tiếp từ `run.executions` của JSON Newman. [Bằng chứng đã bỏ payload/token](auth-phone-evidence.json) chứa mã HTTP và số assertion của từng request.
- **Collection SHA-256:** `9026d7cabfc2d5043769823cb35160073d04e0b96f069cf097f16686dfa2e159` trước khi điền fixture/đổi cổng. Bản runtime chỉ thay bốn biến `baseUrl`, `expiredAccessToken`, `expiredRefreshToken`, `deletedUserToken`; giữ nguyên request và test script.

### Các ca reviewer yêu cầu sửa

| Đầu vào | API | HTTP thực tế | Kết quả |
|---|---|---|---|
| `+++++++++` | PUT /api/auth/me; POST /api/auth/register | 400; 400 | PASS; VALIDATION_FAILED, fieldErrors.phone tiếng Việt |
| `  12345678` | PUT /api/auth/me; POST /api/auth/register | 400; 400 | PASS; khoảng trắng không bù thiếu chữ số |
| `abc` | PUT /api/auth/me; POST /api/auth/register | 400; 400 | PASS; không lưu hồ sơ/không tạo tài khoản |
| `0912  345678` và dấu `+` sai vị trí | PUT /api/auth/me; POST /api/auth/register | 400; 400 | PASS |
| `0912 345 678`, `+84 912 345 678`, đủ 15 chữ số với dấu cách | PUT /api/auth/me; POST /api/auth/register | 200; 201 | PASS; GET sau lưu trả đúng số đã trim |
| null/rỗng/toàn dấu cách | PUT /api/auth/me; POST /api/auth/register | 200; 201 | PASS; phone=null |

### Fixture và khả năng chạy lại

Đầu lượt chạy, MySQL ở schema V3 với `phone VARCHAR(20)` và một số điện thoại đã lưu.
Sau khi khởi động auth với V4, truy vấn `information_schema.columns` xác nhận độ dài 30;
truy vấn lại dữ liệu xác nhận số cũ không đổi (`migrationUpgradePreservesPhone=true` trong bằng chứng).

AUTH-03.8, AUTH-05.5 và AUTH-07.10 đã chạy thật trong **N2** vì có fixture:

1. Auth-service TTL access/refresh 1 giây phát token qua login ở gateway; chờ quá TTL, dừng instance TTL ngắn rồi chạy lại auth với TTL mặc định trên cùng database. Dùng nguyên token đã phát: refresh/access hết hạn đều trả 401.
2. Tạo và đăng nhập tài khoản fixture riêng, giữ JWT còn hạn, xóa đúng tài khoản đó trong database test rồi PUT /me trả 404. Không xóa tài khoản trên database chung.

Collection trong Git cố ý không chứa token. Khi reviewer chạy không điền ba fixture, **202 request** được thực thi, ba ca trên phải ghi **BLOCKED**, không tính PASS. Đây là khác biệt môi trường, không phải thay đổi kỳ vọng.

Lệnh Newman đã chạy (cài Newman 6.2.2 riêng tại `target/phone-tools`):

```bash
pnpm --dir target/phone-tools exec newman run target/phone-acceptance/20261007-164635/runtime-collection.json --reporters cli,json --reporter-json-export target/phone-acceptance/20261007-164635/newman-report.json --timeout-request 15000
```

Report thô/runtime collection giữ dưới `target/phone-acceptance/20261007-164635/` trên máy chạy vì có token. Để chạy trên Docker qua 8080, làm theo [hướng dẫn auth](../../postman/auth.md#chạy-bằng-newman) và cung cấp fixture riêng nếu muốn đủ 205 request.

## HTTP thực tế theo mã ca — N2

Dấu `→` thể hiện thứ tự các request cùng mã ca, kể cả chuẩn bị và đọc lại. Kế hoạch: [auth.md](../auth.md).

| Mã ca | Commit/môi trường | HTTP thực tế | Trạng thái | Bằng chứng |
|---|---|---|---|---|
| AUTH-01.1 | `4005b8f` / N2 | 201 | PASS | 3 assertion đạt; Đăng ký hợp lệ, không token |
| AUTH-01.2 | `4005b8f` / N2 | 201 | PASS | 3 assertion đạt; Token admin không cấp quyền cho người đăng ký |
| AUTH-01.3 | `4005b8f` / N2 | 409 | PASS | 3 assertion đạt; Trùng email |
| AUTH-01.4 | `4005b8f` / N2 | 400 | PASS | 2 assertion đạt; Email sai định dạng |
| AUTH-01.5 | `4005b8f` / N2 | 400 | PASS | 2 assertion đạt; Mật khẩu ngắn |
| AUTH-01.6 | `4005b8f` / N2 | 400 | PASS | 2 assertion đạt; Thiếu tên |
| AUTH-01.7 | `4005b8f` / N2 | 409 | PASS | 2 assertion đạt; Chuẩn hóa email |
| AUTH-01.8 | `4005b8f` / N2 | 400 | PASS | 2 assertion đạt; JSON hỏng |
| AUTH-01.9 | `4005b8f` / N2 | 400 → 400 → 400 → 400 → 400 → 400 → 400 → 400 → 400 → 400 → 401 | PASS | 32 assertion đạt; Đăng ký với phone sai |
| AUTH-01.10 | `4005b8f` / N2 | 201 → 200 → 200 → 201 → 200 → 200 → 201 → 200 → 200 → 201 → 200 → 200 → 201 → 200 → 200 → 201 → 200 → 200 | PASS | 48 assertion đạt; Phone hợp lệ khi đăng ký |
| AUTH-01.11 | `4005b8f` / N2 | 201 → 200 → 200 → 201 → 200 → 200 → 201 → 200 → 200 | PASS | 24 assertion đạt; Phone tùy chọn khi đăng ký |
| AUTH-02.1 | `4005b8f` / N2 | 200 | PASS | 3 assertion đạt; Login không token |
| AUTH-02.2 | `4005b8f` / N2 | 200 | PASS | 3 assertion đạt; Login admin seed |
| AUTH-02.3 | `4005b8f` / N2 | 401 | PASS | 2 assertion đạt; Không tồn tại tài khoản |
| AUTH-02.4 | `4005b8f` / N2 | 401 | PASS | 3 assertion đạt; Sai mật khẩu |
| AUTH-02.5 | `4005b8f` / N2 | 400 | PASS | 2 assertion đạt; Thiếu mật khẩu |
| AUTH-02.6 | `4005b8f` / N2 | 400 | PASS | 2 assertion đạt; Email sai định dạng |
| AUTH-02.7 | `4005b8f` / N2 | 200 | PASS | 3 assertion đạt; Vai trò không bị token gửi kèm chi phối |
| AUTH-02.8 | `4005b8f` / N2 | 200 → 200 → 200 → 200 | PASS | 9 assertion đạt; Sau cấp quyền |
| AUTH-03.1 | `4005b8f` / N2 | 200 → 200 | PASS | 5 assertion đạt; Refresh hợp lệ, không access token |
| AUTH-03.2 | `4005b8f` / N2 | 401 | PASS | 2 assertion đạt; Dùng lại refresh đã rotate |
| AUTH-03.3 | `4005b8f` / N2 | 401 | PASS | 2 assertion đạt; Refresh không tồn tại |
| AUTH-03.4 | `4005b8f` / N2 | 200 → 200 → 401 | PASS | 6 assertion đạt; Refresh bị thu hồi |
| AUTH-03.5 | `4005b8f` / N2 | 400 | PASS | 2 assertion đạt; Body thiếu trường |
| AUTH-03.6 | `4005b8f` / N2 | 400 | PASS | 2 assertion đạt; Chuỗi trống |
| AUTH-03.7 | `4005b8f` / N2 | 200 → 200 → 200 → 200 → 200 | PASS | 11 assertion đạt; Quyền trong DB thay đổi |
| AUTH-03.8 | `4005b8f` / N2 | 401 | PASS | 2 assertion đạt; Refresh hết hạn thật |
| AUTH-03.9 | `4005b8f` / N2 | 401 | PASS | 2 assertion đạt; Access token không thay thế refresh token |
| AUTH-04.1 | `4005b8f` / N2 | 200 → 200 → 401 | PASS | 6 assertion đạt; Logout bằng refresh hợp lệ |
| AUTH-04.2 | `4005b8f` / N2 | 200 | PASS | 2 assertion đạt; Gọi lại logout |
| AUTH-04.3 | `4005b8f` / N2 | 200 | PASS | 2 assertion đạt; Refresh không tồn tại |
| AUTH-04.4 | `4005b8f` / N2 | 200 | PASS | 2 assertion đạt; Không body |
| AUTH-04.5 | `4005b8f` / N2 | 200 | PASS | 2 assertion đạt; Refresh trống |
| AUTH-04.6 | `4005b8f` / N2 | 400 | PASS | 2 assertion đạt; JSON sai cú pháp |
| AUTH-04.7 | `4005b8f` / N2 | 200 | PASS | 2 assertion đạt; Access token còn hạn sau logout |
| AUTH-04.8 | `4005b8f` / N2 | 200 → 200 → 200 → 200 | PASS | 8 assertion đạt; Phiên khác vẫn hoạt động |
| AUTH-05.1 | `4005b8f` / N2 | 200 | PASS | 3 assertion đạt; Đúng danh tính |
| AUTH-05.2 | `4005b8f` / N2 | 401 | PASS | 2 assertion đạt; Không token |
| AUTH-05.3 | `4005b8f` / N2 | 200 | PASS | 3 assertion đạt; Giảng viên cũng được đọc chính mình |
| AUTH-05.4 | `4005b8f` / N2 | 401 | PASS | 2 assertion đạt; Token không hợp lệ |
| AUTH-05.5 | `4005b8f` / N2 | 401 | PASS | 2 assertion đạt; Token hết hạn |
| AUTH-05.6 | `4005b8f` / N2 | 200 | PASS | 3 assertion đạt; Giả danh qua query |
| AUTH-05.7 | `4005b8f` / N2 | 200 | PASS | 3 assertion đạt; Không lộ dữ liệu nhạy cảm |
| AUTH-06.1 | `4005b8f` / N2 | 200 → 200 | PASS | 5 assertion đạt; Cấp giảng viên |
| AUTH-06.2 | `4005b8f` / N2 | 401 | PASS | 2 assertion đạt; Không token |
| AUTH-06.3 | `4005b8f` / N2 | 403 | PASS | 2 assertion đạt; Học viên tự nâng quyền |
| AUTH-06.4 | `4005b8f` / N2 | 403 | PASS | 2 assertion đạt; Giảng viên cấp quyền |
| AUTH-06.5 | `4005b8f` / N2 | 404 | PASS | 2 assertion đạt; User không tồn tại |
| AUTH-06.6 | `4005b8f` / N2 | 400 | PASS | 2 assertion đạt; ID sai kiểu |
| AUTH-06.7 | `4005b8f` / N2 | 400 → 400 → 400 → 400 | PASS | 8 assertion đạt; Roles rỗng/thiếu/null |
| AUTH-06.8 | `4005b8f` / N2 | 400 | PASS | 2 assertion đạt; Mã vai trò không tồn tại |
| AUTH-06.9 | `4005b8f` / N2 | 200 → 200 → 403 | PASS | 7 assertion đạt; Gỡ instructor |
| AUTH-06.10 | `4005b8f` / N2 | 422 | PASS | 2 assertion đạt; Admin tự hạ quyền |
| AUTH-06.11 | `4005b8f` / N2 | 200 | PASS | 2 assertion đạt; Admin cập nhật chính mình, giữ ADMIN |
| AUTH-06.12 | `4005b8f` / N2 | 201 → 201 → 201 → 200 → 200 → 200 | PASS | 12 assertion đạt; JWT cũ sau gỡ quyền |
| AUTH-07.1 | `4005b8f` / N2 | 200 → 200 | PASS | 6 assertion đạt; Sửa hồ sơ hợp lệ |
| AUTH-07.2 | `4005b8f` / N2 | 401 | PASS | 2 assertion đạt; Chưa đăng nhập |
| AUTH-07.3 | `4005b8f` / N2 | 401 | PASS | 2 assertion đạt; Token hỏng |
| AUTH-07.4 | `4005b8f` / N2 | 400 → 400 | PASS | 6 assertion đạt; Thiếu hoặc trắng họ tên |
| AUTH-07.5 | `4005b8f` / N2 | 400 | PASS | 3 assertion đạt; Họ tên quá dài |
| AUTH-07.6 | `4005b8f` / N2 | 400 | PASS | 3 assertion đạt; Số điện thoại quá dài |
| AUTH-07.7 | `4005b8f` / N2 | 200 → 200 → 200 → 200 → 200 → 200 → 200 → 200 | PASS | 24 assertion đạt; Xóa số điện thoại |
| AUTH-07.8 | `4005b8f` / N2 | 200 | PASS | 3 assertion đạt; Giả danh và nâng quyền qua body |
| AUTH-07.9 | `4005b8f` / N2 | 200 | PASS | 3 assertion đạt; Chuẩn hóa khoảng trắng |
| AUTH-07.10 | `4005b8f` / N2 | 404 | PASS | 3 assertion đạt; Người gọi không còn tồn tại |
| AUTH-07.11 | `4005b8f` / N2 | 200 → 200 → 200 → 200 → 200 → 200 → 200 → 200 → 200 | PASS | 27 assertion đạt; Số điện thoại hợp lệ, kiểm biên |
| AUTH-07.12 | `4005b8f` / N2 | 200 → 400 → 400 → 400 → 400 → 400 → 400 → 200 | PASS | 24 assertion đạt; Ký tự sai định dạng |
| AUTH-07.13 | `4005b8f` / N2 | 400 → 200 → 400 → 200 | PASS | 12 assertion đạt; Số điện thoại quá ngắn |
| AUTH-07.14 | `4005b8f` / N2 | 400 → 400 → 400 → 400 → 400 → 400 → 400 → 200 | PASS | 24 assertion đạt; Dấu cộng/khoảng trắng sai vị trí |
| AUTH-08.1 | `4005b8f` / N2 | 200 → 401 → 200 | PASS | 6 assertion đạt; Đổi mật khẩu hợp lệ |
| AUTH-08.2 | `4005b8f` / N2 | 401 → 401 → 200 | PASS | 6 assertion đạt; Thu hồi tất cả phiên |
| AUTH-08.3 | `4005b8f` / N2 | 200 → 200 | PASS | 4 assertion đạt; Không ảnh hưởng người khác |
| AUTH-08.4 | `4005b8f` / N2 | 400 → 200 → 200 | PASS | 7 assertion đạt; Sai mật khẩu hiện tại |
| AUTH-08.5 | `4005b8f` / N2 | 400 → 400 | PASS | 6 assertion đạt; Thiếu/trắng mật khẩu hiện tại |
| AUTH-08.6 | `4005b8f` / N2 | 400 → 400 | PASS | 6 assertion đạt; Thiếu/trắng mật khẩu mới |
| AUTH-08.7 | `4005b8f` / N2 | 400 → 400 | PASS | 6 assertion đạt; Mật khẩu mới ngoài giới hạn |
| AUTH-08.8 | `4005b8f` / N2 | 401 | PASS | 2 assertion đạt; Chưa đăng nhập |
| AUTH-08.9 | `4005b8f` / N2 | 401 | PASS | 2 assertion đạt; Token hỏng |
| AUTH-08.10 | `4005b8f` / N2 | 200 → 200 → 200 | PASS | 6 assertion đạt; Giả danh qua userId |
| AUTH-08.11 | `4005b8f` / N2 | 200 | PASS | 2 assertion đạt; Access token đã phát |

## Lịch sử

- N1 (`ee88921`, báo cáo tại `46a02ab`): 153/153 request và 358/358 assertion PASS trên MySQL/gateway native, đủ ba fixture. Regex lúc đó đếm ký tự và chỉ áp dụng cho cập nhật; **không chứng minh các ca phát sinh sau review đạt**.
- Reviewer chạy Docker/MySQL tại `46a02ab`: 150 request, 351/351 assertion PASS, ba ca BLOCKED vì thiếu fixture; thử tay phát hiện `+++++++++`, `  12345678` và đăng ký `abc` vẫn được lưu. N2 bổ sung chính các ca này và sửa cả hai API.
- Review PR #57 trước đó: 130/133 request, 294/294 assertion PASS, ba ca BLOCKED; kiểm thử giao diện 13/13 là kết quả của reviewer, không phải lượt N2. [Review](https://github.com/ariushieu/e-learning-microservices/pull/57#pullrequestreview-5438638977).

## Email so khớp chính xác (AUTH-12)

Xem [biên bản email ngày 08/10/2026](auth-email.md): kết quả API, migration MySQL thật,
Edge 375/768/1366px và bằng chứng Docker CI. Kết quả cũ bên trên giữ nguyên theo commit đã chạy.

## Quản lý phiên đăng nhập (AUTH-13)

Xem [biên bản phiên đăng nhập ngày 08/10/2026](auth-sessions.md): HTTP thực tế từng ca,
collection MySQL/Docker, 33 kiểm tra Edge và ảnh. Các biên bản cũ giữ nguyên theo commit đã chạy.
