package com.hunre.quizservice.dto;

import com.hunre.quizservice.entity.Quiz;
import com.hunre.quizservice.entity.QuizStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizDetailResponse {

    private Long id;
    private Long courseId;
    private Long lessonId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private BigDecimal passScore;
    private Integer maxAttempts;
    private boolean shuffleQuestions;
    private QuizStatus status;
    private Long createdBy;
    private int totalQuestions;
    private BigDecimal totalScore;
    private Instant createdAt;
    private Instant updatedAt;
    private List<QuestionResponse> questions;

    public static QuizDetailResponse from(Quiz quiz, boolean showAnswersAndExplanation) {
        int totalQuestions = quiz.getQuestions() != null ? quiz.getQuestions().size() : 0;
        BigDecimal totalScore = quiz.getQuestions() != null
                ? quiz.getQuestions().stream()
                .map(q -> q.getScore() != null ? q.getScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        List<QuestionResponse> questionResponses = quiz.getQuestions() != null
                ? quiz.getQuestions().stream()
                .map(q -> QuestionResponse.from(q, showAnswersAndExplanation))
                .toList()
                : List.of();

        return QuizDetailResponse.builder()
                .id(quiz.getId())
                .courseId(quiz.getCourseId())
                .lessonId(quiz.getLessonId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .passScore(quiz.getPassScore())
                .maxAttempts(quiz.getMaxAttempts())
                .shuffleQuestions(quiz.isShuffleQuestions())
                .status(quiz.getStatus())
                .createdBy(quiz.getCreatedBy())
                .totalQuestions(totalQuestions)
                .totalScore(totalScore)
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .questions(questionResponses)
                .build();
    }
}
