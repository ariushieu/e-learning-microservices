# Tình huống test auth-service

Đọc [gateway.md](gateway.md) để tạo ADM/A/B/S, lấy token, quy ước fixture và ghi kết quả.
Mọi request qua `{{baseUrl}}=http://localhost:8080`; đây là **kế hoạch kiểm thử**.
Kết quả từng lượt chạy được ghi riêng trong [biên bản auth](ket-qua/auth.md).
Đợt quản lý người dùng (AUTH-09/10 và hồi quy toàn collection): [biên bản MySQL và web](ket-qua/auth-user-management.md).
Các endpoint auth/user dưới đây không đổi đường dẫn trong đợt chuẩn hóa.
Chỉ `GET /api/users` hỗ trợ phân trang/sort (AUTH-09); các endpoint còn lại không áp ca sort sai.
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
| 9 | Đăng ký với phone sai | — | Lần lượt abc, +++++++++, hai dấu cách đầu + 8 chữ số, 09-12, 0912  345678, dấu + giữa chuỗi/lặp, 16 chữ số, tab, số Unicode; email mới | 400 VALIDATION_FAILED; fieldErrors.phone tiếng Việt; login 401; đăng ký lại cùng email bằng số hợp lệ được 201 |
| 10 | Phone hợp lệ khi đăng ký | — | 9/15 chữ số; 0912345678; 0912 345 678; +84 912 345 678; 15 chữ số cách nhau một dấu cách, có + và khoảng trắng ngoài | 201; trim hai đầu, giữ định dạng bên trong; đăng nhập rồi GET /me trả đúng số (tối đa 30 ký tự sau trim) |
| 11 | Phone tùy chọn khi đăng ký | — | Lần lượt null, chuỗi rỗng, toàn dấu cách (bỏ trường đã kiểm ở ca 1) | 201; phone=null cả phản hồi đăng ký và GET sau đăng nhập |

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

## AUTH-07 — PUT /api/auth/me

Body hợp lệ: `{"fullName":"Nguyễn Văn Quốc","phone":"0901234567"}`. Dùng tài khoản S
đã chuẩn bị; lưu hồ sơ gốc để phục hồi sau ca test. Mọi request đi qua gateway cổng 8080.

| # | Tình huống | Token | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Sửa hồ sơ hợp lệ | S | PUT /api/auth/me + body hợp lệ | 200; data.id của S, tên và số điện thoại mới; GET /api/auth/me phản ánh dữ liệu mới |
| 2 | Chưa đăng nhập | — | Body hợp lệ | 401 |
| 3 | Token hỏng | Token sai chữ ký | Body hợp lệ | 401 |
| 4 | Thiếu hoặc trắng họ tên | S | Bỏ fullName hoặc fullName="   " | 400 VALIDATION_FAILED; fieldErrors có fullName; dữ liệu không đổi |
| 5 | Họ tên quá dài | S | fullName dài 151 ký tự | 400 VALIDATION_FAILED; fieldErrors có fullName |
| 6 | Số điện thoại quá dài | S | phone gồm 16 chữ số | 400 VALIDATION_FAILED; fieldErrors có phone bằng tiếng Việt |
| 7 | Xóa số điện thoại | S | Lưu số hợp lệ trước mỗi lần; lần lượt bỏ phone, phone=null, phone="" hoặc phone="   " | 200; số điện thoại được xóa |
| 8 | Giả danh và nâng quyền qua body | S | Thêm userId=adminId, roles=[ROLE_ADMIN], email và status khác; query ?userId=adminId | 200; chỉ tên/điện thoại của S đổi; admin, email, roles và status không đổi |
| 9 | Chuẩn hóa khoảng trắng | S | fullName="  Quốc  ", phone=" 0901234567 " | 200; trả "Quốc" và "0901234567" |
| 10 | Người gọi không còn tồn tại | JWT hợp lệ của tài khoản đã xóa trong môi trường riêng | Body hợp lệ | 404 RESOURCE_NOT_FOUND |
| 11 | Số điện thoại hợp lệ, kiểm biên | S | Lần lượt phone="091234567" (9 chữ số), "123456789012345" (15), "+84912345678", "0912 345 678", "+84 912 345 678", "  +1 2 3 4 5 6 7 8 9 0 1 2 3 4 5  " | 200; trim hai đầu, trả đúng phone; GET /api/auth/me phản ánh số đã lưu |
| 12 | Ký tự sai định dạng | S | Lưu hồ sơ gốc; gửi phone="abc", "0912abc678", "0912-345-678", chuỗi có tab/xuống dòng hoặc chữ số Unicode | 400 VALIDATION_FAILED; fieldErrors.phone tiếng Việt; GET xác nhận họ tên và số cũ không đổi |
| 13 | Số điện thoại quá ngắn | S | phone="12345678" hoặc "  12345678" (8 chữ số) | 400 VALIDATION_FAILED; fieldErrors.phone tiếng Việt; họ tên và số cũ không đổi |
| 14 | Dấu cộng/khoảng trắng sai vị trí | S | Lần lượt +++++++++, 0912  345678, 0912+345678, 0912345678+, ++84912345678, + 84912345678, 09-12 | 400 VALIDATION_FAILED; fieldErrors.phone tiếng Việt; GET xác nhận hồ sơ cũ không đổi |

Hợp đồng sau review PR #65 áp dụng cho cả đăng ký và sửa hồ sơ: 9–15 **chữ số** ASCII,
dấu `+` tùy chọn ở đầu, giữa các chữ số tối đa một dấu cách; khoảng trắng hai đầu được trim.
Null, rỗng hoặc toàn dấu cách được phép. Không dùng khoảng trắng/dấu cộng để bù thiếu chữ số.
Thông báo `fieldErrors.phone`: "Số điện thoại phải có 9–15 chữ số, có thể bắt đầu bằng + và cách nhau bằng một khoảng trắng".

## AUTH-08 — POST /api/auth/change-password

Tạo tài khoản riêng cho nhóm ca này để không làm hỏng tài khoản cố định của các service khác.
Đăng nhập hai lần để có hai refresh token riêng. Body: `{currentPassword, newPassword}`.

| # | Tình huống | Token | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Đổi mật khẩu hợp lệ | Tài khoản test | Mật khẩu hiện tại đúng, mật khẩu mới hợp lệ | 200; login mật khẩu cũ 401, mật khẩu mới 200 |
| 2 | Thu hồi tất cả phiên | Tài khoản test | Sau ca 1, refresh từng token từ hai lần login trước | 401 cho cả hai; login mới và refresh token mới vẫn 200 |
| 3 | Không ảnh hưởng người khác | Tài khoản test | Đổi mật khẩu rồi refresh bằng token của tài khoản khác | 200 khi refresh tài khoản khác |
| 4 | Sai mật khẩu hiện tại | Tài khoản test | currentPassword sai | 400 VALIDATION_FAILED; fieldErrors.currentPassword; mật khẩu và các phiên cũ vẫn dùng được |
| 5 | Thiếu/trắng mật khẩu hiện tại | Tài khoản test | Bỏ currentPassword hoặc gửi chuỗi trắng | 400 VALIDATION_FAILED; fieldErrors có currentPassword |
| 6 | Thiếu/trắng mật khẩu mới | Tài khoản test | Bỏ newPassword hoặc gửi chuỗi trắng | 400 VALIDATION_FAILED; fieldErrors có newPassword |
| 7 | Mật khẩu mới ngoài giới hạn | Tài khoản test | newPassword dài 5 hoặc 51 ký tự | 400 VALIDATION_FAILED; không thu hồi token |
| 8 | Chưa đăng nhập | — | Body hợp lệ | 401 |
| 9 | Token hỏng | Token sai chữ ký | Body hợp lệ | 401 |
| 10 | Giả danh qua userId | Tài khoản test | Thêm userId của người khác vào body/query | 200; chỉ mật khẩu của người gọi đổi, tài khoản khác không bị ảnh hưởng |
| 11 | Access token đã phát | Access token trước khi đổi | GET /api/auth/me sau đổi, trước khi access token hết hạn | 200; cơ chế hiện tại thu hồi refresh token, access token còn sống tới exp |

## AUTH-09 — GET /api/users (quản trị)

Chỉ ADMIN được gọi. Collection tạo ba tài khoản `qa.management.<runId>.*`: A là học viên,
B có STUDENT + INSTRUCTOR, tài khoản thứ ba có STUDENT + ADMIN. Tìm theo tiền tố riêng để
kết quả không phụ thuộc dữ liệu có sẵn. Mặc định page=0, size=12; size tối đa 100.

| # | Tình huống | Mong đợi |
|---|---|---|
| 1 | Danh sách mặc định | 200; PageResponse, tối đa 12 dòng; đủ trường công khai, không passwordHash/token |
| 2 | Tìm email với chữ hoa và khoảng trắng ngoài | 200; đúng tài khoản A |
| 3 | Tìm theo họ tên | 200; đúng tài khoản A |
| 4 | Kết hợp keyword, role, status | 200; đúng B cho INSTRUCTOR + ACTIVE; không nhân đôi user nhiều vai trò |
| 5 | size=1, page=0 rồi page=1, sort=email,asc | 200; totalElements=3, totalPages=3, hai trang khác user |
| 6 | createdAt/email/fullName, asc/desc | 200; sort hợp lệ, ID là khóa phụ khi trùng giá trị |
| 7 | sort=abcxyz/passwordHash/roles.code/id hoặc direction sai | 400 |
| 8 | page âm/sai kiểu/offset vượt Integer.MAX_VALUE, size=0/101/sai kiểu, role/status không tồn tại | 400 |
| 9 | Thiếu token hoặc token hỏng | 401 |
| 10 | Token học viên hoặc giảng viên | 403 |
| 11 | Trang ngoài phạm vi | 200; content rỗng, tổng số phần tử vẫn đúng |
| 12 | Keyword có `%` hoặc `_` | Tìm theo nghĩa đen, không hoạt động như wildcard |

## AUTH-10 — PATCH /api/users/{id}/status (quản trị)

Dùng A của AUTH-09 với hai phiên refresh riêng, B có phiên riêng để kiểm tra không bị tác động.
Không khóa tài khoản S chung. Khôi phục A về ACTIVE và bỏ quyền ADMIN của fixture sau lượt chạy.

| # | Tình huống | Mong đợi |
|---|---|---|
| 1 | Khóa A, lọc status=LOCKED | 200; trạng thái trả về và danh sách đều LOCKED |
| 2 | A đăng nhập khi bị khóa | 403 |
| 3 | Làm mới cả hai phiên cũ của A | 401 cho mỗi phiên |
| 4 | A dùng access token còn hạn đã phát trước khi khóa | GET /me vẫn 200; status=LOCKED (hành vi JWT đã công bố) |
| 5 | Mở khóa rồi đăng nhập, dùng lại refresh cũ | PATCH 200, login 200; refresh cũ vẫn 401 |
| 6 | Gửi LOCKED/ACTIVE lặp lại | 200, trạng thái đúng, không lỗi 500 |
| 7 | Admin tự khóa mình | 422 BUSINESS_RULE_VIOLATED |
| 8 | Khóa admin khác có nhiều vai trò | 422 BUSINESS_RULE_VIOLATED |
| 9 | ID không tồn tại | 404 |
| 10 | ID sai kiểu | 400 |
| 11 | status thiếu/null/rỗng/PENDING/locked/DELETED | 400 VALIDATION_FAILED; fieldErrors.status; dữ liệu không đổi |
| 12 | Học viên/giảng viên đổi trạng thái | 403 |
| 13 | Thiếu token hoặc token hỏng | 401 |
| 14 | Refresh phiên của B sau các thao tác với A | 200; không thu hồi nhầm người |

### Kiểm tra bổ sung trên controller và web

- Controller: giao dịch phải rollback trạng thái nếu thu hồi token thất bại; login/refresh đồng thời
  phải chờ khóa hàng rồi bị chặn, không phát phiên mới sau khi khóa.
- `/admin/users`: admin tìm `qa.student` đúng một dòng, lọc/sort/phân trang giữ tham số; đổi bộ lọc
  trở về trang đầu; không có kết quả hiển thị EmptyState; lỗi tải hiển thị thông báo có đường thử lại.
- Nút Cấp quyền dùng đúng tài khoản của dòng, điền sẵn vai trò, không còn ô gõ ID; xác nhận/hủy hoạt động.
- Khóa/mở khóa có xác nhận; hủy không thay đổi dữ liệu; thành công cập nhật bảng/toast; lỗi hiện trong
  hộp thoại; lọc ACTIVE thì dòng vừa khóa biến mất. Không khóa được chính mình hoặc admin khác.
- Học viên/giảng viên bị chặn khi mở trang; desktop và bề ngang 375px không tràn trang, bảng cuộn ngang
  trong khung; không lỗi JavaScript. Đọc lại trang phải phản ánh trạng thái/vai trò đã lưu.

## AUTH-11 — GET /api/users/stats (tổng quan quản trị)

Collection dùng số liệu ban đầu làm mốc, tạo hai học viên riêng `qa.stats.<runId>.*`,
không giả định database rỗng. Giữ đúng thứ tự request và khôi phục A về ACTIVE khi xong.

| # | Tình huống | Mong đợi |
|---|---|---|
| 1 | Admin lấy thống kê | 200; đủ total/byRole/byStatus/newLast7Days; đủ ba nhóm vai trò và trạng thái; số nguyên không âm, tổng trạng thái = total |
| 2 | Đăng ký thêm hai học viên rồi đọc lại | 201 mỗi đăng ký; GET 200; total/newLast7Days/STUDENT/ACTIVE tăng 2 |
| 3 | Thêm INSTRUCTOR cho A, vẫn giữ STUDENT | 200; INSTRUCTOR tăng 1, total và STUDENT không tăng thêm; ADMIN không đổi |
| 4 | Khóa A và mở link danh sách bị khóa | 200; LOCKED tăng 1, ACTIVE giảm 1, total/mới 7 ngày/PENDING không đổi; danh sách lọc fixture chỉ có A |
| 5 | Mở khóa A | 200; LOCKED/ACTIVE trở về số trước khi khóa |
| 6 | Thiếu token hoặc token hỏng | 401 |
| 7 | Học viên hoặc giảng viên | 403 |
| 8 | Lấy 5 tài khoản mới nhất | 200; size=5; createdAt giảm dần, hai học viên vừa tạo nằm đầu |

Controller còn kiểm DB rỗng trả đủ nhóm 0, trạng thái PENDING, cửa sổ 7 ngày gồm hai đầu,
loại bản ghi cũ/tương lai. Trình duyệt kiểm `/admin`, link thống kê, sidebar, lỗi tải/thử lại;
375/768/1366px không tràn, mobile thấy ngay nút Cấp quyền/Khóa trên thẻ; nội dung xác nhận
không mất tên/email trong lúc đóng (thành công, hủy và Escape).

## Truy vết nguồn

- [AuthController](../../auth-service/src/main/java/com/hunre/authservice/controller/AuthController.java),
  [UserController](../../auth-service/src/main/java/com/hunre/authservice/controller/UserController.java).
- [AuthServiceImpl](../../auth-service/src/main/java/com/hunre/authservice/service/AuthServiceImpl.java).
- [DTO và validation](../../auth-service/src/main/java/com/hunre/authservice/dto/UpdateUserRolesRequest.java),
  [hướng dẫn auth](../../auth-service/README.md).
