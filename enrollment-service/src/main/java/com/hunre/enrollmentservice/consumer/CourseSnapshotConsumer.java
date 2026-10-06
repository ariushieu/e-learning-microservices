package com.hunre.enrollmentservice.consumer;

import com.hunre.enrollmentservice.entity.CourseSnapshot;
import com.hunre.enrollmentservice.repository.CourseSnapshotRepository;
import com.hunre.sharedcommon.event.CourseUpdatedEvent;
import com.hunre.sharedcommon.event.EventTypes;
import com.hunre.sharedcommon.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Set;

/** Bản sao khóa học: mỗi message thay thế toàn bộ dòng, kể cả các trường null. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseSnapshotConsumer {

    private final ObjectMapper objectMapper;
    private final CourseSnapshotRepository repository;

    @KafkaListener(id = "course-snapshots", topics = KafkaTopics.COURSE_EVENTS,
            groupId = "${spring.kafka.consumer.group-id:enrollment-service}",
            autoStartup = "${spring.kafka.enabled:true}")
    public void onMessage(String payload) {
        CourseUpdatedEvent event;
        try {
            var node = objectMapper.readTree(payload);
            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException("Message phải là JSON object");
            }
            if (!EventTypes.COURSE_UPDATED.equals(node.path("eventType").asString())) {
                return;
            }
            event = objectMapper.treeToValue(node, CourseUpdatedEvent.class);
            validate(event);
        } catch (RuntimeException ex) {
            // Dữ liệu sai không thể sửa bằng retry; không ghi payload vào log.
            log.warn("Bỏ qua course.updated không hợp lệ ({})", ex.getClass().getSimpleName());
            return;
        }

        // Không nuốt lỗi database: error handler retry, chưa commit offset khi ghi thất bại.
        // save() INSERT hoặc UPDATE theo courseId; nhận trùng không tạo thêm dòng.
        repository.save(CourseSnapshot.builder()
                .courseId(event.courseId()).title(event.title()).slug(event.slug())
                .thumbnailUrl(event.thumbnailUrl()).instructorId(event.instructorId())
                .instructorName(event.instructorName()).totalLessons(event.totalLessons())
                .status(event.status()).syncedAt(Instant.now()).build());
    }

    private void validate(CourseUpdatedEvent event) {
        if (event.eventId() == null || event.eventId().isBlank() || event.occurredAt() == null
                || event.courseId() == null || event.courseId() <= 0
                || event.title() == null || event.title().isBlank() || event.title().length() > 200
                || event.slug() == null || event.slug().isBlank()
                || event.totalLessons() == null || event.totalLessons() < 0
                || event.status() == null || !Set.of("DRAFT", "PUBLISHED", "ARCHIVED").contains(event.status())
                || tooLong(event.slug(), 220) || tooLong(event.thumbnailUrl(), 500)
                || tooLong(event.instructorName(), 150)
                || (event.instructorId() != null && event.instructorId() <= 0)) {
            throw new IllegalArgumentException("Thiếu hoặc sai trường trong snapshot khóa học");
        }
    }

    private boolean tooLong(String value, int max) {
        return value != null && value.length() > max;
    }
}
