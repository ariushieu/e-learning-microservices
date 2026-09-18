package com.hunre.courseservice.dto.response;

import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseLevel;
import com.hunre.courseservice.entity.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSummaryResponse {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private Long instructorId;
    private String instructorName;
    private String title;
    private String slug;
    private String summary;
    private String thumbnailUrl;
    private CourseLevel level;
    private BigDecimal price;
    private CourseStatus status;
    private Integer totalLessons;
    private Integer totalDurationSeconds;
    private Integer studentCount;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private LocalDateTime publishedAt;

    public static CourseSummaryResponse from(Course course) {
        if (course == null) {
            return null;
        }

        Long categoryId = null;
        String categoryName = null;
        if (course.getCategory() != null) {
            categoryId = course.getCategory().getId();
            categoryName = course.getCategory().getName();
        }

        return CourseSummaryResponse.builder()
                .id(course.getId())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .instructorId(course.getInstructorId())
                .instructorName(course.getInstructorName())
                .title(course.getTitle())
                .slug(course.getSlug())
                .summary(course.getSummary())
                .thumbnailUrl(course.getThumbnailUrl())
                .level(course.getLevel())
                .price(course.getPrice())
                .status(course.getStatus())
                .totalLessons(course.getTotalLessons())
                .totalDurationSeconds(course.getTotalDurationSeconds())
                .studentCount(course.getStudentCount())
                .ratingAvg(course.getRatingAvg())
                .ratingCount(course.getRatingCount())
                .publishedAt(course.getPublishedAt())
                .build();
    }
}
