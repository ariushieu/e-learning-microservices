package com.hunre.authservice.outbox;

import com.hunre.authservice.domain.User;
import com.hunre.sharedcommon.event.DomainEvent;
import com.hunre.sharedcommon.event.PasswordResetRequestedEvent;
import com.hunre.sharedcommon.event.UserProfileUpdatedEvent;
import com.hunre.sharedcommon.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

/**
 * Ghi sự kiện tài khoản vào outbox trong cùng transaction với thay đổi trên bảng users.
 * OutboxPublisherWorker gửi sang Kafka sau khi commit, nên rollback thì không có sự kiện nào lọt ra.
 */
@Component
@RequiredArgsConstructor
public class AccountEventPublisher {

    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void registered(User user) {
        save(user, UserRegisteredEvent.of(user.getId(), user.getEmail(), user.getFullName()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void profileUpdated(User user) {
        save(user, UserProfileUpdatedEvent.of(user.getId(), user.getEmail(), user.getFullName()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void passwordResetRequested(User user, String rawToken, Instant expiresAt) {
        save(user, PasswordResetRequestedEvent.of(user.getId(), user.getEmail(), user.getFullName(), rawToken, expiresAt));
    }

    private void save(User user, DomainEvent event) {
        outbox.save(OutboxEvent.builder()
                .eventId(event.eventId())
                .aggregateType("USER")
                .aggregateId(String.valueOf(user.getId()))
                .eventType(event.eventType())
                .payload(objectMapper.writeValueAsString(event))
                .build());
    }
}
