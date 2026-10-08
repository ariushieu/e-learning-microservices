import type { Page } from "@/lib/types";

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
    questions: { questionId: number; content: string; gradedAnswers: number; correctRate: number }[];
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
