# Bảng theo dõi công việc

> **Cập nhật lần cuối:** 07/10/2026 — `main` ở `04f3d16`
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
| **Cả nhóm** | service của mình | [Collection Postman của service mình + tự test](#cả-nhóm--collection-postman-của-service-mình) | **Cao** — demo mỗi người tự test service mình | ~2h/người |
| quocluibotre | auth (web) | [Trang hồ sơ: sửa thông tin, đổi mật khẩu](#quocluibotre--trang-hồ-sơ-sửa-thông-tin-đổi-mật-khẩu) | **Cao** — API #48 đã có nhưng web chưa dùng | ~1h30 |
| hiepdeptrai0111 | quiz (web) | [Báo "cần ghi danh" trên trang bài kiểm tra](#hiepdeptrai0111--báo-cần-ghi-danh-trên-trang-bài-kiểm-tra) | Trung bình — phần web của #49 | ~45 phút |
| duyd92689-debug | course (web) | [Lọc nhanh và sắp xếp ở trang chủ](#duyd92689-debug--lọc-nhanh-và-sắp-xếp-ở-trang-chủ) | Trung bình | ~1h |
| phamquyet19042005-netizen | enrollment | [Trang xác minh chứng chỉ công khai](#phamquyet19042005-netizen--trang-xác-minh-chứng-chỉ-công-khai) | Trung bình | ~2h |
| Hiếu | gateway, notification | Collection Postman `gateway`, `notification` và `demo-flow` (luồng xuyên service) | **Cao** | ~2h |
| Hiếu | api-gateway | Giới hạn đăng nhập theo IP thật của người dùng khi đi qua frontend | Thấp — sau demo | ~1h |

**Khung giao diện đã có** (#54): mọi việc web ở trên làm theo [frontend/DESIGN.md](../frontend/DESIGN.md)
và trang `/design`. Chạy cả hệ thống: `docker compose --profile app up -d --build --wait` rồi mở
http://localhost:3000.

**Mỗi người tự test service của mình bằng Postman** — mỗi người một collection riêng, theo quy ước
chung ở [mục dưới](#cả-nhóm--collection-postman-của-service-mình) để file nào import vào cũng chạy
ngay, không phải sửa gì.

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
| phamquyet19042005-netizen | Ghi danh, trang học + tiến độ, khóa của tôi, chứng chỉ | `components/enrollment/`, `app/(learn)/`, `app/(site)/my-courses/`, `app/(site)/certificates/` |
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
gateway lẫn từng service. Gateway giới hạn số request bằng Redis. Toàn bộ 419 test xanh. Cả hệ thống chạy được bằng một lệnh
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

**Không mất sự kiện khi một phần hệ thống chết:**

- Nộp bài đi qua outbox (#32): tắt Kafka thì bài làm vẫn lưu, bật lại là thông báo tới.
- notification-service (#38) và enrollment-service (#41): MySQL tắt giữa chừng thì consumer
  thử lại tới khi MySQL lên. notification-service chuyển message hỏng sang topic `.DLT`.
- course-service: enrollment-service chết thì đề cương vẫn xem được, chỉ ẩn nội dung bài
  thường (#37). Sự kiện khóa học giờ cũng đi qua outbox (#44): Kafka chết thì sự kiện nằm chờ, và
  snapshot cũ không cho người lạ ghi danh vào khóa đã lưu trữ.

**Lỗ hổng đang mở:** không còn. Chưa ghi danh thì không làm được bài kiểm tra (#49); bài đã hoàn
thành không bị hạ về "đang học" (#50); số học viên đếm mỗi người một lần dù hủy rồi ghi danh lại
(#51). Cả ba đã chạy thử thật trước khi merge.

---

## Chi tiết từng việc

### Cả nhóm — collection Postman của service mình

Mỗi người làm **một file collection cho service mình** và tự chạy hết file tình huống test của
service đó. Không gộp chung một file: năm người cùng sửa một JSON lớn thì lần merge nào cũng xung
đột.

| Người | File | Chạy tình huống |
|---|---|---|
| quocluibotre | `docs/postman/auth.postman_collection.json` | [auth.md](test-cases/auth.md) |
| duyd92689-debug | `docs/postman/course.postman_collection.json` (chuyển từ `course-service-v2`) | [course.md](test-cases/course.md) |
| phamquyet19042005-netizen | `docs/postman/enrollment.postman_collection.json` | [enrollment.md](test-cases/enrollment.md) |
| hiepdeptrai0111 | `docs/postman/quiz.postman_collection.json` | [quiz.md](test-cases/quiz.md) |
| Hiếu | `gateway`, `notification`, `demo-flow` | [gateway.md](test-cases/gateway.md), [notification.md](test-cases/notification.md) |

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

### quocluibotre — trang hồ sơ: sửa thông tin, đổi mật khẩu

> API đã có ở #48 (chạy thử 18/18 ca). Một API không có chỗ dùng trên web coi như chưa xong.

**Cần làm** trong `app/(site)/profile/` + `components/auth/`, theo khuôn `FormPage` (xem `/design`):

- Thẻ **"Thông tin cá nhân"** (`FormSection`): họ tên, số điện thoại → `PUT /api/auth/me`.
  Lưu xong `toast.success` và `router.refresh()`. Lỗi 400 hiện dưới đúng ô bằng `fieldErrorMap`.
- Thẻ **"Đổi mật khẩu"**: mật khẩu hiện tại, mật khẩu mới, nhập lại (so khớp ngay trên trình duyệt)
  → `POST /api/auth/change-password`. Sai mật khẩu cũ → lỗi dưới ô "Mật khẩu hiện tại"
  (`fieldErrors.currentPassword`). Thành công → gọi `logoutAction` (refresh token đã bị thu hồi)
  và về `/login`.
- Bỏ `Callout` "sắp có" đang đặt tạm ở trang hồ sơ.

**Lưu ý.** Tên trên menu tài khoản đọc từ access token, nên sau khi sửa họ tên menu vẫn hiện tên cũ
tới khi token được làm mới (tối đa 15 phút). Trang hồ sơ đọc `GET /api/auth/me` nên hiện tên mới ngay.

**Tự kiểm.** Sửa tên → trang hồ sơ hiện tên mới. Đổi mật khẩu sai mật khẩu cũ → lỗi dưới ô, vẫn đăng
nhập. Đổi đúng → bị đăng xuất; đăng nhập mật khẩu cũ → báo sai, mật khẩu mới → vào được.

---

### hiepdeptrai0111 — báo "cần ghi danh" trên trang bài kiểm tra

> Phần web của #49. Backend đã trả 403 khi chưa ghi danh, nhưng web đang hiện thông báo lỗi chung.

**Cần làm** ở `app/(site)/quizzes/[id]/`:

- Trang giới thiệu bài kiểm tra kiểm trước bằng `gatewayOrNull("/api/progress?courseId=…")`
  (404 = chưa ghi danh). Chưa ghi danh (và không phải người tạo bài hay admin) thì thay nút
  "Bắt đầu làm bài" bằng `Callout` tone="info" *"Bạn cần ghi danh khóa học để làm bài kiểm tra"* kèm
  nút **Ghi danh** dẫn về `/courses/{courseId}`.
- Bấm "Bắt đầu" mà vẫn nhận 403 (ví dụ vừa hủy ghi danh ở tab khác) → hiện đúng `Callout` đó thay vì
  lỗi chung. Nhận 502 → `ErrorAlert` *"Không kiểm tra được ghi danh, thử lại sau"*.

**Tự kiểm.** Tài khoản chưa ghi danh mở bài kiểm tra → thấy Callout, không có nút Bắt đầu. Ghi danh
xong quay lại → làm được. Giảng viên tạo bài → làm thử được.

---

### duyd92689-debug — lọc nhanh và sắp xếp ở trang chủ

> `studentCount` giờ đã đúng (#51), nên sắp xếp theo số học viên có ý nghĩa.

**Cần làm** ở `app/(site)/page.tsx` + `components/course/catalog-filters.tsx`:

- Hàng **chip danh mục** (danh mục gốc) phía trên lưới khóa học: bấm là lọc, chip đang chọn tô
  `bg-primary-soft text-primary-strong`, có chip "Tất cả". Vẫn là link GET (`?categoryId=`), giữ
  `keyword` và `level` đang có.
- Ô **Sắp xếp**: Mới nhất, Nhiều học viên, Giá thấp → cao, Giá cao → thấp. Dùng `?sort=` của
  backend (kiểm lại các trường cho phép sort ở course-service; trường sai phải trả 400 chứ không 500).

**Tự kiểm.** Chọn chip → URL có `categoryId`, lưới chỉ còn khóa của danh mục đó; đổi sắp xếp → thứ
tự đúng; tìm kiếm vẫn giữ bộ lọc. Xem ở 375px: chip cuộn ngang được, không tràn trang.

---

### phamquyet19042005-netizen — trang xác minh chứng chỉ công khai

Chứng chỉ có mã `CERT-…` nhưng chưa ai kiểm được mã đó là thật. Nhà tuyển dụng cần một trang công
khai để tra.

**Cần làm.**

- enrollment-service: `GET /api/certificates/verify/{code}` **không cần đăng nhập** — trả tên học
  viên, tên khóa, ngày cấp, mã; không trả email hay id nội bộ. Mã không tồn tại → 404. Khai đường dẫn
  công khai ở cả service lẫn gateway (`elearning.security.public-paths`), và route ở gateway (A4).
- Web: trang `/verify/[code]` theo khuôn `DetailPage` (dải tiêu đề "Xác minh chứng chỉ"), hiện thông
  tin chứng chỉ với `StatusBadge` "Hợp lệ", hoặc `EmptyState` khi không tìm thấy. Trên trang chứng chỉ
  thêm dòng "Xác minh tại …/verify/CERT-…".
- Trang `/verify` không nằm trong danh sách cần đăng nhập ở `src/proxy.ts`.

**Tự kiểm.** Đăng xuất, mở `/verify/<mã thật>` → thấy đúng tên và khóa. Mã sai → không tìm thấy.
Gọi API không token qua gateway → 200; response không có email.

**Dọn thêm (tùy chọn).** #50 đã chặn hạ trạng thái ở backend, nên đoạn chờ `starting.current` trong
`components/enrollment/lesson-actions.tsx` có thể bỏ — giữ cũng không sao.

---

## Đã xong

| Ngày | PR | Việc | Người |
|---|---|---|---|
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
