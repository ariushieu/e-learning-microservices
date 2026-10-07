# Biên bản kiểm thử auth-service

- **Collection:** [`../../postman/auth.postman_collection.json`](../../postman/auth.postman_collection.json)
- **Tình huống:** [`../auth.md`](../auth.md)
- **Môi trường:** Windows local, 07/10/2026; cổng `http://localhost:8080` hiện trả 404 từ tiến trình Python, không phải gateway; máy không có lệnh Docker/Postman CLI. Chưa chạy stack hoặc Postman Runner.
- **Kết quả:** 73 tình huống nghiệp vụ đều **NOT RUN**. Không có HTTP thực tế; không đánh dấu PASS.
- **Kiểm tra tĩnh:** collection parse được như JSON; 133 request có test script `pm.test`; có đủ folder AUTH-01 … AUTH-08 và folder chuẩn bị đầu tiên. Đây không thay thế chạy API qua gateway.
- **Cần chạy lại:** bật stack theo [`docs/postman/auth.md`](../../postman/auth.md), chạy `0. Chuẩn bị`, chạy Runner theo thứ tự và thay `NOT RUN` bằng HTTP/status/evidence thực tế. Các fixture hết hạn hoặc token người dùng đã xóa cần được cấp trong môi trường test; nếu chưa có, ghi `BLOCKED` cho đúng các ca đó.

| Mã ca | Commit/môi trường | HTTP thực tế | Trạng thái | Bằng chứng, issue |
|---|---|---|---|---|
| AUTH-01.1 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-01.2 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-01.3 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-01.4 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-01.5 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-01.6 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-01.7 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-01.8 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-02.1 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-02.2 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-02.3 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-02.4 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-02.5 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-02.6 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-02.7 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-02.8 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-03.1 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-03.2 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-03.3 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-03.4 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-03.5 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-03.6 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-03.7 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-03.8 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-03.9 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-04.1 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-04.2 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-04.3 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-04.4 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-04.5 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-04.6 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-04.7 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-04.8 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-05.1 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-05.2 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-05.3 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-05.4 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-05.5 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-05.6 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-05.7 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-06.1 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-06.2 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-06.3 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-06.4 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-06.5 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-06.6 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-06.7 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-06.8 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-06.9 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-06.10 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-06.11 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-06.12 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-07.1 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-07.2 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-07.3 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-07.4 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-07.5 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-07.6 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-07.7 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-07.8 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-07.9 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-07.10 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-08.1 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-08.2 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-08.3 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-08.4 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-08.5 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-08.6 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-08.7 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-08.8 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-08.9 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-08.10 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
| AUTH-08.11 | Môi trường local (ghi ở trên) | Chưa chạy | NOT RUN | Không có Docker/gateway; chưa thực thi Postman. |
