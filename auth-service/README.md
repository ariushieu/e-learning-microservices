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

## Gán và gỡ vai trò

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
không trắng và tối đa 150 ký tự; `phone` tùy chọn, tối đa 20 ký tự. Hai trường được cắt
khoảng trắng ở đầu/cuối. Bỏ `phone`, gửi `null` hoặc chuỗi trắng sẽ xóa số điện thoại.
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
