# Hướng dẫn cho thành viên nhóm

Tài liệu này dành cho người mới tham gia phát triển dự án. Đọc hết một lượt trước khi
commit dòng code đầu tiên.

- [1. Chuẩn bị máy](#1-chuẩn-bị-máy)
- [2. Clone và thiết lập](#2-clone-và-thiết-lập)
- [3. Quy ước đặt tên nhánh](#3-quy-ước-đặt-tên-nhánh)
- [4. Quy ước viết commit](#4-quy-ước-viết-commit)
- [5. Quy trình pull request](#5-quy-trình-pull-request)
- [6. Trước khi mở pull request](#6-trước-khi-mở-pull-request)
- [7. Làm việc với database](#7-làm-việc-với-database)

## 1. Chuẩn bị máy

| Phần mềm         | Bắt buộc | Ghi chú                                                      |
|------------------|----------|---------------------------------------------------------------|
| JDK 17 trở lên   | Có       | Không cần cài Maven, dự án dùng Maven Wrapper                 |
| Git              | Có       | Trên Windows nên dùng Git for Windows, có sẵn Git Bash        |
| IntelliJ IDEA    | Khuyến nghị | Community Edition là đủ                                    |
| Docker Desktop   | **Không** ở giai đoạn hiện tại | Xem [mục 7](#7-làm-việc-với-database) |

Hiện tại chưa service nào kết nối database, nên **không có Docker vẫn clone về build và
chạy được bình thường**. Khi nào bước cấu hình Spring Data JPA hoàn tất thì mới cần một
MySQL, lúc đó tài liệu này sẽ được cập nhật.

## 2. Clone và thiết lập

```bash
git clone https://github.com/ariushieu/e-learning-microservices.git
cd e-learning-microservices

# Bật git hook kiểm tra commit. Git không tự chia sẻ hook qua clone
# nên mỗi người phải chạy lệnh này một lần trên máy mình.
git config core.hooksPath .githooks

# Build toàn bộ để kiểm tra môi trường
./mvnw clean verify
```

Mở dự án trong IntelliJ: **File > Open**, chọn thư mục gốc (hoặc file `pom.xml` ở gốc),
**không** mở riêng từng thư mục service. Sau đó vào **File > Project Structure > Project SDK**
chọn JDK 17 trở lên.

Chạy một service: mở class `*Application.java` tương ứng rồi nhấn Run. Riêng `shared-common`
là thư viện dùng chung, không có hàm `main` và không chạy độc lập được.

Kiểm tra service đã lên:

```bash
curl http://localhost:8081/actuator/health
# {"status":"UP"}
```

## 3. Quy ước đặt tên nhánh

Không commit thẳng vào `main`. Nhánh `main` đã bật bảo vệ trên GitHub, mọi thay đổi đều
phải đi qua pull request.

```
<loại>/<mô tả ngắn, tiếng Anh, nối bằng gạch ngang>
```

| Ví dụ đúng                   | Dùng khi                          |
|------------------------------|------------------------------------|
| `feat/auth-login`            | Thêm tính năng mới                 |
| `fix/course-duplicate-slug`  | Sửa lỗi                            |
| `docs/api-gateway-routes`    | Chỉ sửa tài liệu                   |
| `refactor/quiz-grading`      | Dọn code, không đổi hành vi        |

Mỗi nhánh giải quyết một việc và nên sống ngắn, vài ngày là merge. Nhánh ôm quá nhiều
việc và để lâu sẽ rất khó merge về sau.

Trước khi bắt đầu, luôn lấy code mới nhất:

```bash
git checkout main
git pull
git checkout -b feat/ten-viec-cua-ban
```

## 4. Quy ước viết commit

### Bắt buộc: viết bằng tiếng Anh

**Toàn bộ lịch sử git của dự án dùng tiếng Anh** — tiêu đề commit, tiêu đề pull request
và mô tả pull request. Không tiếng Việt có dấu, không tiếng Việt không dấu, không emoji.

README, comment trong code, tài liệu trong `docs/` và trao đổi trong nhóm thì vẫn dùng
tiếng Việt bình thường. Quy tắc này chỉ áp dụng cho lịch sử git.

### Định dạng

```
<loại>(<phạm vi tùy chọn>): <mô tả ngắn, tiếng Anh, không quá 72 ký tự>

<phần thân tùy chọn: giải thích VÌ SAO thay đổi, không phải thay đổi CÁI GÌ,
vì cái gì thì đọc diff đã thấy rồi>
```

Các loại hợp lệ:

| Loại       | Dùng cho                                              |
|------------|--------------------------------------------------------|
| `feat`     | Thêm tính năng mới                                     |
| `fix`      | Sửa lỗi                                                |
| `docs`     | Chỉ thay đổi tài liệu                                  |
| `style`    | Định dạng code, không đổi hành vi                      |
| `refactor` | Sửa cấu trúc code, không thêm tính năng, không sửa lỗi |
| `perf`     | Cải thiện hiệu năng                                    |
| `test`     | Thêm hoặc sửa test                                     |
| `build`    | Thay đổi build, dependency, Maven                      |
| `ci`       | Thay đổi GitHub Actions                                |
| `chore`    | Việc lặt vặt không thuộc các loại trên                 |
| `revert`   | Hoàn tác một commit trước đó                           |

### Ví dụ

Đúng:

```
feat(auth): add login endpoint returning a JWT pair
fix(course): reject duplicate slug when updating a course
perf(enrollment): index course_id to speed up the my-courses list
docs: explain the outbox pattern in the database design
```

Sai:

```
them chuc nang dang nhap          → tiếng Việt, lại thiếu tiền tố loại
feat: them API dang nhap          → tiếng Việt không dấu vẫn là tiếng Việt
update code                       → thiếu tiền tố loại, mô tả vô nghĩa
fix: bug                          → không ai biết sửa lỗi gì
feat: add login.                  → không đặt dấu chấm cuối tiêu đề
```

### Hai lớp kiểm tra tự động

1. **Git hook trên máy bạn** chặn ngay lúc `git commit`, với điều kiện đã chạy
   `git config core.hooksPath .githooks` ở [mục 2](#2-clone-và-thiết-lập).
2. **CI trên GitHub** kiểm tra lại toàn bộ commit của pull request và cả tiêu đề pull
   request, phòng trường hợp ai đó quên bật hook.

Muốn tự kiểm tra trước một câu tiêu đề:

```bash
bash scripts/check-commit-subject.sh "feat(auth): add login endpoint"
```

Bộ lọc tiếng Việt không dấu hoạt động theo danh sách cụm từ thường gặp nên không bắt
được mọi trường hợp. Nó là lưới an toàn, không phải cái cớ để viết ẩu.

## 5. Quy trình pull request

```bash
git push -u origin feat/ten-viec-cua-ban
```

Rồi mở pull request trên GitHub nhắm vào `main`.

**Tiêu đề pull request cũng phải theo đúng quy ước ở mục 4.** Lý do: dự án merge kiểu
squash, GitHub lấy tiêu đề pull request làm tiêu đề commit trên `main`. Viết tiêu đề
tiếng Việt nghĩa là đưa tiếng Việt thẳng vào lịch sử `main`.

Phần mô tả pull request nên có:

- Thay đổi những gì và vì sao
- Cách người review kiểm chứng (lệnh chạy, endpoint gọi thử, kết quả mong đợi)
- Ảnh chụp màn hình nếu có thay đổi giao diện

Pull request chỉ được merge khi toàn bộ check của CI xanh. Merge xong thì xóa nhánh.

## 6. Trước khi mở pull request

Chạy đủ ba bước này, đừng để CI phát hiện hộ:

```bash
# 1. Lấy code mới nhất từ main về nhánh của mình
git checkout main && git pull
git checkout feat/ten-viec-cua-ban
git merge main

# 2. Build và chạy toàn bộ test
./mvnw clean verify

# 3. Xem lại đúng những gì mình sắp đưa lên
git diff main...HEAD
```

Bước 3 hay bị bỏ qua nhất và cũng hay lộ ra nhiều thứ không định commit nhất: file cấu
hình cá nhân, code debug, thư mục `target/`.

Không commit các thứ sau: file `.env`, mật khẩu, khóa bí mật, thư mục `target/`, cấu hình
riêng của IDE. Nếu thấy chúng xuất hiện trong `git status`, báo nhóm trưởng thay vì tự ý
sửa `.gitignore`.

## 7. Làm việc với database

Thiết kế database đã hoàn tất, xem [docs/database-design.md](docs/database-design.md).
File migration nằm trong chính service sở hữu schema, tại
`<service>/src/main/resources/db/migration/`.

Hiện **chưa service nào kết nối database**, nên chưa cần cài gì để code và chạy. Khi bước
cấu hình Spring Data JPA hoàn tất, mỗi người sẽ cần một MySQL 8 trên máy mình.

### Nếu bạn dùng MySQL cài trực tiếp trên máy

Tạo 5 database và user ứng dụng bằng một lệnh, chạy với tài khoản `root`:

```bash
mysql -u root -p < infra/mysql/init/01-create-databases.sql
```

File này tạo `auth_db`, `course_db`, `enrollment_db`, `quiz_db`, `notification_db` với bảng
mã `utf8mb4`, tạo user `elearning` (mật khẩu `elearning`, chỉ dùng cho môi trường phát
triển) và cấp quyền trên cả 5 database. Chạy lại nhiều lần không lỗi.

Nạp cấu trúc bảng vào từng database:

```bash
mysql -u elearning -p auth_db         < auth-service/src/main/resources/db/migration/V1__init_auth_schema.sql
mysql -u elearning -p auth_db         < auth-service/src/main/resources/db/migration/V2__seed_roles.sql
mysql -u elearning -p course_db       < course-service/src/main/resources/db/migration/V1__init_course_schema.sql
mysql -u elearning -p enrollment_db   < enrollment-service/src/main/resources/db/migration/V1__init_enrollment_schema.sql
mysql -u elearning -p quiz_db         < quiz-service/src/main/resources/db/migration/V1__init_quiz_schema.sql
mysql -u elearning -p notification_db < notification-service/src/main/resources/db/migration/V1__init_notification_schema.sql
mysql -u elearning -p notification_db < notification-service/src/main/resources/db/migration/V2__seed_notification_templates.sql
```

Bước này chỉ cần làm một lần. Sau khi Flyway được bật, schema sẽ tự chạy lúc service khởi động.

**Yêu cầu phiên bản: MySQL 8.0.16 trở lên.** Schema dùng ràng buộc `CHECK`, mà các bản
cũ hơn chỉ đọc qua rồi bỏ qua, không hề báo lỗi — dữ liệu sai vẫn lọt vào database. Kiểm tra:

```bash
mysql -u root -p -e "SELECT VERSION();"
```

Nếu máy bạn đang chạy MariaDB (thường đi kèm XAMPP) thì báo nhóm trưởng, đừng tự xoay,
vì MariaDB khác MySQL ở kiểu `JSON` và vài hành vi khác.

### Nếu bạn dùng Docker

```bash
docker compose up -d mysql
bash infra/mysql/apply-schema.sh
```

### Thông tin kết nối

Dù cài kiểu nào, **không commit thông tin kết nối của riêng bạn**. File `.env` và
`application-local.properties` đã được `.gitignore` bỏ qua — cứ để cấu hình cá nhân ở đó.
