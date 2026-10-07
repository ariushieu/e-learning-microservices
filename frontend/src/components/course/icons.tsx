import { CircleHelpIcon, FileTextIcon, PaperclipIcon, PlayCircleIcon, type LucideIcon } from "lucide-react";
import { label } from "@/lib/format";
import type { LessonType } from "@/lib/types";

export const LESSON_TYPE_ICONS: Record<LessonType, LucideIcon> = {
  VIDEO: PlayCircleIcon,
  ARTICLE: FileTextIcon,
  FILE: PaperclipIcon,
  QUIZ: CircleHelpIcon,
};

export const lessonTypeLabels: Record<LessonType, string> = {
  VIDEO: label("VIDEO"),
  ARTICLE: label("ARTICLE"),
  FILE: label("FILE"),
  QUIZ: label("QUIZ"),
};

export function LessonTypeIcon({ type, className = "size-4" }: { type: LessonType; className?: string }) {
  const Icon = LESSON_TYPE_ICONS[type] ?? FileTextIcon;
  return <Icon className={className} aria-hidden />;
}
