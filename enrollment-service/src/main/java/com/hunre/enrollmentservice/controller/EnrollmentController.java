package com.hunre.enrollmentservice.controller;

import com.hunre.enrollmentservice.dto.request.EnrollCourseRequest;
import com.hunre.enrollmentservice.dto.response.CertificateResponse;
import com.hunre.enrollmentservice.dto.response.EnrollmentResponse;
import com.hunre.enrollmentservice.service.EnrollmentService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * API Đăng ký khóa học mới.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EnrollmentResponse> enroll(
            @Valid @RequestBody EnrollCourseRequest request,
            HttpServletRequest servletRequest) {

        Long currentUserId = resolveUserId(servletRequest, request.getUserId());
        EnrollmentResponse response = enrollmentService.enroll(currentUserId, request);
        return ApiResponse.ok(response, "Đăng ký khóa học thành công");
    }

    /**
     * API Lấy danh sách khóa học mà người dùng đã đăng ký kèm tiến độ.
     */
    @GetMapping("/my-courses")
    public ApiResponse<PageResponse<EnrollmentResponse>> getMyCourses(
            HttpServletRequest servletRequest,
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 10, sort = "enrolledAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long currentUserId = resolveUserId(servletRequest, userId);
        PageResponse<EnrollmentResponse> response = enrollmentService.getMyCourses(currentUserId, pageable);
        return ApiResponse.ok(response);
    }

    /**
     * API Lấy chi tiết lượt ghi danh theo ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<EnrollmentResponse> getEnrollmentById(
            @PathVariable Long id,
            HttpServletRequest servletRequest) {

        Long currentUserId = resolveUserId(servletRequest, null);
        EnrollmentResponse response = enrollmentService.getEnrollmentById(id, currentUserId);
        return ApiResponse.ok(response);
    }

    /**
     * API Hủy đăng ký khóa học (chuyển trạng thái sang CANCELLED).
     */
    @PatchMapping("/{id}/cancel")
    public ApiResponse<EnrollmentResponse> cancelEnrollment(
            @PathVariable Long id,
            HttpServletRequest servletRequest,
            @RequestParam(required = false) Long userId) {

        Long currentUserId = resolveUserId(servletRequest, userId);
        EnrollmentResponse response = enrollmentService.cancelEnrollment(currentUserId, id);
        return ApiResponse.ok(response, "Hủy đăng ký khóa học thành công");
    }

    /**
     * API Hủy ghi danh / Reset tiến độ khóa học để học viên có thể học lại từ đầu hoặc test lại.
     */
    @DeleteMapping("/course/{courseId}")
    public ApiResponse<Void> unenrollCourse(
            @PathVariable Long courseId,
            HttpServletRequest servletRequest,
            @RequestParam(required = false) Long userId) {

        Long currentUserId = resolveUserId(servletRequest, userId);
        enrollmentService.unenrollCourse(currentUserId, courseId);
        return ApiResponse.message("Đã hủy ghi danh và đặt lại tiến độ khóa học thành công");
    }

    /**
     * API Lấy chứng chỉ hoàn thành khóa học theo ID lượt ghi danh.
     */
    @GetMapping("/{id}/certificate")
    public ApiResponse<CertificateResponse> getCertificate(
            @PathVariable Long id,
            HttpServletRequest servletRequest,
            @RequestParam(required = false) Long userId) {

        Long currentUserId = resolveUserId(servletRequest, userId);
        CertificateResponse response = enrollmentService.getCertificate(currentUserId, id);
        return ApiResponse.ok(response);
    }

    private Long resolveUserId(HttpServletRequest servletRequest, Long fallbackUserId) {
        if (servletRequest != null) {
            Object userObj = servletRequest.getAttribute(JwtAuthenticationFilter.USER_ATTRIBUTE);
            if (userObj instanceof AuthenticatedUser authUser && authUser.userId() != null) {
                return authUser.userId();
            }
        }
        return fallbackUserId;
    }
}
