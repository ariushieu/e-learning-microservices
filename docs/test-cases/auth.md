# Tình huống test auth-service

Đọc [gateway.md](gateway.md) để tạo ADM/A/B/S, lấy token, quy ước fixture và ghi kết quả.
Mọi request qua `{{baseUrl}}=http://localhost:8080`; đây là **kế hoạch, chưa chạy Postman**.
Sáu endpoint auth/user dưới đây không đổi đường dẫn trong đợt chuẩn hóa.
Không endpoint nào hỗ trợ phân trang/sort; không áp ca sort=abcxyz cho chúng.
Các endpoint login/register/refresh/logout là công khai, nên không token không mặc định là 401.

## Body mẫu

```json
{
  "email": "qa.new.{{runId}}@example.com",
  "password": "Test@123456",
  "fullName": "Nguoi dung kiem thu"
}
```

Gọi body trên là **AUTH-REGISTER**. **AUTH-LOGIN**:
`{"email":"qa.student@example.com","password":"Test@123456"}`.
**AUTH-REFRESH**: `{"refreshToken":"{{studentRefresh}}"}`.
**AUTH-ROLES**: `{"roles":["ROLE_STUDENT","ROLE_INSTRUCTOR"]}`.
Ca tạo mới cần email chưa tồn tại; ca refresh phải lấy một cặp token riêng trước mỗi lần thử.

## AUTH-01 — POST /api/auth/register

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Đăng ký hợp lệ, không token | — | POST /api/auth/register + AUTH-REGISTER | 201; data.id có giá trị, roles chỉ ROLE_STUDENT; không passwordHash |
| 2 | Token admin không cấp quyền cho người đăng ký | ADM | Body mới, thêm "roles":["ROLE_ADMIN"] | 201; user mới vẫn chỉ ROLE_STUDENT |
| 3 | Trùng email | — | Gửi lại email đã đăng ký | 409; code=DUPLICATE_RESOURCE |
| 4 | Email sai định dạng | — | AUTH-REGISTER với email="abc" | 400 |
| 5 | Mật khẩu ngắn | — | AUTH-REGISTER với password="12345" | 400 |
| 6 | Thiếu tên | — | AUTH-REGISTER bỏ fullName | 400 |
| 7 | Chuẩn hóa email | — | Sau khi tạo qa.new.{{runId}}@example.com, đăng ký lại bằng chữ HOA | 409; không tạo tài khoản thứ hai |
| 8 | JSON hỏng | — | POST /api/auth/register, body chỉ là dấu { | 400; không 500 |

Không có ID tài nguyên để thử 404 ở endpoint tạo tài khoản; thay bằng trùng email.
Không ghi kết quả 201 nếu chỉ nhận 200 hoặc user mới có quyền admin.

## AUTH-02 — POST /api/auth/login

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Login không token | — (thông tin S) | POST /api/auth/login + AUTH-LOGIN | 200; accessToken, refreshToken, tokenType=Bearer, expiresIn>0 |
| 2 | Login admin seed | — (thông tin ADM) | Email/mật khẩu ADM từ gateway.md | 200; data.user.roles chứa ROLE_ADMIN |
| 3 | Không tồn tại tài khoản | — | Email qa.missing.{{runId}}@example.com và mật khẩu bất kỳ | 401; thông báo không phân biệt với sai mật khẩu |
| 4 | Sai mật khẩu | — | AUTH-LOGIN đổi password="wrong-password" | 401 |
| 5 | Thiếu mật khẩu | — | {"email":"qa.student@example.com"} | 400 |
| 6 | Email sai định dạng | — | {"email":"abc","password":"Test@123456"} | 400 |
| 7 | Vai trò không bị token gửi kèm chi phối | ADM (thông tin S trong body) | AUTH-LOGIN với header token admin | 200; data.user.id=studentId, roles của S |
| 8 | Sau cấp quyền | — (thông tin S) | ADM cấp thêm INSTRUCTOR rồi S login | 200; JWT mới chứa ROLE_INSTRUCTOR; khôi phục vai trò sau ca |

## AUTH-03 — POST /api/auth/refresh-token

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Refresh hợp lệ, không access token | — (refresh của S) | POST /api/auth/refresh-token + AUTH-REFRESH | 200; cặp token mới; data.user.id=studentId |
| 2 | Dùng lại refresh đã rotate | — | Gửi lại refresh cũ của ca 1 | 401 |
| 3 | Refresh không tồn tại | — | {"refreshToken":"not-a-real-refresh-token"} | 401 |
| 4 | Refresh bị thu hồi | — | Logout bằng refresh riêng rồi dùng nó để refresh | 401 |
| 5 | Body thiếu trường | — | {} | 400 |
| 6 | Chuỗi trống | — | {"refreshToken":""} | 400 |
| 7 | Quyền trong DB thay đổi | — (refresh S) | ADM cấp/gỡ INSTRUCTOR rồi refresh S | 200; JWT mới chứa đúng vai trò hiện tại; token cũ vẫn giữ claim cũ |
| 8 | Refresh hết hạn thật | — | Dùng token đã quá hạn theo cấu hình refresh TTL; không tự sửa chuỗi | 401; nếu chưa có fixture ghi BLOCKED |
| 9 | Access token không thay thế refresh token | — | {"refreshToken":"{{studentToken}}"} | 401 |

## AUTH-04 — POST /api/auth/logout

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Logout bằng refresh hợp lệ | — | POST /api/auth/logout + AUTH-REFRESH | 200; refresh tiếp bằng token đó nhận 401 |
| 2 | Gọi lại logout | — | Dùng refresh đã logout | 200; thao tác lặp an toàn |
| 3 | Refresh không tồn tại | — | {"refreshToken":"not-a-real-refresh-token"} | 200; không lộ token có tồn tại |
| 4 | Không body | — | POST /api/auth/logout, body rỗng | 200 |
| 5 | Refresh trống | — | {"refreshToken":""} | 200; controller không @Valid, service bỏ qua |
| 6 | JSON sai cú pháp | — | POST /api/auth/logout, body chỉ dấu { | 400 |
| 7 | Access token còn hạn sau logout | S | Sau logout, GET /api/auth/me bằng access token đã phát | 200; hệ thống chỉ thu hồi refresh, không thu hồi access ngay |
| 8 | Phiên khác vẫn hoạt động | S | Login hai lần; logout refresh phiên 1, refresh phiên 2 | Logout 200; refresh phiên 2 nhận 200 |

## AUTH-05 — GET /api/auth/me

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Đúng danh tính | S | GET /api/auth/me | 200; data.id=studentId |
| 2 | Không token | — | GET /api/auth/me | 401 |
| 3 | Giảng viên cũng được đọc chính mình | A | GET /api/auth/me | 200; data.id=instructorAId |
| 4 | Token không hợp lệ | — | GET /api/auth/me, Bearer invalid | 401 |
| 5 | Token hết hạn | S | GET /api/auth/me bằng token hết hạn chuẩn bị ở G-AUTH.4 | 401 |
| 6 | Giả danh qua query | S | GET /api/auth/me?userId={{adminId}} | 200; vẫn chỉ S |
| 7 | Không lộ dữ liệu nhạy cảm | ADM | GET /api/auth/me | 200; không có passwordHash, tokenHash hay refreshTokens |

Endpoint này không có ID/query đầu vào để thử abc hoặc 404. Trường hợp tài khoản bị xóa sau
khi phát token cần fixture quản trị riêng (chưa có API xóa user), không giả mạo JWT để dựng ca đó.

## AUTH-06 — PATCH /api/users/{id}/roles

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Cấp giảng viên | ADM | PATCH /api/users/{{studentId}}/roles + AUTH-ROLES | 200; roles có STUDENT và INSTRUCTOR; login/refresh S thấy quyền mới |
| 2 | Không token | — | Cùng URL/body | 401 |
| 3 | Học viên tự nâng quyền | S (token chỉ STUDENT) | Cùng URL/body | 403; DB không đổi |
| 4 | Giảng viên cấp quyền | A | Cùng URL/body | 403 |
| 5 | User không tồn tại | ADM | PATCH /api/users/{{missingId}}/roles + AUTH-ROLES | 404 |
| 6 | ID sai kiểu | ADM | PATCH /api/users/abc/roles + AUTH-ROLES | 400 |
| 7 | Roles rỗng/thiếu/null | ADM | Lần lượt {}, {"roles":[]}, {"roles":null}, {"roles":[null]} | 400 mỗi request; không đổi quyền |
| 8 | Mã vai trò không tồn tại | ADM | {"roles":["ROLE_UNKNOWN"]} | 400 |
| 9 | Gỡ instructor | ADM | Với S đang có 2 quyền, gửi {"roles":["ROLE_STUDENT"]} | 200; thay toàn bộ tập quyền; token mới S không tạo quiz được (403) |
| 10 | Admin tự hạ quyền | ADM | PATCH /api/users/{{adminId}}/roles, {"roles":["ROLE_STUDENT"]} | 422; admin vẫn còn quyền |
| 11 | Admin cập nhật chính mình, giữ ADMIN | ADM | PATCH /api/users/{{adminId}}/roles, {"roles":["ROLE_ADMIN"]} | 200 |
| 12 | JWT cũ sau gỡ quyền | S (token INSTRUCTOR cũ, còn hạn) | Sau ca 9, POST /api/quizzes bằng body QUIZ-CREATE hợp lệ | 201 theo JWT stateless; token mới ở ca 9 phải nhận 403 |

Ca 12 dùng quiz test riêng và xóa sau thử bằng A/ADM. Đây là giới hạn token được tài liệu hóa,
không được đánh đồng với việc refresh vẫn cấp quyền đã gỡ.

## Truy vết nguồn

- [AuthController](../../auth-service/src/main/java/com/hunre/authservice/controller/AuthController.java),
  [UserController](../../auth-service/src/main/java/com/hunre/authservice/controller/UserController.java).
- [AuthServiceImpl](../../auth-service/src/main/java/com/hunre/authservice/service/AuthServiceImpl.java).
- [DTO và validation](../../auth-service/src/main/java/com/hunre/authservice/dto/UpdateUserRolesRequest.java),
  [hướng dẫn auth](../../auth-service/README.md).
