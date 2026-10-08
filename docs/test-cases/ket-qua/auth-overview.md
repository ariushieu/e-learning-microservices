# Biên bản tổng quan quản trị và hoàn thiện #70

- API/collection: `897bf3ecc7f90a2306897adba1497252c87547e9`; frontend: `1cad84017f10fd5c6a9fe8bfe4fe0c567c2dd8bd`.
- Thời gian: 2026-10-08T10:52:54.945394+07:00 → 2026-10-08T11:08:42.177114+07:00.
- Collection SHA-256: `105c6b980ef1f8d28f8a3f23524ac260ef0b5658693695af44e0b4c66023d846`.
- Native Windows, JDK 17, MySQL 8.0.43 riêng (13317, UTC), gateway thật (18080), frontend production (3100), Edge headless. Không dùng database đang có của người dùng; tiến trình test đã dừng.
- 291 request, **714/714 assertion PASS**, 114 mã ca PASS, 0 FAIL/BLOCKED/NOT RUN. Ba fixture hết hạn/xóa được cấp thật trong database riêng.
- **54 kiểm tra trình duyệt PASS** ở 1366/768/375px, không mock API. So số trên thẻ với API thật, so 5 tài khoản mới nhất, link bộ lọc và sidebar; hồi quy cấp quyền/khóa/mở và quan sát nội dung hộp thoại khi đóng.
- Maven verify toàn dự án đạt; sau khi đồng bộ #75 chạy lại quiz/shared verify đạt. Tổng báo cáo cuối: **881 test, 0 fail/error/skip** (auth 147). Các ca thống kê bao gồm DB rỗng đủ nhóm 0, PENDING, đa vai trò, cửa sổ 7 ngày với hai đầu mốc, loại dữ liệu cũ/tương lai và 401/403.
- Frontend production build và lint PASS.

## Docker CI

[CI run 37725799164](https://github.com/ariushieu/e-learning-microservices/actions/runs/37725799164), source `202ff93383acb7318702972f078b4d2a94e806ff`, 2026-10-08T04:09:16.626Z → 2026-10-08T04:09:31.162Z.

- Stack Docker build/start + smoke test thành công. Collection qua gateway: **288 request, 707/707 assertion PASS**, 111 mã ca PASS, 0 FAIL; 3 mã ca BLOCKED (AUTH-03.8: refresh hết hạn thật; AUTH-05.5: access hết hạn thật; AUTH-07.10: tài khoản đã xóa).
- Các ca BLOCKED không có fixture ở CI; đã chạy PASS trong lượt native có fixture ở trên. Không gộp hai môi trường thành một lượt chạy.
- Rate limit được bật lại bằng trap sau Newman; job kết thúc thành công.
- [docker-results.json](auth-overview/docker-results.json) lưu từng HTTP/assertion, commit, checksum collection và lý do BLOCKED, không chứa token/body. Backend, frontend và collection không đổi sau commit chạy CI; commit cuối chỉ thêm bằng chứng và chỉnh phạm vi locator của script trình duyệt.
- Checksum Docker `c7dd0b275b3416c3a7ca1a906110676bd6e495c8c2052b82b7d706b833d67c7a` khác bản native vì Git checkout Windows dùng CRLF; đã kiểm tra chuẩn hóa CRLF → LF cho ra đúng checksum Docker. Nội dung JSON giống nhau.

## HTTP theo mã ca

Chi tiết từng request và assertion: [api-results.json](auth-overview/api-results.json). Raw Newman report chứa token chỉ giữ trong target, không commit.

| Mã ca | HTTP thực tế (theo thứ tự) | Kết quả |
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
| AUTH-11.1 | 200 | PASS |
| AUTH-11.2 | 201, 201, 200 | PASS |
| AUTH-11.3 | 200, 200 | PASS |
| AUTH-11.4 | 200, 200, 200 | PASS |
| AUTH-11.5 | 200, 200 | PASS |
| AUTH-11.6 | 401, 401 | PASS |
| AUTH-11.7 | 403, 403 | PASS |
| AUTH-11.8 | 200, 200 | PASS |

## Trình duyệt

| Rộng | Kiểm tra | Kết quả |
|---|---|---|
| 1366px | Anonymous user redirected to login | PASS |
| 1366px | Overview shows live API counts, five newest accounts and active sidebar | PASS |
| 1366px | Locked/instructor cards and View all open the correct user list | PASS |
| 1366px | Admin searches qa.student and sees exactly one account | PASS |
| 1366px | Pagination preserves keyword, role and status | PASS |
| 1366px | Combined filters reset page and do not duplicate multi-role users | PASS |
| 1366px | No matches show empty state | PASS |
| 1366px | Role form targets selected row, preselects roles; cancel/save/reload work | PASS |
| 1366px | Lock and role actions fit inside the viewport without horizontal scrolling | PASS |
| 1366px | Cancel locking leaves account active | PASS |
| 1366px | Closing lock dialog retains name and email throughout exit animation | PASS |
| 1366px | Lock removes row from ACTIVE filter, blocks login/refresh; existing JWT still works | PASS |
| 1366px | Unlock updates filtered list; login succeeds without restoring old refresh | PASS |
| 1366px | Self and other admin cannot be locked from UI | PASS |
| 1366px | Account promoted after dialog opens: server rejects lock and error remains visible | PASS |
| 1366px | API load error is visible and retry recovers | PASS |
| 1366px | No page JavaScript errors or horizontal document overflow | PASS |
| 1366px | Student and instructor cannot open admin page | PASS |
| 768px | Anonymous user redirected to login | PASS |
| 768px | Overview shows live API counts, five newest accounts and active sidebar | PASS |
| 768px | Locked/instructor cards and View all open the correct user list | PASS |
| 768px | Admin searches qa.student and sees exactly one account | PASS |
| 768px | Pagination preserves keyword, role and status | PASS |
| 768px | Combined filters reset page and do not duplicate multi-role users | PASS |
| 768px | No matches show empty state | PASS |
| 768px | Role form targets selected row, preselects roles; cancel/save/reload work | PASS |
| 768px | Lock and role actions fit inside the viewport without horizontal scrolling | PASS |
| 768px | Cancel locking leaves account active | PASS |
| 768px | Closing lock dialog retains name and email throughout exit animation | PASS |
| 768px | Lock removes row from ACTIVE filter, blocks login/refresh; existing JWT still works | PASS |
| 768px | Unlock updates filtered list; login succeeds without restoring old refresh | PASS |
| 768px | Self and other admin cannot be locked from UI | PASS |
| 768px | Account promoted after dialog opens: server rejects lock and error remains visible | PASS |
| 768px | API load error is visible and retry recovers | PASS |
| 768px | No page JavaScript errors or horizontal document overflow | PASS |
| 768px | Student and instructor cannot open admin page | PASS |
| 375px | Anonymous user redirected to login | PASS |
| 375px | Overview shows live API counts, five newest accounts and active sidebar | PASS |
| 375px | Locked/instructor cards and View all open the correct user list | PASS |
| 375px | Admin searches qa.student and sees exactly one account | PASS |
| 375px | Pagination preserves keyword, role and status | PASS |
| 375px | Combined filters reset page and do not duplicate multi-role users | PASS |
| 375px | No matches show empty state | PASS |
| 375px | Role form targets selected row, preselects roles; cancel/save/reload work | PASS |
| 375px | Lock and role actions fit inside the viewport without horizontal scrolling | PASS |
| 375px | Cancel locking leaves account active | PASS |
| 375px | Closing lock dialog retains name and email throughout exit animation | PASS |
| 375px | Lock removes row from ACTIVE filter, blocks login/refresh; existing JWT still works | PASS |
| 375px | Unlock updates filtered list; login succeeds without restoring old refresh | PASS |
| 375px | Self and other admin cannot be locked from UI | PASS |
| 375px | Account promoted after dialog opens: server rejects lock and error remains visible | PASS |
| 375px | API load error is visible and retry recovers | PASS |
| 375px | No page JavaScript errors or horizontal document overflow | PASS |
| 375px | Student and instructor cannot open admin page | PASS |

Ảnh: [desktop](auth-overview/overview-1366.png), [tablet](auth-overview/overview-768.png), [mobile](auth-overview/overview-375.png), [thao tác người dùng trên mobile](auth-overview/users-375.png), [lỗi 422](auth-overview/lock-error-375.png).

Chạy lại theo [hướng dẫn Postman và trình duyệt](../../postman/auth.md). AUTH-11 dùng mốc thống kê ban đầu nên không phụ thuộc số tài khoản có sẵn. Nếu thiếu fixture hết hạn/xóa, ghi BLOCKED cho AUTH-03.8, AUTH-05.5, AUTH-07.10, không đổi thành PASS.
