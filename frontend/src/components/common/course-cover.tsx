import { BookOpenIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { ContourPattern } from "./decor";

// Bốn tông nền đậm của bộ nhận diện; cùng danh mục luôn ra cùng một tông.
const TONES = [
  "bg-[linear-gradient(135deg,#14532d,#166534_60%,#1f7a45)]",
  "bg-[linear-gradient(135deg,#134e5e,#0e7490_60%,#1689a8)]",
  "bg-[linear-gradient(135deg,#78350f,#92400e_55%,#b45309)]",
  "bg-[linear-gradient(135deg,#1e293b,#334155_60%,#475569)]",
];

function toneFor(key: string) {
  let h = 0;
  for (const ch of key) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  return TONES[h % TONES.length];
}

/**
 * Ảnh bìa khóa học. Có `thumbnailUrl` thì hiện ảnh; chưa có thì tự sinh bìa: nền theo danh mục,
 * họa tiết đường đồng mức, tên danh mục — mỗi khóa trông khác nhau thay vì cùng một ô xám.
 */
export function CourseCover({
  title,
  category,
  thumbnailUrl,
  className,
  showLabel = true,
}: {
  title: string;
  category?: string | null;
  thumbnailUrl?: string | null;
  className?: string;
  showLabel?: boolean;
}) {
  if (thumbnailUrl) {
    return (
      // Ảnh bìa do giảng viên dán link từ bất kỳ đâu; next/image cần khai báo trước từng tên miền.
      // eslint-disable-next-line @next/next/no-img-element
      <img src={thumbnailUrl} alt={title} className={cn("aspect-video w-full bg-muted object-cover", className)} />
    );
  }
  return (
    <div className={cn("relative isolate flex aspect-video w-full overflow-hidden text-white", toneFor(category || title), className)} aria-hidden>
      <ContourPattern className="text-white/12" />
      <div className="absolute -right-6 -bottom-8 size-32 rounded-full bg-white/6 blur-2xl" />
      <div className="relative flex w-full flex-col justify-between p-4">
        <span className="flex size-9 items-center justify-center rounded-xl bg-white/12 ring-1 ring-white/15">
          <BookOpenIcon className="size-4.5" />
        </span>
        {showLabel && category && <span className="line-clamp-1 text-xs font-medium tracking-wide text-white/80 uppercase">{category}</span>}
      </div>
    </div>
  );
}
