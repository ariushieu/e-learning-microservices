package com.hunre.courseservice.dto.response;

import java.time.Instant;

public record LiveAttendanceResponse(Long userId, String fullName, boolean host, Instant firstJoinedAt,
                                     Instant lastLeftAt, int joinCount, long attendedSeconds) {}
