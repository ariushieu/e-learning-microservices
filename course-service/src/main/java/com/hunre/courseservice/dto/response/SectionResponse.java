package com.hunre.courseservice.dto.response;

import com.hunre.courseservice.entity.Section;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionResponse {

    private Long id;
    private Long courseId;
    private String title;
    private Integer position;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<LessonResponse> lessons = new ArrayList<>();

    public static SectionResponse from(Section section) {
        if (section == null) {
            return null;
        }

        Long courseId = section.getCourse() != null ? section.getCourse().getId() : null;

        List<LessonResponse> lessonList = new ArrayList<>();
        if (section.getLessons() != null && !section.getLessons().isEmpty()) {
            for (var l : section.getLessons()) {
                lessonList.add(LessonResponse.from(l));
            }
        }

        return SectionResponse.builder()
                .id(section.getId())
                .courseId(courseId)
                .title(section.getTitle())
                .position(section.getPosition())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .lessons(lessonList)
                .build();
    }
}
