package com.hunre.quizservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hunre.quizservice.entity.AnswerOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnswerOptionResponse {

    private Long id;
    private String content;
    private Boolean isCorrect;
    private Integer position;

    public static AnswerOptionResponse from(AnswerOption option, boolean showIsCorrect) {
        return AnswerOptionResponse.builder()
                .id(option.getId())
                .content(option.getContent())
                .isCorrect(showIsCorrect ? option.isCorrect() : null)
                .position(option.getPosition())
                .build();
    }
}
