# Xác thực và phân quyền

Tài liệu này mô tả cách hệ thống biết "ai đang gọi". Đọc trước khi sửa controller, vì cách
lấy danh tính người dùng đã thay đổi.

- [Tóm tắt](#tóm-tắt)
- [Điều phải sửa trong service của bạn](#điều-phải-sửa-trong-service-của-bạn)
- [Luồng token](#luồng-token)
- [Vì sao service tự kiểm chứ không tin gateway](#vì-sao-service-tự-kiểm-chứ-không-tin-gateway)
- [Khai báo đường dẫn công khai](#khai-báo-đường-dẫn-công-khai)
- [Tắt xác thực khi phát triển](#tắt-xác-thực-khi-phát-triển)
- [Phân quyền theo vai trò](#phân-quyền-theo-vai-trò)
- [Cấu hình](#cấu-hình)
- [Những chỗ còn thiếu](#những-chỗ-còn-thiếu)

## Tóm tắt

auth-service phát hành JWT khi đăng nhập. Gateway kiểm token ở vòng ngoài, **và** mỗi
service kiểm lại lần nữa. Controller không còn nhận `userId` từ client mà nhận thẳng
`AuthenticatedUser` đã được xác thực.

Toàn bộ phần dùng chung nằm ở `shared-common`, tự động bật, service không phải khai báo gì
ngoài khóa ký trong `application.properties`.

## Điều phải sửa trong service của bạn

Đây là thay đổi bắt buộc, không phải tùy chọn.

**Trước** — danh tính do client tự khai, ai cũng sửa được:

```java
@PostMapping("/{quizId}/attempts")
public ApiResponse<QuizAttemptResponse> startAttempt(
        @PathVariable Long quizId,
        @RequestParam Long userId) {          // ?userId=5 → làm bài hộ người khác
    return ApiResponse.ok(quizAttemptService.startAttempt(quizId, userId));
}
```

**Sau** — danh tính đến từ token đã kiểm chữ ký:

```java
@PostMapping("/{quizId}/attempts")
public ApiResponse<QuizAttemptResponse> startAttempt(
        @PathVariable Long quizId,
        AuthenticatedUser user) {
    return ApiResponse.ok(quizAttemptService.startAttempt(quizId, user.userId()));
}
```

Không cần annotation, không cần khai báo bean. Cứ thêm tham số kiểu `AuthenticatedUser` là
Spring tự điền vào.

Tầng service bên dưới không phải sửa gì: nó vẫn nhận `Long userId` như cũ.

Những chỗ cần rà trong code hiện tại:

| Service | Chỗ đang để client tự khai |
|---|---|
| quiz-service | `@RequestParam Long userId` ở `QuizAttemptController` (4 endpoint) |
| course-service | `instructorId` trong `CreateCourseRequest` |

## Luồng token

```
1. POST /api/auth/login          → { accessToken, refreshToken }
2. Mọi request sau đó:             Authorization: Bearer <accessToken>
3. Access token sống 15 phút. Hết hạn thì gọi POST /api/auth/refresh-token
```

Nội dung bên trong access token:

| Claim | Ý nghĩa |
|---|---|
| `sub` | id người dùng, đọc ra thành `user.userId()` |
| `email` | email đăng nhập |
| `fullName` | họ tên hiển thị |
| `roles` | danh sách mã vai trò, ví dụ `["ROLE_STUDENT"]` |
| `exp` | thời điểm hết hạn |

Đừng nhồi thêm claim. Token càng to thì mọi request càng nặng, và dữ liệu trong token là
bản chụp lúc đăng nhập nên có thể đã cũ. Cần thông tin khác thì gọi auth-service.

## Vì sao service tự kiểm chứ không tin gateway

Một cách làm phổ biến khác là: gateway kiểm token rồi bóc ra, gắn header `X-User-Id` cho
service phía sau, service cứ thế mà tin. Cách đó gọn hơn nhưng có một lỗ hổng:

```bash
# Gọi thẳng service, bỏ qua gateway, tự chế header
curl -X POST http://localhost:8082/api/courses \
     -H 'X-User-Id: 999' -H 'X-User-Roles: ROLE_ADMIN' -d '{...}'
```

Service không có cách nào phân biệt header đó do gateway gắn hay do người gọi tự bịa. Chỉ
cần một service lỡ mở cổng ra ngoài là toàn bộ phân quyền vô nghĩa.

Ở đây token gốc được chuyển nguyên vẹn xuống service, và service kiểm lại chữ ký. Muốn giả
mạo thì phải có khóa ký. Lệnh curl trên trả về 401.

Giá phải trả: mọi service đều cần `JWT_SECRET`, và gateway làm việc kiểm hai lần. Đổi lại
thì gateway không phải là hàng rào duy nhất — đúng tinh thần phòng thủ nhiều lớp.

## Khai báo đường dẫn công khai

Trong `application.properties` của service:

```properties
elearning.security.public-paths=/actuator/**,GET:/api/courses/**
```

Hai dạng:

| Khai báo | Nghĩa |
|---|---|
| `/api/auth/login` | mọi phương thức HTTP đều công khai |
| `GET:/api/courses/**` | chỉ GET công khai, POST/PUT/DELETE vẫn phải đăng nhập |

**Tiền tố phương thức thường là thứ bạn cần.** Khách chưa đăng nhập phải xem được danh sách
khóa học, nhưng nếu khai `/api/courses/**` theo đường dẫn thì `POST /api/courses` và
`DELETE /api/courses/1` cũng công khai luôn — ai cũng xóa được khóa học của người khác.

Khai lại `public-paths` là **thay thế** toàn bộ danh sách mặc định chứ không cộng thêm, nên
nhớ giữ lại `/actuator/**`.

Danh sách trong service phải khớp với danh sách trong `api-gateway`. Lệch nhau thì gateway
cho qua rồi service chặn, người dùng nhận 401 mà không hiểu vì sao.

## Tắt xác thực khi phát triển

Gọi Postman thẳng vào service mà không muốn đính token:

```properties
elearning.security.enabled=false
```

Lúc đó mọi request được coi là một người dùng giả lập có đủ ba vai trò, nên endpoint nhận
`AuthenticatedUser` vẫn chạy. Khởi động sẽ in cảnh báo:

```
WARN  XÁC THỰC ĐANG TẮT (elearning.security.enabled=false).
      Mọi request được coi là người dùng id=1 với vai trò [ROLE_STUDENT, ROLE_INSTRUCTOR, ROLE_ADMIN].
```

Đổi người dùng giả lập nếu cần:

```properties
elearning.security.dev-user.id=7
elearning.security.dev-user.roles=ROLE_STUDENT
```

Trong test thì cờ này đã được tắt sẵn ở `src/test/resources/application.properties`, nên
`@SpringBootTest` không phải khai khóa ký. Test nào cần kiểm tra phần bảo mật thì bật lại
bằng `@SpringBootTest(properties = "elearning.security.enabled=true")`.

**Đừng tắt ở profile mặc định.** Tắt nghĩa là service tin mọi request.

## Phân quyền theo vai trò

Xác thực trả lời "anh là ai", còn phân quyền trả lời "anh được làm gì". Phần thứ hai hiện
phải tự viết trong service:

```java
if (!user.hasRole(Roles.INSTRUCTOR)) {
    throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ giảng viên mới được tạo khóa học");
}
```

Dùng hằng số `Roles.STUDENT` / `Roles.INSTRUCTOR` / `Roles.ADMIN` chứ đừng gõ chuỗi. Gõ
nhầm `"ROLE_INSTRUCTER"` thì không có lỗi biên dịch nào báo, chỉ có điều lệnh chặn im lặng
mất tác dụng.

## Cấu hình

| Thuộc tính | Mặc định | Ghi chú |
|---|---|---|
| `elearning.security.enabled` | `true` | tắt là service tin mọi request |
| `elearning.security.jwt-secret` | không có | tối thiểu 32 ký tự, **thiếu là service không khởi động** |
| `elearning.security.public-paths` | actuator + 3 endpoint auth | khai lại là thay thế |
| `elearning.security.dev-user.*` | id=1, đủ 3 vai trò | chỉ dùng khi `enabled=false` |

Khóa ký lấy từ biến môi trường `JWT_SECRET`, và **phải giống nhau ở cả 5 service lẫn
gateway**. Khác nhau thì auth-service ký ra token mà service khác không đọc được.

Service thiếu khóa sẽ dừng ngay lúc khởi động với thông báo rõ ràng, thay vì chạy được rồi
âm thầm không bảo vệ gì:

```
elearning.security.jwt-secret phải có ít nhất 32 ký tự, đang có 0
```

## Những chỗ còn thiếu

Ghi ra để không ai tưởng phần này đã xong:

- **Khóa đối xứng.** Cả 5 service dùng chung một khóa HMAC, nghĩa là service nào cũng có thể
  *ký* token chứ không chỉ kiểm. Hệ thống thật nên dùng cặp khóa bất đối xứng (RS256) để chỉ
  auth-service giữ khóa riêng.
- **Chưa thu hồi được access token.** Đăng xuất chỉ thu hồi refresh token; access token đã
  phát vẫn dùng được cho tới khi hết hạn (tối đa 15 phút).
- **Phân quyền chưa có ở đâu cả.** Hiện mới chỉ có xác thực. Ví dụ `GET /api/quizzes/{id}`
  vẫn trả đáp án cho mọi người đã đăng nhập, cần chặn theo vai trò giảng viên.
- **Khóa mặc định nằm trong repo.** Tiện lúc phát triển, nhưng khi triển khai thật phải đặt
  `JWT_SECRET` bằng một chuỗi ngẫu nhiên và không commit nó.
