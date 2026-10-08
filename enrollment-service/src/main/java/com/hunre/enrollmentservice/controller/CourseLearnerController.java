package com.hunre.enrollmentservice.controller;

import com.hunre.enrollmentservice.dto.response.CourseLearnerResponse;
import com.hunre.enrollmentservice.dto.response.CourseLearnerSummaryResponse;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import com.hunre.enrollmentservice.service.CourseLearnerService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CourseLearnerController {
    private final CourseLearnerService learners;

    @GetMapping("/api/courses/{courseId}/learners/summary")
    public ResponseEntity<ApiResponse<CourseLearnerSummaryResponse>> summary(
            @PathVariable @Positive Long courseId, AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(learners.summary(courseId, user)));
    }

    @GetMapping("/api/courses/{courseId}/learners")
    public ResponseEntity<ApiResponse<PageResponse<CourseLearnerResponse>>> list(
            @PathVariable @Positive Long courseId,
            AuthenticatedUser user,
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @SortDefault(sort = "enrolledAt", direction = Sort.Direction.DESC) Sort sort) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(learners.list(courseId, status, PageRequest.of(page, size, sort), user)));
    }
}
