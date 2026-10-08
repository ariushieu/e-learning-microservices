package com.hunre.authservice.dto;

import java.time.Instant;

public record SessionResponse(Long id, Instant createdAt, String device, boolean current) {
}
