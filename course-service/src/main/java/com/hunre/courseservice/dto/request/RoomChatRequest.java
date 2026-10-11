package com.hunre.courseservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomChatRequest(
        @NotBlank String from,
        @NotBlank(message = "Tin nhắn không được để trống")
        @Size(max = 1000, message = "Tin nhắn tối đa 1000 ký tự") String text
) {}
