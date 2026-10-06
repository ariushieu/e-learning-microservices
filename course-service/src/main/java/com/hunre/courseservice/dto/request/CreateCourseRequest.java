package com.hunre.courseservice.dto.request;

import com.hunre.courseservice.entity.CourseLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCourseRequest {

    @NotNull(message = "Danh mục khóa học không được để trống")
    private Long categoryId;

    @Size(max = 150, message = "Tên giảng viên không được vượt quá 150 ký tự")
    private String instructorName;

    @NotBlank(message = "Tiêu đề khóa học không được để trống")
    @Size(max = 200, message = "Tiêu đề khóa học không được vượt quá 200 ký tự")
    private String title;

    @Size(max = 220, message = "Slug không được vượt quá 220 ký tự")
    private String slug;

    @Size(max = 500, message = "Tóm tắt không được vượt quá 500 ký tự")
    private String summary;

    private String description;

    @Size(max = 500, message = "URL ảnh đại diện không được vượt quá 500 ký tự")
    private String thumbnailUrl;

    @Builder.Default
    private CourseLevel level = CourseLevel.BEGINNER;

    @Size(max = 10, message = "Mã ngôn ngữ không được vượt quá 10 ký tự")
    @Builder.Default
    private String language = "vi";

    @DecimalMin(value = "0.00", message = "Học phí phải lớn hơn hoặc bằng 0")
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;
}
