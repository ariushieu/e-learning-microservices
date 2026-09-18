package com.hunre.quizservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuizRequest {

    @NotNull(message = "courseId không được để trống")
    private Long courseId;

    private Long lessonId;

    @NotBlank(message = "tiêu đề không được để trống")
    @Size(max = 200, message = "tiêu đề không được vượt quá 200 ký tự")
    private String title;

    @Size(max = 1000, message = "mô tả không được vượt quá 1000 ký tự")
    private String description;

    @Min(value = 1, message = "thời gian làm bài tối thiểu là 1 phút")
    private Integer timeLimitMinutes;

    @DecimalMin(value = "0.0", message = "điểm đạt tối thiểu là 0%")
    @DecimalMax(value = "100.0", message = "điểm đạt tối đa là 100%")
    private BigDecimal passScore;

    @Min(value = 0, message = "số lần làm bài không được âm")
    private Integer maxAttempts;

    @Builder.Default
    private Boolean shuffleQuestions = false;

    @NotNull(message = "createdBy không được để trống")
    private Long createdBy;
}
