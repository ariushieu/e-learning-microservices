# Chạy collection enrollment qua gateway

Import [enrollment.postman_collection.json](enrollment.postman_collection.json), chọn **No environment**,
chạy toàn collection từ **0. Chuẩn bị**, một iteration, theo thứ tự đã lưu. Collection có 154 request
và hai PUT đồng thời trong script ENROLL-07.16. Không cần nhập ID hoặc token bằng tay.

## Môi trường

Từ gốc repo:

~~~powershell
$env:RATE_LIMIT_ENABLED = "false"
docker compose --profile app up -d --build --wait
bash scripts/smoke-test.sh
~~~

Giữ JWT bật. Tắt rate limit chỉ trong lúc kiểm nghiệp vụ để các lần đăng nhập ở setup không bị 429.
Sau khi test, bỏ biến và tạo lại gateway để khôi phục giới hạn:

~~~powershell
Remove-Item Env:RATE_LIMIT_ENABLED
docker compose --profile app up -d api-gateway
~~~

Tài khoản, tên biến theo [gateway.md](../test-cases/gateway.md). Mật khẩu trong collection chỉ là dữ liệu dev.
Mọi URL dùng collection variable `baseUrl=http://localhost:8080`; không gọi cổng service.
Admin phải tồn tại từ migration. Setup đăng ký A/B/S, lấy ID bằng login, cấp vai trò qua API rồi login lại.
409 khi đăng ký chỉ có nghĩa tài khoản đã tồn tại; bước đăng nhập kế tiếp vẫn phải thành công.

## Dữ liệu và thứ tự

Mỗi lượt tạo một danh mục và bốn khóa có slug chứa runId ngẫu nhiên. Khóa chính có hai bài;
ba khóa còn lại phục vụ ca giả userId/chưa có chứng chỉ, cập nhật đồng thời và ARCHIVED.
Collection chỉ xóa lượt ghi danh thuộc fixture của lần chạy; khóa/danh mục được giữ để đối chiếu.
Không chạy các thư mục ghi/xóa rời rạc khi chưa có tiền điều kiện.

- Ghi danh chờ consumer nạp snapshot: thử lại 404 tối đa 15 lần, cách 2 giây. Nếu chưa sẵn sàng,
  dừng lượt chạy, ghi **BLOCKED** cho ca đó và các ca phụ thuộc; không ghi SQL vào snapshot.
- Thông báo được đọc lại mỗi 2 giây, tối đa 30 giây trước khi ghi lỗi điều tra.
- ENROLL-07.14/15/17 so completedAt với giá trị **đã đọc lại từ database** ở ENROLL-07.1,
  tránh khác biệt độ chính xác giữa Java Instant và cột timestamp(6).
- ENROLL-07.16 gửi hai PUT bằng callback pm.sendRequest, khởi phát cùng một script và đợi
  cả hai kết thúc trước GET kiểm kết quả. Cách này chạy được trong cả Postman và Newman 6.
- ENROLL-10 kiểm API xác minh công khai, không có email/ID trong dữ liệu, quyền các đường dẫn
  lân cận, xóa chứng chỉ và tên khóa không đổi sau khi cấp.

**ENROLL-01.10:** HTTP 404 chưa đủ chứng minh snapshot ARCHIVED đã đến. Đối chiếu thêm log consumer
hoặc đọc snapshot trong database; nếu chưa xác nhận đồng bộ, biên bản vẫn ghi BLOCKED cho tiền điều kiện này.

**ENROLL-09.1–09.5:** cần thao tác MySQL/Kafka trên môi trường thử riêng, thực hiện theo
[enrollment.md](../test-cases/enrollment.md#enroll-09--retry-kafka-và-dlt). Thư mục hướng dẫn cuối
collection không có request giả; kết quả Runner không tính năm ca này là PASS.

## Chạy bằng dòng lệnh

~~~bash
npx --yes newman@6.2.2 run docs/postman/enrollment.postman_collection.json --delay-request 60 --timeout-request 15000 --timeout-script 30000
~~~

Lưu [biên bản](../test-cases/ket-qua/enrollment.md): commit, môi trường, mã HTTP thực tế và bằng chứng.
Không commit file export chứa access token, refresh token, cookie hoặc báo cáo HTTP thô.

Collection được sinh từ [scripts/build-enrollment-postman.mjs](../../scripts/build-enrollment-postman.mjs).
Sửa script, chạy lại từ gốc repo rồi commit cả script và JSON:

~~~bash
node scripts/build-enrollment-postman.mjs
~~~

File enrollment-service.postman_collection.json cũ đã được thay bằng collection này.

