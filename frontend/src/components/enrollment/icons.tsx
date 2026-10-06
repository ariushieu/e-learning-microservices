import { CircleHelpIcon, CirclePlayIcon, FileTextIcon, PaperclipIcon, type LucideIcon } from "lucide-react";
import type { LessonType } from "@/lib/types";
import { cn } from "@/lib/utils";

const ICONS: Record<LessonType, LucideIcon> = {
  VIDEO: CirclePlayIcon,
  ARTICLE: FileTextIcon,
  FILE: PaperclipIcon,
  QUIZ: CircleHelpIcon,
};

/** Biểu tượng theo loại bài học (video, bài đọc, tài liệu, bài kiểm tra). */
export function LessonTypeIcon({ type, className }: { type: LessonType; className?: string }) {
  const Icon = ICONS[type] ?? FileTextIcon;
  return <Icon className={cn("size-4", className)} aria-hidden />;
}
