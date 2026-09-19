package com.hunre.enrollmentservice.dto.response;

import com.hunre.enrollmentservice.entity.LessonProgress;
import com.hunre.enrollmentservice.entity.LessonProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Phản hồi chi tiết tiến độ bài học.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonProgressResponse {

    private Long lessonId;
    private LessonProgressStatus status;
    private Integer watchedSeconds;
    private Instant completedAt;

    public static LessonProgressResponse from(LessonProgress progress) {
        return LessonProgressResponse.builder()
                .lessonId(progress.getLessonId())
                .status(progress.getStatus())
                .watchedSeconds(progress.getWatchedSeconds())
                .completedAt(progress.getCompletedAt())
                .build();
    }
}
