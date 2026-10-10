package com.hunre.sharedcommon.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * Người dùng yêu cầu đặt lại mật khẩu.
 *
 * <p>Phát bởi auth-service lên topic {@link KafkaTopics#AUTH_EVENTS}. notification-service gửi email
 * {@code PASSWORD_RESET} chứa link {@code /reset-password?token=...}, kể cả khi người dùng đã tắt
 * email thông báo: đây là thư bảo mật do chính họ yêu cầu.
 *
 * <p>{@link #token()} là mã thô, chỉ auth-service giữ bản băm. Mã dùng một lần và hết hạn ở
 * {@link #expiresAt()}, nên bản sao nằm trong Kafka hay hàng đợi email chỉ có ích trong thời gian
 * ngắn đó; notification-service xóa link khỏi nội dung đã lưu ngay khi gửi xong.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PasswordResetRequestedEvent(
        String eventId,
        Instant occurredAt,
        Long userId,
        String email,
        String fullName,
        String token,
        Instant expiresAt
) implements DomainEvent {

    @Override
    @JsonProperty("eventType")
    public String eventType() {
        return EventTypes.PASSWORD_RESET_REQUESTED;
    }

    /** Tạo sự kiện mới với {@code eventId} ngẫu nhiên và {@code occurredAt} là hiện tại. */
    public static PasswordResetRequestedEvent of(Long userId, String email, String fullName, String token, Instant expiresAt) {
        return new PasswordResetRequestedEvent(UUID.randomUUID().toString(), Instant.now(), userId, email, fullName,
                token, expiresAt);
    }
}
