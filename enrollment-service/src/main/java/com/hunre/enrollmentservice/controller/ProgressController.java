package com.hunre.enrollmentservice.controller;

import com.hunre.enrollmentservice.dto.request.LessonProgressRequest;
import com.hunre.enrollmentservice.dto.response.CourseProgressResponse;
import com.hunre.enrollmentservice.dto.response.LessonProgressResponse;
import com.hunre.enrollmentservice.service.ProgressService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    /**
     * API Cập nhật tiến độ bài học và tự động tính % hoàn thành khóa học.
     */
    @PutMapping("/api/lessons/{lessonId}/progress")
    public ApiResponse<LessonProgressResponse> updateLessonProgress(
            @PathVariable @Positive Long lessonId,
            @Valid @RequestBody LessonProgressRequest request,
            AuthenticatedUser user) {

        // Quyền học dựa trên lượt ghi danh của chính người gọi, không phụ thuộc vai trò.
        LessonProgressResponse response = progressService.updateLessonProgress(user.userId(), request.forLesson(lessonId));
        return ApiResponse.ok(response, "Cập nhật tiến độ bài học thành công");
    }

    /**
     * API Lấy tiến độ chi tiết của khóa học (gồm % hoàn thành và danh sách các bài học).
     */
    @GetMapping("/api/progress")
    public ApiResponse<CourseProgressResponse> getCourseProgress(
            @RequestParam @Positive Long courseId,
            AuthenticatedUser user) {

        CourseProgressResponse response = progressService.getCourseProgress(user.userId(), courseId);
        return ApiResponse.ok(response);
    }
}
