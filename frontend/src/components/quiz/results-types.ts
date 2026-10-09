import type { Page, QuestionType } from "@/lib/types";

/** pickRate: phần trăm lượt chấm có chọn phương án (câu nhiều đáp án có thể cộng quá 100). */
export type OptionStat = { optionId: number; content: string; correct: boolean; picks: number; pickRate: number };

export type QuestionStat = {
  questionId: number;
  content: string;
  type: QuestionType;
  gradedAnswers: number;
  correctRate: number;
  /** Lượt nộp không chọn phương án nào cho câu này. */
  skippedAnswers: number;
  options: OptionStat[];
};

export type QuizResults = {
  quizId: number;
  quizTitle: string;
  summary: {
    submittedLearners: number;
    submittedAttempts: number;
    expiredAttempts: number;
    averageBestScore: number;
    passRate: number;
    unclassifiedAttempts: number;
    questions: QuestionStat[];
  };
  learners: Page<{
    learnerId: number;
    learnerName: string;
    submittedAttempts: number;
    bestScore: number;
    passed: boolean;
    lastSubmittedAt: string;
  }>;
};
