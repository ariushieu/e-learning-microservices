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
public class UpdateSectionRequest {

    @NotBlank(message = "Tiêu đề chương không được để trống")
    @Size(max = 200, message = "Tiêu đề chương không được vượt quá 200 ký tự")
    private String title;

    @Min(value = 0, message = "Thứ tự hiển thị phải lớn hơn hoặc bằng 0")
    @Builder.Default
    private Integer position = 0;
}
