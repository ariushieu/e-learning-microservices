package com.hunre.quizservice.event;

import com.hunre.sharedcommon.event.KafkaTopics;
import com.hunre.sharedcommon.event.QuizGradedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class QuizEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public QuizEventPublisher(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishQuizGraded(QuizGradedEvent event) {
        String topic = KafkaTopics.QUIZ_EVENTS;
        String key = String.valueOf(event.userId());

        log.info("Phát sự kiện {} lên topic {} với key {}: attemptId={}, score={}",
                event.eventType(), topic, key, event.attemptId(), event.score());

        if (kafkaTemplate == null) {
            log.warn("KafkaTemplate không khả dụng, bỏ qua gửi sự kiện {} sang Kafka", event.eventType());
            return;
        }

        try {
            kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Lỗi khi gửi sự kiện {} lên Kafka: {}", event.eventType(), ex.getMessage());
                } else {
                    log.debug("Gửi thành công sự kiện {} offset={}", event.eventType(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            log.error("Không thể gửi sự kiện {} sang Kafka (broker có thể chưa sẵn sàng): {}",
                    event.eventType(), ex.getMessage());
        }
    }
}
