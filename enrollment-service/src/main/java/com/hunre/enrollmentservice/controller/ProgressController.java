package com.hunre.enrollmentservice.controller;

import com.hunre.enrollmentservice.dto.request.UpdateLessonProgressRequest;
import com.hunre.enrollmentservice.dto.response.CourseProgressResponse;
import com.hunre.enrollmentservice.dto.response.LessonProgressResponse;
import com.hunre.enrollmentservice.service.ProgressService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    /**
     * API Cập nhật tiến độ bài học và tự động tính % hoàn thành khóa học.
     */
    @PostMapping("/lesson")
    public ApiResponse<LessonProgressResponse> updateLessonProgress(
            @Valid @RequestBody UpdateLessonProgressRequest request,
            AuthenticatedUser user) {

        LessonProgressResponse response = progressService.updateLessonProgress(user.userId(), request);
        return ApiResponse.ok(response, "Cập nhật tiến độ bài học thành công");
    }

    /**
     * API Lấy tiến độ chi tiết của khóa học (gồm % hoàn thành và danh sách các bài học).
     */
    @GetMapping("/course/{courseId}")
    public ApiResponse<CourseProgressResponse> getCourseProgress(
            @PathVariable Long courseId,
            AuthenticatedUser user) {

        CourseProgressResponse response = progressService.getCourseProgress(user.userId(), courseId);
        return ApiResponse.ok(response);
    }
}
