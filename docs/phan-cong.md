# Bảng theo dõi công việc

> **Cập nhật lần cuối:** 08/10/2026 — `main` ở `bb5023a`
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
| Hiếu | api-gateway, web | [Khách chưa đăng nhập dùng chung xô API](#hiếu--khách-chưa-đăng-nhập-dùng-chung-xô-api) — 30 khách mở trang chủ thì 4 trang lỗi | **Cao — xong trước demo** | ~2h |
| quocluibotre | auth, web | [Quản lý phiên đăng nhập: xem máy đang đăng nhập, đăng xuất từ xa](#quocluibotre--quản-lý-phiên-đăng-nhập) | Trung bình | ~3h |
| phamquyet19042005-netizen | enrollment | [Đạt bài kiểm tra thì bài học tự hoàn thành (nghe sự kiện Kafka)](#phamquyet19042005-netizen--đạt-bài-kiểm-tra-thì-bài-học-tự-hoàn-thành) | **Cao — chuỗi sự kiện cho demo** | ~3h |
| hiepdeptrai0111 | quiz | [Xáo trộn giữ nguyên trong một lượt làm, thêm xáo đáp án](#hiepdeptrai0111--xáo-trộn-giữ-nguyên-trong-một-lượt-làm-thêm-xáo-đáp-án) — tải lại trang thì câu hỏi đổi chỗ | Trung bình | ~2h |
| duyd92689-debug | course | [Đánh giá chờ phản hồi trong khu giảng dạy, hoàn thiện #84](#duyd92689-debug--đánh-giá-chờ-phản-hồi-hoàn-thiện-84) | Trung bình | ~2h |

Đã xong ở lượt này:

- Số liệu học tập của khóa: tỉ lệ hoàn thành từng bài, bài học viên bỏ dở (#81).
- Email phân biệt dấu: `hocviên@…` không còn vào được `hocvien@…` (#82).
- Nhập câu hỏi từ CSV, một dòng sai thì không nhập câu nào (#83).
- Giảng viên trả lời đánh giá (#84).

Cả 4 pull request đều chạy thử trên Docker trước khi merge. Các điểm nhỏ còn lại ghi trong review và đã
đưa vào việc mới bên dưới.

**Luật merge mới (#77): không tự merge pull request của mình.** Ruleset của `main` giờ bắt CI xanh và
nhóm trưởng duyệt. Mở pull request, chờ CI, rồi để đó: nhóm trưởng chạy thử trên Docker rồi duyệt và merge
bằng **Squash and merge**. CODEOWNERS đã sửa đúng tên GitHub của `DuyJunior` và `Quytsdragon`, nên giờ
pull request đụng phần của hai bạn sẽ tự mời đúng người.

**Khung giao diện đã có** (#54): mọi việc web ở trên làm theo [frontend/DESIGN.md](../frontend/DESIGN.md)
và trang `/design`. Chạy cả hệ thống: `docker compose --profile app up -d --build --wait` rồi mở
http://localhost:3000.

**Collection Postman: đủ cả 7 file**, đều đã chạy thật trên Docker — auth, course, enrollment, quiz
(#56, #57, #59, #60, cập nhật ở #64, #65, #67, #69, #70, #71, #73, #75, #78, #79, #81–#84) và gateway, notification, demo-flow (#66). Collection auth (#78) và enrollment (#81, kèm kiểm giao diện bằng Chromium) chạy luôn trong CI ở mọi pull request. Trước buổi demo, chạy
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

**Gateway giới hạn số lần đăng nhập** (#34): mỗi tài khoản 10 lần liền, sau đó 6 giây mới được
thêm một lần; mỗi máy 60 lần liền cho mọi tài khoản. Chạy Postman Runner dồn dập vẫn có thể nhận
**429**. Đó là gateway chặn đúng, không phải lỗi của service. Chờ một phút, hoặc tắt hẳn trong lúc test:

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
| quocluibotre | `docs/postman/auth.postman_collection.json` | [auth.md](test-cases/auth.md) | Xong #57, #65, #70, #78, #82 — 739/739 assertion (3 ca BLOCKED vì cần fixture riêng); chạy trong CI |
| duyd92689-debug | `docs/postman/course.postman_collection.json` | [course.md](test-cases/course.md) | Xong #56, #64, #69, #79, #84 — 973/973 |
| phamquyet19042005-netizen | `docs/postman/enrollment.postman_collection.json` | [enrollment.md](test-cases/enrollment.md) | Xong #60, #73, #81 — 432/432; chạy trong CI |
| hiepdeptrai0111 | `docs/postman/quiz.postman_collection.json` | [quiz.md](test-cases/quiz.md) | Xong #59, #67, #71, #75, #83 — 548/548 |
| Hiếu | `gateway`, `notification`, `demo-flow` | [gateway.md](test-cases/gateway.md), [notification.md](test-cases/notification.md) | Xong #66, #76 — gateway 33/33 ca (G-RATE 37/37 assertion), notification 55/55 ca, demo-flow 81/81 |

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

### quocluibotre — quản lý phiên đăng nhập

> Mỗi lần đăng nhập, auth-service lưu một refresh token kèm `user_agent`. Nhưng người dùng không xem được
> mình đang đăng nhập ở những máy nào, và không đăng xuất từ xa được: quên đăng xuất ở phòng máy trường
> thì chỉ còn cách đổi mật khẩu. Thêm nữa, mọi lần đăng nhập qua web đang lưu `user_agent = "node"`, vì
> server Next.js gọi gateway bằng user agent của chính nó (xem trong bảng `refresh_tokens` trên Docker).

**Cần làm.**

- auth-service:
  - Access token mang thêm claim `sid`: id của refresh token sinh cùng lúc, cả khi đăng nhập lẫn khi đổi
    token.
  - `GET /api/auth/sessions`: các phiên còn hạn, chưa thu hồi của người gọi, mới nhất trước. Mỗi phiên
    gồm:
    - `id` và `createdAt`.
    - `device`: trình duyệt + hệ điều hành rút từ user agent, ví dụ "Edge trên Windows". Không nhận ra thì
      ghi "Thiết bị khác".
    - `current`: `true` nếu `id` trùng `sid` trong token.
  - `DELETE /api/auth/sessions/{id}`: thu hồi một phiên của chính mình. Phiên của người khác trả 404,
    không trả 403, để không lộ id nào tồn tại.
  - `POST /api/auth/sessions/revoke-others`: thu hồi mọi phiên trừ phiên hiện tại.
  - Không trả IP: qua frontend thì IP luôn là IP container (lý do ghi ở #76).
  - README ghi rõ: phiên bị thu hồi thì refresh token hết dùng ngay, còn access token vẫn chạy tới khi hết
    hạn.
- Web:
  - Server Next.js chuyển `User-Agent` của trình duyệt khi gọi đăng nhập và đổi token. Phần này nằm trong
    `lib/` của Hiếu: sửa luôn trong pull request này, CODEOWNERS sẽ mời Hiếu review.
  - Trang hồ sơ thêm mục "Phiên đăng nhập":
    - Danh sách phiên; phiên đang dùng có nhãn "Thiết bị này".
    - Nút đăng xuất từng phiên, và nút "Đăng xuất mọi thiết bị khác" có hộp xác nhận.
- Hoàn thiện #82: email có khoảng trắng ở đầu hoặc cuối đang bị `@Email` trả 400 trước khi tới `trim()`.
  Cắt khoảng trắng trước khi kiểm, ở cả đăng ký và đăng nhập.

**Tự kiểm.**

- Đăng nhập bằng Edge và bằng Postman → có 2 phiên. Xem từ Edge thì phiên Edge mang nhãn "Thiết bị này".
- Từ web đăng xuất phiên Postman → Postman gọi `refresh-token` nhận 401.
- "Đăng xuất mọi thiết bị khác" → chỉ còn phiên hiện tại, web vẫn dùng tiếp được.
- Xóa phiên của người khác → 404. Không token → 401.
- Đăng nhập bằng `" hocvien@hunre.edu.vn "` (có khoảng trắng) → 200.

Thêm nhóm ca vào `auth.md` và collection. Collection auth chạy trong CI, nên ca mới phải ổn định.

---

### phamquyet19042005-netizen — đạt bài kiểm tra thì bài học tự hoàn thành

> Bài kiểm tra gắn được với bài học (`quizzes.lesson_id`), nhưng làm đạt rồi học viên vẫn phải tự bấm
> "Đánh dấu hoàn thành". Vì vậy số liệu #81 thấp hơn thực tế. quiz-service đã phát `QuizGradedEvent`
> lên Kafka sau mỗi lần chấm (notification-service đang nghe), nên enrollment-service chỉ cần nghe thêm.
> Đây cũng là chỗ demo rõ nhất cảnh các service nói chuyện với nhau qua sự kiện.

**Cần làm.**

- Sự kiện:
  - shared-common: thêm `lessonId` (có thể null) vào `QuizGradedEvent`. Record đã có `ignoreUnknown`,
    nên notification-service bản cũ vẫn đọc được.
  - quiz-service: điền `lessonId` của quiz khi phát sự kiện. Chỉ một dòng ở `QuizAttemptServiceImpl`;
    CODEOWNERS sẽ mời hiepdeptrai0111 và Hiếu review.
- enrollment-service, consumer mới cho topic sự kiện quiz (group riêng):
  - Chỉ xử lý khi thỏa cả bốn điều kiện:
    - `passed` là true.
    - `lessonId` khác null.
    - Học viên có lượt ghi danh `ACTIVE` vào `courseId`.
    - Bài có trong đề cương của snapshot.

    Thiếu điều kiện nào thì bỏ qua, không báo lỗi.
  - Đánh dấu bài `COMPLETED` bằng đúng đường đi của `PUT /api/lessons/{id}/progress`, để tiến độ, hoàn
    thành khóa, chứng chỉ và thông báo chạy y như khi học viên tự bấm. Bài đã xong thì giữ nguyên (#50).
  - Kafka giao lại cùng một sự kiện thì không làm hai lần: dựa vào `eventId`.
  - MySQL tắt giữa chừng thì thử lại, như consumer `course.updated` (#41).
- Web, trang học: bài có bài kiểm tra hiện dòng "Đạt bài kiểm tra thì bài này tự hoàn thành". Làm đạt
  xong, quay lại trang học thấy bài đã được tích.

**Tự kiểm.** Khóa 2 bài, S đã xong bài 1, quiz gắn với bài 2:

- S làm đạt quiz → vài giây sau bài 2 `COMPLETED`, khóa `COMPLETED`, có chứng chỉ và thông báo.
- S làm trượt → không đổi.
- Quiz không gắn bài nào → không đổi.
- Tác giả làm thử (không ghi danh) → không lỗi, không tạo gì.
- Lượt ghi danh đã `CANCELLED` → bỏ qua.
- Gửi cùng một sự kiện hai lần → chỉ một lần cập nhật.

Thêm nhóm ca vào `enrollment.md` và collection. Collection enrollment chạy trong CI (#81), nên chờ sự kiện
bằng cách hỏi lại vài lần (tối đa vài giây), không `sleep` cố định.

---

### hiepdeptrai0111 — xáo trộn giữ nguyên trong một lượt làm, thêm xáo đáp án

> Thấy khi review #83: bật "Xáo trộn câu hỏi" thì `GET /api/quizzes/{id}/take` gọi `Collections.shuffle`
> **mỗi lần tải**. Học viên tải lại trang giữa chừng thì thứ tự câu đổi hết, câu đang làm dở nhảy chỗ.
> Đáp án thì chưa xáo được: cả lớp thấy đáp án đúng ở cùng một vị trí, dễ nhắc nhau "câu 3 chọn B".

**Cần làm.**

- quiz-service:
  - Thứ tự xáo cố định theo lượt làm:
    - `/take` nhận người gọi và tìm lượt `IN_PROGRESS` của họ. Lấy id lượt đó làm seed
      (`new Random(attemptId)`).
    - Cùng một lượt thì tải bao nhiêu lần cũng ra một thứ tự; lượt mới thì thứ tự mới. Không cần lưu thêm
      bảng nào.
  - Thêm cài đặt `shuffleOptions` (migration mới, mặc định `false`): xáo đáp án trong từng câu theo cùng
    seed. Câu `TRUE_FALSE` giữ thứ tự Đúng/Sai.
  - Chấm điểm vẫn theo id đáp án, không theo vị trí.
  - Những chỗ sau luôn hiện thứ tự gốc của đề: trang kết quả, thống kê (#71), CSV (#75), trang soạn đề của
    tác giả.
- Web, cài đặt đề: thêm ô "Xáo trộn đáp án" cạnh "Xáo trộn câu hỏi".

**Tự kiểm.** Đề 5 câu, bật cả hai cài đặt:

- Trong một lượt, S tải `/take` 5 lần → cùng thứ tự câu và đáp án.
- S nộp rồi làm lượt mới → thứ tự khác.
- Chọn đúng hết theo nội dung → 100 điểm dù vị trí đã đổi.
- Câu `TRUE_FALSE` vẫn Đúng trước Sai.
- Tắt cả hai cài đặt → thứ tự gốc.

Thêm nhóm ca vào `quiz.md` và collection.

---

### duyd92689-debug — đánh giá chờ phản hồi, hoàn thiện #84

> Từ #84 giảng viên trả lời được đánh giá, nhưng phải mở từng trang khóa mới biết có đánh giá mới. Giảng
> viên có 5 khóa thì không ai đi mở hết, và đánh giá chê cứ nằm đó không ai trả lời.

**Cần làm.**

- course-service:
  - `GET /api/instructor/reviews?replied=false&courseId=&page=&size=`:
    - Trả đánh giá trên các khóa của người gọi (admin thấy mọi khóa), mới nhất trước.
    - Mỗi dòng thêm `courseId` và `courseTitle`.
    - Trả thêm `unrepliedCount`.
    - Chỉ INSTRUCTOR và ADMIN; học viên 403. Lọc theo `courseId` của khóa người khác → 403.
  - Khai route ở gateway (A4).
- Web, khu giảng dạy:
  - Trang `/instructor/reviews`:
    - Danh sách thẻ đánh giá, lọc "Chưa trả lời / Đã trả lời / Tất cả" và theo khóa.
    - Trả lời ngay tại chỗ bằng `ReviewReply` của #84.
  - Sidebar thêm mục "Đánh giá" kèm số chưa trả lời; trang tổng quan khu giảng dạy thêm một thẻ.
- Hoàn thiện #84 (đã ghi trong review):
  1. Gõ lại nội dung hợp lệ thì xóa lỗi của ô ngay (`setFieldError(undefined)` trong `onChange`). Bộ đếm
     `x/1000` hiện lại.
  2. Phản hồi của admin mang nhãn "Phản hồi của quản trị viên": so `repliedBy` với `instructorId` của khóa
     ở server.
  3. Ghi vào README: `updatedAt` của đánh giá đổi cả khi có phản hồi, nên đừng dùng trường này để biết học
     viên có sửa đánh giá hay không.

**Tự kiểm.**

- A có 2 khóa, mỗi khóa một đánh giá chưa trả lời → danh sách 2, đếm 2.
- Trả lời một đánh giá → đếm 1; bộ lọc "Đã trả lời" thấy nó.
- B không thấy đánh giá trên khóa của A; lọc theo `courseId` của A → 403.
- S → 403. Admin thấy tất cả.
- Admin trả lời → nhãn "Phản hồi của quản trị viên".

Thêm nhóm ca vào `course.md` và collection.

---

### Hiếu — khách chưa đăng nhập dùng chung xô API

> Cùng gốc với lỗi đăng nhập đã sửa ở #76: trang web render trên server Next.js, nên mọi khách chưa đăng
> nhập đều mang IP của container frontend và dùng chung **một** xô API (20 request/giây, dồn tối đa 40).
> Đã thử trên Docker: 30 khách mở trang chủ cùng lúc thì **4/30 trang** hiện "Không tải được danh sách
> khóa học — Bạn gửi quá nhiều yêu cầu", gateway chặn 20 lời gọi của `ip:172.27.0.12`. Người đã đăng nhập
> không bị, vì xô API đếm theo id người dùng.

**Cần làm.**

- Gateway nhận ra request do server frontend gửi bằng một khóa bí mật dùng chung, đặt trong header và
  `.env` (không commit). Request đó đếm ở một xô riêng, rộng hơn hẳn.
  - Request từ ngoài không có khóa đúng thì vẫn đếm theo IP như cũ.
  - Không đọc `X-Forwarded-For`, vì lý do đã ghi ở #76.
- Frontend gửi khóa này trong mọi lời gọi gateway từ server.
- Ca G-RATE mới. Chạy lại phép thử 30 khách → không trang nào lỗi.
- Build lại image frontend trong Docker: image đang chạy vẫn là bản trước #69.

**Sau demo:** kênh email cho thông báo (`emailEnabled` đã lưu nhưng chưa có kênh gửi; notification-service
cũng chưa biết email người dùng).

---

## Đã xong

| Ngày | PR | Việc | Người |
|---|---|---|---|
| 08/10 | #84 | Giảng viên của khóa và admin trả lời, sửa, xóa phản hồi dưới từng đánh giá | duyd92689-debug |
| 08/10 | #83 | Nhập câu hỏi từ CSV: file mẫu, lỗi theo số dòng, đọc được file Excel; route `/api` chuyển nguyên byte file | hiepdeptrai0111 |
| 08/10 | #82 | Email phân biệt dấu (V5 sang `utf8mb4_bin`, dừng nếu trùng), đăng ký chỉ nhận email ASCII | quocluibotre |
| 08/10 | #81 | Số liệu học tập của khóa: tiến độ trung bình, tỉ lệ hoàn thành từng bài, làm nổi bài bị bỏ dở; collection enrollment và kiểm Chromium chạy trong CI | phamquyet19042005-netizen |
| 08/10 | #79 | Admin gỡ đánh giá vi phạm, điểm trung bình tính lại ngay | duyd92689-debug |
| 08/10 | #78 | Trang tổng quan quản trị, thống kê người dùng; bảng người dùng dạng thẻ trên điện thoại; collection auth chạy trong CI | quocluibotre |
| 08/10 | #77 | Ruleset bắt CI xanh và nhóm trưởng duyệt; sửa tên GitHub trong CODEOWNERS | Hiếu |
| 08/10 | #76 | Giới hạn đăng nhập theo tài khoản và theo máy, thay cho chung một xô IP; tách xô đăng nhập khỏi xô API trong Redis | Hiếu |
| 08/10 | #75 | Xuất kết quả bài kiểm tra ra CSV, chống chèn công thức Excel | hiepdeptrai0111 |
| 08/10 | #73 | Giảng viên xem học viên của khóa: tiến độ, ngày hoàn thành, mã chứng chỉ; lọc và sắp xếp | phamquyet19042005-netizen |
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
