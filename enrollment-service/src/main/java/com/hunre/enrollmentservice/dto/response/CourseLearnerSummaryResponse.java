package com.hunre.enrollmentservice.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CourseLearnerSummaryResponse(
        long active,
        long completed,
        long cancelled,
        BigDecimal averageProgress,
        BigDecimal completionRate,
        long certificatesIssued,
        List<LessonCompletion> lessons) {

    public record LessonCompletion(Long lessonId, long completedCount, BigDecimal completionRate) {}
}
