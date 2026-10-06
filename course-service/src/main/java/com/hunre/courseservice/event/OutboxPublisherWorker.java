package com.hunre.courseservice.event;

import com.hunre.courseservice.repository.OutboxEventRepository;
import com.hunre.sharedcommon.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherWorker {
    private final OutboxEventRepository repository;
    private final JdbcTemplate jdbc;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaProvider;

    @Scheduled(fixedDelayString = "${course.outbox.delay-ms:3000}")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void publishPendingEvents() {
        var kafka = kafkaProvider.getIfAvailable();
        if (kafka == null) return;
        // Khóa trong database bảo đảm chỉ một worker gửi trong mỗi lượt, kể cả chạy nhiều instance.
        jdbc.queryForObject("SELECT id FROM outbox_dispatch_lock WHERE id = 1 FOR UPDATE", Long.class);
        for (var event : repository.findTop50ByPublishedAtIsNullOrderByIdAsc()) {
            try {
                // Chờ broker xác nhận; producer có giới hạn delivery.timeout.ms và max.block.ms.
                kafka.send(KafkaTopics.COURSE_EVENTS, event.getAggregateId(), event.getPayload()).get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.warn("Bị ngắt khi gửi outbox khóa học {}; sẽ gửi lại", event.getId(), exception);
                break;
            } catch (Exception exception) {
                log.warn("Chưa gửi được outbox khóa học {}; giữ lại để thử sau", event.getId(), exception);
                break;
            }
            event.setPublishedAt(Instant.now());
            repository.save(event);
        }
    }
}
