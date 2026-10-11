package com.hunre.courseservice.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Trạng thái micro, camera, giơ tay, chia sẻ màn hình của một người trong phòng. */
public record RoomStateRequest(@NotBlank String from, boolean mic, boolean cam, boolean hand, boolean screen) {}
