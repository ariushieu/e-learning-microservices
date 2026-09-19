package com.hunre.enrollmentservice.service;

import com.hunre.enrollmentservice.dto.request.EnrollCourseRequest;
import com.hunre.enrollmentservice.dto.response.EnrollmentResponse;
import com.hunre.sharedcommon.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface EnrollmentService {

    /**
     * Đăng ký một khóa học mới cho học viên.
     *
     * @param currentUserId ID của học viên đang đăng nhập (từ JWT hoặc null)
     * @param request       Dữ liệu yêu cầu ghi danh
     * @return Thông tin lượt ghi danh thành công
     */
    EnrollmentResponse enroll(Long currentUserId, EnrollCourseRequest request);

    /**
     * Lấy danh sách các khóa học mà học viên đã ghi danh kèm tiến độ.
     *
     * @param currentUserId ID của học viên
     * @param pageable      Thông tin phân trang
     * @return Trang kết quả danh sách khóa học
     */
    PageResponse<EnrollmentResponse> getMyCourses(Long currentUserId, Pageable pageable);

    /**
     * Lấy chi tiết một lượt ghi danh theo ID.
     *
     * @param id            ID lượt ghi danh
     * @param currentUserId ID học viên hiện tại (để kiểm tra quyền)
     * @return Chi tiết lượt ghi danh
     */
    EnrollmentResponse getEnrollmentById(Long id, Long currentUserId);

    /**
     * Hủy đăng ký một khóa học (chuyển trạng thái sang CANCELLED).
     *
     * @param currentUserId ID học viên
     * @param enrollmentId  ID lượt ghi danh
     * @return Chi tiết lượt ghi danh sau khi hủy
     */
    EnrollmentResponse cancelEnrollment(Long currentUserId, Long enrollmentId);

    /**
     * Hủy ghi danh / Reset tiến độ khóa học cho học viên (hỗ trợ kiểm thử và bắt đầu học lại từ đầu).
     *
     * @param currentUserId ID học viên
     * @param courseId      ID khóa học
     */
    void unenrollCourse(Long currentUserId, Long courseId);

    /**
     * Lấy chứng chỉ hoàn thành khóa học theo ID lượt ghi danh.
     *
     * @param currentUserId ID học viên
     * @param enrollmentId  ID lượt ghi danh
     * @return Thông tin chứng chỉ
     */
    com.hunre.enrollmentservice.dto.response.CertificateResponse getCertificate(Long currentUserId, Long enrollmentId);
}
