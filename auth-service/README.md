# Auth service — quản lý vai trò

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

## Kiểm thử

Từ thư mục gốc, trên Windows:

```powershell
.\mvnw.cmd -pl auth-service -am verify
```

Test tích hợp bật xác thực thật, dùng HTTP, JPA/H2 và migration seed admin để kiểm tra
đăng nhập admin, cấp/gỡ quyền, refresh JWT, `/me`, cùng các trường hợp 401/403/404/422.
H2 không thay thế việc kiểm tra migration và entity bằng MySQL theo `CONTRIBUTING.md`.

Kiểm thử liên service theo phân công: đăng ký học viên, dùng admin cấp thêm
`ROLE_INSTRUCTOR`, đăng nhập lại rồi gọi `POST /api/quizzes` bằng token mới với body hợp lệ.
Giảng viên phải tạo được bài kiểm tra; gọi cùng endpoint bằng token học viên phải trả 403.
Các service cần dùng cùng `JWT_SECRET`.
