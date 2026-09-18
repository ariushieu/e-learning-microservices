package com.hunre.courseservice.dto.response;

import com.hunre.courseservice.entity.Lesson;
import com.hunre.courseservice.entity.LessonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResponse {

    private Long id;
    private Long sectionId;
    private Long courseId;
    private String title;
    private LessonType type;
    private Integer durationSeconds;
    private Integer position;
    private Boolean isPreview;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<LessonResourceResponse> resources = new ArrayList<>();

    public static LessonResponse from(Lesson lesson) {
        if (lesson == null) {
            return null;
        }

        Long sectionId = lesson.getSection() != null ? lesson.getSection().getId() : null;
        Long courseId = lesson.getCourse() != null ? lesson.getCourse().getId() : null;

        List<LessonResourceResponse> resList = new ArrayList<>();
        if (lesson.getResources() != null && !lesson.getResources().isEmpty()) {
            for (var r : lesson.getResources()) {
                resList.add(LessonResourceResponse.from(r));
            }
        }

        return LessonResponse.builder()
                .id(lesson.getId())
                .sectionId(sectionId)
                .courseId(courseId)
                .title(lesson.getTitle())
                .type(lesson.getType())
                .durationSeconds(lesson.getDurationSeconds())
                .position(lesson.getPosition())
                .isPreview(lesson.getIsPreview())
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .resources(resList)
                .build();
    }
}
