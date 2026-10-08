package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.request.SaveCourseReviewRequest;
import com.hunre.courseservice.dto.response.*;
import com.hunre.courseservice.service.CourseReviewService;
import com.hunre.courseservice.security.CurrentUserProvider;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.dto.*;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}/reviews")
@RequiredArgsConstructor
public class CourseReviewController {
    private final CourseReviewService service;
    private final CurrentUserProvider currentUser;

    @GetMapping
    public ApiResponse<PageResponse<CourseReviewResponse>> list(@PathVariable Long courseId,
                                                               @PageableDefault(size = 5) Pageable pageable) {
        return ApiResponse.ok(service.list(courseId, pageable));
    }

    // GET /me vẫn yêu cầu danh tính dù GET khóa học được khai báo công khai.
    @GetMapping("/me")
    public ApiResponse<MyCourseReviewResponse> mine(@PathVariable Long courseId) {
        var user = currentUser.getCurrentUser().orElseThrow(() ->
                new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập để xem đánh giá của mình"));
        return ApiResponse.ok(service.mine(courseId, user));
    }

    @PutMapping("/me")
    public ApiResponse<CourseReviewResponse> save(@PathVariable Long courseId,
            @Valid @RequestBody SaveCourseReviewRequest request, AuthenticatedUser user) {
        return ApiResponse.ok(service.save(courseId, request, user), "Đã lưu đánh giá của bạn");
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> delete(@PathVariable Long courseId, AuthenticatedUser user) {
        service.delete(courseId, user);
        return ApiResponse.message("Đã xóa đánh giá của bạn");
    }

    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> removeByAdmin(@PathVariable Long courseId, @PathVariable Long reviewId,
                                          AuthenticatedUser user) {
        service.removeByAdmin(courseId, reviewId, user);
        return ApiResponse.message("Đã gỡ đánh giá");
    }
}
