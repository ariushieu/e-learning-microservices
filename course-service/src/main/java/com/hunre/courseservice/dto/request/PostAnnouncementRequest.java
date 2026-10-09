package com.hunre.courseservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostAnnouncementRequest(
        @NotBlank(message = "Tiêu đề thông báo không được để trống")
        @Size(max = 150, message = "Tiêu đề thông báo tối đa 150 ký tự") String title,
        @NotBlank(message = "Nội dung thông báo không được để trống")
        @Size(max = 2000, message = "Nội dung thông báo tối đa 2000 ký tự") String content
) {}
