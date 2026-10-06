import type { ReactNode } from "react";
import type { LessonType } from "@/lib/types";

export const lessonTypeLabels: Record<LessonType, string> = {
  VIDEO: "Video",
  ARTICLE: "Bài đọc",
  FILE: "Tài liệu",
  QUIZ: "Bài kiểm tra",
};

const lessonPaths: Record<LessonType, ReactNode> = {
  VIDEO: (
    <>
      <circle cx="12" cy="12" r="10" />
      <polygon points="10 8 16 12 10 16 10 8" />
    </>
  ),
  ARTICLE: (
    <>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <polyline points="14 2 14 8 20 8" />
      <line x1="16" y1="13" x2="8" y2="13" />
      <line x1="16" y1="17" x2="8" y2="17" />
    </>
  ),
  FILE: <path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />,
  QUIZ: (
    <>
      <circle cx="12" cy="12" r="10" />
      <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
      <line x1="12" y1="17" x2="12.01" y2="17" />
    </>
  ),
};

function Svg({ className, children }: { className: string; children: ReactNode }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden
    >
      {children}
    </svg>
  );
}

export function LessonTypeIcon({ type, className = "h-4 w-4" }: { type: LessonType; className?: string }) {
  return <Svg className={className}>{lessonPaths[type] ?? lessonPaths.ARTICLE}</Svg>;
}

export function CheckIcon({ className = "h-4 w-4" }: { className?: string }) {
  return (
    <Svg className={className}>
      <polyline points="20 6 9 17 4 12" />
    </Svg>
  );
}

export function ChevronIcon({ className = "h-4 w-4" }: { className?: string }) {
  return (
    <Svg className={className}>
      <polyline points="6 9 12 15 18 9" />
    </Svg>
  );
}
