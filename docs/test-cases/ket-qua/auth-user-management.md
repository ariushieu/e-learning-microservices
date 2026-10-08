# Biên bản kiểm thử quản lý người dùng cho admin

- Mã nguồn chạy collection: `87931b8b344ab90d8f3234658dd55bd5a5156399`; giao diện: `a006eaacf35ca9686ef0329b381821faf0e3b4f9`. Giữa hai bản chỉ bổ sung độ rộng tối thiểu của cột người dùng trên mobile và liên kết biên bản; backend/collection không đổi.
- Thời gian: 2026-10-08T09:07:28.195175+07:00 → 2026-10-08T09:14:07.458323+07:00.
- Collection SHA-256: `00298f1a6f3c6a983aa7216b5bf979a38fb4c5f6e2995c7e52163bead60dc37b`.
- Windows, JDK 17, MySQL 8.0.43 riêng tại 13317; auth/course/quiz và gateway là JAR thật. Gateway 18080; frontend production 3100; Edge headless ở 1366px và 375px.
- Máy kiểm thử không có Docker. **Đây là kết quả native MySQL, không phải kết quả collection trên Docker.** Job CI “Full stack in Docker” kiểm tra stack và smoke-test riêng; không chạy collection này.
- Rate limit chỉ tắt cho tiến trình gateway kiểm thử. Không đổi cấu hình commit hoặc gateway đang có của người dùng. Toàn bộ tiến trình kiểm thử đã dừng; database riêng nằm trong target, không ảnh hưởng dữ liệu hiện hữu.
- Fixture hết hạn được cấp thật qua gateway với TTL 1 giây, chờ hết hạn rồi khởi động lại auth với TTL bình thường. Fixture tài khoản đã xóa dùng JWT đã phát trước khi xóa đúng user trong database riêng. Cả ba ca trước đây BLOCKED đều được chạy.

## Kết quả

- Newman 6.2.2: **274 request, 672/672 assertion PASS**, không lỗi script; 106 mã ca PASS, 0 FAIL, 0 BLOCKED, 0 NOT RUN.
- Giao diện thật: **28 kiểm tra PASS** (desktop/mobile), không mock API.
- Maven toàn dự án verify PASS; chạy lại auth + shared sau ca giới hạn offset cuối cùng cũng PASS. Tổng báo cáo cuối: **809 test, 0 lỗi/fail/skip** (auth 142, gồm 34 ca quản lý người dùng).
- Frontend production build và ESLint PASS.
- Nâng schema V3 → V4 trên dữ liệu thật vẫn giữ đúng số điện thoại (kiểm tra hồi quy).

## HTTP thực tế theo mã ca

Một mã ca có thể gồm nhiều request để chuẩn bị, thực hiện và đọc lại dữ liệu. Cột HTTP giữ nguyên thứ tự thực thi; chi tiết request/assertion trong [api-results.json](auth-user-management/api-results.json), đã bỏ token và body nhạy cảm.

| Mã ca | HTTP thực tế | Kết quả |
|---|---|---|
| AUTH-01.1 | 201 | PASS |
| AUTH-01.2 | 201 | PASS |
| AUTH-01.3 | 409 | PASS |
| AUTH-01.4 | 400 | PASS |
| AUTH-01.5 | 400 | PASS |
| AUTH-01.6 | 400 | PASS |
| AUTH-01.7 | 409 | PASS |
| AUTH-01.8 | 400 | PASS |
| AUTH-01.9 | 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 401 | PASS |
| AUTH-01.10 | 201, 200, 200, 201, 200, 200, 201, 200, 200, 201, 200, 200, 201, 200, 200, 201, 200, 200 | PASS |
| AUTH-01.11 | 201, 200, 200, 201, 200, 200, 201, 200, 200 | PASS |
| AUTH-02.1 | 200 | PASS |
| AUTH-02.2 | 200 | PASS |
| AUTH-02.3 | 401 | PASS |
| AUTH-02.4 | 401 | PASS |
| AUTH-02.5 | 400 | PASS |
| AUTH-02.6 | 400 | PASS |
| AUTH-02.7 | 200 | PASS |
| AUTH-02.8 | 200, 200, 200, 200 | PASS |
| AUTH-03.1 | 200, 200 | PASS |
| AUTH-03.2 | 401 | PASS |
| AUTH-03.3 | 401 | PASS |
| AUTH-03.4 | 200, 200, 401 | PASS |
| AUTH-03.5 | 400 | PASS |
| AUTH-03.6 | 400 | PASS |
| AUTH-03.7 | 200, 200, 200, 200, 200 | PASS |
| AUTH-03.8 | 401 | PASS |
| AUTH-03.9 | 401 | PASS |
| AUTH-04.1 | 200, 200, 401 | PASS |
| AUTH-04.2 | 200 | PASS |
| AUTH-04.3 | 200 | PASS |
| AUTH-04.4 | 200 | PASS |
| AUTH-04.5 | 200 | PASS |
| AUTH-04.6 | 400 | PASS |
| AUTH-04.7 | 200 | PASS |
| AUTH-04.8 | 200, 200, 200, 200 | PASS |
| AUTH-05.1 | 200 | PASS |
| AUTH-05.2 | 401 | PASS |
| AUTH-05.3 | 200 | PASS |
| AUTH-05.4 | 401 | PASS |
| AUTH-05.5 | 401 | PASS |
| AUTH-05.6 | 200 | PASS |
| AUTH-05.7 | 200 | PASS |
| AUTH-06.1 | 200, 200 | PASS |
| AUTH-06.2 | 401 | PASS |
| AUTH-06.3 | 403 | PASS |
| AUTH-06.4 | 403 | PASS |
| AUTH-06.5 | 404 | PASS |
| AUTH-06.6 | 400 | PASS |
| AUTH-06.7 | 400, 400, 400, 400 | PASS |
| AUTH-06.8 | 400 | PASS |
| AUTH-06.9 | 200, 200, 403 | PASS |
| AUTH-06.10 | 422 | PASS |
| AUTH-06.11 | 200 | PASS |
| AUTH-06.12 | 201, 201, 201, 200, 200, 200 | PASS |
| AUTH-07.1 | 200, 200 | PASS |
| AUTH-07.2 | 401 | PASS |
| AUTH-07.3 | 401 | PASS |
| AUTH-07.4 | 400, 400 | PASS |
| AUTH-07.5 | 400 | PASS |
| AUTH-07.6 | 400 | PASS |
| AUTH-07.7 | 200, 200, 200, 200, 200, 200, 200, 200 | PASS |
| AUTH-07.8 | 200 | PASS |
| AUTH-07.9 | 200 | PASS |
| AUTH-07.10 | 404 | PASS |
| AUTH-07.11 | 200, 200, 200, 200, 200, 200, 200, 200, 200 | PASS |
| AUTH-07.12 | 200, 400, 400, 400, 400, 400, 400, 200 | PASS |
| AUTH-07.13 | 400, 200, 400, 200 | PASS |
| AUTH-07.14 | 400, 400, 400, 400, 400, 400, 400, 200 | PASS |
| AUTH-08.1 | 200, 401, 200 | PASS |
| AUTH-08.2 | 401, 401, 200 | PASS |
| AUTH-08.3 | 200, 200 | PASS |
| AUTH-08.4 | 400, 200, 200 | PASS |
| AUTH-08.5 | 400, 400 | PASS |
| AUTH-08.6 | 400, 400 | PASS |
| AUTH-08.7 | 400, 400 | PASS |
| AUTH-08.8 | 401 | PASS |
| AUTH-08.9 | 401 | PASS |
| AUTH-08.10 | 200, 200, 200 | PASS |
| AUTH-08.11 | 200 | PASS |
| AUTH-09.1 | 200 | PASS |
| AUTH-09.2 | 200 | PASS |
| AUTH-09.3 | 200 | PASS |
| AUTH-09.4 | 200 | PASS |
| AUTH-09.5 | 200, 200 | PASS |
| AUTH-09.6 | 200, 200, 200, 200, 200, 200 | PASS |
| AUTH-09.7 | 400, 400, 400, 400, 400 | PASS |
| AUTH-09.8 | 400, 400, 400, 400, 400, 400, 400, 400 | PASS |
| AUTH-09.9 | 401, 401 | PASS |
| AUTH-09.10 | 403, 403 | PASS |
| AUTH-09.11 | 200 | PASS |
| AUTH-09.12 | 200, 200 | PASS |
| AUTH-10.1 | 200, 200 | PASS |
| AUTH-10.2 | 403 | PASS |
| AUTH-10.3 | 401, 401 | PASS |
| AUTH-10.4 | 200 | PASS |
| AUTH-10.5 | 200, 200, 401 | PASS |
| AUTH-10.6 | 200, 200, 200, 200 | PASS |
| AUTH-10.7 | 422 | PASS |
| AUTH-10.8 | 422 | PASS |
| AUTH-10.9 | 404 | PASS |
| AUTH-10.10 | 400 | PASS |
| AUTH-10.11 | 400, 400, 400, 400, 400, 400 | PASS |
| AUTH-10.12 | 403, 403 | PASS |
| AUTH-10.13 | 401, 401 | PASS |
| AUTH-10.14 | 200 | PASS |

## Giao diện trên trình duyệt

| Bề ngang | Kiểm tra | Kết quả |
|---|---|---|
| 1366px | Anonymous user redirected to login | PASS |
| 1366px | Admin searches qa.student and sees exactly one account | PASS |
| 1366px | Pagination preserves keyword, role and status | PASS |
| 1366px | Combined filters reset page and do not duplicate multi-role users | PASS |
| 1366px | No matches show empty state | PASS |
| 1366px | Role form targets selected row, preselects roles; cancel/save/reload work | PASS |
| 1366px | Cancel locking leaves account active | PASS |
| 1366px | Lock removes row from ACTIVE filter, blocks login/refresh; existing JWT still works | PASS |
| 1366px | Unlock updates filtered list; login succeeds without restoring old refresh | PASS |
| 1366px | Self and other admin cannot be locked from UI | PASS |
| 1366px | Account promoted after dialog opens: server rejects lock and error remains visible | PASS |
| 1366px | API load error is visible and retry recovers | PASS |
| 1366px | No page JavaScript errors or horizontal document overflow | PASS |
| 1366px | Student and instructor cannot open admin page | PASS |
| 375px | Anonymous user redirected to login | PASS |
| 375px | Admin searches qa.student and sees exactly one account | PASS |
| 375px | Pagination preserves keyword, role and status | PASS |
| 375px | Combined filters reset page and do not duplicate multi-role users | PASS |
| 375px | No matches show empty state | PASS |
| 375px | Role form targets selected row, preselects roles; cancel/save/reload work | PASS |
| 375px | Cancel locking leaves account active | PASS |
| 375px | Lock removes row from ACTIVE filter, blocks login/refresh; existing JWT still works | PASS |
| 375px | Unlock updates filtered list; login succeeds without restoring old refresh | PASS |
| 375px | Self and other admin cannot be locked from UI | PASS |
| 375px | Account promoted after dialog opens: server rejects lock and error remains visible | PASS |
| 375px | API load error is visible and retry recovers | PASS |
| 375px | No page JavaScript errors or horizontal document overflow | PASS |
| 375px | Student and instructor cannot open admin page | PASS |

Kiểm tra gồm tìm qa.student đúng một dòng; lọc kết hợp; phân trang giữ tham số; đổi lọc về trang đầu; cấp quyền đúng dòng với xác nhận/hủy và đọc lại; khóa/mở cập nhật bảng theo bộ lọc; chặn self/admin; lỗi 422 hiển thị ngay trong hộp thoại khi dữ liệu thay đổi; lỗi tải và thử lại; phân quyền trang; không lỗi JavaScript hoặc tràn ngang cả trang.

Ảnh thực tế: [desktop](auth-user-management/users-1366.png), [375px](auth-user-management/users-375.png), [lỗi khóa 422 trên mobile](auth-user-management/lock-error-375.png). Bảng cuộn ngang bên trong khung ở màn hình nhỏ.

## Kiểm tra giao dịch và đồng thời

`UserManagementIntegrationTest` kiểm tra rollback khi thu hồi token lỗi, và dùng latch để giữ khóa hàng trong lúc login/refresh đồng thời. Login/refresh phải chờ transaction khóa hoàn tất rồi nhận 403/401. Kiểm tra này dùng H2 trong suite Java; collection MySQL xác nhận hành vi tuần tự, thu hồi cả hai phiên, không ảnh hưởng phiên của user khác.

## Chạy lại

Theo [hướng dẫn Runner/Newman](../../postman/auth.md), chạy cả collection theo thứ tự AUTH-01 đến AUTH-10. Nếu thiếu ba fixture thật, giữ BLOCKED cho AUTH-03.8, AUTH-05.5, AUTH-07.10; không ghi PASS thay. Raw Newman report chứa token nên chỉ giữ trong target, không commit.
