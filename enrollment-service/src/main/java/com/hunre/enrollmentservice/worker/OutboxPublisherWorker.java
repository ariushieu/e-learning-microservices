package com.hunre.enrollmentservice.worker;

import com.hunre.enrollmentservice.entity.OutboxEvent;
import com.hunre.enrollmentservice.repository.OutboxEventRepository;
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

/**
 * Worker định kỳ quét các sự kiện chưa gửi trong bảng {@code outbox_events}
 * và đẩy lên Kafka topic {@link KafkaTopics#ENROLLMENT_EVENTS}.
 *
 * <p>Áp dụng mẫu Transactional Outbox: các nghiệp vụ ghi danh, tiến độ, chứng chỉ
 * lưu sự kiện vào {@code outbox_events} trong cùng transaction cơ sở dữ liệu.
 * Worker này chạy độc lập ở nền, đọc sự kiện chưa gửi và phát lên Kafka với cơ chế thử lại.
 */
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
            log.trace("KafkaTemplate không khả dụng, bỏ qua gửi outbox");
            return;
        }

        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Tìm thấy {} sự kiện outbox chưa gửi", pendingEvents.size());
        for (OutboxEvent event : pendingEvents) {
            boolean sent = publishEvent(event);
            if (!sent) {
                // Nếu 1 event gửi thất bại (ví dụ mất kết nối Kafka), dừng vòng lặp đợt này
                // để giữ đúng thứ tự sự kiện và tránh spam log lỗi
                log.warn("Tạm dừng đợt gửi outbox hiện tại do sự kiện id={} gửi thất bại", event.getId());
                break;
            }
        }
    }

    /**
     * Gửi một sự kiện lên Kafka và đánh dấu thời điểm published_at nếu thành công.
     *
     * @return {@code true} nếu gửi thành công và đã lưu trạng thái, {@code false} nếu lỗi
     */
    public boolean publishEvent(OutboxEvent event) {
        String topic = resolveTopic(event);
        String key = resolveKey(event);

        try {
            kafkaTemplate.send(topic, key, event.getPayload()).get(5, TimeUnit.SECONDS);

            event.setPublishedAt(Instant.now());
            outboxEventRepository.save(event);
            log.info("Đã gửi outbox event [id={}, eventId={}, type={}] lên topic {} (key={})",
                    event.getId(), event.getEventId(), event.getEventType(), topic, key);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Bị ngắt quãng khi gửi outbox event id {}", event.getId(), e);
            return false;
        } catch (Exception e) {
            log.error("Gửi outbox event [id={}, eventId={}, type={}] thất bại: {}",
                    event.getId(), event.getEventId(), event.getEventType(), e.getMessage());
            return false;
        }
    }

    private String resolveTopic(OutboxEvent event) {
        // Toàn bộ sự kiện của enrollment-service thuộc topic ENROLLMENT_EVENTS
        return KafkaTopics.ENROLLMENT_EVENTS;
    }

    private String resolveKey(OutboxEvent event) {
        if (event.getPayload() != null) {
            try {
                JsonNode node = objectMapper.readTree(event.getPayload());
                JsonNode userIdNode = node.get("userId");
                if (userIdNode != null && !userIdNode.isNull()) {
                    String userIdText = userIdNode.asString();
                    return userIdText != null ? userIdText : userIdNode.toString();
                }
            } catch (Exception e) {
                log.warn("Không thể trích xuất userId từ payload của outbox event id {}: {}",
                        event.getId(), e.getMessage());
            }
        }
        return event.getAggregateId();
    }
}
