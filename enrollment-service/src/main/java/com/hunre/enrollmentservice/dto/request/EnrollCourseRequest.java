package com.hunre.enrollmentservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu ghi danh khóa học.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollCourseRequest {

    @NotNull(message = "ID khóa học không được để trống")
    @Positive(message = "ID khóa học phải là số dương")
    private Long courseId;

    /**
     * Tùy chọn truyền userId khi test thủ công không qua JWT.
     * Nếu có JWT token, hệ thống sẽ ưu tiên dùng userId từ token.
     */
    private Long userId;
}
