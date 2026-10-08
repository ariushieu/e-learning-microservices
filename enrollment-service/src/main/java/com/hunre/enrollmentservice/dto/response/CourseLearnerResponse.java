package com.hunre.enrollmentservice.dto.response;

import com.hunre.enrollmentservice.entity.Enrollment;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** userId supports the legacy-name fallback; no email or authentication details leave this API. */
public record CourseLearnerResponse(
        Long enrollmentId,
        Long userId,
        String learnerName,
        EnrollmentStatus status,
        BigDecimal progressPercent,
        Instant enrolledAt,
        Instant lastAccessedAt,
        Instant completedAt,
        String certificateCode
) {
    public static CourseLearnerResponse from(Enrollment enrollment, String certificateCode) {
        return new CourseLearnerResponse(enrollment.getId(), enrollment.getUserId(), enrollment.getLearnerName(),
                enrollment.getStatus(), enrollment.getProgressPercent(), enrollment.getEnrolledAt(),
                enrollment.getLastAccessedAt(), enrollment.getCompletedAt(), certificateCode);
    }
}
