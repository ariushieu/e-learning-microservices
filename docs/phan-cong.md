# Bảng theo dõi công việc

> **Cập nhật lần cuối:** 08/10/2026 — `main` ở `d47bc5a`
>
> File này là nơi duy nhất ghi ai đang làm gì. Xong một việc thì nhóm trưởng cập nhật ngay
> tại đây, nên **cứ `git pull` là biết việc tiếp theo của mình**, không phải hỏi ai.

- [Việc của bạn](#việc-của-bạn)
- [Giao diện web: ai giữ trang nào](#giao-diện-web-ai-giữ-trang-nào)
- [Khi nào test toàn bộ API bằng Postman](#khi-nào-test-toàn-bộ-api-bằng-postman)
- [Quy tắc viết API](#quy-tắc-viết-api)
- [Trạng thái hệ thống](#trạng-thái-hệ-thống)
- [Chi tiết từng việc](#chi-tiết-từng-việc)
- [Đã xong](#đã-xong)
- [Hai cái bẫy của Spring Boot 4](#hai-cái-bẫy-của-spring-boot-4)
- [Trước khi code](#trước-khi-code)

## Việc của bạn

| Người | Service | Việc đang mở | Ưu tiên | Cỡ |
|---|---|---|---|---|
| Hiếu | api-gateway, web | [Giới hạn đăng nhập theo IP thật](#hiếu--giới-hạn-đăng-nhập-theo-ip-thật) — hiện cả lớp dùng chung một xô | **Cao — xong trước demo** | ~1h |
| quocluibotre | auth | [Trang tổng quan quản trị, hoàn thiện #70](#quocluibotre--trang-tổng-quan-quản-trị) | Trung bình | ~2h |
| phamquyet19042005-netizen | enrollment | [Giảng viên xem học viên của khóa và tiến độ](#phamquyet19042005-netizen--giảng-viên-xem-học-viên-của-khóa) — #73 chạy thử đạt, đang để draft: bấm **Ready for review** để merge | Trung bình | ~3h |
| hiepdeptrai0111 | quiz | [Tải kết quả bài kiểm tra dạng CSV, hoàn thiện #71](#hiepdeptrai0111--tải-kết-quả-bài-kiểm-tra-dạng-csv) | Trung bình | ~2h |
| duyd92689-debug | course | [Admin gỡ đánh giá vi phạm](#duyd92689-debug--admin-gỡ-đánh-giá-vi-phạm) | Trung bình | ~2h |

Bốn việc của nhóm đều là **tính năng cho giảng viên và admin** — phần học viên đã đủ cho demo. Đã xong
ở lượt này: tài liệu đính kèm trên khu giảng dạy (#69, #72), admin tìm và khóa tài khoản (#70), giảng viên
xem kết quả bài kiểm tra (#71).

**Khung giao diện đã có** (#54): mọi việc web ở trên làm theo [frontend/DESIGN.md](../frontend/DESIGN.md)
và trang `/design`. Chạy cả hệ thống: `docker compose --profile app up -d --build --wait` rồi mở
http://localhost:3000.

**Collection Postman: đủ cả 7 file**, đều đã chạy thật trên Docker — auth, course, enrollment, quiz
(#56, #57, #59, #60, cập nhật ở #64, #65, #67, #69, #70, #71) và gateway, notification, demo-flow (#66). Trước buổi demo, chạy
`demo-flow` trên máy mình để xem cả chuỗi còn thông (81 assertion, khoảng 12 giây). Quy ước chung ở
[mục dưới](#cả-nhóm--collection-postman-của-service-mình). Ca FAIL hay việc mới sinh ra khi chạy
collection thì sửa ở PR riêng như các việc trong bảng trên.

**Tài khoản giảng viên và admin tạo bằng API** (#27), không cần SQL:

```
Admin có sẵn: admin@elearning.hunre.edu.vn / Admin@123456   (tài khoản dev — đổi khi triển khai thật)
Cấp quyền:    PATCH /api/users/{id}/roles   {"roles": ["ROLE_STUDENT", "ROLE_INSTRUCTOR"]}
```

Người được cấp quyền phải **đăng nhập lại** mới nhận vai trò mới — token cũ vẫn mang vai trò
cũ tới khi hết hạn.

## Giao diện web: ai giữ trang nào

**Ai giữ service nào thì giữ luôn các trang web gọi API của service đó.** Thêm hoặc sửa API thì
sửa giao diện tương ứng **trong cùng pull request**. Một API không có chỗ dùng trên web coi như
chưa xong. `.github/CODEOWNERS` tự gắn đúng người review khi pull request đụng vào phần của ai.

| Người | Trang | Thư mục trong `frontend/src/` |
|---|---|---|
| quocluibotre | Đăng nhập, đăng ký, hồ sơ, cấp quyền (`/admin/users`) | `components/auth/`, `app/(auth)/`, `app/(site)/profile/`, `app/(dashboard)/admin/users/` |
| duyd92689-debug | Trang chủ (danh mục khóa), chi tiết khóa, khu giảng dạy (khóa, chương, bài), danh mục (`/admin/categories`) | `components/course/`, `app/(site)/page.tsx`, `app/(site)/courses/`, `app/(dashboard)/instructor/` (trừ `quizzes/`), `app/(dashboard)/admin/categories/` |
| phamquyet19042005-netizen | Ghi danh, trang học + tiến độ, khóa của tôi, chứng chỉ, xác minh chứng chỉ | `components/enrollment/`, `app/(learn)/`, `app/(site)/my-courses/`, `app/(site)/certificates/`, `app/(site)/verify/` |
| hiepdeptrai0111 | Làm bài, kết quả, soạn đề | `components/quiz/`, `app/(site)/quizzes/`, `app/(site)/attempts/`, `app/(dashboard)/instructor/quizzes/` |
| Hiếu | Khung chung: hệ thống giao diện (`/design`), layout, header, sidebar, đăng nhập/cookie, `proxy.ts`, route `/api`, thông báo | `components/ui/`, `components/common/`, `components/templates/`, `components/layout/`, `components/notification/`, `lib/`, `app/layout.tsx`, `app/(site)/design/`, `proxy.ts` |

**Trước khi viết trang:** đọc [frontend/DESIGN.md](../frontend/DESIGN.md) và mở trang `/design`
trên web. Đó là khung giao diện chung: màu xanh lá theo token, 5 khuôn trang trong
`components/templates/`, các khối dùng chung trong `components/common/`. Mỗi người chỉ thiết kế
component riêng của service mình trên khung đó, không tự viết nút, bảng, màu riêng. Việc thiết kế
tiếp theo của từng người nằm ở mục
[Việc thiết kế tiếp theo](../frontend/DESIGN.md#việc-thiết-kế-tiếp-theo-của-từng-người).
Chạy thử bằng `pnpm dev` trong `frontend/` với backend đang chạy.

## Khi nào test toàn bộ API bằng Postman

**Sẵn sàng — đủ 4/4.** Mỗi người làm collection của service mình rồi tự test, cách làm ở
[Collection Postman của service mình](#cả-nhóm--collection-postman-của-service-mình).

| # | Điều kiện | Xong ở |
|---|---|---|
| 1 | API gán vai trò | #27 |
| 2 | Phân quyền course-service, lọc khóa `DRAFT` | #31, #37 |
| 3 | Ghi danh chạy thông (`course.updated` + nạp snapshot + gửi outbox) | #29, #31, #41 |
| 4 | Chuẩn hóa đường dẫn | #37 (course), #41 (enrollment), #42 (quiz) |

**Môi trường test đã sẵn.** Không ai phải tự bật 6 service trong IntelliJ:

```bash
git pull
docker compose --profile app up -d --build --wait
bash scripts/smoke-test.sh        # 12 dòng OK là cả 6 service đã lên, thông database và Redis
```

Mọi request trong Postman đi qua **gateway `http://localhost:8080`**. Đừng gọi thẳng cổng
8081–8085: trong Docker các cổng đó không mở ra ngoài, và gọi thẳng thì bỏ qua đúng hai thứ
hay hỏng nhất là định tuyến và kiểm token ở vòng ngoài.

**Gateway giới hạn số lần đăng nhập theo địa chỉ IP** (#34): 10 lần liền, sau đó 6 giây mới
được thêm một lần. Mọi request từ máy mình vào Docker đều mang chung một IP, nên khi chạy
Postman Runner hay nhiều người đăng nhập liên tục trên cùng một máy sẽ nhận **429**. Đó là
gateway chặn đúng, không phải lỗi của service. Chờ một phút, hoặc tắt hẳn trong lúc test:

```bash
RATE_LIMIT_ENABLED=false docker compose --profile app up -d api-gateway
```

Bật lại bằng cùng lệnh, bỏ `RATE_LIMIT_ENABLED=false`. Chi tiết ở
[README](../README.md#giới-hạn-request).

**Collection Postman:** mỗi service một file trong `docs/postman/`, mỗi người giữ file của service
mình — quy ước ở [Collection Postman của service mình](#cả-nhóm--collection-postman-của-service-mình).
Hai collection cũ `course-service.*` và `enrollment-service.*` **không dùng lại được** (gọi thẳng cổng
service hoặc đường dẫn cũ) — xóa khi có file mới.

**Danh sách tình huống test** (#36): `docs/test-cases/` — mỗi service một file, đọc
[gateway.md](test-cases/gateway.md) trước để tạo bốn tài khoản cố định.

## Quy tắc viết API

Mới thêm: **[docs/api-conventions.md](api-conventions.md)** — luật chung cho cả 5 service,
mỗi quy tắc có số hiệu để review chỉ cần ghi *"vi phạm A2"*.

Đọc trước khi viết endpoint mới. Tóm tắt phần bắt buộc:

| Mã | Quy tắc |
|---|---|
| A1 | Danh tính lấy từ token, không nhận `userId`/`instructorId`/`createdBy` từ client |
| A2 | Mọi endpoint ghi phải kiểm vai trò |
| A3 | Endpoint công khai phải tự lọc trạng thái, không trả dữ liệu chưa xuất bản |
| A4 | Thêm controller mới thì khai route ở gateway (CI kiểm) |
| A5 | Dùng `ApiResponse` và `ErrorCode`, không tự chế hình dạng response |

Bốn trong năm quy tắc này sinh ra từ lỗi có thật trong repo, ghi rõ trong tài liệu.

## Trạng thái hệ thống

Năm service đã có code, database chạy tự động bằng Flyway, xác thực JWT hoạt động ở cả
gateway lẫn từng service. Gateway giới hạn số request bằng Redis. Toàn bộ test Maven xanh trên CI. Cả hệ thống chạy được bằng một lệnh
`docker compose --profile app up -d --build --wait`, xem
[README](../README.md#cách-nhanh-nhất-chạy-cả-hệ-thống-bằng-docker).

**Cả chuỗi đã chạy thông** (lần đầu, khi review #41 ngày 06/10):

```
admin cấp quyền giảng viên → tạo khóa, chương, bài → xuất bản
  → course.updated lên Kafka → enrollment-service tự nạp course_snapshots
học viên ghi danh → xem nội dung bài → cập nhật tiến độ tới 100%
  → nhận đủ 3 thông báo: ghi danh thành công, hoàn thành khóa, cấp chứng chỉ có mã
học viên làm bài kiểm tra → nộp → thông báo "đạt 50.00 điểm"
```

Từ #58 thông báo tới **ngay lập tức** (SSE, Redis pub/sub giữa các bản notification-service): chuông
nhảy số và hiện toast, bấm vào mở đúng trang. Từ #60 ai cũng tra được chứng chỉ ở `/verify/<mã>`
mà không cần đăng nhập.

**Không mất sự kiện khi một phần hệ thống chết:**

- Nộp bài đi qua outbox (#32): tắt Kafka thì bài làm vẫn lưu, bật lại là thông báo tới.
- notification-service (#38) và enrollment-service (#41): MySQL tắt giữa chừng thì consumer
  thử lại tới khi MySQL lên. notification-service chuyển message hỏng sang topic `.DLT`.
- course-service: enrollment-service chết thì đề cương vẫn xem được, chỉ ẩn nội dung bài
  thường (#37). Sự kiện khóa học giờ cũng đi qua outbox (#44): Kafka chết thì sự kiện nằm chờ, và
  snapshot cũ không cho người lạ ghi danh vào khóa đã lưu trữ.

**Lỗ hổng đang mở:** không còn.

Đã đóng: chưa ghi danh thì không làm được bài (#49), bài đã hoàn thành không bị hạ trạng thái (#50),
số học viên đếm mỗi người một lần (#51), lọc theo danh mục cha thấy cả khóa ở danh mục con (#56), lộ
đáp án khi đang làm bài và nộp quá giờ bị trả về "đang làm" (#67), số điện thoại sai định dạng khi
đăng ký và sửa hồ sơ (#65).

---

## Chi tiết từng việc

### Cả nhóm — collection Postman của service mình

Mỗi người làm **một file collection cho service mình** và tự chạy hết file tình huống test của
service đó. Không gộp chung một file: năm người cùng sửa một JSON lớn thì lần merge nào cũng xung
đột.

| Người | File | Chạy tình huống | Trạng thái (chạy thật trên Docker) |
|---|---|---|---|
| quocluibotre | `docs/postman/auth.postman_collection.json` | [auth.md](test-cases/auth.md) | Xong #57, #65, #70 — 665/665 assertion (3 ca BLOCKED vì cần fixture riêng) |
| duyd92689-debug | `docs/postman/course.postman_collection.json` | [course.md](test-cases/course.md) | Xong #56, #64, #69 — 827/827 |
| phamquyet19042005-netizen | `docs/postman/enrollment.postman_collection.json` | [enrollment.md](test-cases/enrollment.md) | Xong #60 — 310/310 |
| hiepdeptrai0111 | `docs/postman/quiz.postman_collection.json` | [quiz.md](test-cases/quiz.md) | Xong #59, #67, #71 — 479/479 |
| Hiếu | `gateway`, `notification`, `demo-flow` | [gateway.md](test-cases/gateway.md), [notification.md](test-cases/notification.md) | Xong #66 — gateway 31/31 ca, notification 55/55 ca, demo-flow 81/81 |

Chạy collection bằng dòng lệnh (chỉ dùng pnpm):

```bash
RATE_LIMIT_ENABLED=false docker compose --profile app up -d --wait api-gateway
pnpm dlx newman@6.2.2 run docs/postman/<service>.postman_collection.json
docker compose --profile app up -d --wait api-gateway     # bật lại giới hạn request
```

Collection tạo dữ liệu thật (tài khoản QA, khóa, danh mục). Chạy trên máy mình rồi dọn, đừng chạy
trước giờ demo trên máy demo.

**Quy ước chung** — để file của ai import vào máy ai cũng chạy ngay:

- **Không dùng file environment.** Mọi biến là *collection variable*, có sẵn giá trị mặc định:
  `baseUrl` = `http://localhost:8080` (luôn đi qua gateway).
- **Thư mục đầu tiên "0. Chuẩn bị"**: đăng nhập admin, đăng ký và đăng nhập các tài khoản cố định
  theo [gateway.md](test-cases/gateway.md#môi-trường-và-cách-ghi-kết-quả). Script tự lưu token:
  `pm.collectionVariables.set("studentToken", pm.response.json().data.accessToken)`.
  Tên biến dùng đúng như gateway.md: `adminToken`, `tokenA`, `tokenB`, `studentToken`, các `...Id`.
- **Tên request = mã ca** trong file tình huống (ví dụ `AUTH-07.4 Họ tên trắng`), và mỗi request có
  `pm.test` kiểm mã HTTP mong đợi — để Runner chạy cả thư mục là ra PASS/FAIL.
- Request tạo dữ liệu thì lưu id vào biến cho request sau; không gõ tay id.

**Cách chạy.**

```bash
git pull
docker compose --profile app up -d --build --wait
bash scripts/smoke-test.sh        # 12 dòng OK mới bắt đầu
```

Import file của mình, chạy "0. Chuẩn bị" rồi chạy cả collection bằng Runner. Gateway chặn đăng nhập
dồn dập (429) — xem cách tắt tạm ở [mục trên](#khi-nào-test-toàn-bộ-api-bằng-postman).

**Ghi biên bản** vào `docs/test-cases/ket-qua/<service>.md` theo bảng mẫu ở đầu
[gateway.md](test-cases/gateway.md#môi-trường-và-cách-ghi-kết-quả): mã ca, commit đã chạy, mã HTTP
thực tế, PASS / FAIL / BLOCKED. Collection + biên bản chung một pull request.

**Ca FAIL** thì sửa trong pull request khác, ghi mã ca trong mô tả (ví dụ "sửa QUIZ-03.4"). Đừng sửa
mong đợi trong file tình huống cho khớp với kết quả — trừ khi chắc chắn tình huống viết sai, và khi
đó ghi lý do.

**Khóa id 1 ("Kien truc Microservices") chưa ghi danh được:** khóa này xuất bản trước khi có sự kiện
đồng bộ nên enrollment-service chưa biết nó. Giảng viên của khóa hoặc admin gọi `PUT /api/courses/1`
với nguyên dữ liệu cũ là khóa được đồng bộ. Hoặc dùng khóa mới tạo trong "0. Chuẩn bị".

---

### quocluibotre — trang tổng quan quản trị

> `/admin` hiện chỉ chuyển thẳng sang `/admin/users`. Từ #70 admin đã tìm và khóa được tài khoản,
> nhưng vào khu quản trị chưa thấy hệ thống đang có bao nhiêu người, bao nhiêu giảng viên, ai bị khóa.
> Lúc demo, đây là trang đầu tiên người xem thấy khi đăng nhập bằng admin.

**Cần làm.**

- auth-service, chỉ ADMIN (người khác 403, không token 401):
  - `GET /api/users/stats` trả `total`, `byRole` (`ROLE_STUDENT`, `ROLE_INSTRUCTOR`, `ROLE_ADMIN`),
    `byStatus` (`ACTIVE`, `LOCKED`, `PENDING`), `newLast7Days`.
  - Đếm bằng truy vấn `COUNT … GROUP BY`, không tải hết bảng `users` lên rồi đếm trong Java.
  - Một người nhiều vai trò thì được đếm ở mỗi vai trò, README ghi rõ để không ai cộng `byRole` ra
    `total`.
- Web `/admin` (thay trang chuyển hướng hiện tại):
  - `StatGrid` + `Stat`: tổng người dùng, giảng viên, admin, bị khóa, mới trong 7 ngày.
  - Thẻ "bị khóa" là link tới `/admin/users?status=LOCKED`, thẻ giảng viên tới
    `/admin/users?role=ROLE_INSTRUCTOR`.
  - Bảng 5 tài khoản mới nhất, dùng lại `GET /api/users?sort=createdAt,desc&size=5`, có link "Xem tất cả".
  - Sidebar thêm mục "Tổng quan" trên "Người dùng & quyền" (`components/layout/` là của Hiếu,
    CODEOWNERS tự mời review).
- Hoàn thiện #70, góp ý khi review:
  - `user-management-table.tsx`: lúc hộp xác nhận đang mờ dần, tiêu đề chớp thành "Khóa tài khoản ?"
    vì `setChanging(null)` xóa tên trước khi hộp đóng xong. Giữ user đang chọn tới khi hiệu ứng đóng
    kết thúc, hoặc tách `open` khỏi dữ liệu hiển thị.
  - `admin/users/page.tsx` và `user-management-table.tsx`: tách các dòng JSX dài cho giống các file
    khác trong `components/`.
  - Ở 375px phải cuộn ngang bảng mới thấy nút Khóa / Cấp quyền: đưa thao tác lên dòng tên, hoặc hiện
    dạng thẻ trên điện thoại.

**Tự kiểm.** Đăng ký thêm 2 học viên → `total` và `newLast7Days` tăng 2. Cấp giảng viên cho một người →
`byRole.ROLE_INSTRUCTOR` tăng 1, `total` không đổi. Khóa một người → `byStatus.LOCKED` = 1, bấm thẻ ra đúng
người đó. Học viên gọi → 403. Thêm ca vào `auth.md` và collection, chạy lại collection auth trên Docker.

---

### phamquyet19042005-netizen — giảng viên xem học viên của khóa

> Khu giảng dạy chưa cho giảng viên biết ai đang học khóa mình và học tới đâu.
> `course_snapshots.instructor_id` đã có, nên enrollment-service tự kiểm được quyền, không phải gọi
> sang course-service.

**Cần làm.**

- enrollment-service:
  - Migration mới: thêm `learner_name` vào `enrollments`, lấy từ token lúc ghi danh và lúc kích hoạt
    lại (giống `learner_name` của chứng chỉ ở #60). Lượt cũ để NULL; web hiện "Học viên #<id>".
  - `GET /api/courses/{courseId}/learners?status=&page=&size=`:
    - Chỉ giảng viên của khóa (theo snapshot) hoặc ADMIN; người khác 403; khóa chưa có snapshot 404.
    - Trả `enrollmentId`, `learnerName`, `status`, `progressPercent`, `enrolledAt`,
      `lastAccessedAt`, `completedAt`, `certificateCode` (nếu đã cấp). Không trả email.
    - Phân trang; sort `enrolledAt`, `progressPercent`.
  - Route (A4): đường dẫn nằm dưới `/api/courses/**` của course-service, nên khai route riêng ở gateway
    với `order` âm, giống `enrollment-lesson-progress` (routes[5]).
  - Gateway đang mở công khai `GET:/api/courses/**`, nên enrollment-service phải tự trả 401 khi không
    có token. Có ca test riêng cho chuyện này.
- Web: component `components/enrollment/course-learners.tsx` gắn vào trang `/instructor/courses/{id}`.
  - Bảng: tên, trạng thái, thanh tiến độ (`ProgressMeter`), ngày ghi danh, ngày hoàn thành.
  - Lọc theo trạng thái; khóa chưa có ai thì hiện `EmptyState`.
  - Trang đó là của duyd92689-debug: chỉ thêm một dòng import và gắn component; CODEOWNERS tự mời
    duyd review.

**Tự kiểm.** A thấy S và B với đúng tiến độ; S học xong 1/2 bài → bảng hiện 50%. B (giảng viên khác)
gọi → 403, S gọi → 403, không token → 401. Thêm nhóm ca ENROLL-11 vào `enrollment.md` và collection.

---

### hiepdeptrai0111 — tải kết quả bài kiểm tra dạng CSV

> Từ #71 giảng viên xem được kết quả trên web, nhưng muốn nhập điểm vào bảng điểm của lớp thì phải chép
> tay từng dòng. Tên học viên do người dùng tự đặt, nên file xuất ra phải an toàn khi mở bằng Excel.

**Cần làm.**

- quiz-service:
  - `GET /api/quizzes/{quizId}/results/export`: cùng quyền với `/results` (tác giả hoặc ADMIN; người
    khác 403, không token 401, quiz không tồn tại 404).
  - Trả `text/csv; charset=UTF-8` có BOM đầu file để Excel hiện đúng tiếng Việt, kèm
    `Content-Disposition: attachment; filename="ket-qua-quiz-<id>.csv"`.
  - Mỗi học viên một dòng, không phân trang, cùng cách tính với `/results`: học viên, số lượt nộp, điểm
    cao nhất, đạt / chưa đạt, lần nộp gần nhất (giờ Việt Nam).
  - Chống CSV injection: ô bắt đầu bằng `=`, `+`, `-`, `@`, tab hoặc CR thì thêm `'` ở đầu. Ô có dấu
    phẩy, ngoặc kép hoặc xuống dòng thì đặt trong ngoặc kép.
  - Không khai `produces` cứng: route `/api` của web luôn gửi `Accept: application/json`, đặt
    `Content-Type` trong `ResponseEntity`.
- Web:
  - Nút "Tải CSV" trên `/instructor/quizzes/{id}/results`, là link thường tới
    `/api/quizzes/{id}/results/export`.
  - Thêm `content-disposition` vào `PASS_HEADERS` trong `app/api/[...path]/route.ts` để file tải về đúng
    tên (file của Hiếu, CODEOWNERS tự mời review).
- Hoàn thiện #71: `results/page.tsx` tách các dòng JSX dài (header bảng, `TableRow` viết trên một dòng)
  cho giống các file khác trong `components/`.

**Tự kiểm.** S đổi họ tên thành `=1+1` rồi nộp bài → trong file, ô tên là `'=1+1`, mở bằng Excel không thành
công thức. Tên có dấu hiện đúng trong Excel. B (không phải tác giả) tải → 403. Bấm nút trên web → tải về
file `ket-qua-quiz-<id>.csv`. Thêm ca vào `quiz.md` và collection.

---

### duyd92689-debug — admin gỡ đánh giá vi phạm

> Từ #64 học viên viết được nhận xét công khai trên trang khóa học, nhưng chỉ chính người viết xóa được.
> Một nhận xét chửi bới hay quảng cáo sẽ nằm đó mãi và vẫn kéo điểm trung bình của khóa.

**Cần làm.**

- course-service:
  - `DELETE /api/courses/{courseId}/reviews/{reviewId}` (B3), chỉ ADMIN; học viên và giảng viên của
    khóa 403, không token 401.
  - Đánh giá không thuộc khóa đó → 404, để không xóa nhầm qua `courseId` khác.
  - Khóa hàng khóa học trước khi xóa (giống `requireLearnerAndLock`) rồi `recalculateRating`: điểm
    trung bình và số lượt đánh giá cập nhật ngay.
  - Gỡ xong, người viết vẫn viết lại được nếu còn đủ điều kiện, giống khi tự xóa.
- Web, mục "Đánh giá từ học viên" ở trang khóa học:
  - Admin thấy nút "Gỡ" trên từng nhận xét, có hộp thoại xác nhận; người khác không thấy nút.
  - Gỡ xong thì danh sách, điểm và số sao cập nhật ngay.
**Tự kiểm.** S đánh giá 5, B đánh giá 1 → trung bình 3. Admin gỡ đánh giá của B → trung bình 5, còn 1 lượt.
Gỡ với `courseId` của khóa khác → 404. A (giảng viên của khóa) gọi → 403. B viết lại được. Thêm ca vào
`course.md` và collection, chạy lại collection course trên Docker.

---

### Hiếu — giới hạn đăng nhập theo IP thật

> Phát hiện khi review: đăng nhập trên web đi qua Server Action của Next.js, nên gateway thấy **mọi
> người dùng chung một IP là container frontend**. Sau 10 lần đăng nhập trong một phút, cả lớp nhận
> 429 — buổi demo cho người xem thử đăng nhập là dính. Đã thử trên Docker: 11 lần đăng nhập của 11 email
> khác nhau gửi từ container frontend thì lần thứ 11 nhận 429, trong khi `curl` từ máy ngoài cùng lúc
> vẫn 401 bình thường.

**Cần làm.**

- Gateway chỉ tin `X-Forwarded-For` khi kết nối đến từ proxy tin cậy, khai bằng biến môi trường;
  trong Docker là container frontend. Từ IP khác thì vẫn bỏ qua header như bây giờ (G-RATE-4).
- Frontend gửi IP thật của trình duyệt khi đăng nhập, đăng ký, refresh token.
- Ca G-RATE mới trong collection gateway.

**Sau demo:** kênh email cho thông báo (`emailEnabled` đã lưu nhưng chưa có kênh gửi; notification-service
cũng chưa biết email người dùng).

---

## Đã xong

| Ngày | PR | Việc | Người |
|---|---|---|---|
| 08/10 | #72 | Gom kiểm link về `lib/safe-url.ts`, chặn link có dấu `\`, `aria-label` sao đọc "4,5" | duyd92689-debug |
| 08/10 | #71 | Giảng viên xem kết quả bài kiểm tra: điểm trung bình, tỉ lệ đạt, tỉ lệ đúng từng câu; không tính lượt làm thử | hiepdeptrai0111 |
| 08/10 | #70 | Admin tìm, lọc người dùng; khóa / mở khóa tài khoản (thu hồi refresh token); cấp quyền ngay trên từng dòng | quocluibotre |
| 08/10 | #69 | Thêm / xóa tài liệu đính kèm trong form sửa bài, chỉ nhận URL `http(s)`, tổng điểm hiện nửa sao | duyd92689-debug |
| 07/10 | #67 | Không lộ đáp án khi đang làm bài (QUIZ-14.8), nộp quá giờ lưu đúng EXPIRED | hiepdeptrai0111 |
| 07/10 | #65 | Kiểm định dạng số điện thoại khi đăng ký và sửa hồ sơ, form hiện lỗi dưới ô | quocluibotre |
| 07/10 | #64 | Đánh giá khóa học: sao + nhận xét, điểm trung bình trên thẻ khóa | duyd92689-debug |
| 07/10 | #63 | Mã chứng chỉ không ngắt dòng ở màn rộng, biên bản MySQL của #60 | phamquyet19042005-netizen |
| 07/10 | #66 | Collection gateway, notification, demo-flow; `readAt` khớp giá trị đã lưu (MySQL làm tròn nano giây) | Hiếu |
| 07/10 | #62 | Tên trên header đổi ngay sau khi sửa hồ sơ | Hiếu |
| 07/10 | #58 | Thông báo tức thời (SSE + Redis pub/sub), đọc tất cả, bấm thông báo mở đúng trang, cài đặt nhận thông báo | Hiếu |
| 07/10 | #60 | Xác minh chứng chỉ công khai `/verify/<mã>`, mã chứng chỉ không lộ id, collection enrollment | phamquyet19042005-netizen |
| 07/10 | #56 | Chip danh mục và sắp xếp ở trang chủ, lọc danh mục cha gồm cả danh mục con, collection course | duyd92689-debug |
| 07/10 | #59 | Trang bài kiểm tra báo "cần ghi danh", collection quiz | hiepdeptrai0111 |
| 07/10 | #57 | Trang hồ sơ: sửa thông tin, đổi mật khẩu; collection auth | quocluibotre |
| 07/10 | #51 | Đếm mỗi học viên một lần kể cả hủy rồi ghi danh lại (`course_learners`), khóa khi sửa đề cương, chặn xóa khóa có học viên | duyd92689-debug |
| 07/10 | #54 | Khung giao diện: token màu xanh lá, 5 khuôn trang, khối dùng chung, trang `/design`, `DESIGN.md` | Hiếu |
| 06/10 | #50 | Không hạ trạng thái bài đã hoàn thành; thử lại có giới hạn, message hỏng sang `.DLT` | phamquyet19042005-netizen |
| 06/10 | #49 | Chỉ người đã ghi danh mới làm bài kiểm tra | hiepdeptrai0111 |
| 06/10 | #48 | API sửa hồ sơ và đổi mật khẩu (thu hồi mọi refresh token) | quocluibotre |
| 06/10 | #53 | Giao diện shadcn/ui, chia trang web theo service | Hiếu |
| 06/10 | #52 | `pnpm start` chạy được, bỏ script dữ liệu mẫu | Hiếu |
| 06/10 | #47 | Giao việc phát hiện khi làm giao diện web | Hiếu |
| 06/10 | #46 | Giao diện web Next.js nối qua gateway | Hiếu |
| 06/10 | #45 | Chặn giảng viên sửa bài kiểm tra của người khác, ẩn bài nháp với học viên | hiepdeptrai0111 |
| 06/10 | #44 | Học viên giữ quyền học khi khóa bị lưu trữ; course-service gửi sự kiện qua outbox | duyd92689-debug |
| 06/10 | #41 | enrollment-service tự nạp `course_snapshots` từ Kafka, kiểm bài học trước khi ghi tiến độ, chuẩn hóa đường dẫn | phamquyet19042005-netizen |
| 06/10 | #42 | Outbox quiz giữ nguyên điểm số (`50.00`), chuẩn hóa đường dẫn quiz | hiepdeptrai0111 |
| 06/10 | #39 | Sửa tiêu đề pull request là check tự chạy lại | Hiếu |
| 06/10 | #37 | Chủ sở hữu chương/bài học, trả nội dung bài học theo quyền, chuẩn hóa đường dẫn course, collection Postman qua gateway | duyd92689-debug |
| 06/10 | #36 | Tình huống test cho cả 5 service (`docs/test-cases/`), chặn vai trò `null` | quocluibotre |
| 06/10 | #38 | notification-service thử lại khi lỗi tạm thời, message hỏng sang `.DLT` | Hiếu |
| 03/10 | #34 | Gateway giới hạn số request bằng Redis: chống dò mật khẩu, Redis chết thì vẫn cho qua | Hiếu |
| 03/10 | #32 | quiz-service gửi sự kiện chấm điểm qua outbox | hiepdeptrai0111 |
| 25/09 | #29 | Gửi outbox của enrollment-service lên Kafka | phamquyet19042005-netizen |
| 25/09 | #31 | Phân quyền course-service, ẩn khóa DRAFT, phát `course.updated` | duyd92689-debug |
| 25/09 | #27 | API gán vai trò, admin đầu tiên, `/me` dùng `AuthenticatedUser` | quocluibotre |
| 25/09 | #30 | CI chặn cấu hình tắt xác thực; sửa hướng dẫn tắt xác thực trên máy (thiếu bước bật profile) | Hiếu |
| 25/09 | #28 | Chạy cả hệ thống bằng Docker; gateway trả 502 sau tối đa 3 giây khi một service chết | Hiếu |
| 25/09 | #26 | Lệnh đồng bộ nhánh ghi rõ tên nhánh | Hiếu |
| 25/09 | #25 | Bỏ `createdBy` khỏi body, người tạo bài kiểm tra lấy từ token | hiepdeptrai0111 |
| 25/09 | #24 | `CourseUpdatedEvent`: hợp đồng sự kiện khóa học cho `course_snapshots` | Hiếu |
| 25/09 | #23 | Sửa route `/api/progress` bị sót ở gateway, `?sort=` sai trả 400, quy tắc viết API | Hiếu |
| 19/09 | #22 | Biến danh sách phân công thành bảng theo dõi sống | Hiếu |
| 19/09 | #21 | Bỏ `?userId=` ở 4 endpoint làm bài | hiepdeptrai0111 |
| 19/09 | #20 | Bảng theo dõi công việc | Hiếu |
| 19/09 | #13 | Chặn xem đáp án và quản lý bài kiểm tra theo vai trò, nối Flyway | hiepdeptrai0111 |
| 19/09 | #15 | enrollment-service: ghi danh, tiến độ, chứng chỉ | phamquyet19042005-netizen |
| 19/09 | #18 | notification-service: dựng thông báo từ sự kiện Kafka | Hiếu |
| 19/09 | #17 | CI chặn sửa migration đã merge | Hiếu |
| 19/09 | #16 | CI đối chiếu entity với schema bằng MySQL thật | Hiếu |
| 19/09 | #14 | Nối auto-configuration của Flyway | Hiếu |
| 19/09 | #11 | course-service dùng `Instant` thay `LocalDateTime` | duyd92689-debug |
| 18/09 | #12 | Xác thực JWT ở gateway và từng service | Hiếu |
| 18/09 | #9 | course-service: danh mục, khóa học, chương trình học | duyd92689-debug |
| 18/09 | #8 | auth-service: đăng ký, đăng nhập, JWT | quocluibotre |
| 18/09 | #7 | quiz-service: bài kiểm tra, câu hỏi, chấm điểm | hiepdeptrai0111 |

---

## Hai cái bẫy của Spring Boot 4

Cả hai đều khiến service chạy bình thường mà **không báo lỗi gì**, nên rất tốn thời gian nếu
không biết trước. Ai làm phần Kafka đều sẽ gặp.

### 1. Auto-configuration nằm ở module riêng

Spring Boot 4 tách auto-configuration ra khỏi thư viện gốc. Khai thư viện thôi là không đủ:

| Muốn dùng | Phải khai thêm |
|---|---|
| Flyway | `org.springframework.boot:spring-boot-flyway` |
| Kafka | `org.springframework.boot:spring-boot-kafka` |
| MockMvc trong test | `org.springframework.boot:spring-boot-starter-webmvc-test` |

Thiếu module này thì `@KafkaListener` không được đăng ký, hoặc migration không chạy — và
**log không có lấy một dòng nào nhắc tới Kafka hay Flyway**. Service khởi động sạch sẽ, health
báo UP, mọi thứ trông bình thường.

Ở Spring Boot 3 thì chỉ cần thư viện gốc, nên mọi hướng dẫn trên mạng đều thiếu dòng này.

Kèm theo đó là vài lớp bị đổi gói, tìm theo tên cũ sẽ không ra:

| Lớp | Gói cũ (Boot 3) | Gói mới (Boot 4) |
|---|---|---|
| `AutoConfigureMockMvc` | `...boot.test.autoconfigure.web.servlet` | `...boot.webmvc.test.autoconfigure` |
| `ErrorWebExceptionHandler` | `...boot.web.reactive.error` | `...boot.webflux.error` |
| `PropertyReferenceException` | `...data.mapping` | `...data.core` |

### 2. Spring Kafka vẫn dùng Jackson 2, Boot 4 đã sang Jackson 3

`JsonSerializer` và `JsonDeserializer` của Spring Kafka 4 chạy trên
`com.fasterxml.jackson` (Jackson 2), trong khi Spring Boot 4 dùng `tools.jackson`
(Jackson 3). Bản Jackson 2 lọt vào classpath qua thư viện khác lại không có module xử lý
kiểu thời gian, nên mọi sự kiện đều chết ở trường `occurredAt`:

```
InvalidDefinitionException: Java 8 date/time type `java.time.Instant` not supported
by default (through reference chain: QuizGradedEvent["occurredAt"])
```

**Cách làm đúng:** dùng `StringSerializer` / `StringDeserializer`, rồi tự chuyển JSON bằng
`tools.jackson.databind.ObjectMapper` (tiêm thẳng vào constructor, Spring có sẵn bean này).
Xem `QuizEventPublisher` trong quiz-service và `KafkaEventConsumer` trong
notification-service.

Thêm một lợi ích: message không còn header `__TypeId__` chứa tên lớp Java, nên đổi tên lớp
bên gửi không làm hỏng bên nhận. Consumer định tuyến theo trường `eventType` trong nội dung.

---

## Trước khi code

### Đồng bộ nhánh

Tất cả các PR đều merge kiểu squash, nên nhánh cũ sẽ xung đột với chính code mình vừa được
merge. Chạy một lần trước khi bắt đầu, **thay `quiz-service` bằng tên nhánh của bạn ở cả ba
chỗ**:

```bash
git fetch origin
git checkout quiz-service
git log --oneline origin/main..quiz-service     # in ra dòng nào là còn việc chưa merge: DỪNG, hỏi nhóm trưởng
git reset --hard origin/main
git push --force-with-lease origin quiz-service
```

**Lệnh push phải ghi tên nhánh.** Bản cũ trong file này viết `git push --force-with-lease`
trống trơn và thiếu cả `git checkout`, nên đang đứng ở nhánh nào là đè lên nhánh đó — ngày
25/09 nhánh `auth-service` đã bị đẩy từ máy của một người không phụ trách nó. Lần đó may là
nhánh không có gì chưa merge nên không mất code, nhưng lần sau thì chưa chắc. Ghi rõ
`origin quiz-service` thì dù lỡ đứng nhầm nhánh, lệnh cũng chỉ đụng đúng nhánh của bạn.

### Nếu có sửa entity hoặc migration

```bash
docker compose up -d mysql
./mvnw -DskipTests package
bash scripts/verify-schema.sh
```

`./mvnw verify` **không** bắt được lệch giữa entity và migration, vì test chạy trên H2 với
`ddl-auto=create-drop` — schema dựng từ chính entity nên hai bên không bao giờ gặp nhau.

Và **đừng sửa file migration đã vào main**. Flyway lưu checksum, sửa lại thì máy nào đã chạy
sẽ không khởi động được, máy nào chưa chạy thì nhận schema khác. Đổi schema thì thêm file mới
`V<n>__*.sql` với câu `ALTER TABLE`. CI có job kiểm việc này.

### Nếu có thêm hoặc sửa endpoint

Đọc [api-conventions.md](api-conventions.md) và chạy phần tự kiểm ở cuối tài liệu đó. Hai
việc hay quên nhất:

- Khai route ở gateway cho tiền tố mới (A4) — CI có `GatewayRouteCoverageTest` kiểm.
- Gọi thử **qua gateway cổng 8080**, không gọi thẳng cổng của service. Gọi thẳng thì bỏ qua
  cả định tuyến lẫn lớp kiểm token vòng ngoài, đúng hai thứ hay hỏng nhất.

### Lấy danh tính người gọi

Không controller nào được nhận `userId`, `instructorId` hay `createdBy` từ client, dù qua
query param hay qua body. Nhận `AuthenticatedUser user` rồi dùng `user.userId()`. Chi tiết ở
[authentication.md](authentication.md).

### Tài liệu nên đọc

| Làm phần nào | Đọc gì |
|---|---|
| Bất cứ endpoint nào | [api-conventions.md](api-conventions.md) — luật đặt đường dẫn, mã lỗi, phân quyền |
| Bất cứ endpoint nào | [authentication.md](authentication.md) — lấy danh tính, chặn theo vai trò |
| Kafka, sự kiện | [notifications.md](notifications.md), [shared-contracts.md](shared-contracts.md) |
| Entity, migration | [database-design.md](database-design.md) |
| Quy ước chung | [CONTRIBUTING.md](../CONTRIBUTING.md) |
