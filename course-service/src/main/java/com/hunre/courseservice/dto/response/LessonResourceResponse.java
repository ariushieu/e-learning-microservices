package com.hunre.courseservice.dto.response;

import com.hunre.courseservice.entity.LessonResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResourceResponse {

    private Long id;
    private Long lessonId;
    private String name;
    private String fileUrl;
    private LocalDateTime createdAt;

    public static LessonResourceResponse from(LessonResource resource) {
        if (resource == null) {
            return null;
        }

        Long lessonId = resource.getLesson() != null ? resource.getLesson().getId() : null;

        return LessonResourceResponse.builder()
                .id(resource.getId())
                .lessonId(lessonId)
                .name(resource.getName())
                .fileUrl(resource.getFileUrl())
                .createdAt(resource.getCreatedAt())
                .build();
    }
}
