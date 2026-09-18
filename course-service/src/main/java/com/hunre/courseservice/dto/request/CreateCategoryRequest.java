package com.hunre.courseservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class CreateCategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 150, message = "Tên danh mục không được vượt quá 150 ký tự")
    private String name;

    @Size(max = 180, message = "Slug không được vượt quá 180 ký tự")
    private String slug;

    private Long parentId;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    @Min(value = 0, message = "Vị trí hiển thị phải lớn hơn hoặc bằng 0")
    @Builder.Default
    private Integer position = 0;
}
