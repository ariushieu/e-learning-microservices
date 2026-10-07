package com.hunre.courseservice.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record SaveCourseReviewRequest(
        @NotNull @DecimalMin("1") @DecimalMax("5") @Digits(integer = 1, fraction = 0) BigDecimal rating,
        @Size(max = 2000) String comment
) {}
