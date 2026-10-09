package com.hunre.sharedcommon.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * Một tài khoản mới vừa đăng ký.
 *
 * <p>Phát bởi auth-service lên topic {@link KafkaTopics#AUTH_EVENTS}. notification-service lưu
 * email và tên vào bảng {@code user_contacts} của mình rồi gửi email chào mừng.
 *
 * <p>Các sự kiện nghiệp vụ khác (ghi danh, chứng chỉ...) cố ý không mang email. Chỉ sự kiện của
 * auth-service, nơi sở hữu thông tin liên lạc, mới mang; ai cần gửi thư thì giữ bản sao từ đây.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserRegisteredEvent(
        String eventId,
        Instant occurredAt,
        Long userId,
        String email,
        String fullName
) implements DomainEvent {

    @Override
    @JsonProperty("eventType")
    public String eventType() {
        return EventTypes.USER_REGISTERED;
    }

    /** Tạo sự kiện mới với {@code eventId} ngẫu nhiên và {@code occurredAt} là hiện tại. */
    public static UserRegisteredEvent of(Long userId, String email, String fullName) {
        return new UserRegisteredEvent(UUID.randomUUID().toString(), Instant.now(), userId, email, fullName);
    }
}
