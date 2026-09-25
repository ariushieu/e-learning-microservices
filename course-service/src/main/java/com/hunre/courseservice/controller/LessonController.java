package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.request.CreateLessonRequest;
import com.hunre.courseservice.dto.request.CreateLessonResourceRequest;
import com.hunre.courseservice.dto.request.UpdateLessonRequest;
import com.hunre.courseservice.dto.response.LessonResourceResponse;
import com.hunre.courseservice.dto.response.LessonResponse;
import com.hunre.courseservice.service.CurriculumService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LessonController {

    private final CurriculumService curriculumService;

    @GetMapping("/api/lessons/{id}")
    public ApiResponse<LessonResponse> getLessonById(@PathVariable Long id) {
        return ApiResponse.ok(curriculumService.getLessonById(id));
    }

    @PostMapping("/api/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LessonResponse> createLesson(
            @Valid @RequestBody CreateLessonRequest request,
            AuthenticatedUser user) {
        requireCurriculumManager(user);
        return ApiResponse.ok(curriculumService.createLesson(request), "Tạo bài học thành công");
    }

    @PutMapping("/api/lessons/{id}")
    public ApiResponse<LessonResponse> updateLesson(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLessonRequest request,
            AuthenticatedUser user) {
        requireCurriculumManager(user);
        return ApiResponse.ok(curriculumService.updateLesson(id, request), "Cập nhật bài học thành công");
    }

    @DeleteMapping("/api/lessons/{id}")
    public ApiResponse<Void> deleteLesson(@PathVariable Long id, AuthenticatedUser user) {
        requireCurriculumManager(user);
        curriculumService.deleteLesson(id);
        return ApiResponse.message("Đã xóa bài học");
    }

    @PostMapping("/api/lessons/{lessonId}/resources")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LessonResourceResponse> addResource(
            @PathVariable Long lessonId,
            @Valid @RequestBody CreateLessonResourceRequest request,
            AuthenticatedUser user) {
        requireCurriculumManager(user);
        return ApiResponse.ok(curriculumService.addResource(lessonId, request), "Đính kèm tài liệu thành công");
    }

    @DeleteMapping("/api/lessons/resources/{resourceId}")
    public ApiResponse<Void> deleteResource(@PathVariable Long resourceId, AuthenticatedUser user) {
        requireCurriculumManager(user);
        curriculumService.deleteResource(resourceId);
        return ApiResponse.message("Đã xóa tài liệu đính kèm");
    }

    private void requireCurriculumManager(AuthenticatedUser user) {
        if (!user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Chỉ giảng viên hoặc quản trị viên mới có quyền quản lý bài học");
        }
    }
}
