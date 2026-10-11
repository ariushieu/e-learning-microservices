package com.hunre.courseservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ScheduleLiveSessionRequest(
        @NotBlank(message = "Tên buổi học không được để trống")
        @Size(max = 150, message = "Tên buổi học tối đa 150 ký tự") String title,
        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự") String description,
        @NotNull(message = "Chọn thời gian bắt đầu") Instant scheduledAt,
        @NotNull(message = "Nhập thời lượng buổi học")
        @Min(value = 15, message = "Buổi học dài ít nhất 15 phút")
        @Max(value = 300, message = "Buổi học dài tối đa 300 phút") Integer durationMinutes
) {}
