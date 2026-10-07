# Chạy Postman collection cho auth-service

Collection: [`auth.postman_collection.json`](auth.postman_collection.json). Bộ tình huống đối chiếu: [`../test-cases/auth.md`](../test-cases/auth.md). Collection dùng collection variables, không cần import environment; mặc định mọi request đi qua gateway tại `http://localhost:8080`.

## Chuẩn bị và chạy

1. Cập nhật mã nguồn, khởi động stack: `docker compose --profile app up -d --build --wait`.
2. Chạy `bash scripts/smoke-test.sh`; bắt đầu khi các kiểm tra đều báo `OK`.
3. Tắt rate limit trước khi chạy Runner (lệnh Bash/Git Bash): `RATE_LIMIT_ENABLED=false docker compose --profile app up -d --wait api-gateway`. Nếu không tắt, đăng nhập liên tục có thể nhận `429`; reviewer đã gặp tình huống này trong lần chạy đầu.
4. Import `auth.postman_collection.json` vào Postman. Không import hoặc tạo environment cho collection này.
5. Chạy folder **0. Chuẩn bị** một lần trước khi chạy các folder AUTH. Folder này đăng nhập admin, tạo hoặc đăng nhập A/B/S, đồng bộ vai trò theo bộ tình huống và lưu ID/token trong collection variables. Tài khoản dev mặc định được định nghĩa ở collection; không thay bằng tài khoản thật.
6. Chạy từng folder AUTH-01 … AUTH-08 bằng Runner. Có thể chạy cả collection sau folder chuẩn bị. Giữ thứ tự request trong folder; một số ca đổi vai trò, xoay/thu hồi token, đổi mật khẩu hoặc xóa fixture mà các ca sau dùng.
7. Bật lại rate limit sau khi chạy, kể cả khi có ca FAIL: `RATE_LIMIT_ENABLED=true docker compose --profile app up -d --wait api-gateway`. Kiểm tra lệnh kết thúc thành công; nếu lỗi, khắc phục và chạy lại bước này.

## Chạy bằng Newman

Chạy nguyên khối sau từ thư mục gốc bằng Bash/Git Bash. Collection chạy cả folder chuẩn bị, không cần file environment. Khối lệnh bật lại rate limit khi kết thúc, kể cả khi Newman báo lỗi:

```bash
(
  set -e
  docker compose --profile app up -d --build --wait
  bash scripts/smoke-test.sh
  trap 'RATE_LIMIT_ENABLED=true docker compose --profile app up -d --wait api-gateway' EXIT
  RATE_LIMIT_ENABLED=false docker compose --profile app up -d --wait api-gateway
  mkdir -p target
  pnpm dlx newman@6.2.2 run docs/postman/auth.postman_collection.json \
    --reporters cli,json --reporter-json-export target/auth-newman-report.json
)
```

Lưu kết quả trước khi dọn dữ liệu test. File JSON Newman có thể chứa token và thông tin đăng nhập dev; giữ tại `target/`, không commit file thô. Chỉ đưa mã HTTP, kết quả assertion đã loại thông tin nhạy cảm, commit và môi trường chạy vào biên bản. Nếu thiếu fixture, request bị bỏ qua vẫn phải ghi `BLOCKED` dù Newman kết thúc thành công. Kiểm tra gateway được bật lại rate limit sau lệnh cuối; nếu quá trình bị tắt cưỡng bức, chạy thủ công bước 7 ở trên.

## Biến và dữ liệu

Các biến token, ID và dữ liệu test là collection variables và được cập nhật bởi script của request. `runId` nên là chuỗi riêng cho mỗi đợt chạy; slug và email do collection tạo sẽ dựa vào nó. Request dùng ID chưa tồn tại lấy từ `missingId`; không gõ ID tài nguyên thủ công.

`expiredAccessToken`, `expiredRefreshToken` và `deletedUserToken` để trống mặc định. Chỉ điền fixture thật do môi trường kiểm thử cấp. Khi để trống, request tương ứng sẽ bị bỏ qua và ghi chú ở Console; ghi ca đó là **BLOCKED** trong biên bản, không tính là PASS. Không tự sửa payload JWT hoặc thời hạn để giả lập token hết hạn. `deletedUserToken` cần token hợp lệ đã phát trước khi tài khoản bị xóa trong môi trường test riêng.

Các ca thay đổi hồ sơ lưu hồ sơ S cũ và có request khôi phục. Nhóm AUTH-08 dùng tài khoản test riêng để đổi mật khẩu; không dùng tài khoản cố định A/B/S cho thao tác này. Nếu dừng giữa folder, kiểm tra dữ liệu và collection variables trước khi chạy lại: token refresh cũ có thể đã bị rotate/thu hồi, quyền S có thể đang tạm đổi, hoặc fixture đã được tạo. Có thể chạy lại folder **0. Chuẩn bị** để nạp lại các tài khoản/token cố định, đặt `runId` mới rồi chạy lại chuỗi phụ thuộc từ đầu.

## Ghi nhận kết quả

Ghi từng mã ca, commit/môi trường, HTTP thực tế, trạng thái PASS/FAIL/BLOCKED/NOT RUN và bằng chứng đã che token vào [`../test-cases/ket-qua/auth.md`](../test-cases/ket-qua/auth.md). Không sửa kỳ vọng của `test-cases/auth.md` để khớp kết quả API. Ca FAIL cần được sửa trong pull request riêng theo hướng dẫn trong bảng phân công.
