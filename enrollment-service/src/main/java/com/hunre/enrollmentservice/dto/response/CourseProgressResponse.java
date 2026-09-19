package com.hunre.enrollmentservice.dto.response;

import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Phản hồi chi tiết tiến độ khóa học gồm % hoàn thành và danh sách các bài học.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseProgressResponse {

    private Long courseId;
    private Long enrollmentId;
    private String courseTitle;
    private EnrollmentStatus status;
    private BigDecimal progressPercent;
    private int completedLessonsCount;
    private int totalLessonsCount;
    private Instant lastAccessedAt;

    private String certificateCode;

    @Builder.Default
    private List<LessonProgressResponse> lessons = new ArrayList<>();
}
