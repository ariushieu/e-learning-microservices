package com.hunre.authservice.dto;

import java.time.Instant;

public record SessionResponse(Long id, Instant createdAt, Instant startedAt, String device, boolean current) {
}
