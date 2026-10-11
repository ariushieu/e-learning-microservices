package com.hunre.courseservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;
import java.util.List;

/**
 * Tin báo hiệu WebRTC (offer, answer, ICE candidate) gửi cho các người khác trong phòng.
 * Trình duyệt gom nhiều tin vào một request để không vượt hạn mức request của gateway.
 */
public record RoomSignalRequest(
        @NotBlank String from,
        @NotEmpty @Size(max = 50) List<@Valid @NotNull Signal> messages
) {
    public record Signal(
            @NotBlank String to,
            @NotBlank @Pattern(regexp = "offer|answer|candidate") String type,
            @NotNull JsonNode data
    ) {}
}
