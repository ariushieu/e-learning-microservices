package com.hunre.quizservice.dto;

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
public class QuizResultResponse {

    private Long attemptId;
    private Long quizId;
    private String quizTitle;
    private Long userId;
    private Integer attemptNo;
    private BigDecimal score;
    private BigDecimal passScore;
    private boolean passed;
    private Instant startedAt;
    private Instant submittedAt;
    private List<QuestionResultResponse> questionResults;
}
