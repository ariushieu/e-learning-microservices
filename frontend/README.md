# Frontend — HUNRE E-Learning

Next.js 16 (App Router) + React 19 + TypeScript + Tailwind CSS 4, quản lý gói bằng **pnpm**.

- [Chạy](#chạy)
- [Kiến trúc](#kiến-trúc)
- [Các trang](#các-trang)
- [Thêm trang mới](#thêm-trang-mới)
- [Những điều cần biết](#những-điều-cần-biết)

## Chạy

**Cả hệ thống bằng Docker** (cách dùng khi demo):

```bash
docker compose --profile app up -d --build --wait
```

Mở http://localhost:3000. Database mới chỉ có tài khoản admin `admin@elearning.hunre.edu.vn` /
`Admin@123456`; đăng ký thêm tài khoản rồi cấp quyền giảng viên ở trang **Quản trị**.

**Chỉ frontend trên máy** (backend chạy Docker hoặc IntelliJ, gateway ở cổng 8080):

```bash
cd frontend
cp .env.example .env.local
pnpm install
pnpm dev                           # http://localhost:3000

# hoặc chạy bản production trên máy
pnpm build
pnpm start
```

Trong Docker, image build ở chế độ `standalone` (Dockerfile đặt `NEXT_OUTPUT=standalone`) và chạy
`node server.js` — đó là bản `pnpm start` gọn nhẹ, image không cần mang theo pnpm và toàn bộ
`node_modules`. Trên máy thì không bật chế độ này, nên `pnpm start` chạy bình thường.

Trước khi mở pull request:

```bash
pnpm next typegen && pnpm tsc --noEmit && pnpm lint && pnpm build
```

CI chạy đúng bốn lệnh này ở job **Frontend (Next.js)**.

## Kiến trúc

```
trình duyệt ──► Next.js :3000 ──────────────► API Gateway :8080 ──► 5 service
                 │  Server Component: gọi gateway trực tiếp ở server
                 │  /api/*  (route handler): chuyển tiếp request của Client Component
                 │  proxy.ts: làm mới token, chặn trang theo vai trò
                 └  cookie httpOnly: el_access, el_refresh
```

**Trình duyệt không bao giờ cầm token.** Đăng nhập là Server Action (`src/lib/server/auth-actions.ts`):
gọi gateway, ghi access token và refresh token vào cookie `httpOnly`. JavaScript trên trang không
đọc được cookie đó, nên một đoạn script độc chèn vào trang cũng không lấy được phiên đăng nhập.

**Hai đường gọi API, cùng một đường dẫn:**

| Ở đâu | Dùng | Đi qua |
|---|---|---|
| Server Component, Server Action | `gateway()` trong `src/lib/server/gateway.ts` | thẳng tới `GATEWAY_URL` |
| Client Component | `api()` trong `src/lib/client.ts` | `/api/...` của Next.js → `src/app/api/[...path]/route.ts` gắn token từ cookie → gateway |

Đường dẫn giữ nguyên như của gateway: `api("/api/courses/1")` ở trình duyệt chính là
`GET /api/courses/1` ở gateway. Trình duyệt chỉ nói chuyện với một origin nên không cần CORS.

**Token hết hạn** (access token sống 15 phút): `src/proxy.ts` đổi cặp mới trước khi trang render,
vì Server Component không ghi được cookie. Route `/api/*` cũng tự đổi khi gateway trả 401. Refresh
token bị thu hồi ngay khi đổi, nên cặp mới phải được lưu — đây là lý do việc đổi không được nằm
trong Server Component.

**Chặn trang** ở `src/proxy.ts`: `/learn`, `/my-courses`, `/quizzes`, `/attempts`, `/notifications`,
`/certificates`, `/profile` cần đăng nhập; `/instructor/*` cần giảng viên hoặc admin; `/admin/*` cần
admin. Đây chỉ là lớp ngoài cho dễ dùng — backend vẫn kiểm lại mọi request.

**Kiểu dữ liệu** ở `src/lib/types.ts` khớp đúng tên trường DTO bên Java. Đổi DTO thì sửa ở đây.

## Các trang

| Đường dẫn | Ai | Việc |
|---|---|---|
| `/` | mọi người | Danh mục khóa học, tìm kiếm, lọc theo danh mục và trình độ |
| `/courses/[id]` | mọi người | Chi tiết khóa, đề cương, xem thử, ghi danh |
| `/learn/[courseId]` | đã ghi danh | Học bài (video YouTube/mp4, bài đọc, tệp), đánh dấu hoàn thành, tiến độ, bài kiểm tra |
| `/my-courses` | đăng nhập | Khóa đã ghi danh, tiến độ, hủy ghi danh |
| `/certificates/[enrollmentId]` | đăng nhập | Chứng chỉ, in ra A4 |
| `/quizzes/[id]` | đăng nhập | Làm bài kiểm tra có đếm giờ |
| `/attempts/[id]` | đăng nhập | Kết quả, đáp án đúng, giải thích |
| `/notifications` | đăng nhập | Hộp thư thông báo, chuông trên thanh điều hướng |
| `/profile` | đăng nhập | Hồ sơ, mã người dùng để nhờ admin cấp quyền |
| `/instructor` | giảng viên | Khóa học mình dạy (kể cả bản nháp) |
| `/instructor/courses/new`, `/instructor/courses/[id]` | giảng viên | Tạo, sửa khóa; soạn chương, bài, tài liệu; xuất bản |
| `/instructor/quizzes/[id]` | giảng viên | Soạn đề, câu hỏi, xuất bản bài kiểm tra |
| `/admin` | admin | Cấp vai trò, quản lý danh mục |

## Thêm trang mới

1. Đọc dữ liệu trong Server Component bằng `gateway()`. Chỉ phần cần bấm, gõ mới tách thành Client
   Component (`"use client"`) và gọi `api()`; sửa xong gọi `router.refresh()`.
2. `params` và `searchParams` của trang là **Promise** ở Next 16: `const { id } = await params`.
   Kiểu `PageProps<"/duong-dan/[id]">` sinh bởi `pnpm next typegen`.
3. Trang cần đăng nhập thì thêm tiền tố vào `requiredAccess()` trong `src/proxy.ts`.
4. Dùng thành phần sẵn có trong `src/components/ui.tsx` (Button, Card, Badge, Alert...) để giao diện
   đồng nhất.

Next 16 khác nhiều so với tài liệu trên mạng. Tài liệu đúng phiên bản nằm ở
`node_modules/next/dist/docs/` — xem `AGENTS.md`.

## Những điều cần biết

- **Giờ luôn hiển thị theo giờ Việt Nam** (`formatDate` trong `ui.tsx`). Container chạy giờ UTC;
  không cố định múi giờ thì trang render ở server lệch 7 tiếng so với trình duyệt.
- **Nội dung thông báo có thẻ `<b>`** từ mẫu nhưng tên khóa học chèn vào không được backend escape.
  Hiển thị qua `safeNotificationHtml()`, chỉ giữ lại đúng thẻ `<b>`.
- **Giới hạn đăng nhập theo IP** của gateway (10 lần/phút) giờ tính theo IP của server Next.js, tức
  là chung cho mọi người dùng web. Khi demo nhiều người đăng nhập dồn dập, tạm tắt bằng
  `RATE_LIMIT_ENABLED=false docker compose --profile app up -d api-gateway`.
- `COOKIE_SECURE=true` khi web chạy sau HTTPS; để `false` với `http://localhost`, nếu không trình
  duyệt bỏ cookie và không ai đăng nhập được.
