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
    /** skippedAnswers: bài nộp không chọn phương án nào cho câu này. */
    public record QuestionRate(Long questionId, String content, String type, long gradedAnswers, BigDecimal correctRate,
                               long skippedAnswers, List<OptionPick> options) {}
    /** pickRate: phần trăm bài nộp (đã chấm câu này) có chọn phương án; câu nhiều đáp án có thể cộng lại quá 100. */
    public record OptionPick(Long optionId, String content, boolean correct, long picks, BigDecimal pickRate) {}
}
