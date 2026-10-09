package com.hunre.enrollmentservice.controller;

import com.hunre.enrollmentservice.analytics.InstructorAnalyticsService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Thống kê ghi danh và hoàn thành trên các khóa của giảng viên, theo từng ngày và từng khóa. */
@RestController
@Validated
@RequiredArgsConstructor
public class InstructorAnalyticsController {
    private final InstructorAnalyticsService analytics;

    @GetMapping("/api/instructor/analytics")
    public ResponseEntity<ApiResponse<InstructorAnalyticsService.Result>> get(
            AuthenticatedUser user, @RequestParam(defaultValue = "30") @Min(7) @Max(90) int days) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(analytics.get(days, user)));
    }
}
