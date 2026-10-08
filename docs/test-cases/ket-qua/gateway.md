# Biên bản gateway — 07/10/2026, chạy lại G-RATE ngày 08/10/2026

Bản đã kiểm: `main 1873f06` cùng nhánh `test/gateway-notification-collections` (chỉ thêm collection,
biên bản và bản sửa `readAt` của notification-service — không đụng gateway).
Collection: [`docs/postman/gateway.postman_collection.json`](../../postman/gateway.postman_collection.json).

## Môi trường

- Docker Compose trên Windows 11 (`docker compose --profile app up -d --build --wait`): MySQL 8,
  Kafka KRaft, Redis 7, 5 service, gateway, frontend. JWT bật. Mọi request đi qua
  `http://localhost:8080`.
- Newman 6.2.2 qua `pnpm dlx newman@6.2.2`.
- Ba lượt chạy:
  1. Cả collection, `RATE_LIMIT_ENABLED=false`: 49 request, **93/93 assertion**. Thư mục G-AUTH-4 tự
     bỏ qua (`runSlowTests=false`), G-RATE tự bỏ qua vì gateway tắt giới hạn.
  2. Riêng thư mục `6. G-RATE`, giới hạn request **bật** (mặc định của Docker): 12 request,
     **30/30 assertion**, khoảng 4 phút.
  3. Cả collection với `--env-var runSlowTests=true` cho G-AUTH-4 (chờ token 15 phút hết hạn):
     51 request, **98/98 assertion**; G-AUTH-4 chờ 954 giây.
- Các ca dừng container (G-ROUTE-7/8, G-RATE-7) chạy tay bằng `docker compose stop` + `curl`, ghi
  lại ở cột bằng chứng.

**08/10/2026, nhánh `feat/gateway-login-limit-per-account`:** đăng nhập, đăng ký chuyển sang đếm
theo tài khoản, thêm xô theo IP; thêm ca G-RATE-9, 10, đổi ý nghĩa G-RATE-5. Chạy lại trên Docker
với gateway build từ nhánh này:

1. Cả collection, `RATE_LIMIT_ENABLED=false`: 49 request, **93/93 assertion**.
2. Riêng thư mục `6. G-RATE`, giới hạn bật: 14 request trong thư mục (191 request tính cả request
   gửi từ script), **37/37 assertion**, khoảng 5 phút.
3. G-RATE-7 chạy tay lại (xem bảng).

## Kết quả

**33 ca: 33 PASS.** Không FAIL, không BLOCKED.

| Mã ca | Môi trường | HTTP thực tế | Kết quả | Bằng chứng |
|---|---|---|---|---|
| G-AUTH-1 | Docker, rate limit tắt | 200 | PASS | `data.id` = studentId, không có `password`/`passwordHash` |
| G-AUTH-2 | như trên | 401 | PASS | `code=UNAUTHORIZED`, đúng vỏ lỗi chung |
| G-AUTH-3 | như trên | 401 | PASS | Bearer `abc.def.ghi` |
| G-AUTH-4 | `runSlowTests=true` | 401 | PASS | Chờ tới `exp` + 60 giây rồi gọi lại bằng đúng token đó; sau đó refresh token của S vẫn đổi được cặp mới (200) |
| G-AUTH-5 | rate limit tắt | 401, 401 | PASS | Hai ca: thêm `ROLE_ADMIN` vào `roles`, và đổi `sub` sang adminId; giữ chữ ký cũ. Script kiểm token giả đúng là đã đổi claim |
| G-AUTH-6 | như trên | 401 | PASS | `X-User-Id: adminId`, `X-User-Roles: ROLE_ADMIN`, không token |
| G-AUTH-7 | như trên | 200, 403 | PASS | Token S + header giả: `/api/auth/me` vẫn là S; `PATCH /api/users/{id}/roles` vẫn 403 — header không nâng quyền |
| G-AUTH-8 | như trên | 401 | PASS | `POST /api/courses` body hợp lệ, không token |
| G-ROUTE-1 | như trên | 200 | PASS | auth-service |
| G-ROUTE-2 | như trên | 200 | PASS | course-service, không token |
| G-ROUTE-3 | như trên | 200 | PASS | Danh sách chỉ có lượt của S |
| G-ROUTE-4 | như trên | 200 | PASS | `PUT /api/lessons/{id}/progress` tới enrollment-service, `lessonId` và `status` đúng |
| G-ROUTE-5 | như trên | 200 | PASS | `GET /api/attempts/{id}` tới quiz-service, đúng người làm, điểm 50 |
| G-ROUTE-6 | như trên | 200 | PASS | notification-service |
| G-ROUTE-7 | Docker, chạy tay | 502 ×5 | PASS | Dừng lần lượt auth/course/enrollment/quiz/notification: 502 `EXTERNAL_SERVICE_ERROR` sau 2,8 giây; trong lúc đó 4 service còn lại đều 200 |
| G-ROUTE-8 | Docker, chạy tay | 200 ×5 | PASS | `up -d --wait` lại từng service, gọi lại đúng request: 200 |
| G-ROUTE-9 | rate limit tắt | 404 | PASS | Vỏ lỗi chung, `path=/api/does-not-exist` |
| G-RATE-1 | rate limit bật | 401 | PASS | Sau 61 giây không đăng nhập bằng email S: `X-RateLimit-Remaining=54` (60 − 6) |
| G-RATE-2 | như trên | 401 ×10, 429 | PASS | 11 lần đăng nhập sai cùng email S trong dưới 6 giây; lần 11: `Retry-After=6`, `code=TOO_MANY_REQUESTS` |
| G-RATE-3 | như trên | 401 | PASS | Chờ 9 giây rồi đăng nhập sai lại. Lần chạy đầu chờ 7 giây nhận 429: đồng hồ máy ảo Docker (Redis lấy giờ từ đó) chạy chậm hơn máy thật khi đang chỉnh giờ, đo được lệch tới 12% |
| G-RATE-4 | như trên | 429 ×4 | PASS | Đổi `X-Forwarded-For` mỗi lần vẫn 429 |
| G-RATE-9 | như trên | 429 ×3 | PASS | `  QA.Student@Example.com `, `qa.studént@example.com`, `QA.STUDENT@EXAMPLE.COM` đều chung xô với email S |
| G-RATE-5 | như trên | 401, 401 | PASS | Ngay khi S hết lượt: đăng nhập sai bằng email B vẫn 401; refresh token giả 401 — không bị chặn theo S |
| G-RATE-6 | như trên | 200/429, 200 | PASS | S dồn 60 request `GET /api/notifications` cùng lúc: có 429; ngay sau đó B gọi: 200 |
| G-RATE-7 | Docker, chạy tay | 200 | PASS | 08/10: `docker compose stop redis`: `GET /api/notifications` 200 với `X-RateLimit-Remaining: -1` (0,56 giây); đăng nhập sai vẫn tới auth-service (401, 1,1 giây vì hỏi hai xô). Bật lại Redis: header về số thật (39) |
| G-RATE-8 | rate limit bật | 200 ×31 | PASS | 31 lần `/actuator/health` liền nhau đều `UP`, không 429 |
| G-RATE-10 | như trên | 401 ×60–62, 429 | PASS | 70 lần đăng nhập sai cùng lúc, mỗi lần một email: khoảng 60 lần đầu 401, còn lại 429 `Retry-After=2`; refresh giả ngay sau đó 429 |
| G-CORS-1 | rate limit tắt | 200 | PASS | `Access-Control-Allow-Origin: http://localhost:3000`, `Allow-Credentials: true` |
| G-CORS-2 | như trên | 200 | PASS | `Access-Control-Allow-Headers` có `authorization` |
| G-CORS-3 | như trên | 200 | PASS | `Access-Control-Allow-Methods` có `PATCH` |
| G-CORS-4 | như trên | 403 | PASS | Origin `https://untrusted.example`: không có `Access-Control-Allow-Origin` |
| G-CORS-5 | như trên | 403 | PASS | `TRACE` |
| G-CORS-6 | rate limit bật | 200 | PASS | Chạy ngay sau G-RATE-5 khi xô đăng nhập của S đã cạn |

## Ghi chú

- Chạy G-RATE riêng, khi gateway bật giới hạn: phần "0. Chuẩn bị" gửi 10 lần đăng nhập, đăng ký
  của nhiều tài khoản khác nhau. Từ 08/10 mỗi tài khoản có xô riêng, nên chạy lại ngay không còn dính
  429 ở SETUP như trước (trước đây cả 10 lần chung một xô theo IP).
- Sau lượt chạy, dữ liệu QA (danh mục, khóa, quiz, lượt làm bài, thông báo) đã được xóa về trạng thái
  trước khi chạy.
