package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.request.ChangeCourseStatusRequest;
import com.hunre.courseservice.dto.request.CreateCourseRequest;
import com.hunre.courseservice.dto.request.UpdateCourseRequest;
import com.hunre.courseservice.dto.response.CourseResponse;
import com.hunre.courseservice.dto.response.CourseSummaryResponse;
import com.hunre.courseservice.entity.CourseLevel;
import com.hunre.courseservice.service.CourseService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ApiResponse<PageResponse<CourseSummaryResponse>> getPublishedCourses(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) CourseLevel level,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(courseService.getPublishedCourses(categoryId, level, keyword, pageable));
    }

    @GetMapping("/instructor/{instructorId}")
    public ApiResponse<PageResponse<CourseSummaryResponse>> getInstructorCourses(
            @PathVariable Long instructorId,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(courseService.getInstructorCourses(instructorId, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseResponse> getCourseById(@PathVariable Long id) {
        return ApiResponse.ok(courseService.getCourseById(id));
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<CourseResponse> getCourseBySlug(@PathVariable String slug) {
        return ApiResponse.ok(courseService.getCourseBySlug(slug));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return ApiResponse.ok(courseService.createCourse(request), "Tạo khóa học thành công");
    }

    @PutMapping("/{id}")
    public ApiResponse<CourseResponse> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCourseRequest request) {
        return ApiResponse.ok(courseService.updateCourse(id, request), "Cập nhật khóa học thành công");
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<CourseResponse> changeCourseStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeCourseStatusRequest request) {
        return ApiResponse.ok(courseService.changeCourseStatus(id, request), "Thay đổi trạng thái khóa học thành công");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ApiResponse.message("Đã xóa khóa học");
    }
}
