package com.hunre.courseservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nội dung một câu hỏi hoặc một câu trả lời trong bài học. */
public record PostLessonTextRequest(
        @NotBlank(message = "Nội dung không được để trống")
        @Size(max = 2000, message = "Nội dung tối đa 2000 ký tự") String content
) {}
