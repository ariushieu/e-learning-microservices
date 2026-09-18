package com.hunre.quizservice.dto;

import com.hunre.quizservice.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class UpdateQuestionRequest {

    @NotBlank(message = "nội dung câu hỏi không được để trống")
    private String content;

    @NotNull(message = "loại câu hỏi không được để trống")
    private QuestionType type;

    @DecimalMin(value = "0.01", message = "điểm câu hỏi phải lớn hơn 0")
    private BigDecimal score;

    private Integer position;

    private String explanation;

    @NotEmpty(message = "danh sách phương án trả lời không được để trống")
    @Valid
    private List<AnswerOptionRequest> options;
}
