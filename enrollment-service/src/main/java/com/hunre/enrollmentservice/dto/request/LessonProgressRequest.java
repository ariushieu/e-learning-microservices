package com.hunre.enrollmentservice.dto.request;

import com.hunre.enrollmentservice.entity.LessonProgressStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** lessonId chỉ lấy từ URL; userId chỉ lấy từ token. */
public record LessonProgressRequest(
        @NotNull @Positive Long courseId,
        @NotNull LessonProgressStatus status,
        @Min(0) Integer watchedSeconds) {

    public UpdateLessonProgressRequest forLesson(Long lessonId) {
        return UpdateLessonProgressRequest.builder().courseId(courseId).lessonId(lessonId)
                .status(status).watchedSeconds(watchedSeconds == null ? 0 : watchedSeconds).build();
    }
}
