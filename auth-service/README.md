# Auth service — tài khoản, hồ sơ và vai trò

Service chạy mặc định tại `http://localhost:8081`; gateway chuyển tiếp cả
`/api/auth/**` và `/api/users/**`. Xác thực phải được bật khi kiểm thử phân quyền.

## Admin đầu tiên (chỉ dùng cho dev)

Flyway chạy `V3__seed_admin.sql` một lần để tạo tài khoản:

- Email: `admin@elearning.hunre.edu.vn`
- Mật khẩu dev: `Admin@123456`
- Vai trò: `ROLE_ADMIN`

Mật khẩu được lưu bằng BCrypt. Migration hiện nằm trong đường dẫn mặc định, không bị
giới hạn bởi profile dev: **phải đổi mật khẩu tài khoản này trước khi triển khai thật**.
Không sửa migration đã áp dụng và không cấp admin tự động dựa trên email đăng ký.

## Quản lý người dùng cho admin

Hai endpoint dưới đây chỉ cho `ROLE_ADMIN`; thiếu token trả 401, học viên/giảng viên trả 403.

- `GET /api/users?keyword=&role=&status=&page=0&size=12&sort=createdAt,desc`:
  tìm theo email hoặc họ tên (trim, không phân biệt hoa/thường, `%` và `_` là ký tự tìm kiếm
  bình thường). `role` nhận `ROLE_STUDENT`, `ROLE_INSTRUCTOR`, `ROLE_ADMIN`; `status` nhận
  `PENDING`, `ACTIVE`, `LOCKED`. Các bộ lọc kết hợp bằng AND; bỏ trống thì không lọc.
  Trả `PageResponse<UserResponse>` gồm `content`, `page`, `size`, `totalElements`, `totalPages`,
  `first`, `last`; không trả mật khẩu/token. Mặc định 12 dòng, mới nhất trước; `page >= 0`,
  `size` từ 1 đến 100, offset (`page * size`) không vượt quá 2147483647. Chỉ sort `createdAt`, `email`, `fullName`; thêm ID làm khóa phụ để
  phân trang ổn định khi trùng giá trị. Sort/lọc/tham số phân trang sai trả 400; trang vượt
  phạm vi trả danh sách rỗng.
- `PATCH /api/users/{id}/status`, body `{"status":"LOCKED"}` hoặc `{"status":"ACTIVE"}`:
  trả 200 với user đã cập nhật; dữ liệu sai trả 400, user không tồn tại trả 404.
  Tự khóa hoặc khóa bất kỳ tài khoản có `ROLE_ADMIN` trả 422.
  Khóa user và thu hồi **mọi refresh token** trong cùng transaction; lỗi thu hồi sẽ rollback.
  Dùng cùng khóa hàng với login, refresh, đổi mật khẩu và cập nhật vai trò.
  Gửi lại trạng thái hiện tại vẫn trả 200. Mở khóa không phục hồi refresh token đã thu hồi.

**Access token đã phát vẫn dùng được tới khi hết hạn (mặc định/tối đa 15 phút theo cấu hình
của dự án)**: gateway xác thực JWT, không tra database mỗi request. Khóa chặn đăng nhập mới
(403); refresh token đã bị thu hồi trả 401. Sau mở khóa, người dùng đăng nhập lại để có phiên mới.

Trang `/admin/users` có bảng, tìm kiếm, lọc, sắp xếp và phân trang. Cấp quyền trên từng dòng
dùng lại form vai trò với tài khoản đã chọn; khóa/mở khóa có xác nhận, phản hồi thành công/lỗi.

## Gán và gỡ vai trò (API)

Đăng nhập admin qua `POST /api/auth/login`, lấy `data.accessToken`, rồi gọi:

```http
PATCH /api/users/2/roles
Authorization: Bearer <admin-access-token>
Content-Type: application/json

{"roles":["ROLE_STUDENT","ROLE_INSTRUCTOR"]}
```

`roles` là **toàn bộ danh sách vai trò cần giữ lại**. Muốn gỡ quyền giảng viên,
gửi `{"roles":["ROLE_STUDENT"]}`. Các mã hợp lệ là `ROLE_STUDENT`,
`ROLE_INSTRUCTOR`, `ROLE_ADMIN`; danh sách phải có ít nhất một vai trò và không chứa `null`.

| Kết quả | HTTP |
|---|---|
| Cập nhật thành công, trả thông tin người dùng và vai trò mới | 200 |
| Body thiếu/sai vai trò, danh sách rỗng hoặc chứa null | 400 |
| Thiếu token hoặc token không hợp lệ | 401 |
| Người gọi không có `ROLE_ADMIN` | 403 |
| Không tìm thấy người dùng | 404 |
| Admin tự gỡ quyền admin của chính mình | 422 |

Người được đổi quyền phải **đăng nhập lại hoặc làm mới token** để nhận JWT chứa quyền mới.
Access token cũ vẫn giữ quyền cũ đến khi hết hạn (mặc định 15 phút), kể cả khi quyền đã bị gỡ.
`GET /api/auth/me` lấy ID từ `AuthenticatedUser` đã được filter kiểm tra và trả dữ liệu
hiện tại trong database; vai trò trong phản hồi có thể mới hơn vai trò của JWT đang dùng.

## Sửa hồ sơ của mình

Gọi qua gateway `http://localhost:8080` với access token của tài khoản cần sửa:

```http
PUT /api/auth/me
Authorization: Bearer <access-token>
Content-Type: application/json

{"fullName":"Nguyễn Văn Quốc","phone":"0901234567"}
```

Trả 200 với `ApiResponse<UserResponse>` giống `GET /api/auth/me`. `fullName` bắt buộc,
không trắng và tối đa 150 ký tự. `phone` dùng cùng ràng buộc ở đăng ký và cập nhật:
9–15 **chữ số ASCII** (`0`–`9`), dấu `+` tùy chọn chỉ ở đầu, giữa các chữ số tối đa
một khoảng trắng thường. Không đếm dấu `+` hoặc khoảng trắng vào số chữ số.
Cho phép khoảng trắng ở hai đầu; cả đăng ký và cập nhật đều cắt chúng trước khi lưu.
Bỏ `phone`, gửi `null`, chuỗi rỗng hoặc toàn khoảng trắng thường sẽ lưu `null`
(xóa số điện thoại ở API cập nhật).
Ví dụ `0912 345 678`, `+84 912 345 678` hợp lệ. `abc`, `+++++++++`, `  12345678`,
`0912  345678`, dấu `+` giữa/cuối chuỗi hoặc số có 16 chữ số trả 400
`VALIDATION_FAILED`, kèm thông báo tiếng Việt trong `fieldErrors.phone`;
không đổi hồ sơ hoặc tạo tài khoản nếu dữ liệu sai.
Hai DTO dùng hằng số chung `PhoneConstraints`. Migration V4 mở rộng `users.phone`
lên 30 ký tự để lưu đủ 15 chữ số + 14 dấu cách + dấu `+`, giữ nguyên định dạng bên trong.
API chỉ cập nhật hai trường này trên tài khoản lấy từ JWT; không đổi email, vai trò,
trạng thái hay mật khẩu theo các trường gửi thêm. JWT đã phát vẫn mang tên cũ;
`GET /api/auth/me` trả hồ sơ mới ngay, JWT mới có tên mới sau login/refresh.

## Đổi mật khẩu của mình

```http
POST /api/auth/change-password
Authorization: Bearer <access-token>
Content-Type: application/json

{"currentPassword":"<mật khẩu hiện tại>","newPassword":"<mật khẩu mới>"}
```

Mật khẩu mới không trắng, dài 6–50 ký tự như quy tắc đăng ký; mật khẩu không được trim.
Thành công trả 200 với `ApiResponse` và thông báo yêu cầu đăng nhập lại. Mật khẩu được
mã hóa BCrypt và **mọi refresh token của tài khoản**, kể cả phiên đang gọi, bị thu hồi
trong cùng giao dịch. Token của người khác không bị ảnh hưởng. Login/refresh và đổi mật
khẩu khóa cùng dòng người dùng để phiên cũ không phát hành thêm token trong lúc đổi mật khẩu.

Sai mật khẩu hiện tại trả 400, không đổi dữ liệu hoặc thu hồi token:

```json
{
  "success": false,
  "code": "VALIDATION_FAILED",
  "message": "Mật khẩu hiện tại không chính xác",
  "path": "/api/auth/change-password",
  "fieldErrors": [{"field":"currentPassword","message":"Mật khẩu hiện tại không chính xác"}]
}
```

Response thực tế còn có `timestamp`. Lỗi ràng buộc đầu vào cũng trả 400
`VALIDATION_FAILED` với `fieldErrors`; hai API trả 401 khi thiếu/hỏng token và 404 nếu
tài khoản trong JWT không còn tồn tại. Mọi vai trò đã xác thực đều được sửa tài khoản của mình.

Frontend nên xóa phiên hiện tại sau khi đổi mật khẩu thành công và chuyển về đăng nhập.
**Access token đã phát vẫn hợp lệ tới khi hết hạn** (mặc định 15 phút), vì cơ chế hiện tại
chỉ thu hồi refresh token. Các phiên khác phải đăng nhập lại khi cần refresh.

## Kiểm thử

### Tổng quan quản trị

`GET /api/users/stats` chỉ dành cho ADMIN (không token 401, học viên/giảng viên 403).
Response `ApiResponse.data` có `total`, `byRole` (ROLE_STUDENT, ROLE_INSTRUCTOR, ROLE_ADMIN),
`byStatus` (ACTIVE, LOCKED, PENDING) và `newLast7Days`. Các nhóm không có người vẫn trả 0.
Một người nhiều vai trò được đếm ở **mỗi vai trò**; không cộng `byRole` để suy ra `total`.
Tổng người dùng là tổng ba trạng thái, kể cả tài khoản chưa có vai trò.

`newLast7Days` tính trong cửa sổ 7 × 24 giờ lùi từ thời điểm nhận yêu cầu, dùng `Instant` UTC,
bao gồm hai đầu mốc và không đếm thời gian tạo trong tương lai. Thống kê dùng ba truy vấn đếm
(vai trò/trạng thái dùng `COUNT ... GROUP BY`), không tải entity người dùng. Giao dịch đọc
REPEATABLE_READ giữ các truy vấn trên cùng snapshot MySQL.

Trang `/admin` hiển thị số liệu và 5 tài khoản mới nhất. Thẻ giảng viên/tài khoản bị khóa dẫn
tới danh sách đã lọc; thống kê và bảng tải độc lập để một phần lỗi không che phần còn lại.

### Chạy kiểm thử

Từ thư mục gốc, trên Windows:

```powershell
.\mvnw.cmd -pl auth-service -am verify
```

Test tích hợp bật xác thực thật, dùng HTTP, JPA/H2 và migration seed admin để kiểm tra
đăng nhập admin, cấp/gỡ quyền, refresh JWT, `/me`, cùng các trường hợp 401/403/404/422.
H2 không thay thế việc kiểm tra migration và entity bằng MySQL theo `CONTRIBUTING.md`.

`ProfileManagementIntegrationTest` kiểm hai API qua HTTP với JWT và database thật trong
H2: giới hạn dữ liệu, danh tính từ token, thu hồi nhiều phiên, giữ phiên của người khác,
rollback khi thu hồi lỗi và login/refresh đồng thời với đổi mật khẩu. Các ca thủ công
`AUTH-07` và `AUTH-08` nằm trong `docs/test-cases/auth.md` để chạy qua gateway.

Kiểm thử liên service theo phân công: đăng ký học viên, dùng admin cấp thêm
`ROLE_INSTRUCTOR`, đăng nhập lại rồi gọi `POST /api/quizzes` bằng token mới với body hợp lệ.
Giảng viên phải tạo được bài kiểm tra; gọi cùng endpoint bằng token học viên phải trả 403.
Các service cần dùng cùng `JWT_SECRET`.
