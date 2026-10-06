package com.hunre.enrollmentservice.dto.request;

import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeEnrollmentStatusRequest(
        @NotNull(message = "Trạng thái ghi danh không được để trống") EnrollmentStatus status) {
}
