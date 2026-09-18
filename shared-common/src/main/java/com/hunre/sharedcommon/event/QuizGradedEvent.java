package com.hunre.sharedcommon.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Một lượt làm bài kiểm tra đã được chấm xong.
 *
 * <p>Phát bởi quiz-service lên topic {@link KafkaTopics#QUIZ_EVENTS}.
 * notification-service gửi thông báo theo mẫu {@code QUIZ_GRADED};
 * enrollment-service có thể nghe để cập nhật tiến độ học.
 *
 * <p>{@code score} là phần trăm từ 0 đến 100, kiểu {@link BigDecimal} để khớp với cột
 * {@code DECIMAL(5,2)} trong {@code quiz_attempts.score}. Không dùng {@code double} cho
 * điểm số vì số thực dấu phẩy động làm tròn sai lệch khi cộng dồn.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuizGradedEvent(
        String eventId,
        Instant occurredAt,
        Long attemptId,
        Long quizId,
        Long courseId,
        Long userId,
        String quizTitle,
        BigDecimal score,
        boolean passed
) implements DomainEvent {

    @Override
    @JsonProperty("eventType")
    public String eventType() {
        return EventTypes.QUIZ_GRADED;
    }

    public static QuizGradedEvent of(
            Long attemptId,
            Long quizId,
            Long courseId,
            Long userId,
            String quizTitle,
            BigDecimal score,
            boolean passed) {

        return new QuizGradedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                attemptId,
                quizId,
                courseId,
                userId,
                quizTitle,
                score,
                passed);
    }
}
