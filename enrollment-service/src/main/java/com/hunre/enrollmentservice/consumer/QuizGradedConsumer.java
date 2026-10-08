package com.hunre.enrollmentservice.consumer;

import com.hunre.sharedcommon.event.EventTypes;
import com.hunre.sharedcommon.event.KafkaTopics;
import com.hunre.sharedcommon.event.QuizGradedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/** Nghe độc lập với notification-service: mỗi group đều nhận được kết quả chấm bài. */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizGradedConsumer {
    private final ObjectMapper mapper;
    private final QuizProgressProcessor processor;

    @KafkaListener(id = "quiz-progress", topics = KafkaTopics.QUIZ_EVENTS,
            groupId = "${app.quiz-progress.group-id:enrollment-quiz-progress}",
            autoStartup = "${spring.kafka.enabled:true}")
    public void onMessage(String payload) {
        QuizGradedEvent event;
        try {
            var node = mapper.readTree(payload);
            if (node == null || !node.isObject() || !node.path("eventType").isString()
                    || node.path("eventType").asString().isBlank()) {
                throw new IllegalArgumentException("Thiếu hoặc sai eventType");
            }
            if (!EventTypes.QUIZ_GRADED.equals(node.path("eventType").asString())) return;
            if (!node.path("passed").isBoolean()) {
                throw new IllegalArgumentException("passed phải là boolean");
            }
            event = mapper.treeToValue(node, QuizGradedEvent.class);
            validate(event);
        } catch (JacksonException | IllegalArgumentException ex) {
            throw new InvalidQuizEventException("Sự kiện chấm bài không hợp lệ", ex);
        }

        try {
            processor.process(event);
        } catch (DuplicateQuizEventException ex) {
            // Transaction trùng đã rollback ở proxy trước khi tới đây. Có thể commit offset.
            log.debug("Bỏ qua quiz event đã xử lý: {}", event.eventId());
        }
        // Không bắt lỗi MySQL: error handler dùng lại ngân sách retry và DLT của course consumer.
    }

    private void validate(QuizGradedEvent event) {
        if (event.eventId() == null || event.eventId().length() != 36
                || !UUID.fromString(event.eventId()).toString().equals(event.eventId())
                || event.occurredAt() == null || !positive(event.attemptId()) || !positive(event.quizId())
                || !positive(event.courseId()) || !positive(event.userId())
                || (event.lessonId() != null && !positive(event.lessonId()))) {
            throw new IllegalArgumentException("Thiếu hoặc sai định danh sự kiện chấm bài");
        }
    }

    private boolean positive(Long id) {
        return id != null && id > 0;
    }
}
