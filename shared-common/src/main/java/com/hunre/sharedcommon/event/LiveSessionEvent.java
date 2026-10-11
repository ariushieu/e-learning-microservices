package com.hunre.sharedcommon.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Một buổi học trực tuyến vừa được lên lịch, vừa bắt đầu hoặc vừa bị hủy.
 *
 * <p>Phát bởi course-service lên topic {@link KafkaTopics#COURSE_EVENTS}. Ba loại dùng chung một
 * lớp vì mang cùng dữ liệu, chỉ khác {@link #eventType()}: {@link EventTypes#LIVE_SESSION_SCHEDULED},
 * {@link EventTypes#LIVE_SESSION_STARTED}, {@link EventTypes#LIVE_SESSION_CANCELLED}.
 * notification-service tạo thông báo và email cho từng người trong {@link #recipientIds()}.
 *
 * <p>Như {@link CourseAnnouncementPostedEvent}, người nhận được chốt lúc phát sự kiện từ bảng
 * {@code course_learners} của course-service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LiveSessionEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long sessionId,
        Long courseId,
        String courseTitle,
        String title,
        String hostName,
        Instant scheduledAt,
        Integer durationMinutes,
        List<Long> recipientIds
) implements DomainEvent {

    private static final Set<String> TYPES = Set.of(EventTypes.LIVE_SESSION_SCHEDULED,
            EventTypes.LIVE_SESSION_STARTED, EventTypes.LIVE_SESSION_CANCELLED);

    /** Tạo sự kiện mới với {@code eventId} ngẫu nhiên và {@code occurredAt} là hiện tại. */
    public static LiveSessionEvent of(String eventType, Long sessionId, Long courseId, String courseTitle,
                                      String title, String hostName, Instant scheduledAt,
                                      Integer durationMinutes, List<Long> recipientIds) {
        if (!TYPES.contains(eventType)) {
            throw new IllegalArgumentException("Không phải loại sự kiện buổi học trực tuyến: " + eventType);
        }
        return new LiveSessionEvent(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now(),
                sessionId,
                courseId,
                courseTitle,
                title,
                hostName,
                scheduledAt,
                durationMinutes,
                List.copyOf(recipientIds));
    }
}
