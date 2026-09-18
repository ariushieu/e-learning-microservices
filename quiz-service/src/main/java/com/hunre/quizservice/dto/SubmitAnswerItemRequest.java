package com.hunre.quizservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitAnswerItemRequest {

    @NotNull(message = "questionId không được để trống")
    private Long questionId;

    @Builder.Default
    private Set<Long> selectedOptionIds = new HashSet<>();
}
