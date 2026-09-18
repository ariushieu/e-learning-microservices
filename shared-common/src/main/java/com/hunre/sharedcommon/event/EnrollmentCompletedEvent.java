package com.hunre.sharedcommon.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Người học đã hoàn thành toàn bộ khóa học (tiến độ đạt 100%).
 *
 * <p>Phát bởi enrollment-service lên topic {@link KafkaTopics#ENROLLMENT_EVENTS}.
 * notification-service gửi thông báo theo mẫu {@code COURSE_COMPLETED}.
 *
 * <p>Sự kiện này không đồng nghĩa với việc chứng chỉ đã được cấp. Việc cấp chứng chỉ
 * phát ra {@link CertificateIssuedEvent} riêng, vì có khóa học không cấp chứng chỉ.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EnrollmentCompletedEvent(
        String eventId,
        Instant occurredAt,
        Long enrollmentId,
        Long userId,
        Long courseId,
        String courseTitle,
        Instant completedAt
) implements DomainEvent {

    @Override
    @JsonProperty("eventType")
    public String eventType() {
        return EventTypes.ENROLLMENT_COMPLETED;
    }

    public static EnrollmentCompletedEvent of(
            Long enrollmentId, Long userId, Long courseId, String courseTitle, Instant completedAt) {

        return new EnrollmentCompletedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                enrollmentId,
                userId,
                courseId,
                courseTitle,
                completedAt);
    }
}
