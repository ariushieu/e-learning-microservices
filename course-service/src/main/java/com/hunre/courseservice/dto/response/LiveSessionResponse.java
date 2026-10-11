package com.hunre.courseservice.dto.response;

import com.hunre.courseservice.entity.LiveSession;
import java.time.Instant;

/**
 * {@code attendeeCount} (số học viên đã vào) chỉ trả cho người quản lý khóa; {@code participantCount}
 * là số người đang ở trong phòng lúc này.
 */
public record LiveSessionResponse(Long id, Long courseId, String courseTitle, String title, String description,
                                  Long hostId, String hostName, Instant scheduledAt, int durationMinutes,
                                  String status, Instant startedAt, Instant endedAt, Instant createdAt,
                                  boolean canHost, Integer attendeeCount, int participantCount) {
    public static LiveSessionResponse from(LiveSession s, String courseTitle, boolean canHost,
                                           Integer attendeeCount, int participantCount) {
        return new LiveSessionResponse(s.getId(), s.getCourseId(), courseTitle, s.getTitle(), s.getDescription(),
                s.getHostId(), s.getHostName() == null ? "Giảng viên" : s.getHostName(), s.getScheduledAt(),
                s.getDurationMinutes(), s.getStatus().name(), s.getStartedAt(), s.getEndedAt(), s.getCreatedAt(),
                canHost, canHost ? attendeeCount : null, participantCount);
    }
}
