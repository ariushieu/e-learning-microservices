package com.hunre.sharedcommon.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * Ảnh chụp thông tin liên lạc hiện tại của một tài khoản, sau khi người dùng sửa hồ sơ.
 *
 * <p>Phát bởi auth-service lên topic {@link KafkaTopics#AUTH_EVENTS}. notification-service ghi đè
 * dòng tương ứng trong {@code user_contacts}. Migration V7 của auth-service cũng phát sự kiện này
 * cho mọi tài khoản có sẵn để nạp bảng lần đầu (không dùng {@link UserRegisteredEvent} để không ai
 * nhận lại email chào mừng).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserProfileUpdatedEvent(
        String eventId,
        Instant occurredAt,
        Long userId,
        String email,
        String fullName
) implements DomainEvent {

    @Override
    @JsonProperty("eventType")
    public String eventType() {
        return EventTypes.USER_PROFILE_UPDATED;
    }

    /** Tạo sự kiện mới với {@code eventId} ngẫu nhiên và {@code occurredAt} là hiện tại. */
    public static UserProfileUpdatedEvent of(Long userId, String email, String fullName) {
        return new UserProfileUpdatedEvent(UUID.randomUUID().toString(), Instant.now(), userId, email, fullName);
    }
}
