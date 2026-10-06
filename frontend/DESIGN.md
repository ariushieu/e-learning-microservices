# Hệ thống giao diện HUNRE E-Learning

Khung giao diện chung cho cả nhóm. Mục tiêu: năm người làm năm service mà web vẫn trông như **một**
sản phẩm.

**Xem trực quan:** chạy web rồi mở [`/design`](http://localhost:3000/design). Trang này có mọi màu,
cỡ chữ, component, khối dùng chung và khuôn trang, kèm tên file để import.

- [Làm một trang mới trong 5 bước](#làm-một-trang-mới-trong-5-bước)
- [Màu](#màu)
- [Chữ](#chữ)
- [Khuôn trang](#khuôn-trang)
- [Khối dùng chung](#khối-dùng-chung)
- [Việc thiết kế tiếp theo của từng người](#việc-thiết-kế-tiếp-theo-của-từng-người)
- [Kiểm tra trước khi mở pull request](#kiểm-tra-trước-khi-mở-pull-request)

## Làm một trang mới trong 5 bước

1. **Chọn khuôn** trong `src/components/templates/` (bảng ở [Khuôn trang](#khuôn-trang)). Mỗi trang
   dùng đúng một khuôn.
2. **Ghép khối có sẵn** trong `src/components/common/` và `src/components/ui/` (shadcn).
3. **Chỉ tự vẽ phần riêng của service mình**, đặt trong `src/components/<service>/`. Ví dụ thẻ câu hỏi
   của quiz, trình soạn đề cương của course.
4. **Màu qua token, chữ qua thang chữ.** Không viết mã màu, không dùng `emerald-600`, `blue-50`...
5. **Đủ ba trạng thái:** đang tải (`Skeleton` hoặc `loading.tsx`), trống (`EmptyState`), lỗi
   (`ErrorAlert`).

Thiếu một khối dùng chung thì báo Hiếu thêm vào `common/`. Đừng copy một khối rồi sửa lặt vặt
trong thư mục mình, vì làm vậy là có hai bản lệch nhau.

## Màu

Màu chủ đạo **xanh lá rừng**, gắn với tài nguyên – môi trường. Mọi màu khai báo trong
`src/app/globals.css`.

| Token | Dùng cho |
|---|---|
| `primary` (#166534) | Nút chính, liên kết, mục đang chọn |
| `primary-soft` / `primary-strong` | Nền nhạt của mục đang chọn / chữ đặt trên nền nhạt, hover nút |
| `sidebar` (#0B3B24) | Sidebar khu giảng dạy, dải tiêu đề trang chi tiết, trang đăng nhập |
| `background` / `card` | Nền trang (trắng ngả xanh) / thẻ, bảng, form (trắng) |
| `muted-foreground` | Chữ phụ, nhãn |

Màu trạng thái, mỗi màu có ba mức: `x` (đậm: icon, nút), `x-soft` (nền nhạt), `x-strong` (chữ trên
nền nhạt). Cả ba mức đều đạt tương phản ≥ 4.5:1.

| Tông | Token | Khi nào |
|---|---|---|
| Thông tin | `info` | Đang học, đang làm, gợi ý |
| Thành công | `success` | Đã xuất bản, đã nộp, lưu xong |
| Cảnh báo | `warning` | Chờ duyệt, hết giờ |
| Lỗi / hủy | `destructive` | Đã hủy, chưa đạt, xóa |
| **Thành tích** | `achievement` (vàng) | **Hoàn thành, đạt, chứng chỉ** |

"Hoàn thành/đạt" dùng **vàng thành tích**, không dùng xanh lá, vì xanh lá đã là màu chính. Một ô
xanh lá sẽ không cho biết đó là "nút" hay "đạt".

```tsx
<div className="rounded-xl bg-info-soft p-4 text-info-strong">…</div>   // đúng
<div className="rounded-xl bg-blue-50 p-4 text-blue-700">…</div>        // sai
```

Trạng thái của backend luôn hiển thị qua `<StatusBadge status={enrollment.status} />`. Khi có trạng
thái mới, thêm vào `STATUS_TONES` (`common/status-badge.tsx`) và `LABELS` (`lib/format.ts`).

## Chữ

Font **Be Vietnam Pro**, đủ dấu tiếng Việt. Dùng lớp của thang chữ, không tự ghép
`text-2xl font-semibold tracking-tight`:

| Lớp | Dùng cho |
|---|---|
| `text-display` | Tiêu đề dải hero |
| `text-title` | Tiêu đề trang (h1), `PageHeader` đã dùng sẵn |
| `text-heading` | Tiêu đề phần (h2), dùng qua `<Section>` |
| `text-subheading` | Tiêu đề thẻ, nhóm |
| `text-sm text-muted-foreground` | Chữ phụ, meta |
| `text-caption` | Chú thích nhỏ nhất |
| `text-eyebrow` | Nhãn nhỏ viết hoa phía trên tiêu đề |

Số liệu thêm `tabular-nums` để các chữ số thẳng cột.

## Khuôn trang

Layout của route group đã lo header, sidebar, lề và chiều rộng. **Trang không tự bọc container.**

| Khuôn | Bố cục | Trang đang dùng |
|---|---|---|
| `ListPage` + `CardGrid` | Tiêu đề → `Toolbar` (tìm, lọc) → lưới thẻ / bảng → phân trang | Trang chủ, Khóa học của tôi, Thông báo |
| `DetailPage` + `DetailHero` | Dải xanh đậm (đường dẫn, tiêu đề, meta) → nội dung chính + cột phụ dính bên phải | Chi tiết khóa, Giới thiệu bài kiểm tra, Kết quả |
| `FormPage` + `FormSection` | Tiêu đề → nhóm ô (mô tả trái, ô nhập phải) → `FormActions` | Tạo khóa học, Hồ sơ |
| `DashboardPage` | Tiêu đề → hàng `Stat` → các `Section` | Khu giảng dạy, Quản trị |
| `FocusLayout` | Thanh trên (quay lại, tiến độ) → nội dung → bảng phụ phải. Không menu chung | Trang học |

```tsx
// Trang danh sách mới
export default async function Page() {
  const data = await gateway<Page<Item>>("/api/items");
  return (
    <ListPage title="Tên trang" description="Một câu nói trang này để làm gì." actions={<Button>Tạo mới</Button>}>
      {data.content.length === 0 ? <EmptyState icon={InboxIcon} title="Chưa có gì" /> : <CardGrid>…</CardGrid>}
    </ListPage>
  );
}
```

`DetailHero` phải là phần tử **đầu tiên** của trang trong `(site)`, vì nó tự kéo sát lên header và
trải hết chiều ngang.

## Khối dùng chung

Tất cả nằm ở `src/components/common/`. Ví dụ sống có trên trang `/design`.

| Khối | Thay cho |
|---|---|
| `PageHeader`, `Crumbs` | Tiêu đề trang tự viết |
| `Section` | `<h2 className="text-lg font-semibold">` + nút bên phải |
| `FormField`, `FormSection`, `FormActions`, `NativeSelect` | Nhãn + ô nhập + ghi chú + lỗi tự ghép |
| `FactList`, `Fact` | `<dl>` nhãn – giá trị tự viết (`layout="rows"` hoặc `"grid"`) |
| `DataTableCard` | Bảng trong `Card` + trường hợp rỗng |
| `ProgressMeter` | `Progress` + tự tính `Math.round(progressPercent)` |
| `CourseCover` | Ô xám thay ảnh bìa; tự sinh bìa theo danh mục |
| `Callout` | Ô thông báo nổi bật tô màu tay (chúc mừng, nhắc ghi danh) |
| `Stat`, `StatGrid`, `IconTile` | Ô số liệu, ô icon tự vẽ |
| `StatusBadge` | Huy hiệu trạng thái tô màu tay |
| `EmptyState`, `ErrorAlert`, `StatusPage` | Trang/khối trống, lỗi |
| `Toolbar`, `Pagination`, `PrevNextPagination` | Thanh lọc, phân trang |

Quy ước phản hồi:

- Lỗi từ API: `ErrorAlert`.
- Bấm xong thành công: `toast.success(...)`.
- Thông điệp nổi bật trong trang: `Callout`.
- Xóa hoặc lưu trữ: luôn hỏi lại bằng `AlertDialog`, không dùng `window.confirm`.

Nút:

- Mỗi màn hình chỉ có **một** nút chính (`variant` mặc định). Nút phụ dùng `outline`, nút ít quan trọng dùng `ghost`, nút xóa dùng `destructive`.
- Nút gọi hành động lớn (CTA) dùng `size="lg"`.
- Khi đang xử lý, hiện `Loader2Icon className="animate-spin"`.
- Nút chỉ có icon phải có `aria-label`.

## Việc thiết kế tiếp theo của từng người

Khung và các trang hiện có đã chuyển sang hệ thống này. Phần còn lại là component riêng của từng
service. Mỗi người thiết kế trong thư mục của mình, theo khuôn và khối ở trên.

| Người | Việc |
|---|---|
| **quocluibotre** (auth) | Form sửa hồ sơ + đổi mật khẩu trên `/profile` (`FormPage` + 2 `FormSection`, gọi `PUT /api/auth/me`, `POST /api/auth/change-password`; đổi mật khẩu xong đăng xuất). Bảng người dùng cho admin khi có API danh sách. |
| **duyd92689-debug** (course) | Trang chủ: lọc theo danh mục dạng chip, sắp xếp. Trình soạn đề cương: kéo đổi thứ tự (nếu kịp). Ô "Đánh giá" khi có API review. |
| **phamquyet19042005-netizen** (enrollment) | Trang học: ghi chú bài học, tự chuyển bài khi video hết. Chứng chỉ: trang xác minh công khai theo mã chứng chỉ. |
| **hiepdeptrai0111** (quiz) | Làm bài: lưu đáp án nháp khi lỡ tải lại trang. Kết quả: biểu đồ điểm các lượt. Soạn đề: nhập nhiều câu một lúc. |
| **Hiếu** | Thêm khối dùng chung khi có người cần; giao diện tối (token đã chừa sẵn). |

## Kiểm tra trước khi mở pull request

- [ ] Trang dùng một khuôn trong `templates/`, không tự bọc container
- [ ] Không có mã màu hoặc màu Tailwind thô (`grep -rE "(emerald|amber|blue|red|zinc)-[0-9]" src/components/<service>`)
- [ ] Trạng thái qua `StatusBadge`; ngày giờ qua `formatDate`/`formatDay` (giờ Việt Nam)
- [ ] Đủ ba trạng thái: đang tải, trống, lỗi
- [ ] Xem ở 375px, 768px, 1366px: không cuộn ngang, chữ dài không vỡ khung
- [ ] Bấm Tab đi hết trang được, thấy rõ ô đang focus; nút chỉ có icon có `aria-label`
- [ ] Không dùng emoji làm icon (dùng lucide)
- [ ] `pnpm next typegen && pnpm tsc --noEmit && pnpm lint && pnpm build` không lỗi
