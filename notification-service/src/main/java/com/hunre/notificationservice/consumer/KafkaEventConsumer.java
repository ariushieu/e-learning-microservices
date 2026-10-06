package com.hunre.notificationservice.consumer;

import com.hunre.notificationservice.repository.ProcessedEventRepository;
import com.hunre.sharedcommon.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
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
 * <p><b>Lỗi thì ném ra, không nuốt.</b> Bản trước bắt mọi exception, ghi log rồi đi tiếp —
 * MySQL chập chờn vài giây là sự kiện trong khoảng đó mất hẳn, vì Kafka coi như đã nhận
 * xong. Giờ exception đi lên {@code DefaultErrorHandler} trong
 * {@code KafkaErrorHandlingConfig}: lỗi tạm thời thì thử lại, message hỏng thì sang topic
 * {@code .DLT}. Consumer vẫn không bị kẹt, vì message hỏng chỉ đi qua một lần.
 */
@Component
@RequiredArgsConstructor
public class KafkaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final EventProcessor eventProcessor;
    private final ProcessedEventRepository processedEventRepository;
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

        JsonNode node;
        try {
            node = objectMapper.readTree(payload);
        } catch (JacksonException ex) {
            throw new InvalidEventException("Message trên topic " + topic + " không phải JSON hợp lệ", ex);
        }

        String eventId = text(node, "eventId");
        String eventType = text(node, "eventType");
        if (eventId == null || eventType == null) {
            throw new InvalidEventException("Message trên topic " + topic + " thiếu eventId hoặc eventType");
        }

        try {
            eventProcessor.process(eventId, eventType, topic, payload);
        } catch (DataIntegrityViolationException ex) {
            // Kafka bảo đảm at-least-once nên nhận lại một sự kiện đã xử lý là chuyện
            // bình thường, không phải lỗi. Đây chính là lúc bảng processed_events làm việc.
            //
            // Nhưng phải hỏi lại sổ cho chắc: lỗi ràng buộc cũng có thể đến từ bảng
            // notifications (tiêu đề quá dài, thiếu userId). Coi nhầm loại đó là "trùng" thì
            // transaction đã rollback, sổ không có dòng nào, và thông báo mất không dấu vết.
            if (processedEventRepository.existsById(eventId)) {
                log.debug("Sự kiện {} đã xử lý trước đó, bỏ qua", eventId);
                return;
            }
            throw new InvalidEventException(
                    "Sự kiện " + eventId + " loại " + eventType + " vi phạm ràng buộc dữ liệu", ex);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }
}
