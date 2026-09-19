package com.hunre.enrollmentservice.dto.response;

import com.hunre.enrollmentservice.entity.Enrollment;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Phản hồi chi tiết lượt ghi danh.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponse {

    private Long id;
    private Long userId;
    private Long courseId;
    private String courseTitle;
    private EnrollmentStatus status;
    private BigDecimal progressPercent;
    private Instant enrolledAt;
    private Instant completedAt;
    private Instant lastAccessedAt;

    public static EnrollmentResponse from(Enrollment enrollment, String courseTitle) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .userId(enrollment.getUserId())
                .courseId(enrollment.getCourseId())
                .courseTitle(courseTitle)
                .status(enrollment.getStatus())
                .progressPercent(enrollment.getProgressPercent())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .lastAccessedAt(enrollment.getLastAccessedAt())
                .build();
    }
}
