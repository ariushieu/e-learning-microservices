package com.hunre.quizservice.dto;

import com.hunre.quizservice.entity.QuizStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateQuizStatusRequest(
        @NotNull(message = "trạng thái không được để trống") QuizStatus status
) {
}
