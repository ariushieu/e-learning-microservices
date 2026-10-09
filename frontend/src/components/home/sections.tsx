import {
  ArrowRightIcon,
  AwardIcon,
  BookOpenIcon,
  CodeXmlIcon,
  GraduationCapIcon,
  LeafIcon,
  MessagesSquareIcon,
  QuoteIcon,
  ShieldCheckIcon,
  StarIcon,
  UsersIcon,
  type LucideIcon,
} from "lucide-react";
import Link from "next/link";
import { ContourPattern, FullBleed } from "@/components/common/decor";
import { CourseCover } from "@/components/common/course-cover";
import { Section } from "@/components/common/section";
import { CatalogSearch } from "@/components/course/catalog-filters";
import { RatingStars } from "@/components/course/rating-stars";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { formatNumber, initials } from "@/lib/format";
import type { Category, CourseSummary } from "@/lib/types";
import { cn } from "@/lib/utils";
import type { InstructorSummary, PlatformStats } from "./catalog-data";
import { countInCategory } from "./catalog-data";
import { VerifyCertificateForm } from "./verify-form";

/* ------------------------------------------------------------------ Hero */

export function HomeHero({ covers, stats }: { covers: CourseSummary[]; stats: PlatformStats }) {
  const [a, b, c] = covers;
  return (
    <FullBleed className="relative isolate -mt-10 overflow-hidden bg-sidebar text-white" inner="relative grid items-center gap-12 pt-14 pb-28 lg:grid-cols-[1.05fr_1fr] lg:pt-20 lg:pb-32">
      <ContourPattern className="-z-10 text-white/[0.07]" />
      <div className="absolute -top-24 -right-24 -z-10 size-96 rounded-full bg-sidebar-primary/20 blur-3xl" aria-hidden />
      <div className="max-w-xl space-y-6">
        <p className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-medium ring-1 ring-white/15">
          <LeafIcon className="size-3.5 text-sidebar-primary" aria-hidden />
          Trường Đại học Tài nguyên và Môi trường Hà Nội
        </p>
        <h1 className="text-display text-white">
          Học cùng giảng viên HUNRE, <span className="text-sidebar-primary">nhận chứng chỉ</span> khi hoàn thành
        </h1>
        <p className="text-lg leading-relaxed text-white/80">
          Khóa học về công nghệ, tài nguyên – môi trường và kỹ năng. Video, bài đọc, bài kiểm tra tự chấm và chứng chỉ có mã
          xác minh công khai.
        </p>
        <CatalogSearch keyword="" />
        <div className="flex flex-wrap items-center gap-x-6 gap-y-3 text-sm text-white/80">
          <Button asChild size="lg" variant="secondary">
            <Link href="/courses">
              Xem {formatNumber(stats.courses)} khóa học <ArrowRightIcon />
            </Link>
          </Button>
          <Link href="#xac-minh" className="inline-flex items-center gap-1.5 underline-offset-4 hover:text-white hover:underline">
            <ShieldCheckIcon className="size-4" aria-hidden /> Xác minh chứng chỉ
          </Link>
        </div>
      </div>

      {a && (
        <div className="relative mx-auto hidden h-[25rem] w-full max-w-lg lg:block" aria-hidden>
          <Cover course={a} className="absolute top-0 right-0 w-[78%] rotate-2" />
          {b && <Cover course={b} className="absolute bottom-6 left-0 w-[58%] -rotate-3" />}
          {c && <Cover course={c} className="absolute right-6 bottom-0 w-[40%] rotate-3" />}
          {stats.ratingAvg !== null && (
            <FloatCard className="absolute top-[42%] -left-6">
              <StarIcon className="size-5 fill-achievement text-achievement" />
              <span>
                <b className="text-base tabular-nums">{formatNumber(stats.ratingAvg, 1)}</b>/5 · {formatNumber(stats.reviews)} đánh giá
              </span>
            </FloatCard>
          )}
          <FloatCard className="absolute -top-4 left-10">
            <AwardIcon className="size-5 text-achievement" />
            <span>Chứng chỉ có mã xác minh</span>
          </FloatCard>
        </div>
      )}
    </FullBleed>
  );
}

function Cover({ course, className }: { course: CourseSummary; className?: string }) {
  return (
    <div className={cn("overflow-hidden rounded-2xl shadow-2xl ring-1 ring-white/20", className)}>
      <CourseCover title={course.title} category={course.categoryName} thumbnailUrl={course.thumbnailUrl} />
    </div>
  );
}

function FloatCard({ className, children }: { className?: string; children: React.ReactNode }) {
  return (
    <div className={cn("flex items-center gap-2 rounded-xl bg-card px-3.5 py-2.5 text-sm text-foreground shadow-raised ring-1 ring-border", className)}>
      {children}
    </div>
  );
}

/* ------------------------------------------------------------------ Số liệu */

export function StatsBand({ stats }: { stats: PlatformStats }) {
  const items: { icon: LucideIcon; value: string; label: string }[] = [
    { icon: BookOpenIcon, value: formatNumber(stats.courses), label: "khóa học đang mở" },
    { icon: GraduationCapIcon, value: formatNumber(stats.instructors), label: "giảng viên" },
    { icon: UsersIcon, value: formatNumber(stats.enrollments), label: "lượt học viên ghi danh" },
    { icon: StarIcon, value: stats.ratingAvg === null ? "—" : `${formatNumber(stats.ratingAvg, 1)}/5`, label: `từ ${formatNumber(stats.reviews)} đánh giá` },
  ];
  return (
    <div className="relative z-10 -mt-16 mb-14 grid grid-cols-2 overflow-hidden rounded-2xl bg-card shadow-raised ring-1 ring-border lg:grid-cols-4">
      {items.map((i, index) => (
        <div key={i.label} className={cn("flex items-center gap-4 p-5 sm:p-6", index > 0 && "lg:border-l", index % 2 === 1 && "border-l lg:border-l", index > 1 && "border-t lg:border-t-0")}>
          <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-primary-soft text-primary-strong">
            <i.icon className="size-5" aria-hidden />
          </span>
          <div className="min-w-0">
            <p className="text-title leading-none tabular-nums">{i.value}</p>
            <p className="mt-1 text-sm text-muted-foreground">{i.label}</p>
          </div>
        </div>
      ))}
    </div>
  );
}

/* ------------------------------------------------------------------ Lĩnh vực */

const FIELD_STYLE: { match: RegExp; icon: LucideIcon; tone: string }[] = [
  { match: /tài nguyên|môi trường|khí hậu|gis/i, icon: LeafIcon, tone: "from-primary-strong to-primary" },
  { match: /công nghệ|lập trình|phần mềm|dữ liệu/i, icon: CodeXmlIcon, tone: "from-info-strong to-info" },
  { match: /kỹ năng/i, icon: MessagesSquareIcon, tone: "from-achievement-strong to-achievement" },
];

export function FieldTiles({ categories, courses }: { categories: Category[]; courses: CourseSummary[] }) {
  const fields = categories.map((c) => ({ category: c, count: countInCategory(c, courses) })).filter((f) => f.count > 0);
  if (fields.length === 0) return null;
  return (
    <Section className="mb-16" title="Lĩnh vực đào tạo" description="Chọn lĩnh vực để xem các khóa học liên quan.">
      <div className="grid gap-5 md:grid-cols-3">
        {fields.map(({ category, count }) => {
          const style = FIELD_STYLE.find((s) => s.match.test(category.name)) ?? FIELD_STYLE[1];
          const Icon = style.icon;
          return (
            <Card key={category.id} className="group relative gap-0 overflow-hidden p-0 transition-shadow hover:shadow-raised">
              <Link href={`/courses?categoryId=${category.id}`} className="absolute inset-0 z-10" aria-label={`Xem khóa học ${category.name}`} />
              <div className={cn("relative flex h-28 items-end overflow-hidden bg-gradient-to-br p-5 text-white", style.tone)}>
                <ContourPattern className="text-white/15" />
                <Icon className="absolute top-5 right-5 size-10 text-white/80 transition-transform group-hover:scale-110" aria-hidden />
                <p className="relative text-sm font-medium text-white/85">{formatNumber(count)} khóa học</p>
              </div>
              <CardContent className="space-y-3 p-5">
                <h3 className="text-subheading">{category.name}</h3>
                {category.description && <p className="text-sm text-muted-foreground">{category.description}</p>}
                {category.subCategories.length > 0 && (
                  <div className="relative z-20 flex flex-wrap gap-1.5">
                    {category.subCategories.map((s) => (
                      <Link key={s.id} href={`/courses?categoryId=${s.id}`} className="rounded-full bg-muted px-2.5 py-1 text-xs text-muted-foreground hover:bg-primary-soft hover:text-primary-strong">
                        {s.name}
                      </Link>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          );
        })}
      </div>
    </Section>
  );
}

/* ------------------------------------------------------------------ Giảng viên */

export function InstructorAvatar({ name, className }: { name: string; className?: string }) {
  return (
    <span aria-hidden className={cn("flex shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-sidebar to-primary font-semibold text-white ring-4 ring-primary-soft", className)}>
      {initials(name)}
    </span>
  );
}

export function InstructorCard({ instructor }: { instructor: InstructorSummary }) {
  return (
    <Card className="group relative h-full gap-4 p-5 transition-shadow hover:shadow-raised">
      <Link href={`/instructors/${instructor.id}`} className="absolute inset-0 z-10" aria-label={`Hồ sơ ${instructor.name}`} />
      <div className="flex items-center gap-4">
        <InstructorAvatar name={instructor.name} className="size-14 text-lg" />
        <div className="min-w-0">
          <h3 className="truncate text-subheading group-hover:text-primary">{instructor.name}</h3>
          <p className="truncate text-sm text-muted-foreground">{[...new Set(instructor.courses.map((c) => c.categoryName))].join(" · ")}</p>
        </div>
      </div>
      <dl className="grid grid-cols-3 gap-2 rounded-xl bg-muted/60 p-3 text-center">
        <div>
          <dd className="font-semibold tabular-nums">{instructor.courses.length}</dd>
          <dt className="text-caption text-muted-foreground">khóa học</dt>
        </div>
        <div>
          <dd className="font-semibold tabular-nums">{formatNumber(instructor.students)}</dd>
          <dt className="text-caption text-muted-foreground">học viên</dt>
        </div>
        <div>
          <dd className="font-semibold tabular-nums">{instructor.ratingAvg === null ? "—" : formatNumber(instructor.ratingAvg, 1)}</dd>
          <dt className="text-caption text-muted-foreground">điểm đánh giá</dt>
        </div>
      </dl>
      <ul className="space-y-1.5 text-sm">
        {instructor.courses.slice(0, 3).map((c) => (
          <li key={c.id} className="flex items-center gap-2 text-muted-foreground">
            <BookOpenIcon className="size-3.5 shrink-0" aria-hidden />
            <span className="truncate">{c.title}</span>
          </li>
        ))}
      </ul>
    </Card>
  );
}

/* ------------------------------------------------------------------ Cảm nhận */

export interface Testimonial {
  id: number;
  rating: number;
  comment: string;
  authorName: string;
  courseId: number;
  courseTitle: string;
}

export function Testimonials({ items }: { items: Testimonial[] }) {
  if (items.length === 0) return null;
  return (
    <Section className="mb-16" title="Học viên nói gì" description="Đánh giá thật từ người đã ghi danh khóa học.">
      <div className="grid gap-5 md:grid-cols-3">
        {items.map((t) => (
          <Card key={t.id} className="relative h-full gap-4 p-6">
            <QuoteIcon className="size-8 text-primary/25" aria-hidden />
            <p className="flex-1 leading-relaxed">{t.comment}</p>
            <div className="flex items-center justify-between gap-3 border-t pt-4">
              <div className="min-w-0">
                <p className="truncate font-medium">{t.authorName}</p>
                <Link href={`/courses/${t.courseId}`} className="block truncate text-sm text-muted-foreground hover:text-primary">
                  {t.courseTitle}
                </Link>
              </div>
              <span className="shrink-0 text-achievement [&_svg]:size-4">
                <RatingStars rating={t.rating} />
              </span>
            </div>
          </Card>
        ))}
      </div>
    </Section>
  );
}

/* ------------------------------------------------------------------ Xác minh + giảng viên */

export function VerifyAndTeach() {
  return (
    <div id="xac-minh" className="mb-6 grid scroll-mt-24 gap-5 md:grid-cols-2">
      <Card className="gap-4 p-6 sm:p-8">
        <span className="flex size-12 items-center justify-center rounded-xl bg-achievement-soft text-achievement-strong">
          <ShieldCheckIcon className="size-6" aria-hidden />
        </span>
        <div className="space-y-1">
          <h2 className="text-heading">Xác minh chứng chỉ</h2>
          <p className="text-sm text-muted-foreground">
            Nhà tuyển dụng hoặc nhà trường nhập mã in trên chứng chỉ để kiểm tra, không cần tài khoản.
          </p>
        </div>
        <VerifyCertificateForm />
      </Card>
      <Card className="relative isolate gap-4 overflow-hidden bg-sidebar p-6 text-white sm:p-8">
        <ContourPattern className="-z-10 text-white/[0.08]" />
        <span className="flex size-12 items-center justify-center rounded-xl bg-white/10 ring-1 ring-white/15">
          <GraduationCapIcon className="size-6" aria-hidden />
        </span>
        <div className="space-y-1">
          <h2 className="text-heading text-white">Dành cho giảng viên</h2>
          <p className="text-sm text-white/75">
            Soạn đề cương, nhập câu hỏi từ CSV, theo dõi tiến độ từng học viên và trả lời đánh giá ngay trên một trang.
          </p>
        </div>
        <div>
          <Button asChild variant="secondary" size="lg">
            <Link href="/instructor">
              Vào khu giảng dạy <ArrowRightIcon />
            </Link>
          </Button>
        </div>
      </Card>
    </div>
  );
}
