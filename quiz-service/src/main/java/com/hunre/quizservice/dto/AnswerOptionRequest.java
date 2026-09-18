package com.hunre.quizservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class AnswerOptionRequest {

    private Long id;

    @NotBlank(message = "nội dung phương án không được để trống")
    @Size(max = 1000, message = "nội dung phương án không được quá 1000 ký tự")
    private String content;

    @Builder.Default
    private Boolean isCorrect = false;

    private Integer position;
}
