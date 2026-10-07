# Biên bản kiểm thử auth-service

## Nguồn và môi trường chạy thật

- **Nguồn kết quả R1:** [review ngày 07/10/2026 trên PR #57][R1], được reviewer xác nhận lại trong [comment chạy độc lập][R2]. Biên bản tổng hợp kết quả chạy của reviewer; người cập nhật tài liệu chưa chạy lại trên máy local vì không có Docker. Không sử dụng kết quả CI thay cho kết quả Newman.
- **Mốc collection được review:** PR #57 tại commit `4e6ee7d12f837d08739588d269314931d22edaa6`. Reviewer chạy bản gộp `main + #56 + #57`; SHA của bản gộp thử nghiệm không được công bố trong review.
- **Môi trường:** backend Docker, mọi request qua gateway `http://localhost:8080`; rate limit tạm tắt. Frontend production chạy riêng và được kiểm bằng Playwright trên Edge.
- **Lệnh reviewer sử dụng:** `pnpm dlx newman@6.2.2 run docs/postman/auth.postman_collection.json`.
- **Kết quả Newman:** chạy 130/133 request; **294/294 assertion PASS**. Ba request bị bỏ qua do thiếu fixture. Theo các assertion hiện có của collection, 73 mã tình huống được tổng hợp thành **70 PASS, 0 FAIL, 3 BLOCKED**.
- **Cách đối chiếu HTTP:** các mã trong bảng được suy ra từ assertion HTTP trong [collection](../../postman/auth.postman_collection.json) tại commit trên và xác nhận toàn bộ assertion PASS của reviewer. Chưa nhận file JSON Newman hoặc response thô để trích xuất độc lập. Dấu `→` chỉ thứ tự các request cùng mã ca trong collection, bao gồm chuẩn bị/khôi phục nếu có; mỗi mã ứng với một request, kể cả mã lặp lại.
- **Kiểm thử web bổ sung:** reviewer xác nhận 13/13 ca PASS, gồm lưu/xóa số điện thoại, validation inline, so khớp mật khẩu không gọi API, logout/xóa cookie sau đổi mật khẩu, đăng nhập bằng mật khẩu mới, không lỗi JavaScript và không tràn ở 375px.
- **Dữ liệu sau test:** theo review, database đã được khôi phục về bản chụp trước khi chạy. Review không xác nhận trạng thái rate limit sau chạy; hướng dẫn [chạy lại](../../postman/auth.md#chạy-bằng-newman) bắt buộc bật lại cả khi Newman lỗi.

Kế hoạch gốc: [auth.md](../auth.md). Các ca BLOCKED dưới đây không có HTTP thực tế và không tính vào PASS. Khi có fixture, chạy lại đúng ca và bổ sung kết quả cùng mốc code; không thay kỳ vọng bằng token giả.

| Mã ca | Commit/môi trường | HTTP xác nhận qua assertion | Trạng thái | Bằng chứng / lý do |
|---|---|---|---|---|
| AUTH-01.1 | R1 / `4e6ee7d` | 201 | PASS | [R1] — Đăng ký hợp lệ, không token. |
| AUTH-01.2 | R1 / `4e6ee7d` | 201 | PASS | [R1] — Token admin không cấp quyền cho người đăng ký. |
| AUTH-01.3 | R1 / `4e6ee7d` | 409 | PASS | [R1] — Trùng email. |
| AUTH-01.4 | R1 / `4e6ee7d` | 400 | PASS | [R1] — Email sai định dạng. |
| AUTH-01.5 | R1 / `4e6ee7d` | 400 | PASS | [R1] — Mật khẩu ngắn. |
| AUTH-01.6 | R1 / `4e6ee7d` | 400 | PASS | [R1] — Thiếu tên. |
| AUTH-01.7 | R1 / `4e6ee7d` | 409 | PASS | [R1] — Chuẩn hóa email. |
| AUTH-01.8 | R1 / `4e6ee7d` | 400 | PASS | [R1] — JSON hỏng. |
| AUTH-02.1 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Login không token. |
| AUTH-02.2 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Login admin seed. |
| AUTH-02.3 | R1 / `4e6ee7d` | 401 | PASS | [R1] — Không tồn tại tài khoản. |
| AUTH-02.4 | R1 / `4e6ee7d` | 401 | PASS | [R1] — Sai mật khẩu. |
| AUTH-02.5 | R1 / `4e6ee7d` | 400 | PASS | [R1] — Thiếu mật khẩu. |
| AUTH-02.6 | R1 / `4e6ee7d` | 400 | PASS | [R1] — Email sai định dạng. |
| AUTH-02.7 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Vai trò không bị token gửi kèm chi phối. |
| AUTH-02.8 | R1 / `4e6ee7d` | 200 → 200 → 200 → 200 | PASS | [R1] — Chuẩn bị cấp instructor; JWT mới nhận quyền; Khôi phục quyền; Đăng nhập lại sau khôi phục. |
| AUTH-03.1 | R1 / `4e6ee7d` | 200 → 200 | PASS | [R1] — Chuẩn bị token riêng; Refresh hợp lệ. |
| AUTH-03.2 | R1 / `4e6ee7d` | 401 | PASS | [R1] — Dùng lại refresh đã rotate. |
| AUTH-03.3 | R1 / `4e6ee7d` | 401 | PASS | [R1] — Refresh không tồn tại. |
| AUTH-03.4 | R1 / `4e6ee7d` | 200 → 200 → 401 | PASS | [R1] — Chuẩn bị token thu hồi; Thu hồi token; Token đã thu hồi. |
| AUTH-03.5 | R1 / `4e6ee7d` | 400 | PASS | [R1] — Body thiếu trường. |
| AUTH-03.6 | R1 / `4e6ee7d` | 400 | PASS | [R1] — Chuỗi trống. |
| AUTH-03.7 | R1 / `4e6ee7d` | 200 → 200 → 200 → 200 → 200 | PASS | [R1] — Lưu JWT trước cấp quyền; Cấp instructor; Refresh nhận quyền hiện tại; Khôi phục quyền; Đăng nhập lại S. |
| AUTH-03.8 | R1 / `4e6ee7d` | Không gửi request | BLOCKED | Thiếu expiredRefreshToken: cần refresh token thật đã quá TTL; không dùng token hỏng thay thế. |
| AUTH-03.9 | R1 / `4e6ee7d` | 401 | PASS | [R1] — Access token không thay thế refresh token. |
| AUTH-04.1 | R1 / `4e6ee7d` | 200 → 200 → 401 | PASS | [R1] — Chuẩn bị phiên đăng xuất; Đăng xuất hợp lệ; Xác nhận token bị thu hồi. |
| AUTH-04.2 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Gọi lại logout. |
| AUTH-04.3 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Refresh không tồn tại. |
| AUTH-04.4 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Không body. |
| AUTH-04.5 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Refresh trống. |
| AUTH-04.6 | R1 / `4e6ee7d` | 400 | PASS | [R1] — JSON sai cú pháp. |
| AUTH-04.7 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Access token còn hạn sau logout. |
| AUTH-04.8 | R1 / `4e6ee7d` | 200 → 200 → 200 → 200 | PASS | [R1] — Phiên một; Phiên hai; Đăng xuất phiên một; Phiên hai vẫn hoạt động. |
| AUTH-05.1 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Đúng danh tính. |
| AUTH-05.2 | R1 / `4e6ee7d` | 401 | PASS | [R1] — Không token. |
| AUTH-05.3 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Giảng viên cũng được đọc chính mình. |
| AUTH-05.4 | R1 / `4e6ee7d` | 401 | PASS | [R1] — Token không hợp lệ. |
| AUTH-05.5 | R1 / `4e6ee7d` | Không gửi request | BLOCKED | Thiếu expiredAccessToken: cần access token thật đã quá exp; không sửa claim/chữ ký để giả lập. |
| AUTH-05.6 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Giả danh qua query. |
| AUTH-05.7 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Không lộ dữ liệu nhạy cảm. |
| AUTH-06.1 | R1 / `4e6ee7d` | 200 → 200 | PASS | [R1] — Cấp instructor; Nhận JWT instructor. |
| AUTH-06.2 | R1 / `4e6ee7d` | 401 | PASS | [R1] — Không token. |
| AUTH-06.3 | R1 / `4e6ee7d` | 403 | PASS | [R1] — Học viên tự nâng quyền. |
| AUTH-06.4 | R1 / `4e6ee7d` | 403 | PASS | [R1] — Giảng viên cấp quyền. |
| AUTH-06.5 | R1 / `4e6ee7d` | 404 | PASS | [R1] — User không tồn tại. |
| AUTH-06.6 | R1 / `4e6ee7d` | 400 | PASS | [R1] — ID sai kiểu. |
| AUTH-06.7 | R1 / `4e6ee7d` | 400 → 400 → 400 → 400 | PASS | [R1] — Roles thiếu; Roles rỗng; Roles null; Roles phần tử null. |
| AUTH-06.8 | R1 / `4e6ee7d` | 400 | PASS | [R1] — Mã vai trò không tồn tại. |
| AUTH-06.9 | R1 / `4e6ee7d` | 200 → 200 → 403 | PASS | [R1] — Gỡ instructor; Token mới chỉ STUDENT; JWT mới không tạo quiz. |
| AUTH-06.10 | R1 / `4e6ee7d` | 422 | PASS | [R1] — Admin tự hạ quyền. |
| AUTH-06.11 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Admin cập nhật chính mình, giữ ADMIN. |
| AUTH-06.12 | R1 / `4e6ee7d` | 201 → 201 → 201 → 200 → 200 → 200 | PASS | [R1] — Chuẩn bị danh mục riêng; Chuẩn bị khóa do S sở hữu; JWT instructor cũ còn hiệu lực; Dọn quiz thử nghiệm; Dọn khóa thử nghiệm; Dọn danh mục thử nghiệm. |
| AUTH-07.1 | R1 / `4e6ee7d` | 200 → 200 | PASS | [R1] — Lưu hồ sơ; Đọc lại dữ liệu mới. |
| AUTH-07.2 | R1 / `4e6ee7d` | 401 | PASS | [R1] — Chưa đăng nhập. |
| AUTH-07.3 | R1 / `4e6ee7d` | 401 | PASS | [R1] — Token hỏng. |
| AUTH-07.4 | R1 / `4e6ee7d` | 400 → 400 | PASS | [R1] — Họ tên thiếu; Họ tên trắng. |
| AUTH-07.5 | R1 / `4e6ee7d` | 400 | PASS | [R1] — Họ tên quá dài. |
| AUTH-07.6 | R1 / `4e6ee7d` | 400 | PASS | [R1] — Số điện thoại quá dài. |
| AUTH-07.7 | R1 / `4e6ee7d` | 200 → 200 → 200 | PASS | [R1] — Xóa phone bỏ; Xóa phone null; Xóa phone trắng. |
| AUTH-07.8 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Giả danh và nâng quyền qua body. |
| AUTH-07.9 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Chuẩn hóa khoảng trắng. |
| AUTH-07.10 | R1 / `4e6ee7d` | Không gửi request | BLOCKED | Thiếu deletedUserToken: cần JWT còn hạn của tài khoản đã bị xóa trong môi trường riêng; chưa có API xóa user. |
| AUTH-08.1 | R1 / `4e6ee7d` | 200 → 401 → 200 | PASS | [R1] — Đổi mật khẩu; Mật khẩu cũ bị từ chối; Mật khẩu mới dùng được. |
| AUTH-08.2 | R1 / `4e6ee7d` | 401 → 401 → 200 | PASS | [R1] — Phiên một bị thu hồi; Phiên hai bị thu hồi; Phiên mới vẫn refresh được. |
| AUTH-08.3 | R1 / `4e6ee7d` | 200 → 200 | PASS | [R1] — Lấy phiên của S; Tài khoản khác không ảnh hưởng. |
| AUTH-08.4 | R1 / `4e6ee7d` | 400 → 200 → 200 | PASS | [R1] — Mật khẩu hiện tại sai; Mật khẩu không đổi; Phiên không bị thu hồi. |
| AUTH-08.5 | R1 / `4e6ee7d` | 400 → 400 | PASS | [R1] — Mật khẩu hiện tại thiếu; Mật khẩu hiện tại trắng. |
| AUTH-08.6 | R1 / `4e6ee7d` | 400 → 400 | PASS | [R1] — Mật khẩu mới thiếu; Mật khẩu mới trắng. |
| AUTH-08.7 | R1 / `4e6ee7d` | 400 → 400 | PASS | [R1] — Mật khẩu mới dài 5; Mật khẩu mới dài 51. |
| AUTH-08.8 | R1 / `4e6ee7d` | 401 | PASS | [R1] — Chưa đăng nhập. |
| AUTH-08.9 | R1 / `4e6ee7d` | 401 | PASS | [R1] — Token hỏng. |
| AUTH-08.10 | R1 / `4e6ee7d` | 200 → 200 → 200 | PASS | [R1] — Không đổi mật khẩu người khác; Tài khoản test đổi đúng; S vẫn giữ mật khẩu cũ. |
| AUTH-08.11 | R1 / `4e6ee7d` | 200 | PASS | [R1] — Access token đã phát. |

[R1]: https://github.com/ariushieu/e-learning-microservices/pull/57#pullrequestreview-5438638977
[R2]: https://github.com/ariushieu/e-learning-microservices/pull/57#issuecomment-6033297978
