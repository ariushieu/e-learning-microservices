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

> **Luôn mở thư mục gốc, đừng mở riêng thư mục service.** Cả 5 service đều phụ thuộc
> `shared-common`. Mở riêng một service thì IntelliJ không thấy module đó và báo không tìm
> thấy `com.hunre:shared-common`. Chạy bằng dòng lệnh cũng vậy, phải thêm `-am`:
>
> ```bash
> ./mvnw -pl auth-service -am spring-boot:run
> ```
>
> Hoặc cài `shared-common` vào kho Maven trên máy một lần bằng `./mvnw install -DskipTests`,
> nhớ chạy lại mỗi khi `shared-common` thay đổi.

Kiểm tra service đã lên:

```bash
curl http://localhost:8081/actuator/health
# {"status":"UP"}
```

## 3. Quy ước đặt tên nhánh

Không commit thẳng vào `main`. Nhánh `main` đã bật bảo vệ trên GitHub, mọi thay đổi đều
phải đi qua pull request.

Mỗi thành viên có **một nhánh cố định mang tên service mình phụ trách**, dùng lâu dài
trong suốt dự án, không xóa sau mỗi lần merge:

| Nhánh                 | Người phụ trách          |
|-----------------------|--------------------------|
| `auth-service`        | Người làm auth-service   |
| `course-service`      | Người làm course-service |
| `enrollment-service`  | Người làm enrollment     |
| `quiz-service`        | Người làm quiz-service   |

Chỉ làm việc trên nhánh của mình. Muốn sửa code trong service của người khác thì báo
người đó, đừng tự sửa trên nhánh mình rồi để lúc merge mới lộ ra.

Riêng thư mục `shared-common/` là **của chung**. Nó chứa hợp đồng giữa các service: vỏ
response, mã lỗi, payload sự kiện Kafka. Sửa ở đó ảnh hưởng cả 5 service, nên phải báo
nhóm trước khi đụng vào. Đọc [docs/shared-contracts.md](docs/shared-contracts.md) để biết
cái gì được đưa vào đó và thay đổi nào là phá vỡ hợp đồng.

### Quan trọng: đồng bộ lại nhánh sau mỗi lần pull request được merge

Dự án merge kiểu squash — GitHub gộp toàn bộ pull request thành **một commit mới** trên
`main`, và commit đó không có liên hệ lịch sử nào với các commit trên nhánh bạn. Với nhánh
dùng một lần thì không sao vì merge xong là xóa. Với nhánh sống lâu dài thì đây là cái bẫy:

> Git không biết phần việc của bạn đã vào `main` rồi. Lần sau bạn chạy `git merge main`,
> nó sẽ bắt bạn giải quyết xung đột **với chính code của mình vừa merge tuần trước**.
> Càng để lâu càng rối, và lỗi này lặp lại sau mỗi lần merge.

Cách tránh, làm **ngay sau khi pull request của bạn được merge**:

```bash
git checkout <nhánh của bạn>
git fetch origin
git reset --hard origin/main                          # nhánh trở về đúng bằng main
git push --force-with-lease origin <nhánh của bạn>    # đồng bộ lên GitHub
```

Nhánh vẫn còn nguyên và bạn làm tiếp trên đó như bình thường, chỉ là điểm xuất phát được
đưa về trùng với `main`. Từ đó `git merge main` sẽ sạch.

**Lệnh push luôn ghi tên nhánh.** `git push --force-with-lease` trống trơn sẽ đẩy nhánh
*đang đứng*, nên lỡ `checkout` nhầm là ghi đè nhánh của người khác. Chuyện này đã xảy ra
một lần với `auth-service`. GitHub không chặn được việc đó — repo cá nhân không cho giới hạn
từng người chỉ được đẩy nhánh nào — nên chỗ chặn duy nhất là chính câu lệnh.

**Chỉ chạy `reset --hard` khi nhánh của bạn không còn gì chưa merge.** Lệnh này xóa sạch
commit chưa vào `main`. Kiểm tra trước bằng:

```bash
git log --oneline origin/main..<nhánh của bạn>
```

Không in ra dòng nào nghĩa là an toàn. Nếu có dòng, dừng lại và hỏi nhóm trưởng.

### Lấy code mới từ main về nhánh của mình

Làm thường xuyên, ít nhất mỗi ngày một lần và luôn làm trước khi mở pull request:

```bash
git checkout <nhánh của bạn>
git fetch origin
git merge origin/main
```

Nhánh sống lâu mà không đồng bộ với `main` sẽ trôi xa dần, tới lúc merge thì xung đột
chồng chất và rất khó gỡ. Merge sớm, merge thường xuyên, mỗi lần một ít.

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
git push origin <nhánh của bạn>
```

Rồi mở pull request trên GitHub nhắm vào `main`.

Đừng dồn cả tháng công việc vào một pull request. Làm xong một phần chạy được thì mở pull
request cho phần đó, kể cả khi service chưa hoàn chỉnh. Pull request nhỏ thì review nhanh,
merge sớm, và nhánh của bạn không bị trôi xa khỏi `main`.

**Tiêu đề pull request cũng phải theo đúng quy ước ở mục 4.** Lý do: dự án merge kiểu
squash, GitHub lấy tiêu đề pull request làm tiêu đề commit trên `main`. Viết tiêu đề
tiếng Việt nghĩa là đưa tiếng Việt thẳng vào lịch sử `main`.

Phần mô tả pull request nên có:

- Thay đổi những gì và vì sao
- Cách người review kiểm chứng (lệnh chạy, endpoint gọi thử, kết quả mong đợi)
- Ảnh chụp màn hình nếu có thay đổi giao diện

Pull request chỉ được merge khi toàn bộ check của CI xanh.

**Merge xong thì KHÔNG xóa nhánh** — nhánh của bạn dùng lâu dài. Thay vào đó đồng bộ lại
nhánh về `main` theo hướng dẫn ở [mục 3](#quan-trọng-đồng-bộ-lại-nhánh-sau-mỗi-lần-pull-request-được-merge).
Bỏ qua bước này là lần merge sau sẽ gặp xung đột với chính code của mình.

Nếu GitHub hiện nút "Delete branch" sau khi merge thì đừng bấm.

## 6. Trước khi mở pull request

Chạy đủ ba bước này, đừng để CI phát hiện hộ:

```bash
# 1. Lấy code mới nhất từ main về nhánh của mình
git fetch origin
git merge origin/main

# 2. Build và chạy toàn bộ test
./mvnw clean verify

# 3. Xem lại đúng những gì mình sắp đưa lên
git diff origin/main...HEAD
```

Bước 3 hay bị bỏ qua nhất và cũng hay lộ ra nhiều thứ không định commit nhất: file cấu
hình cá nhân, code debug, thư mục `target/`.

Không commit các thứ sau: file `.env`, mật khẩu, khóa bí mật, thư mục `target/`, cấu hình
riêng của IDE. Nếu thấy chúng xuất hiện trong `git status`, báo nhóm trưởng thay vì tự ý
sửa `.gitignore`.

### Nếu bạn có sửa entity hoặc migration

`./mvnw verify` **không** bắt được lệch giữa entity và migration, vì test chạy trên H2 với
`ddl-auto=create-drop` — schema được dựng từ chính entity nên hai bên không bao giờ gặp
nhau. Lỗi chỉ lộ khi chạy với MySQL thật, thường là lúc sắp demo.

Chạy thêm bước này:

```bash
docker compose up -d mysql
./mvnw -DskipTests package
bash scripts/verify-schema.sh
```

Script dựng database rỗng, để Flyway chạy migration, rồi khởi động từng service với
`ddl-auto=validate`. Lệch một cột là nó báo đúng tên cột đó:

```
✘ course-service không khởi động được với schema do Flyway dựng

   Entity không khớp schema:
     Schema validation: missing column [thumbnail_url] in table [courses]
```

CI cũng chạy đúng script này ở job **Schema matches entities (MySQL)**.

### Đổi schema thì thêm file mới, đừng sửa file cũ

File migration đã vào `main` thì coi như đã chạy trên máy người khác. Flyway lưu checksum
của từng file, nên sửa lại file cũ sẽ khiến máy nào đã chạy nó báo lỗi checksum và không
khởi động được, còn máy nào chưa chạy thì nhận một schema khác hẳn.

Muốn đổi schema thì thêm file mới:

```sql
-- course-service/src/main/resources/db/migration/V3__them_cot_thumbnail.sql
ALTER TABLE courses ADD COLUMN thumbnail_url VARCHAR(500) NULL;
```

Kiểm tra trước khi mở PR:

```bash
bash scripts/check-migrations.sh
```

CI cũng kiểm ở job **Merged migrations unchanged**. Job schema ở trên *không* bắt được lỗi
này, vì CI luôn dựng database mới nên bản sửa lúc nào cũng chạy trót lọt.

Service của bạn được kiểm tự động ngay khi pom khai `spring-boot-flyway`, không phải sửa
file CI. Chưa khai thì Flyway không chạy và service bị bỏ qua.

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
```

Không cần chạy gì thêm: Flyway tự áp dụng migration khi service khởi động. Muốn xem schema
trước khi chạy service thì dùng `bash infra/mysql/apply-schema.sh`.

### Thông tin kết nối

Dù cài kiểu nào, **không commit thông tin kết nối của riêng bạn**. File `.env` và
`application-local.properties` đã được `.gitignore` bỏ qua — cứ để cấu hình cá nhân ở đó.


## 8. Xác thực: việc bạn phải sửa trong service của mình

Hệ thống đã có JWT. Controller **không được nhận `userId` từ client nữa**.

Sai — ai cũng gọi `?userId=5` để thao tác thay người khác:

```java
public ApiResponse<X> startAttempt(@PathVariable Long quizId, @RequestParam Long userId)
```

Đúng — danh tính lấy từ token đã kiểm chữ ký:

```java
public ApiResponse<X> startAttempt(@PathVariable Long quizId, AuthenticatedUser user) {
    return ApiResponse.ok(service.startAttempt(quizId, user.userId()));
}
```

Chỉ cần thêm tham số kiểu `AuthenticatedUser`, không cần annotation hay khai báo bean.
Tầng service bên dưới giữ nguyên chữ ký `Long userId`.

Muốn chặn theo vai trò thì dùng hằng số, đừng gõ chuỗi:

```java
if (!user.hasRole(Roles.INSTRUCTOR)) {
    throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ giảng viên mới được tạo khóa học");
}
```

### Test bằng Postman

Hai cách, chọn một:

1. Lấy token thật: gọi `POST /api/auth/login`, copy `accessToken`, đính vào header
   `Authorization: Bearer <token>` cho mọi request sau đó.
2. Tắt xác thực khi chạy máy mình: thêm `elearning.security.enabled=false` vào
   `application-local.properties`. Mọi request sẽ được coi là một người dùng giả lập có đủ
   ba vai trò.

Đừng commit cách 2 vào `application.properties`.

Chi tiết đầy đủ — đường dẫn công khai, phân quyền, cấu hình khóa ký — ở
[docs/authentication.md](docs/authentication.md).


## 9. Viết endpoint mới

Luật chung cho cả 5 service ở [docs/api-conventions.md](docs/api-conventions.md). Mỗi quy
tắc có số hiệu, nên lúc review chỉ cần ghi *"vi phạm A2"* là người kia biết tra ở đâu.

Năm điều bắt buộc, sai là pull request bị trả lại:

| Mã | Quy tắc |
|---|---|
| A1 | Danh tính lấy từ token, không nhận `userId`/`instructorId`/`createdBy` từ client |
| A2 | Mọi endpoint ghi phải kiểm vai trò |
| A3 | Endpoint công khai phải tự lọc trạng thái, không trả dữ liệu chưa xuất bản |
| A4 | Thêm controller mới thì khai route ở gateway |
| A5 | Dùng `ApiResponse` và `ErrorCode`, không tự chế hình dạng response |

A4 là cái hay quên nhất và khó đoán nhất: quên khai route thì gọi qua gateway nhận **404
dù service chạy hoàn toàn bình thường**, còn gọi thẳng cổng nội bộ thì vẫn đúng — rất dễ
tưởng là lỗi của frontend. CI có `GatewayRouteCoverageTest` bắt việc này.

Vì vậy khi thử endpoint mới, **gọi qua gateway cổng 8080** chứ đừng gọi thẳng cổng của
service. Danh sách lệnh tự kiểm ở cuối [api-conventions.md](docs/api-conventions.md).
