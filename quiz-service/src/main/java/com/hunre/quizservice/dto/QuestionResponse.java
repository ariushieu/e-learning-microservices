package com.hunre.quizservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hunre.quizservice.entity.Question;
import com.hunre.quizservice.entity.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionResponse {

    private Long id;
    private String content;
    private QuestionType type;
    private BigDecimal score;
    private Integer position;
    private String explanation;
    private List<AnswerOptionResponse> options;

    public static QuestionResponse from(Question question, boolean showAnswersAndExplanation) {
        return QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .type(question.getType())
                .score(question.getScore())
                .position(question.getPosition())
                .explanation(showAnswersAndExplanation ? question.getExplanation() : null)
                .options(question.getOptions().stream()
                        .map(opt -> AnswerOptionResponse.from(opt, showAnswersAndExplanation))
                        .toList())
                .build();
    }
}
