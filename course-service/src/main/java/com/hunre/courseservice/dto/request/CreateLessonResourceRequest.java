package com.hunre.courseservice.dto.request;

import com.hunre.courseservice.validation.HttpUrl;

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
public class CreateLessonResourceRequest {

    @NotBlank(message = "Tên tài liệu không được để trống")
    @Size(max = 200, message = "Tên tài liệu không được vượt quá 200 ký tự")
    private String name;

    @NotBlank(message = "URL file không được để trống")
    @HttpUrl
    @Size(max = 500, message = "URL file không được vượt quá 500 ký tự")
    private String fileUrl;
}
