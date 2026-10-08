package com.hunre.enrollmentservice.service;

import com.hunre.enrollmentservice.dto.response.CourseLearnerResponse;
import com.hunre.enrollmentservice.dto.response.CourseLearnerSummaryResponse;
import com.hunre.enrollmentservice.entity.Certificate;
import com.hunre.enrollmentservice.entity.Enrollment;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import com.hunre.enrollmentservice.entity.LessonProgressStatus;
import com.hunre.enrollmentservice.repository.CertificateRepository;
import com.hunre.enrollmentservice.repository.CourseSnapshotRepository;
import com.hunre.enrollmentservice.repository.EnrollmentRepository;
import com.hunre.enrollmentservice.repository.LessonProgressRepository;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseLearnerService {
    private static final Set<String> SORT_FIELDS = Set.of("enrolledAt", "progressPercent");
    private final CourseSnapshotRepository snapshots;
    private final EnrollmentRepository enrollments;
    private final CertificateRepository certificates;
    private final LessonProgressRepository progress;

    @Transactional(readOnly = true)
    public PageResponse<CourseLearnerResponse> list(Long courseId, EnrollmentStatus status,
                                                   Pageable pageable, AuthenticatedUser user) {
        requireInstructorAccess(courseId, user);
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

    @Transactional(readOnly = true)
    public CourseLearnerSummaryResponse summary(Long courseId, AuthenticatedUser user) {
        requireInstructorAccess(courseId, user);
        long active = 0, completed = 0, cancelled = 0;
        BigDecimal progressTotal = BigDecimal.ZERO;
        // At most three aggregate rows; no learner entities are materialized.
        for (var row : enrollments.summarizeByCourseId(courseId)) {
            switch (row.getStatus()) {
                case ACTIVE -> active = row.getEnrollmentCount();
                case COMPLETED -> completed = row.getEnrollmentCount();
                case CANCELLED -> cancelled = row.getEnrollmentCount();
            }
            if (row.getStatus() != EnrollmentStatus.CANCELLED) {
                progressTotal = progressTotal.add(row.getProgressTotal());
            }
        }
        long learners = active + completed;
        var lessonCounts = progress.summarizeByCourseId(courseId,
                        LessonProgressStatus.COMPLETED, EnrollmentStatus.CANCELLED).stream()
                .map(row -> new CourseLearnerSummaryResponse.LessonCompletion(row.getLessonId(),
                        row.getCompletedCount(), percentage(row.getCompletedCount(), learners))).toList();
        return new CourseLearnerSummaryResponse(active, completed, cancelled,
                divide(progressTotal, learners), percentage(completed, learners),
                certificates.countByCourseId(courseId), lessonCounts);
    }

    private static BigDecimal percentage(long numerator, long denominator) {
        return divide(BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100)), denominator);
    }

    private static BigDecimal divide(BigDecimal numerator, long denominator) {
        return denominator == 0 ? BigDecimal.ZERO.setScale(2)
                : numerator.divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private void requireInstructorAccess(Long courseId, AuthenticatedUser user) {
        if (user == null || user.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để xem học viên");
        }
        var course = snapshots.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", courseId));
        if (!user.hasRole(Roles.ADMIN)
                && !(user.hasRole(Roles.INSTRUCTOR) && user.userId().equals(course.getInstructorId()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ giảng viên của khóa hoặc quản trị viên được xem học viên");
        }
    }
}
