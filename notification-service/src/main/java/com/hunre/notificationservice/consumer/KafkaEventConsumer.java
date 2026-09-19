package com.hunre.notificationservice.consumer;

import com.hunre.sharedcommon.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Nhận sự kiện từ ba topic và giao cho {@link EventProcessor} xử lý.
 *
 * <p><b>Vì sao đọc chuỗi thô rồi tự phân tích.</b> Cách thông thường là để Spring Kafka
 * chuyển JSON thành đối tượng dựa vào header {@code __TypeId__} mà bên gửi gắn vào. Làm vậy
 * thì tên lớp Java của service phát sự kiện trở thành một phần của hợp đồng: đổi gói hay
 * đổi tên lớp bên ấy là bên này hỏng. Ở đây định tuyến bằng trường {@code eventType} trong
 * chính nội dung message — thứ đã được
 * {@code DomainEventSerializationTest} bên shared-common khóa lại.
 *
 * <p>Message hỏng không được ném ra ngoài: ném thì Kafka gửi lại mãi và consumer đứng yên
 * tại chỗ đó, không nhận được sự kiện nào sau nó nữa.
 */
@Component
@RequiredArgsConstructor
public class KafkaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final EventProcessor eventProcessor;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {
                    KafkaTopics.ENROLLMENT_EVENTS,
                    KafkaTopics.QUIZ_EVENTS,
                    KafkaTopics.COURSE_EVENTS
            },
            groupId = "${spring.kafka.consumer.group-id}",
            // Cùng một công tắc với quiz-service: máy không chạy Kafka thì đặt
            // spring.kafka.enabled=false, service vẫn khởi động, chỉ không nghe gì.
            autoStartup = "${spring.kafka.enabled:true}")
    public void onMessage(String payload,
                          @org.springframework.messaging.handler.annotation.Header(
                                  name = "kafka_receivedTopic", required = false) String topic) {

        String eventId;
        String eventType;

        try {
            JsonNode node = objectMapper.readTree(payload);
            eventId = text(node, "eventId");
            eventType = text(node, "eventType");
        } catch (RuntimeException ex) {
            log.error("Message trên topic {} không phải JSON hợp lệ, bỏ qua: {}",
                    topic, ex.getMessage());
            return;
        }

        if (eventId == null || eventType == null) {
            log.error("Message trên topic {} thiếu eventId hoặc eventType, bỏ qua", topic);
            return;
        }

        try {
            eventProcessor.process(eventId, eventType, topic, payload);
        } catch (DataIntegrityViolationException ex) {
            // Kafka bảo đảm at-least-once nên nhận lại một sự kiện đã xử lý là chuyện
            // bình thường, không phải lỗi. Đây chính là lúc bảng processed_events làm việc.
            log.debug("Sự kiện {} đã xử lý trước đó, bỏ qua", eventId);
        } catch (RuntimeException ex) {
            // Không ném tiếp: ném thì Kafka gửi lại đúng message này mãi và mọi sự kiện
            // phía sau bị chặn lại. Ghi log để còn lần ra, rồi đi tiếp.
            log.error("Lỗi khi xử lý sự kiện {} loại {} trên topic {}",
                    eventId, eventType, topic, ex);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }
}
