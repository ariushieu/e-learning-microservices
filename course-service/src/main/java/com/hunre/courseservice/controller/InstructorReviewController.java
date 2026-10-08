package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.response.InstructorReviewsResponse;
import com.hunre.courseservice.service.CourseReviewService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/instructor/reviews")
public class InstructorReviewController {
    private final CourseReviewService service;

    @GetMapping
    public ApiResponse<InstructorReviewsResponse> list(@RequestParam(required = false) Boolean replied,
            @RequestParam(required = false) Long courseId, @PageableDefault(size = 10) Pageable pageable,
            AuthenticatedUser user) {
        return ApiResponse.ok(service.inbox(replied, courseId, pageable, user));
    }
}
