# Quy tắc viết API

Tài liệu này là luật chung cho cả 5 service. Mỗi quy tắc có số hiệu để lúc review pull
request chỉ cần ghi *"vi phạm A2"* thay vì giải thích lại từ đầu.

Quy tắc sinh ra từ những lỗi đã thật sự xảy ra trong repo này, không phải chép từ sách.
Chỗ nào có ví dụ sai là code đã từng nằm trong `main`.

- [A. Bắt buộc](#a-bắt-buộc) — sai là pull request bị trả lại
- [B. Đặt đường dẫn](#b-đặt-đường-dẫn)
- [C. Dữ liệu trả về](#c-dữ-liệu-trả-về)
- [D. Mã trạng thái HTTP](#d-mã-trạng-thái-http)
- [E. Tự kiểm trước khi mở pull request](#e-tự-kiểm-trước-khi-mở-pull-request)

---

## A. Bắt buộc

### A1. Danh tính lấy từ token, không bao giờ từ client

Không nhận `userId`, `instructorId`, `createdBy`, `studentId` qua query param **hay body**.

```java
// Sai — ai cũng đổi số để thao tác thay người khác
public ApiResponse<X> start(@PathVariable Long quizId, @RequestParam Long userId)

// Sai — cùng lỗi, chỉ khác là giấu trong body
public class CreateCourseRequest {
    private Long instructorId;
}

// Đúng
public ApiResponse<X> start(@PathVariable Long quizId, AuthenticatedUser user) {
    return ApiResponse.ok(service.start(quizId, user.userId()));
}
```

Chỉ cần thêm tham số kiểu `AuthenticatedUser`, không cần annotation gì. Tầng service bên
dưới giữ nguyên chữ ký `Long userId`.

**Ngoại lệ duy nhất:** admin thao tác trên tài khoản người khác thì id nằm ở đường dẫn
(`PATCH /api/users/{id}/roles`) và phải kiểm `user.hasRole(Roles.ADMIN)` trước.

### A2. Endpoint ghi phải kiểm vai trò

Mọi `POST`, `PUT`, `PATCH`, `DELETE` phải trả lời được câu hỏi *"ai gọi được?"* ngay trong
code, không phải trong đầu người viết.

```java
private void requireQuizManager(AuthenticatedUser user) {
    if (!user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN)) {
        throw new BusinessException(ErrorCode.FORBIDDEN,
                "Chỉ giảng viên hoặc quản trị viên mới có quyền quản lý bài kiểm tra");
    }
}
```

Dùng hằng số trong `Roles`, đừng gõ chuỗi `"ROLE_INSTRUCTOR"`.

Đã xảy ra: cả 4 controller của course-service không có lấy một dòng kiểm quyền — đếm
`hasRole|hasAnyRole|AuthenticatedUser` ra đúng 0. Học viên thường tạo được khóa học đứng
tên giảng viên khác.

### A3. Endpoint công khai chỉ trả dữ liệu đã xuất bản

Đường dẫn nằm trong `elearning.security.public-paths` thì **khách chưa đăng nhập gọi
được**. Truy vấn phía sau phải tự lọc trạng thái, đừng tin vào việc "chắc không ai đoán ra
id".

```java
// Sai — công khai mà không lọc: khách đọc được khóa học còn đang DRAFT
public CourseResponse getCourseById(Long id) {
    return CourseResponse.from(findOrThrow(id));
}
```

Đã xảy ra: `GET /api/courses/{id}`, `/slug/{slug}` và `/instructor/{id}` đều công khai và
đều không lọc `status`, nên nội dung chưa công bố đọc được thoải mái, kể cả cả cây chương
trình học qua `/api/courses/{id}/curriculum`.

Cách làm đúng ở cùng file: `getPublishedCourses` có
`predicates.add(cb.equal(root.get("status"), CourseStatus.PUBLISHED))`.

### A4. Thêm controller mới thì khai route ở gateway

Gateway định tuyến theo tiền tố khai tay trong
`api-gateway/src/main/resources/application.properties`. Không khai thì gọi qua gateway
nhận **404 dù service chạy hoàn toàn bình thường** — gọi thẳng cổng nội bộ vẫn đúng, nên
rất dễ tưởng là lỗi của frontend.

Đã xảy ra với `ProgressController`. Giờ đã có `GatewayRouteCoverageTest` quét mã nguồn các
module và bắt lỗi này ở CI, nhưng biết trước vẫn hơn là để CI đỏ.

Nếu endpoint mới cần công khai thì khai **cả hai chỗ**: `public-paths` của gateway và
`public-paths` của service. Lệch nhau thì gateway cho qua rồi service chặn, người dùng
nhận 401 mà không hiểu vì sao.

### A5. Không tự chế hình dạng response hay mã lỗi

Thành công thì trả `ApiResponse.ok(...)`. Lỗi thì ném `BusinessException` với một
`ErrorCode` có sẵn — đừng `return ResponseEntity.status(400).body(Map.of("error", ...))`,
và đừng thêm mã lỗi riêng trong service. Thiếu mã thì thêm vào enum `ErrorCode` để cả 5
service dùng chung.

`GlobalExceptionHandler` lo phần còn lại. Nếu service cần xử lý riêng một loại exception
thì viết `@RestControllerAdvice` riêng **và phải kèm `@Order`** ưu tiên cao hơn — Spring
dừng ở advice đầu tiên có method khớp chứ không chọn advice cụ thể hơn, mà
`GlobalExceptionHandler` có `@ExceptionHandler(Exception.class)` bắt tất. Xem
`SortPropertyExceptionHandler` làm mẫu.

---

## B. Đặt đường dẫn

### B1. Danh từ số nhiều, tiếng Anh, gạch nối

```
/api/courses          không phải /api/course, /api/getCourses, /api/khoa-hoc
/api/refresh-token    không phải /api/refreshToken, /api/refresh_token
```

### B2. Lọc bằng query param, không bằng đoạn đường dẫn

```
Sai                                Đúng
/api/courses/instructor/{id}       /api/courses?instructorId={id}
/api/quizzes/course/{id}           /api/quizzes?courseId={id}
/api/progress/course/{id}          /api/progress?courseId={id}
```

Đoạn đường dẫn để chỉ **một tài nguyên**, query param để **lọc tập hợp**. Trộn hai thứ thì
thêm bộ lọc thứ hai là phải đẻ thêm đường dẫn, và `/api/courses/instructor/5` trông như
"tài nguyên instructor nằm trong courses" chứ không phải "lọc theo giảng viên".

### B3. Tài nguyên con phải lồng đủ cha

```
Sai:  DELETE /api/lessons/resources/{resourceId}
Đúng: DELETE /api/lessons/{lessonId}/resources/{resourceId}
```

Thiếu cha thì server không kiểm được tài liệu đó có thuộc bài học đang thao tác không —
cùng loại lỗi với việc nhận `userId` từ client.

Ghi và đọc phải cùng một kiểu lồng. Hiện `GET /api/courses/{courseId}/curriculum` lồng,
nhưng `POST /api/sections` lại nhận `courseId` trong body — chọn một.

### B4. Đổi trạng thái: `PATCH /{id}/status` kèm body

Tài nguyên có enum trạng thái (course, quiz, enrollment) thì dùng **một** endpoint:

```
PATCH /api/courses/{id}/status     {"status": "PUBLISHED"}
```

Không đẻ mỗi trạng thái một đường dẫn (`/publish`, `/archive`, `/cancel`): thêm trạng thái
là thêm endpoint, và động từ trong URL thì không còn là REST nữa.

Cờ bật/tắt đơn giản, không phải enum, thì giữ nguyên dạng ngắn: `PATCH
/api/notifications/{id}/read`.

### B5. Không nhét động từ hay từ thừa vào đường dẫn

```
Sai:  GET /api/quizzes/{id}/attempts/history
Đúng: GET /api/quizzes/{id}/attempts
```

`GET` đã có nghĩa là "lấy". `/history`, `/list`, `/getAll`, `/detail` đều thừa.

### B6. Tài nguyên riêng tư: gốc tập hợp mặc định là của người đang đăng nhập

```
Sai:  GET /api/enrollments/my-courses
Đúng: GET /api/enrollments
```

Hộp thư, lượt ghi danh, bài làm đều là dữ liệu riêng. Vì A1 đã bắt buộc danh tính lấy từ
token nên `GET /api/enrollments` không thể trả về của người khác — thêm `/my-` chỉ làm
đường dẫn dài ra. `NotificationController` đang làm đúng.

### B7. Một tài nguyên, một đường dẫn gốc

```
Sai:  POST /api/quizzes/{quizId}/attempts     (tạo)
      GET  /api/quizzes/attempts/{attemptId}  (đọc — nhánh khác hẳn)
Đúng: POST /api/quizzes/{quizId}/attempts
      GET  /api/attempts/{attemptId}
```

Tạo trong ngữ cảnh cha thì lồng; sau khi có id riêng thì tài nguyên đứng độc lập.

---

## C. Dữ liệu trả về

### C1. Danh sách phải phân trang

Trả `PageResponse<T>` qua `Pageable`, trừ khi số phần tử có trần rõ ràng và nhỏ (danh sách
chương của một khóa học). Trả `List` trần cho dữ liệu tăng theo thời gian (bài làm, thông
báo, khóa học) là để ngỏ một câu truy vấn không giới hạn.

```java
@GetMapping
public ApiResponse<PageResponse<CourseSummaryResponse>> getCourses(
        @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {
```

Luôn khai `@PageableDefault` — không khai thì mặc định là 20 phần tử sắp xếp ngẫu nhiên
theo thứ tự database trả về, và hai lần gọi có thể ra hai thứ tự khác nhau.

### C2. Trả DTO, không trả entity

Entity lộ cả quan hệ lazy lẫn cột nội bộ, và đổi entity là đổi luôn hợp đồng với frontend
mà không ai nhận ra. Mỗi tài nguyên có `XxxResponse` riêng.

### C3. Cắt bớt trường theo vai trò ngay ở tầng dựng DTO

Cùng một tài nguyên, người khác vai trò thấy khác nhau. Làm ở chỗ dựng DTO, không làm ở
frontend.

```java
QuizDetailResponse.from(quiz, /* kèm đáp án */ false);
```

Đã xảy ra: `/api/quizzes/{id}/questions` từng trả cả `isCorrect` của từng đáp án cho học
viên đang làm bài.

### C4. Trường trong response phải có endpoint dựng được nó

Nghe hiển nhiên nhưng đã hụt: `LessonResponse` không có `content` lẫn `contentUrl`, nên
bài giảng nằm trong database mà không API nào đọc ra được — website học trực tuyến không
có đường xem bài học.

---

## D. Mã trạng thái HTTP

| Mã | Khi nào | Cách trả |
|---|---|---|
| 200 | Đọc, sửa, xóa thành công | mặc định |
| 201 | Tạo mới thành công | `@ResponseStatus(HttpStatus.CREATED)` |
| 400 | Sai định dạng, thiếu tham số, sai kiểu | `ErrorCode.BAD_REQUEST` / `VALIDATION_FAILED` |
| 401 | Chưa đăng nhập hoặc token hỏng | filter tự lo |
| 403 | Đã đăng nhập nhưng không đủ quyền | `ErrorCode.FORBIDDEN` |
| 404 | Không tìm thấy | `ResourceNotFoundException` |
| 409 | Trùng: email, slug, ghi danh hai lần | `DuplicateResourceException` |
| 422 | Hợp lệ về hình thức nhưng sai quy tắc nghiệp vụ | `ErrorCode.BUSINESS_RULE_VIOLATED` |

Vài điểm hay nhầm:

- **`POST /api/auth/login` trả 200, không phải 201.** Đăng nhập không tạo ra tài nguyên nào.
- **403 hay 404 khi xem đồ của người khác?** Mặc định 403. Chọn 404 khi chính việc thừa
  nhận "có tồn tại" đã là rò rỉ — `NotificationController` cố ý trả 404 để người ngoài
  không dò được người khác có bao nhiêu thông báo. Chọn 404 thì ghi rõ lý do trong Javadoc.
- **Đừng để lỗi của client thành 500.** Client gõ sai thì 4xx. Gặp 500 mà nguyên nhân là
  dữ liệu người dùng gửi lên thì đó là lỗi cần sửa, không phải chuyện bình thường.

---

## E. Tự kiểm trước khi mở pull request

Chạy service rồi bắn thử, đừng chỉ đọc code:

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"sv@hunre.edu.vn","password":"..."}' | jq -r .data.accessToken)

# 1. Gọi qua GATEWAY (cổng 8080), không gọi thẳng service — A4
curl -i -H "Authorization: Bearer $TOKEN" localhost:8080/api/<endpoint-moi>

# 2. Không token: phải 401, trừ khi cố ý công khai
curl -i localhost:8080/api/<endpoint-moi>

# 3. Sai vai trò: phải 403, không phải 404 hay 200 — A2
# 4. Không tồn tại: phải 404
curl -i -H "Authorization: Bearer $TOKEN" localhost:8080/api/<endpoint-moi>/99999

# 5. Sai kiểu tham số: phải 400
curl -i -H "Authorization: Bearer $TOKEN" localhost:8080/api/<endpoint-moi>/abc

# 6. Nếu có phân trang, sắp xếp bằng tên trường bịa: phải 400
curl -i -H "Authorization: Bearer $TOKEN" "localhost:8080/api/<endpoint-moi>?sort=abcxyz"
```

Endpoint công khai thì thêm một bước: tạo một bản ghi ở trạng thái chưa xuất bản rồi gọi
**không kèm token** — không được thấy nó (A3).

---

## Đọc thêm

| Chủ đề | Tài liệu |
|---|---|
| Lấy danh tính, đường dẫn công khai, cấu hình khóa ký | [authentication.md](authentication.md) |
| `ApiResponse`, `ErrorResponse`, `ErrorCode`, sự kiện Kafka | [shared-contracts.md](shared-contracts.md) |
| Entity và migration | [database-design.md](database-design.md) |
| Quy ước nhánh, commit, pull request | [../CONTRIBUTING.md](../CONTRIBUTING.md) |
