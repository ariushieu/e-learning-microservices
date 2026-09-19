package com.hunre.enrollmentservice.service.impl;

import com.hunre.enrollmentservice.client.CourseClient;
import com.hunre.enrollmentservice.client.CourseDto;
import com.hunre.enrollmentservice.dto.request.EnrollCourseRequest;
import com.hunre.enrollmentservice.dto.response.CertificateResponse;
import com.hunre.enrollmentservice.dto.response.EnrollmentResponse;
import com.hunre.enrollmentservice.entity.Certificate;
import com.hunre.enrollmentservice.entity.CourseSnapshot;
import com.hunre.enrollmentservice.entity.Enrollment;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import com.hunre.enrollmentservice.entity.OutboxEvent;
import com.hunre.enrollmentservice.repository.CertificateRepository;
import com.hunre.enrollmentservice.repository.CourseSnapshotRepository;
import com.hunre.enrollmentservice.repository.EnrollmentRepository;
import com.hunre.enrollmentservice.repository.LessonProgressRepository;
import com.hunre.enrollmentservice.repository.OutboxEventRepository;
import com.hunre.enrollmentservice.service.EnrollmentService;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.event.EnrollmentCreatedEvent;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.DuplicateResourceException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseSnapshotRepository courseSnapshotRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CertificateRepository certificateRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final CourseClient courseClient;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    @Transactional
    public EnrollmentResponse enroll(Long currentUserId, EnrollCourseRequest request) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Người dùng chưa được xác thực");
        }
        Long courseId = request.getCourseId();

        // 1. Kiểm tra khóa học tồn tại trong snapshot và đã PUBLISHED
        CourseDto course = courseClient.getCourseById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", courseId));

        if (!"PUBLISHED".equalsIgnoreCase(course.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Khóa học chưa được xuất bản nên không thể ghi danh");
        }

        // 2. Kiểm tra học viên đã đăng ký khóa học này chưa (hoặc đã bị hủy)
        Optional<Enrollment> existingOpt = enrollmentRepository.findByUserIdAndCourseId(currentUserId, courseId);
        if (existingOpt.isPresent()) {
            Enrollment existing = existingOpt.get();
            if (existing.getStatus() == EnrollmentStatus.CANCELLED) {
                // Tái kích hoạt lại lượt ghi danh đã từng hủy
                existing.setStatus(EnrollmentStatus.ACTIVE);
                existing.setLastAccessedAt(Instant.now());
                Enrollment reactivated = enrollmentRepository.save(existing);
                saveEnrollmentCreatedOutboxEvent(reactivated, course.getTitle());
                log.info("Học viên id={} kích hoạt lại lượt ghi danh khóa học id={}", currentUserId, courseId);
                return EnrollmentResponse.from(reactivated, course.getTitle());
            }
            throw new DuplicateResourceException("học viên đã đăng ký khóa học này");
        }
        if (enrollmentRepository.existsByUserIdAndCourseId(currentUserId, courseId)) {
            throw new DuplicateResourceException("học viên đã đăng ký khóa học này");
        }

        // 3. Tạo bản ghi ghi danh
        Instant now = Instant.now();
        Enrollment enrollment = Enrollment.builder()
                .userId(currentUserId)
                .courseId(courseId)
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(BigDecimal.ZERO)
                .enrolledAt(now)
                .lastAccessedAt(now)
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        // 4. Ghi sự kiện ra Transactional Outbox
        saveEnrollmentCreatedOutboxEvent(savedEnrollment, course.getTitle());

        log.info("Học viên id={} ghi danh thành công khóa học id={}", currentUserId, courseId);
        return EnrollmentResponse.from(savedEnrollment, course.getTitle());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EnrollmentResponse> getMyCourses(Long currentUserId, Pageable pageable) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Người dùng chưa được xác thực");
        }
        Page<Enrollment> page = enrollmentRepository.findAllByUserId(currentUserId, pageable);

        List<EnrollmentResponse> responses = page.getContent().stream()
                .map(enrollment -> {
                    String title = courseSnapshotRepository.findById(enrollment.getCourseId())
                            .map(com.hunre.enrollmentservice.entity.CourseSnapshot::getTitle)
                            .orElseGet(() -> courseClient.getCourseById(enrollment.getCourseId())
                                    .map(CourseDto::getTitle)
                                    .orElse("Khóa học #" + enrollment.getCourseId()));
                    return EnrollmentResponse.from(enrollment, title);
                })
                .toList();

        return PageResponse.of(responses, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(Long enrollmentId, Long currentUserId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("lượt ghi danh", "id", enrollmentId));

        if (currentUserId != null && !enrollment.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập lượt ghi danh này");
        }

        String courseTitle = courseSnapshotRepository.findById(enrollment.getCourseId())
                .map(com.hunre.enrollmentservice.entity.CourseSnapshot::getTitle)
                .orElseGet(() -> courseClient.getCourseById(enrollment.getCourseId())
                        .map(CourseDto::getTitle)
                        .orElse("Khóa học #" + enrollment.getCourseId()));

        return EnrollmentResponse.from(enrollment, courseTitle);
    }

    @Override
    @Transactional
    public EnrollmentResponse cancelEnrollment(Long currentUserId, Long enrollmentId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Người dùng chưa được xác thực");
        }
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("lượt ghi danh", "id", enrollmentId));

        if (!enrollment.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền hủy lượt ghi danh của người khác");
        }

        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Khóa học đã hoàn thành, không thể hủy ghi danh");
        }

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        enrollment.setLastAccessedAt(Instant.now());
        Enrollment saved = enrollmentRepository.save(enrollment);

        String courseTitle = courseSnapshotRepository.findById(enrollment.getCourseId())
                .map(com.hunre.enrollmentservice.entity.CourseSnapshot::getTitle)
                .orElseGet(() -> courseClient.getCourseById(enrollment.getCourseId())
                        .map(CourseDto::getTitle)
                        .orElse("Khóa học #" + enrollment.getCourseId()));

        log.info("Học viên id={} đã hủy lượt ghi danh id={} khóa học id={}",
                currentUserId, enrollmentId, enrollment.getCourseId());
        return EnrollmentResponse.from(saved, courseTitle);
    }

    @Override
    @Transactional
    public void unenrollCourse(Long currentUserId, Long courseId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Người dùng chưa được xác thực");
        }
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(currentUserId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("lượt ghi danh của khóa học", "courseId", courseId));

        // Xóa sạch tiến độ bài học của lượt ghi danh này
        lessonProgressRepository.deleteAllByEnrollmentId(enrollment.getId());

        // Xóa chứng chỉ nếu có
        certificateRepository.findByEnrollmentId(enrollment.getId())
                .ifPresent(certificateRepository::delete);

        // Xóa bản ghi ghi danh
        enrollmentRepository.delete(enrollment);

        log.info("Đã hủy và xóa sạch lượt ghi danh id={} của học viên id={} cho khóa học id={}",
                enrollment.getId(), currentUserId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateResponse getCertificate(Long currentUserId, Long enrollmentId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Người dùng chưa được xác thực");
        }
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("lượt ghi danh", "id", enrollmentId));

        if (!enrollment.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền xem chứng chỉ này");
        }

        Certificate certificate = certificateRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khóa học này chưa hoàn thành hoặc chưa được cấp chứng chỉ"));

        String courseTitle = courseSnapshotRepository.findById(enrollment.getCourseId())
                .map(CourseSnapshot::getTitle)
                .orElseGet(() -> courseClient.getCourseById(enrollment.getCourseId())
                        .map(CourseDto::getTitle)
                        .orElse("Khóa học #" + enrollment.getCourseId()));

        return CertificateResponse.from(certificate, enrollment.getUserId(), enrollment.getCourseId(), courseTitle);
    }

    private void saveEnrollmentCreatedOutboxEvent(Enrollment enrollment, String courseTitle) {
        try {
            EnrollmentCreatedEvent event = EnrollmentCreatedEvent.of(
                    enrollment.getId(),
                    enrollment.getUserId(),
                    enrollment.getCourseId(),
                    courseTitle
            );

            OutboxEvent outbox = OutboxEvent.builder()
                    .eventId(event.eventId())
                    .aggregateType("ENROLLMENT")
                    .aggregateId(String.valueOf(enrollment.getId()))
                    .eventType(event.eventType())
                    .payload(objectMapper.writeValueAsString(event))
                    .build();

            outboxEventRepository.save(outbox);
        } catch (Exception e) {
            log.error("Lỗi tuần tự hóa EnrollmentCreatedEvent sang JSON", e);
            throw new IllegalStateException("Lỗi tuần tự hóa sự kiện outbox: " + e.getMessage(), e);
        }
    }
}
