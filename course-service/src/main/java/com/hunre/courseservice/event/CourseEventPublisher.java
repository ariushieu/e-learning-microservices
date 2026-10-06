package com.hunre.courseservice.event;

import com.hunre.courseservice.entity.Course;
import com.hunre.sharedcommon.event.CourseUpdatedEvent;
import com.hunre.sharedcommon.event.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Gửi thông tin trạng thái khóa học lên Kafka topic {@link KafkaTopics#COURSE_EVENTS}.
 *
 * <p>Tự chuyển đổi sự kiện sang JSON bằng {@link ObjectMapper} (Jackson 3 của Spring Boot 4)
 * và gửi dưới dạng chuỗi UTF-8 qua {@code KafkaTemplate<String, String>}.
 */
@Component
@Slf4j
public class CourseEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public CourseEventPublisher(@Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Phát sự kiện {@link CourseUpdatedEvent} chứa ảnh chụp toàn bộ trạng thái hiện tại của khóa học.
     *
     * @param course thực thể khóa học
     */
    public void publishCourseUpdated(Course course) {
        if (course == null) {
            return;
        }

        CourseUpdatedEvent event = CourseUpdatedEvent.of(
                course.getId(),
                course.getTitle(),
                course.getSlug(),
                course.getThumbnailUrl(),
                course.getInstructorId(),
                course.getInstructorName(),
                course.getTotalLessons(),
                course.getStatus().name()
        );

        String topic = KafkaTopics.COURSE_EVENTS;
        // Khóa message là courseId để mọi phiên bản của cùng một khóa học tới đúng thứ tự
        String key = String.valueOf(course.getId());

        log.info("Phát sự kiện {} lên topic {} với key {}: status={}, totalLessons={}",
                event.eventType(), topic, key, event.status(), event.totalLessons());

        if (kafkaTemplate == null) {
            log.warn("KafkaTemplate không khả dụng, bỏ qua gửi sự kiện {} sang Kafka", event.eventType());
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (RuntimeException ex) {
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
