package com.hunre.quizservice.event;

import com.hunre.sharedcommon.event.KafkaTopics;
import com.hunre.sharedcommon.event.QuizGradedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Gửi kết quả chấm bài lên Kafka.
 *
 * <p><b>Vì sao tự chuyển sang JSON rồi gửi chuỗi.</b> Cách thường dùng là để
 * {@code JsonSerializer} của Spring Kafka lo. Nhưng ở Spring Kafka 4, lớp đó vẫn chạy trên
 * Jackson 2 ({@code com.fasterxml.jackson.databind.ObjectMapper}), trong khi Spring Boot 4
 * đã chuyển sang Jackson 3 ({@code tools.jackson}). Bản Jackson 2 lọt vào classpath qua
 * dependency khác không có module xử lý kiểu thời gian, nên mọi sự kiện đều chết ngay khi
 * gặp trường {@code occurredAt}:
 *
 * <pre>
 * InvalidDefinitionException: Java 8 date/time type `java.time.Instant`
 *     not supported by default (through reference chain: QuizGradedEvent["occurredAt"])
 * </pre>
 *
 * <p>Dùng thẳng Jackson 3 thì gửi và nhận cùng một thư viện, và cũng chính là thư viện mà
 * {@code DomainEventSerializationTest} bên shared-common dùng để khóa hình dạng JSON của
 * các sự kiện. Đổi lại, message không còn header {@code __TypeId__} chứa tên lớp Java —
 * điều đó tốt: consumer định tuyến theo trường {@code eventType} trong nội dung, nên đổi
 * tên lớp bên này không làm hỏng bên kia.
 */
@Component
@Slf4j
public class QuizEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public QuizEventPublisher(@Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishQuizGraded(QuizGradedEvent event) {
        String topic = KafkaTopics.QUIZ_EVENTS;
        // Khóa theo người dùng để mọi sự kiện của cùng một học viên nằm chung một phân vùng,
        // nhờ đó consumer xử lý chúng đúng thứ tự.
        String key = String.valueOf(event.userId());

        log.info("Phát sự kiện {} lên topic {} với key {}: attemptId={}, score={}",
                event.eventType(), topic, key, event.attemptId(), event.score());

        if (kafkaTemplate == null) {
            log.warn("KafkaTemplate không khả dụng, bỏ qua gửi sự kiện {} sang Kafka", event.eventType());
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (RuntimeException ex) {
            // Chuyển sang JSON mà hỏng là lỗi lập trình, không phải sự cố hạ tầng.
            // Ghi cả exception chứ không chỉ getMessage(), nếu không thì nguyên nhân thật
            // nằm ở lớp cause bị mất hẳn và log chỉ còn một câu vô nghĩa.
            log.error("Không chuyển được sự kiện {} sang JSON", event.eventType(), ex);
            return;
        }

        try {
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Lỗi khi gửi sự kiện {} lên Kafka", event.eventType(), ex);
                } else {
                    log.debug("Gửi thành công sự kiện {} offset={}", event.eventType(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            log.error("Không thể gửi sự kiện {} sang Kafka", event.eventType(), ex);
        }
    }
}
