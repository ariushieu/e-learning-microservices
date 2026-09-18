package com.hunre.quizservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hunre.quizservice.entity.AttemptStatus;
import com.hunre.quizservice.entity.QuizAttempt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuizAttemptResponse {

    private Long id;
    private Long quizId;
    private String quizTitle;
    private Long userId;
    private Integer attemptNo;
    private AttemptStatus status;
    private BigDecimal score;
    private Boolean passed;
    private Instant startedAt;
    private Instant submittedAt;
    private Integer timeLimitMinutes;
    private Long remainingSeconds;

    public static QuizAttemptResponse from(QuizAttempt attempt) {
        Long remainingSec = null;
        Integer limitMin = attempt.getQuiz().getTimeLimitMinutes();
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS && limitMin != null && limitMin > 0) {
            long elapsedSec = Duration.between(attempt.getStartedAt(), Instant.now()).getSeconds();
            long totalAllowedSec = limitMin * 60L;
            remainingSec = Math.max(0, totalAllowedSec - elapsedSec);
        }

        return QuizAttemptResponse.builder()
                .id(attempt.getId())
                .quizId(attempt.getQuiz().getId())
                .quizTitle(attempt.getQuiz().getTitle())
                .userId(attempt.getUserId())
                .attemptNo(attempt.getAttemptNo())
                .status(attempt.getStatus())
                .score(attempt.getScore())
                .passed(attempt.getPassed())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .timeLimitMinutes(limitMin)
                .remainingSeconds(remainingSec)
                .build();
    }
}
