package com.hunre.sharedcommon.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Chứng chỉ hoàn thành khóa học vừa được cấp.
 *
 * <p>Phát bởi enrollment-service lên topic {@link KafkaTopics#ENROLLMENT_EVENTS}.
 * notification-service gửi email theo mẫu {@code CERTIFICATE_ISSUED}, trong đó
 * {@code certificateCode} và {@code certificateUrl} được điền vào nội dung thư.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CertificateIssuedEvent(
        String eventId,
        Instant occurredAt,
        Long certificateId,
        Long enrollmentId,
        Long userId,
        Long courseId,
        String courseTitle,
        String certificateCode,
        String certificateUrl
) implements DomainEvent {

    @Override
    @JsonProperty("eventType")
    public String eventType() {
        return EventTypes.CERTIFICATE_ISSUED;
    }

    public static CertificateIssuedEvent of(
            Long certificateId,
            Long enrollmentId,
            Long userId,
            Long courseId,
            String courseTitle,
            String certificateCode,
            String certificateUrl) {

        return new CertificateIssuedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                certificateId,
                enrollmentId,
                userId,
                courseId,
                courseTitle,
                certificateCode,
                certificateUrl);
    }
}
