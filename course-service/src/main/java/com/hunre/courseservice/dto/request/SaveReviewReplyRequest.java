package com.hunre.courseservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveReviewReplyRequest(
        @NotBlank(message = "Nội dung phản hồi không được để trống")
        @Size(max = 1000, message = "Nội dung phản hồi tối đa 1000 ký tự") String content
) {}
