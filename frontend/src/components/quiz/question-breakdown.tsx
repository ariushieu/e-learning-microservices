import { CheckIcon, TriangleAlertIcon } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";
import { QuestionTypeTag } from "./labels";
import type { QuestionStat } from "./results-types";

const letter = (i: number) => String.fromCharCode(65 + (i % 26));

/**
 * Phương án sai "gây nhầm lẫn": phương án sai được chọn nhiều nhất, khi được chọn nhiều hơn
 * mọi đáp án đúng (hòa thì không đánh dấu, tránh gắn nhãn tràn lan khi lớp còn ít bài nộp). Gợi ý giảng viên xem lại cách ra đề hoặc bài giảng.
 */
function distractorOf(question: QuestionStat): number | null {
  const wrong = question.options.filter((o) => !o.correct && o.picks > 0);
  if (wrong.length === 0) return null;
  const top = wrong.reduce((a, b) => (b.picks > a.picks ? b : a));
  const bestCorrect = Math.max(0, ...question.options.filter((o) => o.correct).map((o) => o.picks));
  return top.picks > bestCorrect ? top.optionId : null;
}

/** Một thẻ cho mỗi câu: tỉ lệ đúng và phân bố lượt chọn từng phương án. */
export function QuestionBreakdown({ question, index }: { question: QuestionStat; index: number }) {
  const graded = question.gradedAnswers;
  const low = graded > 0 && question.correctRate < 50;
  const distractor = distractorOf(question);

  return (
    <Card className={cn(low && "border-warning/40 bg-warning-soft/40")}>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2">
          <div className="min-w-0 flex-1 space-y-1.5">
            <div className="flex flex-wrap items-center gap-2 text-caption text-muted-foreground">
              <span className="font-semibold text-foreground">Câu {index + 1}</span>
              <QuestionTypeTag type={question.type} />
            </div>
            <p className="break-words whitespace-pre-line">{question.content}</p>
          </div>
          <div className="text-right">
            <p className="text-heading tabular-nums">{graded === 0 ? "—" : `${formatNumber(question.correctRate, 2)}%`}</p>
            <p className="text-caption text-muted-foreground">
              {graded === 0 ? "Chưa có dữ liệu" : `trả lời đúng · ${formatNumber(graded)} lượt chấm`}
            </p>
            {low && <p className="mt-1 text-caption font-medium text-warning-strong">Cần xem xét</p>}
          </div>
        </div>

        <ul className="space-y-2.5" aria-label={`Phân bố lựa chọn câu ${index + 1}`}>
          {question.options.map((option, i) => {
            const isDistractor = option.optionId === distractor;
            return (
              <li key={option.optionId} className="space-y-1">
                <div className="flex items-start justify-between gap-3 text-sm">
                  <span className="flex min-w-0 items-start gap-2">
                    <span
                      className={cn(
                        "mt-px inline-flex size-5 shrink-0 items-center justify-center rounded-full text-[11px] font-semibold",
                        option.correct ? "bg-success text-primary-foreground" : "bg-muted text-muted-foreground",
                      )}
                    >
                      {option.correct ? <CheckIcon className="size-3" aria-hidden /> : letter(i)}
                    </span>
                    <span className="min-w-0 break-words">
                      {option.content}
                      {option.correct && <span className="sr-only"> (đáp án đúng)</span>}
                      {isDistractor && (
                        <span className="ml-2 inline-flex items-center gap-1 text-caption font-medium text-warning-strong">
                          <TriangleAlertIcon className="size-3.5" aria-hidden />
                          Gây nhầm lẫn
                        </span>
                      )}
                    </span>
                  </span>
                  <span className="shrink-0 text-muted-foreground tabular-nums">
                    {formatNumber(option.picks)} · {formatNumber(option.pickRate, 1)}%
                  </span>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-muted" aria-hidden>
                  <div
                    className={cn(
                      "h-full rounded-full",
                      option.correct ? "bg-success" : isDistractor ? "bg-warning" : "bg-muted-foreground/40",
                    )}
                    style={{ width: `${Math.min(100, Number(option.pickRate))}%` }}
                  />
                </div>
              </li>
            );
          })}
        </ul>

        {(question.skippedAnswers > 0 || question.type === "MULTIPLE_CHOICE") && graded > 0 && (
          <p className="text-caption text-muted-foreground">
            {question.skippedAnswers > 0 && `${formatNumber(question.skippedAnswers)} lượt bỏ trống câu này. `}
            {question.type === "MULTIPLE_CHOICE" && "Câu nhiều đáp án: một lượt có thể chọn nhiều phương án nên tổng có thể quá 100%."}
          </p>
        )}
      </CardContent>
    </Card>
  );
}
