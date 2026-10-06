import { ChevronDownIcon, LockIcon } from "lucide-react";
import Link from "next/link";
import { LessonContent } from "@/components/enrollment/lesson-content";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { Badge } from "@/components/ui/badge";
import { formatDuration } from "@/lib/format";
import type { Lesson, Section } from "@/lib/types";
import { LessonTypeIcon } from "./icons";

/** Đề cương ở trang chi tiết khóa học. Bài xem thử mở ngay tại chỗ, không cần ghi danh. */
export function CurriculumList({
  sections,
  courseId,
  canLearn,
}: {
  sections: Section[];
  courseId: number;
  canLearn: boolean;
}) {
  if (sections.length === 0) {
    return <p className="rounded-xl border border-dashed px-4 py-8 text-center text-sm text-muted-foreground">Khóa học chưa có nội dung.</p>;
  }

  return (
    <Accordion
      type="multiple"
      defaultValue={sections.map((s) => String(s.id))}
      className="overflow-hidden rounded-xl ring-1 ring-foreground/10"
    >
      {sections.map((s, i) => (
        <AccordionItem key={s.id} value={String(s.id)}>
          <AccordionTrigger className="items-center gap-4 rounded-none bg-muted/50 px-4 py-3 hover:no-underline">
            <span className="flex-1 font-semibold">
              Chương {i + 1}: {s.title}
            </span>
            <span className="shrink-0 text-xs font-normal text-muted-foreground tabular-nums">
              {s.lessons.length} bài · {formatDuration(s.lessons.reduce((t, l) => t + l.durationSeconds, 0))}
            </span>
          </AccordionTrigger>
          <AccordionContent className="pb-0">
            <ul className="divide-y">
              {s.lessons.map((l) => (
                <LessonRow key={l.id} lesson={l} courseId={courseId} canLearn={canLearn} />
              ))}
              {s.lessons.length === 0 && <li className="px-4 py-3 text-sm text-muted-foreground">Chưa có bài học.</li>}
            </ul>
          </AccordionContent>
        </AccordionItem>
      ))}
    </Accordion>
  );
}

function LessonRow({ lesson, courseId, canLearn }: { lesson: Lesson; courseId: number; canLearn: boolean }) {
  const meta = (
    <>
      <LessonTypeIcon type={lesson.type} className="size-4 shrink-0 text-muted-foreground" />
      <span className="min-w-0 flex-1 truncate">{lesson.title}</span>
      {lesson.isPreview && (
        <Badge variant="outline" className="border-0 bg-primary/10 text-primary">
          Xem thử
        </Badge>
      )}
      <span className="w-16 shrink-0 text-right text-xs text-muted-foreground tabular-nums">{formatDuration(lesson.durationSeconds)}</span>
    </>
  );

  if (canLearn) {
    return (
      <li>
        <Link
          href={`/learn/${courseId}?lesson=${lesson.id}`}
          className="flex items-center gap-3 px-4 py-3 text-sm no-underline! transition-colors hover:bg-muted/50"
        >
          {meta}
        </Link>
      </li>
    );
  }
  if (lesson.isPreview) {
    // <details> gốc: mở được bài xem thử cả khi chưa tải xong JavaScript.
    return (
      <li>
        <details className="group/preview">
          <summary className="flex cursor-pointer list-none items-center gap-3 px-4 py-3 text-sm transition-colors hover:bg-muted/50 [&::-webkit-details-marker]:hidden">
            {meta}
            <ChevronDownIcon className="size-4 shrink-0 text-muted-foreground transition-transform group-open/preview:rotate-180" />
          </summary>
          <div className="border-t bg-muted/30 p-4">
            <LessonContent lesson={lesson} />
          </div>
        </details>
      </li>
    );
  }
  return (
    <li className="flex items-center gap-3 px-4 py-3 text-sm text-foreground/80">
      {meta}
      <LockIcon className="size-3.5 shrink-0 text-muted-foreground/60" aria-label="Cần ghi danh" />
    </li>
  );
}
