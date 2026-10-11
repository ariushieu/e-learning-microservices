package com.hunre.courseservice.dto.request;

import jakarta.validation.constraints.NotBlank;

/** {@code from} là mã kết nối của người gửi; {@code target} là người bị tác động (tắt micro), có thể bỏ trống. */
public record RoomPeerRequest(@NotBlank String from, String target) {}
