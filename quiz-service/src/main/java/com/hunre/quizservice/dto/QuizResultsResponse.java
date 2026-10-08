package com.hunre.quizservice.dto;

import com.hunre.sharedcommon.dto.PageResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record QuizResultsResponse(Long quizId, String quizTitle, Summary summary,
                                  PageResponse<Learner> learners) {
    public record Summary(long submittedLearners, long submittedAttempts, long expiredAttempts,
                          BigDecimal averageBestScore, BigDecimal passRate, long unclassifiedAttempts,
                          List<QuestionRate> questions) {}
    public record Learner(Long learnerId, String learnerName, long submittedAttempts,
                          BigDecimal bestScore, boolean passed, Instant lastSubmittedAt) {}
    public record QuestionRate(Long questionId, String content, long gradedAnswers, BigDecimal correctRate) {}
}
