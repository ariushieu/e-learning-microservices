package com.hunre.authservice.dto;

import java.time.Instant;

public record LoginEventResponse(boolean success, String device, Instant createdAt) {
}
