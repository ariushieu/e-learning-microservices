package com.hunre.quizservice.dto;

import com.hunre.quizservice.entity.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionResultResponse {

    private Long questionId;
    private String content;
    private QuestionType type;
    private BigDecimal questionScore;
    private BigDecimal earnedScore;
    private boolean isCorrect;
    private String explanation;
    private Set<Long> selectedOptionIds;
    private Set<Long> correctOptionIds;
    private List<AnswerOptionResponse> options;
}
