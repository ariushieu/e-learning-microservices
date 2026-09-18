package com.hunre.courseservice.dto.request;

import com.hunre.courseservice.entity.CourseStatus;
import jakarta.validation.constraints.NotNull;
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
public class ChangeCourseStatusRequest {

    @NotNull(message = "Trạng thái mới không được để trống")
    private CourseStatus status;
}
