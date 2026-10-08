package com.hunre.enrollmentservice.service;

import com.hunre.enrollmentservice.dto.response.CourseLearnerResponse;
import com.hunre.enrollmentservice.entity.Certificate;
import com.hunre.enrollmentservice.entity.Enrollment;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import com.hunre.enrollmentservice.repository.CertificateRepository;
import com.hunre.enrollmentservice.repository.CourseSnapshotRepository;
import com.hunre.enrollmentservice.repository.EnrollmentRepository;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseLearnerService {
    private static final Set<String> SORT_FIELDS = Set.of("enrolledAt", "progressPercent");
    private final CourseSnapshotRepository snapshots;
    private final EnrollmentRepository enrollments;
    private final CertificateRepository certificates;

    @Transactional(readOnly = true)
    public PageResponse<CourseLearnerResponse> list(Long courseId, EnrollmentStatus status,
                                                   Pageable pageable, AuthenticatedUser user) {
        if (user == null || user.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để xem học viên");
        }
        var course = snapshots.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", courseId));
        if (!user.hasRole(Roles.ADMIN)
                && !(user.hasRole(Roles.INSTRUCTOR) && user.userId().equals(course.getInstructorId()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ giảng viên của khóa hoặc quản trị viên được xem học viên");
        }
        for (var order : pageable.getSort()) {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Chỉ sắp xếp theo enrolledAt hoặc progressPercent");
            }
        }
        if (pageable.getOffset() > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Trang vượt quá giới hạn phân trang");
        }
        // A unique tie-breaker keeps equal timestamps/progress stable across pages.
        var sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().and(Sort.by(Sort.Direction.DESC, "id")));
        var page = status == null ? enrollments.findAllByCourseId(courseId, sorted)
                : enrollments.findAllByCourseIdAndStatus(courseId, status, sorted);
        if (page.isEmpty()) {
            return PageResponse.of(java.util.List.of(), page.getNumber(), page.getSize(), page.getTotalElements());
        }
        // One batched lookup, not a certificate query per learner.
        var codes = certificates.findAllByEnrollmentIdIn(page.getContent().stream().map(Enrollment::getId).toList())
                .stream().collect(Collectors.toMap(Certificate::getEnrollmentId, Certificate::getCertificateCode));
        var rows = page.getContent().stream()
                .map(enrollment -> CourseLearnerResponse.from(enrollment, codes.get(enrollment.getId()))).toList();
        return PageResponse.of(rows, page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
