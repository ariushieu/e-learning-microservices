package com.hunre.authservice.outbox;

import com.hunre.sharedcommon.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Gửi sự kiện tài khoản đã commit sang Kafka; dòng gửi lỗi được thử lại ở lượt sau. */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisherWorker {

    private final OutboxEventRepository outboxEventRepository;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.publisher.delay-ms:3000}")
    public void publishPendingEvents() {
        if (kafkaTemplate == null) {
            return;
        }
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByPublishedAtIsNullOrderByIdAsc();
        for (OutboxEvent event : pendingEvents) {
            if (!publishEvent(event)) {
                // Dừng lượt này để giữ thứ tự; lượt sau gửi lại từ dòng lỗi.
                break;
            }
        }
    }

    public boolean publishEvent(OutboxEvent event) {
        try {
            // Khóa là userId: mọi sự kiện của một người vào cùng partition, giữ đúng thứ tự.
            kafkaTemplate.send(KafkaTopics.AUTH_EVENTS, event.getAggregateId(), event.getPayload()).get(5, TimeUnit.SECONDS);
            event.setPublishedAt(Instant.now());
            outboxEventRepository.save(event);
            log.info("Published auth outbox event id={} type={}", event.getId(), event.getEventType());
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing auth outbox event id={}", event.getId(), ex);
            return false;
        } catch (Exception ex) {
            log.error("Could not publish auth outbox event id={}", event.getId(), ex);
            return false;
        }
    }
}
