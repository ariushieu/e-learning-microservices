import type { Section } from "@/lib/types";

export interface LearningSummary {
  active: number;
  completed: number;
  cancelled: number;
  averageProgress: number;
  completionRate: number;
  certificatesIssued: number;
  lessons: { lessonId: number; completedCount: number; completionRate: number }[];
}

export const DROP_THRESHOLD = 20;

export function lessonCompletionRows(sections: Section[], counts: LearningSummary["lessons"]) {
  const byId = new Map(counts.map((lesson) => [lesson.lessonId, lesson]));
  // Curriculum is authoritative for order and visibility; untouched lessons default to zero.
  const rows = [...sections].sort((a, b) => a.position - b.position || a.id - b.id)
    .flatMap((section) => [...section.lessons].sort((a, b) => a.position - b.position || a.id - b.id)
      .map((lesson) => ({ id: lesson.id, title: lesson.title, sectionTitle: section.title,
        completedCount: byId.get(lesson.id)?.completedCount ?? 0,
        completionRate: byId.get(lesson.id)?.completionRate ?? 0 })));
  return rows.map((row, index) => {
    // Round percentage points to avoid binary floating-point errors at the threshold.
    const drop = index > 0 ? Math.round((rows[index - 1].completionRate - row.completionRate) * 100) / 100 : 0;
    return { ...row, drop, highlighted: drop >= DROP_THRESHOLD };
  });
}
