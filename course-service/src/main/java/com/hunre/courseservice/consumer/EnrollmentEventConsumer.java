package com.hunre.courseservice.consumer;

import com.hunre.courseservice.repository.ProcessedEventRepository;
import com.hunre.sharedcommon.event.EnrollmentCreatedEvent;
import com.hunre.sharedcommon.event.EventTypes;
import com.hunre.sharedcommon.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Chỉ đếm enrollment.created, không đếm sự kiện hoàn thành/cấp chứng chỉ. */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentEventConsumer {
    private final EnrollmentEventProcessor processor;
    private final ProcessedEventRepository processedEvents;
    private final ObjectMapper mapper;

    @KafkaListener(topics = KafkaTopics.ENROLLMENT_EVENTS,
            groupId = "${spring.kafka.consumer.group-id:course-service}",
            autoStartup = "${spring.kafka.enabled:true}")
    public void onMessage(String payload) {
        EnrollmentCreatedEvent event;
        try {
            JsonNode node = mapper.readTree(payload);
            if (node == null || !node.isObject() || !node.path("eventType").isString()
                    || node.path("eventType").asString().isBlank()) {
                throw new InvalidEventException("Message thiếu eventType hợp lệ");
            }
            if (!EventTypes.ENROLLMENT_CREATED.equals(node.path("eventType").asString())) return;
            JsonNode eventId = node.path("eventId");
            if (!eventId.isString() || eventId.asString().isBlank() || eventId.asString().length() > 36
                    || !positiveId(node.path("courseId")) || !positiveId(node.path("userId"))
                    || !positiveId(node.path("enrollmentId")) || !node.path("occurredAt").isString()) {
                throw new InvalidEventException("Sự kiện ghi danh thiếu trường hoặc sai kiểu dữ liệu");
            }
            event = mapper.readValue(payload, EnrollmentCreatedEvent.class);
            if (event.occurredAt() == null) {
                throw new InvalidEventException("Sự kiện ghi danh thiếu thời điểm phát sinh");
            }
        } catch (JacksonException ex) {
            throw new InvalidEventException("Sự kiện ghi danh không phải JSON hợp lệ", ex);
        }
        try {
            processor.process(event);
        } catch (DataIntegrityViolationException ex) {
            // Kiểm tra sau khi transaction lỗi đã rollback; không nuốt lỗi ràng buộc khác.
            if (processedEvents.existsById(event.eventId())) {
                log.debug("Bỏ qua sự kiện ghi danh trùng {}", event.eventId());
                return;
            }
            throw new InvalidEventException("Sự kiện ghi danh vi phạm ràng buộc dữ liệu", ex);
        }
    }

    private boolean positiveId(JsonNode node) {
        return node.isIntegralNumber() && node.canConvertToLong() && node.asLong() > 0;
    }
}
