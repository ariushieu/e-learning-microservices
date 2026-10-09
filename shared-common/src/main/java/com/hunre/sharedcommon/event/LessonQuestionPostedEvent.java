package com.hunre.sharedcommon.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Học viên vừa đặt câu hỏi trong một bài học. Phát bởi course-service lên
 * {@link KafkaTopics#COURSE_EVENTS}; notification-service báo cho giảng viên của khóa.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LessonQuestionPostedEvent(
        String eventId,
        Instant occurredAt,
        Long questionId,
        Long lessonId,
        Long courseId,
        String courseTitle,
        String lessonTitle,
        Long askerId,
        String askerName,
        Long instructorId,
        String preview
) implements DomainEvent {

    @Override
    @JsonProperty("eventType")
    public String eventType() {
        return EventTypes.LESSON_QUESTION_POSTED;
    }

    public static LessonQuestionPostedEvent of(Long questionId, Long lessonId, Long courseId, String courseTitle,
                                               String lessonTitle, Long askerId, String askerName,
                                               Long instructorId, String preview) {
        return new LessonQuestionPostedEvent(UUID.randomUUID().toString(), Instant.now(), questionId, lessonId,
                courseId, courseTitle, lessonTitle, askerId, askerName, instructorId, preview);
    }
}
