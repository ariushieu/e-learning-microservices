package com.hunre.courseservice.dto.request;

import com.hunre.courseservice.entity.LessonType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLessonRequest {

    @NotNull(message = "ID chương không được để trống")
    private Long sectionId;

    @NotBlank(message = "Tiêu đề bài học không được để trống")
    @Size(max = 200, message = "Tiêu đề bài học không được vượt quá 200 ký tự")
    private String title;

    @Builder.Default
    private LessonType type = LessonType.VIDEO;

    @Min(value = 0, message = "Thời lượng bài học phải lớn hơn hoặc bằng 0")
    @Builder.Default
    private Integer durationSeconds = 0;

    @Min(value = 0, message = "Thứ tự hiển thị phải lớn hơn hoặc bằng 0")
    @Builder.Default
    private Integer position = 0;

    @Builder.Default
    private Boolean isPreview = false;
}
