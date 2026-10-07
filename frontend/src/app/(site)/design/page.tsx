import {
  AwardIcon,
  BellIcon,
  BookOpenIcon,
  CheckIcon,
  ClockIcon,
  GraduationCapIcon,
  InfoIcon,
  LayoutDashboardIcon,
  ListVideoIcon,
  PaletteIcon,
  PlusIcon,
  SearchIcon,
  TrophyIcon,
  UsersIcon,
  XIcon,
} from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import type { ReactNode } from "react";
import { Callout } from "@/components/common/callout";
import { CourseCover } from "@/components/common/course-cover";
import { DataTableCard } from "@/components/common/data-table-card";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorAlert } from "@/components/common/error-alert";
import { Fact, FactList } from "@/components/common/fact-list";
import { FormField } from "@/components/common/form-field";
import { IconTile } from "@/components/common/icon-tile";
import { NativeSelect } from "@/components/common/native-select";
import { Crumbs } from "@/components/common/page-header";
import { Pagination } from "@/components/common/pagination";
import { ProgressMeter } from "@/components/common/progress-meter";
import { Section } from "@/components/common/section";
import { Stat, StatGrid } from "@/components/common/stat";
import { STATUS_TONES, StatusBadge, type Tone } from "@/components/common/status-badge";
import { DetailHero, HeroMeta } from "@/components/templates/detail-page";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { ChoiceDemo, DialogDemo, LoadingButtonDemo, TabsDemo, ToastDemo } from "./demos";

export const metadata: Metadata = { title: "Hệ thống giao diện" };

const TOC = [
  ["mau", "Màu"],
  ["chu", "Chữ"],
  ["hinh-khoi", "Bo góc, bóng"],
  ["nut", "Nút"],
  ["form", "Form"],
  ["trang-thai", "Trạng thái"],
  ["phan-hoi", "Phản hồi"],
  ["khoi", "Khối dùng chung"],
  ["khuon", "Khuôn trang"],
  ["quy-tac", "Nên / không nên"],
  ["phan-cong", "Ai làm gì"],
] as const;

const COLOR_GROUPS: { title: string; items: { token: string; hex: string; use: string; dark?: boolean }[] }[] = [
  {
    title: "Thương hiệu",
    items: [
      { token: "primary", hex: "#166534", use: "Nút chính, liên kết, mục đang chọn", dark: true },
      { token: "primary-strong", hex: "#14532D", use: "Hover nút chính, chữ trên nền nhạt", dark: true },
      { token: "primary-soft", hex: "#E8F1EC", use: "Nền nhạt: mục đang chọn, ô icon" },
      { token: "sidebar", hex: "#0B3B24", use: "Sidebar, dải tiêu đề trang chi tiết", dark: true },
    ],
  },
  {
    title: "Nền và chữ",
    items: [
      { token: "background", hex: "#F7FAF8", use: "Nền trang" },
      { token: "card", hex: "#FFFFFF", use: "Thẻ, bảng, form" },
      { token: "foreground", hex: "#13221B", use: "Chữ chính", dark: true },
      { token: "muted-foreground", hex: "#5B6B63", use: "Chữ phụ, nhãn (5.6:1)", dark: true },
      { token: "muted", hex: "#EEF3F0", use: "Nền khối phụ, skeleton" },
      { token: "border", hex: "#DDE5E0", use: "Viền, đường kẻ" },
    ],
  },
];

const TONE_ROWS: { tone: Tone; name: string; token: string; use: string }[] = [
  { tone: "info", name: "Thông tin", token: "info", use: "Đang học, đang làm, gợi ý" },
  { tone: "success", name: "Thành công", token: "success", use: "Đã xuất bản, đã nộp, lưu xong" },
  { tone: "warning", name: "Cảnh báo", token: "warning", use: "Chờ duyệt, hết giờ, sắp hết hạn" },
  { tone: "danger", name: "Lỗi / hủy", token: "destructive", use: "Đã hủy, chưa đạt, xóa" },
  { tone: "achievement", name: "Thành tích", token: "achievement", use: "Hoàn thành, đạt, chứng chỉ" },
];

const TYPE_SCALE = [
  { cls: "text-display", sample: "Học mọi lúc, mọi nơi", use: "Tiêu đề dải hero" },
  { cls: "text-title", sample: "Khóa học của tôi", use: "Tiêu đề trang (h1)" },
  { cls: "text-heading", sample: "Nội dung khóa học", use: "Tiêu đề phần (h2) — dùng qua <Section>" },
  { cls: "text-subheading", sample: "Chương 1. Tổng quan", use: "Tiêu đề thẻ, nhóm" },
  { cls: "text-base", sample: "Khóa học đi từ nguyên lý tới thực hành trên chính hệ thống này.", use: "Đoạn văn" },
  { cls: "text-sm text-muted-foreground", sample: "TS. Nguyễn Văn An · 4 bài · 37 phút", use: "Chữ phụ, meta" },
  { cls: "text-caption text-muted-foreground", sample: "Cập nhật 7 thg 10, 2026", use: "Chú thích nhỏ nhất" },
  { cls: "text-eyebrow text-primary", sample: "Kiến trúc phần mềm", use: "Nhãn nhỏ trên tiêu đề" },
];

const TEMPLATES: { name: string; file: string; use: string; pages: [string, string][]; sketch: ReactNode }[] = [
  {
    name: "ListPage",
    file: "templates/list-page.tsx",
    use: "Tiêu đề → Toolbar (tìm, lọc) → lưới thẻ / bảng / dòng → phân trang.",
    pages: [
      ["/", "Trang chủ"],
      ["/my-courses", "Khóa học của tôi"],
      ["/notifications", "Thông báo"],
    ],
    sketch: (
      <>
        <Bar w="w-1/3" />
        <Bar w="w-2/3" h="h-3" muted />
        <div className="mt-2 grid grid-cols-3 gap-1.5">
          {[0, 1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="h-8 rounded bg-primary/15" />
          ))}
        </div>
      </>
    ),
  },
  {
    name: "DetailPage + DetailHero",
    file: "templates/detail-page.tsx",
    use: "Dải xanh đậm (đường dẫn, tiêu đề, meta) → nội dung chính + cột phụ dính bên phải.",
    pages: [
      ["/courses/12", "Chi tiết khóa học"],
      ["/quizzes/11", "Giới thiệu bài kiểm tra"],
    ],
    sketch: (
      <>
        <div className="-mx-3 -mt-3 mb-2 space-y-1 rounded-t-lg bg-sidebar p-3">
          <Bar w="w-1/2" light />
          <Bar w="w-1/3" h="h-2" light />
        </div>
        <div className="grid grid-cols-[1fr_35%] gap-1.5">
          <div className="space-y-1.5">
            <Bar w="w-full" h="h-6" muted />
            <Bar w="w-full" h="h-6" muted />
          </div>
          <div className="h-14 rounded bg-primary/15" />
        </div>
      </>
    ),
  },
  {
    name: "FormPage",
    file: "templates/form-page.tsx",
    use: "Tiêu đề → các FormSection (mô tả trái, ô nhập phải) → FormActions.",
    pages: [
      ["/instructor/courses/new", "Tạo khóa học"],
      ["/profile", "Hồ sơ"],
    ],
    sketch: (
      <>
        <Bar w="w-1/3" />
        {[0, 1].map((i) => (
          <div key={i} className="mt-2 grid grid-cols-[30%_1fr] gap-1.5">
            <Bar w="w-full" h="h-2" muted />
            <div className="space-y-1 rounded border bg-card p-1.5">
              <Bar w="w-full" h="h-2.5" muted />
              <Bar w="w-full" h="h-2.5" muted />
            </div>
          </div>
        ))}
      </>
    ),
  },
  {
    name: "DashboardPage",
    file: "templates/dashboard-page.tsx",
    use: "Tiêu đề → hàng ô số liệu (Stat) → các Section (bảng, danh sách).",
    pages: [
      ["/instructor", "Khóa học tôi dạy"],
      ["/admin/users", "Quản trị"],
    ],
    sketch: (
      <div className="grid grid-cols-[22%_1fr] gap-1.5">
        <div className="h-24 rounded bg-sidebar" />
        <div className="space-y-1.5">
          <div className="grid grid-cols-4 gap-1">
            {[0, 1, 2, 3].map((i) => (
              <div key={i} className="h-5 rounded border bg-card" />
            ))}
          </div>
          <Bar w="w-full" h="h-14" muted />
        </div>
      </div>
    ),
  },
  {
    name: "FocusLayout",
    file: "templates/focus-layout.tsx",
    use: "Không menu chung: thanh trên (quay lại, tiến độ) + nội dung + bảng phụ phải.",
    pages: [["/learn/12", "Trang học"]],
    sketch: (
      <>
        <Bar w="w-full" h="h-3" muted />
        <div className="mt-1.5 grid grid-cols-[1fr_30%] gap-1.5">
          <div className="aspect-video rounded bg-foreground/80" />
          <div className="space-y-1 border-l pl-1.5">
            {[0, 1, 2, 3].map((i) => (
              <Bar key={i} w="w-full" h="h-2" muted />
            ))}
          </div>
        </div>
      </>
    ),
  },
];

const OWNERS: { who: string; service: string; folder: string; build: string }[] = [
  { who: "Quốc", service: "auth", folder: "components/auth, (auth), profile, admin/users", build: "Form hồ sơ + đổi mật khẩu (FormPage + FormSection), bảng người dùng (DataTableCard)" },
  { who: "Duy", service: "course", folder: "components/course, trang chủ, courses, instructor", build: "CourseCard, CourseHero (DetailHero), trình soạn đề cương, bộ lọc (Toolbar)" },
  { who: "Quyết", service: "enrollment", folder: "components/enrollment, (learn), my-courses, certificates", build: "Trang học (FocusLayout), thẻ ghi danh (ProgressMeter, FactList), chứng chỉ" },
  { who: "Hiệp", service: "quiz", folder: "components/quiz, quizzes, attempts, instructor/quizzes", build: "Câu hỏi khi làm bài, bảng điểm (FactList grid), trình soạn câu hỏi" },
  { who: "Hiếu", service: "notification + khung", folder: "components/{ui,common,layout,templates,notification}, lib", build: "Token, khối dùng chung, khuôn trang, trang này" },
];

export default function DesignPage() {
  return (
    <>
      <DetailHero
        crumbs={[{ href: "/", label: "Trang chủ" }, { label: "Hệ thống giao diện" }]}
        eyebrow="Design system · v1"
        title="Hệ thống giao diện HUNRE E-Learning"
        description="Màu, chữ, component và khuôn trang dùng chung cho cả nhóm. Làm trang mới: chọn khuôn, ghép khối có sẵn, chỉ tự vẽ phần riêng của service mình."
        meta={
          <>
            <HeroMeta icon={<PaletteIcon />}>Xanh lá tài nguyên – môi trường</HeroMeta>
            <HeroMeta icon={<GraduationCapIcon />}>Be Vietnam Pro</HeroMeta>
            <HeroMeta icon={<LayoutDashboardIcon />}>shadcn/ui + Tailwind 4</HeroMeta>
          </>
        }
        actions={
          <Button asChild size="lg" variant="secondary">
            <a href="https://github.com/ariushieu/e-learning-microservices/blob/main/frontend/DESIGN.md">Đọc quy tắc (DESIGN.md)</a>
          </Button>
        }
      />

      <div className="grid gap-10 lg:grid-cols-[12rem_minmax(0,1fr)]">
        <nav aria-label="Mục lục" className="hidden lg:block">
          <ul className="sticky top-24 space-y-1 text-sm">
            {TOC.map(([id, name]) => (
              <li key={id}>
                <a href={`#${id}`} className="block rounded-md px-3 py-1.5 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground">
                  {name}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div className="min-w-0 space-y-16">
          <Section id="mau" title="Màu" description="Chỉ dùng tên token (bg-primary, text-info-strong...). Không viết mã màu, không dùng emerald/blue/amber của Tailwind trong component.">
            <div className="space-y-8">
              {COLOR_GROUPS.map((g) => (
                <div key={g.title} className="space-y-3">
                  <h3 className="text-subheading">{g.title}</h3>
                  <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                    {g.items.map((c) => (
                      <div key={c.token} className="flex items-center gap-3 rounded-xl border bg-card p-3">
                        <span className="size-12 shrink-0 rounded-lg ring-1 ring-black/5" style={{ background: c.hex }} />
                        <div className="min-w-0 text-sm">
                          <code className="font-mono text-xs font-semibold">{c.token}</code>
                          <p className="text-caption text-muted-foreground uppercase">{c.hex}</p>
                          <p className="truncate text-xs text-muted-foreground">{c.use}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
              <div className="space-y-3">
                <h3 className="text-subheading">Trạng thái — mỗi màu có 3 mức</h3>
                <p className="text-sm text-muted-foreground">
                  <code className="font-mono text-xs">x</code> đậm (icon, nút) · <code className="font-mono text-xs">x-soft</code> nền nhạt ·{" "}
                  <code className="font-mono text-xs">x-strong</code> chữ đặt trên nền nhạt. Hoàn thành/đạt dùng vàng thành tích, không dùng xanh lá.
                </p>
                <div className="overflow-hidden rounded-xl border bg-card">
                  {TONE_ROWS.map((t) => (
                    <div key={t.tone} className="flex flex-wrap items-center gap-4 border-b p-3 last:border-0">
                      <IconTile icon={t.tone === "achievement" ? AwardIcon : InfoIcon} tone={t.tone} size="sm" />
                      <div className="w-28 text-sm font-medium">{t.name}</div>
                      <code className="w-48 font-mono text-xs text-muted-foreground">
                        {t.token} / -soft / -strong
                      </code>
                      <span className="flex-1 text-sm text-muted-foreground">{t.use}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </Section>

          <Section id="chu" title="Chữ" description="Be Vietnam Pro, đủ dấu tiếng Việt. Dùng các lớp thang chữ thay cho tự ghép cỡ chữ + độ đậm.">
            <Card className="py-0">
              {TYPE_SCALE.map((t) => (
                <div key={t.cls} className="grid gap-1 border-b px-4 py-4 last:border-0 sm:grid-cols-[13rem_minmax(0,1fr)] sm:items-baseline">
                  <div>
                    <code className="font-mono text-xs font-semibold">{t.cls.split(" ")[0]}</code>
                    <p className="text-caption text-muted-foreground">{t.use}</p>
                  </div>
                  <p className={`${t.cls} truncate`}>{t.sample}</p>
                </div>
              ))}
            </Card>
          </Section>

          <Section id="hinh-khoi" title="Bo góc, bóng, khoảng cách">
            <div className="grid gap-4 sm:grid-cols-3">
              <div className="rounded-lg border bg-card p-4 text-sm">
                <p className="font-medium">rounded-lg</p>
                <p className="text-muted-foreground">Nút, ô nhập</p>
              </div>
              <div className="rounded-xl bg-card p-4 text-sm shadow-card ring-1 ring-border">
                <p className="font-medium">rounded-xl + shadow-card</p>
                <p className="text-muted-foreground">Thẻ (Card làm sẵn)</p>
              </div>
              <div className="rounded-xl bg-card p-4 text-sm shadow-raised ring-1 ring-border">
                <p className="font-medium">shadow-raised</p>
                <p className="text-muted-foreground">Hover thẻ, khối nổi</p>
              </div>
            </div>
            <p className="text-sm text-muted-foreground">
              Nhịp khoảng cách: trong thẻ <code className="font-mono text-xs">space-y-4</code>, giữa các phần{" "}
              <code className="font-mono text-xs">space-y-10</code>, lưới <code className="font-mono text-xs">gap-6</code>.
            </p>
          </Section>

          <Section id="nut" title="Nút" description="Mỗi màn hình chỉ một nút chính (default). Thao tác phụ: outline. Ít quan trọng: ghost. Xóa: destructive + hỏi lại.">
            <Card>
              <CardContent className="space-y-5">
                <div className="flex flex-wrap items-center gap-2">
                  <Button>Ghi danh ngay</Button>
                  <Button variant="outline">Xem trước</Button>
                  <Button variant="secondary">Lưu nháp</Button>
                  <Button variant="ghost">Bỏ qua</Button>
                  <Button variant="destructive">Hủy ghi danh</Button>
                  <Button variant="link">Xem tất cả</Button>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <Button size="lg">
                    <PlusIcon /> size=&quot;lg&quot; (CTA)
                  </Button>
                  <Button>size mặc định</Button>
                  <Button size="sm">size=&quot;sm&quot;</Button>
                  <Button size="icon" variant="outline" aria-label="Tìm kiếm">
                    <SearchIcon />
                  </Button>
                  <Button disabled>Đã tắt</Button>
                  <LoadingButtonDemo />
                </div>
                <p className="text-caption text-muted-foreground">
                  Trên màn cảm ứng nút tự cao lên 40px. Nút chỉ có icon bắt buộc có aria-label.
                </p>
              </CardContent>
            </Card>
          </Section>

          <Section id="form" title="Form" description="Nhãn luôn hiện (không chỉ placeholder), lỗi nằm ngay dưới ô, dấu * cho ô bắt buộc.">
            <Card>
              <CardContent className="grid gap-5 sm:grid-cols-2">
                <FormField id="demo-title" label="Tên khóa học" required hint="Tối đa 200 ký tự.">
                  <Input id="demo-title" defaultValue="Kiến trúc Microservices" />
                </FormField>
                <FormField id="demo-price" label="Học phí (đ)" error="Học phí không được âm">
                  <Input id="demo-price" defaultValue="-1" aria-invalid />
                </FormField>
                <FormField id="demo-level" label="Trình độ">
                  <NativeSelect id="demo-level" defaultValue="BEGINNER">
                    <option value="BEGINNER">Cơ bản</option>
                    <option value="INTERMEDIATE">Trung cấp</option>
                  </NativeSelect>
                </FormField>
                <FormField id="demo-search" label="Tìm kiếm">
                  <Input id="demo-search" placeholder="Tên khóa học, nội dung..." />
                </FormField>
                <FormField id="demo-desc" label="Mô tả" className="sm:col-span-2">
                  <Textarea id="demo-desc" placeholder="Giới thiệu ngắn về khóa học" />
                </FormField>
              </CardContent>
            </Card>
            <Card>
              <CardContent>
                <ChoiceDemo />
              </CardContent>
            </Card>
          </Section>

          <Section id="trang-thai" title="Trạng thái" description="<StatusBadge status={...}> cho mọi enum backend. Trạng thái mới: thêm vào STATUS_TONES và LABELS.">
            <Card>
              <CardContent className="space-y-4">
                <div className="flex flex-wrap gap-2">
                  {Object.keys(STATUS_TONES).map((s) => (
                    <StatusBadge key={s} status={s} />
                  ))}
                </div>
                <div className="flex flex-wrap gap-2">
                  <Badge>Badge default</Badge>
                  <Badge variant="secondary">Cơ bản</Badge>
                  <Badge variant="outline">outline</Badge>
                </div>
                <p className="text-caption text-muted-foreground">Trình độ, loại bài, loại câu hỏi: Badge variant=&quot;secondary&quot; + label(enum).</p>
              </CardContent>
            </Card>
          </Section>

          <Section id="phan-hoi" title="Phản hồi" description="Lỗi API: ErrorAlert. Sau khi bấm thành công: toast. Thông điệp nổi bật trong trang: Callout. Xóa: AlertDialog.">
            <div className="grid gap-4 lg:grid-cols-2">
              <ErrorAlert title="Không tải được khóa học" message="course-service đang tạm ngưng, thử lại sau." />
              <Callout icon={TrophyIcon} tone="achievement" title="Chúc mừng! Bạn đã hoàn thành khóa học." action={<Button variant="outline">Xem chứng chỉ</Button>}>
                Chứng chỉ đã sẵn sàng để xem và in.
              </Callout>
              <Callout icon={InfoIcon} tone="info" title="Bạn cần ghi danh để làm bài kiểm tra">
                Ghi danh miễn phí, tiến độ được lưu tự động.
              </Callout>
              <Card>
                <CardContent className="space-y-4">
                  <ToastDemo />
                  <DialogDemo />
                </CardContent>
              </Card>
            </div>
            <Card>
              <CardContent>
                <TabsDemo />
              </CardContent>
            </Card>
          </Section>

          <Section id="khoi" title="Khối dùng chung" description="Nằm ở components/common. Thấy mình sắp viết lại một trong các khối này thì import, đừng copy.">
            <div className="space-y-8">
              <Demo name="Crumbs, PageHeader, Section" file="common/page-header.tsx, common/section.tsx">
                <Crumbs items={[{ href: "#", label: "Khám phá" }, { href: "#", label: "Kiến trúc phần mềm" }, { label: "Microservices" }]} />
                <Section title="Câu hỏi" count={5} description="Tổng 5 điểm" actions={<Button size="sm" variant="outline"><PlusIcon /> Thêm câu hỏi</Button>}>
                  <p className="text-sm text-muted-foreground">Nội dung của phần…</p>
                </Section>
              </Demo>

              <Demo name="Stat, StatGrid" file="common/stat.tsx">
                <StatGrid>
                  <Stat label="Tổng số khóa" value={3} icon={BookOpenIcon} />
                  <Stat label="Học viên" value={128} icon={UsersIcon} tone="info" />
                  <Stat label="Hoàn thành" value="42%" icon={AwardIcon} tone="achievement" />
                  <Stat label="Chờ duyệt" value={1} icon={ClockIcon} tone="warning" hint="Cần xử lý" />
                </StatGrid>
              </Demo>

              <Demo name="CourseCover — ảnh bìa tự sinh theo danh mục" file="common/course-cover.tsx">
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  {["Kiến trúc phần mềm", "Cơ sở dữ liệu", "Kỹ năng", "Trắc địa bản đồ"].map((c) => (
                    <CourseCover key={c} title={c} category={c} className="rounded-xl" />
                  ))}
                </div>
              </Demo>

              <div className="grid gap-8 lg:grid-cols-2">
                <Demo name="FactList rows" file="common/fact-list.tsx">
                  <Card>
                    <CardContent>
                      <FactList>
                        <Fact label="Ghi danh" value="6 thg 10, 2026" />
                        <Fact label="Học gần nhất" value="7 thg 10, 2026" />
                        <Fact label="Trạng thái" value={<StatusBadge status="ACTIVE" />} />
                      </FactList>
                    </CardContent>
                  </Card>
                </Demo>
                <Demo name="FactList grid" file="common/fact-list.tsx">
                  <Card>
                    <CardContent>
                      <FactList layout="grid" columns={3}>
                        <Fact label="Điểm" value="4/5" hint="80%" />
                        <Fact label="Đúng" value="4 câu" />
                        <Fact label="Thời gian" value="6 phút" />
                      </FactList>
                    </CardContent>
                  </Card>
                </Demo>
                <Demo name="ProgressMeter" file="common/progress-meter.tsx">
                  <Card>
                    <CardContent className="space-y-4">
                      <ProgressMeter value={0} />
                      <ProgressMeter value={45.5} detail="2/4 bài · 46%" />
                      <ProgressMeter value={100} label="Hoàn thành" />
                    </CardContent>
                  </Card>
                </Demo>
                <Demo name="IconTile" file="common/icon-tile.tsx">
                  <div className="flex flex-wrap gap-3">
                    {(["primary", "info", "success", "warning", "danger", "achievement", "neutral"] as Tone[]).map((t) => (
                      <IconTile key={t} icon={t === "achievement" ? AwardIcon : t === "danger" ? XIcon : t === "success" ? CheckIcon : BellIcon} tone={t} />
                    ))}
                  </div>
                </Demo>
              </div>

              <Demo name="DataTableCard" file="common/data-table-card.tsx">
                <DataTableCard title="Khóa học tôi dạy" actions={<Button size="sm"><PlusIcon /> Tạo khóa học</Button>}>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Khóa học</TableHead>
                        <TableHead>Trạng thái</TableHead>
                        <TableHead className="text-right">Học viên</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {[
                        ["Kiến trúc Microservices với Spring Boot", "PUBLISHED", 24],
                        ["SQL căn bản cho người mới bắt đầu", "DRAFT", 0],
                      ].map(([t, s, n]) => (
                        <TableRow key={t as string}>
                          <TableCell className="font-medium">{t}</TableCell>
                          <TableCell>
                            <StatusBadge status={s as string} />
                          </TableCell>
                          <TableCell className="text-right tabular-nums">{n}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </DataTableCard>
              </Demo>

              <div className="grid gap-8 lg:grid-cols-2">
                <Demo name="EmptyState" file="common/empty-state.tsx">
                  <EmptyState icon={ListVideoIcon} title="Chưa ghi danh khóa nào" description="Khám phá khóa học và bắt đầu học ngay." action={<Button>Khám phá khóa học</Button>} />
                </Demo>
                <Demo name="Pagination" file="common/pagination.tsx">
                  <Pagination page={1} totalPages={6} hrefFor={(p) => `#trang-${p}`} />
                </Demo>
              </div>
            </div>
          </Section>

          <Section id="khuon" title="Khuôn trang" description="Mỗi trang chọn đúng một khuôn trong components/templates. Layout (site) / (dashboard) / (learn) đã lo header, sidebar, lề.">
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {TEMPLATES.map((t) => (
                <Card key={t.name} className="gap-3">
                  <CardHeader>
                    <div className="space-y-1.5 overflow-hidden rounded-lg border bg-background p-3">{t.sketch}</div>
                  </CardHeader>
                  <CardContent className="space-y-2">
                    <CardTitle className="font-mono text-sm">{t.name}</CardTitle>
                    <CardDescription>{t.use}</CardDescription>
                    <p className="text-caption text-muted-foreground">{t.file}</p>
                    <div className="flex flex-wrap gap-x-3 gap-y-1 pt-1 text-sm">
                      {t.pages.map(([href, name]) => (
                        <Link key={href} href={href} className="text-primary hover:underline">
                          {name}
                        </Link>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          </Section>

          <Section id="quy-tac" title="Nên / không nên">
            <div className="grid gap-4 md:grid-cols-2">
              <Rules
                good
                items={[
                  "Bắt đầu trang bằng một khuôn trong components/templates",
                  "Màu qua token: bg-primary, bg-info-soft text-info-strong",
                  "Trạng thái qua <StatusBadge>, ngày giờ qua formatDate (giờ Việt Nam)",
                  "Có đủ 3 trạng thái: đang tải (Skeleton), trống (EmptyState), lỗi (ErrorAlert)",
                  "Thử ở 375px, 768px, 1366px trước khi mở PR",
                  "Icon lucide size-4; nút chỉ có icon phải có aria-label",
                ]}
              />
              <Rules
                items={[
                  "Viết mã màu, hoặc emerald-600 / blue-50 / amber-… trong component",
                  "Tự ghép text-2xl font-semibold… thay cho text-title / <Section>",
                  "window.confirm, alert(): dùng AlertDialog, toast",
                  "Emoji làm icon; ảnh nền chữ cái đầu hoặc gradient sặc sỡ",
                  "Nhiều nút chính trên một màn hình",
                  "Copy một khối có sẵn rồi sửa lặt vặt — sửa khối gốc hoặc báo Hiếu",
                ]}
              />
            </div>
          </Section>

          <Section id="phan-cong" title="Ai làm gì" description="Ai giữ service nào thì giữ giao diện service đó. Khối dùng chung thiếu gì: báo Hiếu thêm vào common.">
            <DataTableCard>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Người</TableHead>
                    <TableHead>Service</TableHead>
                    <TableHead className="hidden md:table-cell">Thư mục</TableHead>
                    <TableHead>Thiết kế tiếp</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {OWNERS.map((o) => (
                    <TableRow key={o.who}>
                      <TableCell className="font-medium">{o.who}</TableCell>
                      <TableCell>
                        <Badge variant="secondary">{o.service}</Badge>
                      </TableCell>
                      <TableCell className="hidden max-w-64 text-xs whitespace-normal text-muted-foreground md:table-cell">{o.folder}</TableCell>
                      <TableCell className="max-w-80 text-sm whitespace-normal">{o.build}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </DataTableCard>
          </Section>
        </div>
      </div>
    </>
  );
}

function Bar({ w, h = "h-3.5", muted, light }: { w: string; h?: string; muted?: boolean; light?: boolean }) {
  return <div className={`${w} ${h} rounded ${light ? "bg-white/40" : muted ? "bg-muted-foreground/15" : "bg-foreground/70"}`} />;
}

function Demo({ name, file, children }: { name: string; file: string; children: ReactNode }) {
  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="text-subheading">{name}</h3>
        <code className="font-mono text-xs text-muted-foreground">{file}</code>
      </div>
      <div className="rounded-xl border border-dashed p-4 sm:p-5">{children}</div>
    </div>
  );
}

function Rules({ good, items }: { good?: boolean; items: string[] }) {
  const Icon = good ? CheckIcon : XIcon;
  return (
    <Card>
      <CardHeader>
        <CardTitle className={good ? "text-success-strong" : "text-destructive-strong"}>{good ? "Nên" : "Không nên"}</CardTitle>
      </CardHeader>
      <CardContent>
        <ul className="space-y-2.5 text-sm">
          {items.map((i) => (
            <li key={i} className="flex gap-2.5">
              <Icon className={`mt-0.5 size-4 shrink-0 ${good ? "text-success" : "text-destructive"}`} aria-hidden />
              {i}
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}
