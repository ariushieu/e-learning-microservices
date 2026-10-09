package com.hunre.sharedcommon.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Có người trả lời một câu hỏi trong bài học. Phát bởi course-service lên
 * {@link KafkaTopics#COURSE_EVENTS}; notification-service báo cho người đã hỏi.
 *
 * <p>{@code answererRole} là INSTRUCTOR, ADMIN hoặc STUDENT, để thông báo nói rõ ai trả lời.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LessonQuestionAnsweredEvent(
        String eventId,
        Instant occurredAt,
        Long questionId,
        Long answerId,
        Long lessonId,
        Long courseId,
        String lessonTitle,
        Long askerId,
        Long answererId,
        String answererName,
        String answererRole,
        String preview
) implements DomainEvent {

    @Override
    @JsonProperty("eventType")
    public String eventType() {
        return EventTypes.LESSON_QUESTION_ANSWERED;
    }

    public static LessonQuestionAnsweredEvent of(Long questionId, Long answerId, Long lessonId, Long courseId,
                                                 String lessonTitle, Long askerId, Long answererId,
                                                 String answererName, String answererRole, String preview) {
        return new LessonQuestionAnsweredEvent(UUID.randomUUID().toString(), Instant.now(), questionId, answerId,
                lessonId, courseId, lessonTitle, askerId, answererId, answererName, answererRole, preview);
    }
}
