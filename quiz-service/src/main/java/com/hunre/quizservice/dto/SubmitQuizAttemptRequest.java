package com.hunre.quizservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitQuizAttemptRequest {

    @NotNull(message = "danh sách câu trả lời không được để trống")
    @Valid
    @Builder.Default
    private List<SubmitAnswerItemRequest> answers = new ArrayList<>();
}
