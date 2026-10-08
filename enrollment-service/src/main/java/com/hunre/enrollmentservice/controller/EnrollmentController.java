package com.hunre.enrollmentservice.controller;

import com.hunre.enrollmentservice.dto.request.ChangeEnrollmentStatusRequest;
import com.hunre.enrollmentservice.dto.request.EnrollCourseRequest;
import com.hunre.enrollmentservice.dto.response.CertificateResponse;
import com.hunre.enrollmentservice.dto.response.EnrollmentResponse;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import com.hunre.enrollmentservice.service.EnrollmentService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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

    // Mọi tài khoản đã đăng nhập được học; service kiểm quyền sở hữu bằng userId từ JWT.
    private final EnrollmentService enrollmentService;

    /**
     * API Đăng ký khóa học mới.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EnrollmentResponse> enroll(
            @Valid @RequestBody EnrollCourseRequest request,
            AuthenticatedUser user) {

        EnrollmentResponse response = enrollmentService.enroll(user.userId(), user.fullName(), request);
        return ApiResponse.ok(response, "Đăng ký khóa học thành công");
    }

    /**
     * API Lấy danh sách khóa học mà người dùng đã đăng ký kèm tiến độ.
     */
    @GetMapping
    public ApiResponse<PageResponse<EnrollmentResponse>> getMyCourses(
            AuthenticatedUser user,
            @PageableDefault(size = 10, sort = "enrolledAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<EnrollmentResponse> response = enrollmentService.getMyCourses(user.userId(), pageable);
        return ApiResponse.ok(response);
    }

    /**
     * API Lấy chi tiết lượt ghi danh theo ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<EnrollmentResponse> getEnrollmentById(
            @PathVariable Long id,
            AuthenticatedUser user) {

        EnrollmentResponse response = enrollmentService.getEnrollmentById(id, user.userId());
        return ApiResponse.ok(response);
    }

    /**
     * API Hủy đăng ký khóa học (chuyển trạng thái sang CANCELLED).
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<EnrollmentResponse> changeStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ChangeEnrollmentStatusRequest request,
            AuthenticatedUser user) {

        // ACTIVE và COMPLETED do nghiệp vụ ghi danh/tiến độ quyết định, không cho tự cấp chứng chỉ.
        if (request.status() != EnrollmentStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Chỉ được yêu cầu chuyển trạng thái ghi danh sang CANCELLED");
        }
        EnrollmentResponse response = enrollmentService.cancelEnrollment(user.userId(), id);
        return ApiResponse.ok(response, "Hủy đăng ký khóa học thành công");
    }

    /**
     * API Hủy ghi danh / Reset tiến độ khóa học để học viên có thể học lại từ đầu hoặc test lại.
     */
    @DeleteMapping
    public ApiResponse<Void> unenrollCourse(
            @RequestParam @Positive Long courseId,
            AuthenticatedUser user) {

        enrollmentService.unenrollCourse(user.userId(), courseId);
        return ApiResponse.message("Đã hủy ghi danh và đặt lại tiến độ khóa học thành công");
    }

    /**
     * API Lấy chứng chỉ hoàn thành khóa học theo ID lượt ghi danh.
     */
    @GetMapping("/{id}/certificate")
    public ApiResponse<CertificateResponse> getCertificate(
            @PathVariable Long id,
            AuthenticatedUser user) {

        CertificateResponse response = enrollmentService.getCertificate(user.userId(), id);
        return ApiResponse.ok(response);
    }
}
