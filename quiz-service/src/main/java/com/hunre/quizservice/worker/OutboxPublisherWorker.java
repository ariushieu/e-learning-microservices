package com.hunre.quizservice.worker;

import com.hunre.quizservice.entity.OutboxEvent;
import com.hunre.quizservice.repository.OutboxEventRepository;
import com.hunre.sharedcommon.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Sends committed quiz events to Kafka and retries rows left unpublished after failures. */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisherWorker {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.publisher.delay-ms:3000}")
    public void publishPendingEvents() {
        if (kafkaTemplate == null) {
            return;
        }

        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEvent event : pendingEvents) {
            if (!publishEvent(event)) {
                // Stop this batch to preserve order and retry the failed event next time.
                break;
            }
        }
    }

    public boolean publishEvent(OutboxEvent event) {
        try {
            String key = userIdFrom(event);
            kafkaTemplate.send(KafkaTopics.QUIZ_EVENTS, key, event.getPayload()).get(5, TimeUnit.SECONDS);

            event.setPublishedAt(Instant.now());
            outboxEventRepository.save(event);
            log.info("Published quiz outbox event id={} eventId={}", event.getId(), event.getEventId());
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing quiz outbox event id={}", event.getId(), ex);
            return false;
        } catch (Exception ex) {
            log.error("Could not publish quiz outbox event id={}: {}", event.getId(), ex.getMessage());
            return false;
        }
    }

    private String userIdFrom(OutboxEvent event) {
        try {
            JsonNode userId = objectMapper.readTree(event.getPayload()).get("userId");
            if (userId != null && !userId.isNull()) {
                return userId.asString();
            }
        } catch (Exception ex) {
            log.warn("Could not read userId from quiz outbox event id={}: {}", event.getId(), ex.getMessage());
        }
        return event.getAggregateId();
    }
}
