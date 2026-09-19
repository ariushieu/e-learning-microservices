package com.hunre.enrollmentservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hunre.enrollmentservice.client.AuthClient;
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
    private final AuthClient authClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public EnrollmentResponse enroll(Long currentUserId, EnrollCourseRequest request) {
        Long effectiveUserId = resolveUserId(currentUserId, request.getUserId());
        Long courseId = request.getCourseId();

        // 1. Kiểm tra học viên tồn tại bên auth-service
        authClient.getUserById(effectiveUserId)
                .orElseThrow(() -> new ResourceNotFoundException("người dùng", "id", effectiveUserId));

        // 2. Kiểm tra khóa học tồn tại bên course-service
        CourseDto course = courseClient.getCourseById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("khóa học", "id", courseId));

        // 3. Kiểm tra trạng thái xuất bản của khóa học
        if (!"PUBLISHED".equalsIgnoreCase(course.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Khóa học chưa được xuất bản nên không thể ghi danh");
        }

        // 4. Kiểm tra học viên đã đăng ký khóa học này chưa (hoặc đã bị hủy)
        Optional<Enrollment> existingOpt = enrollmentRepository.findByUserIdAndCourseId(effectiveUserId, courseId);
        if (existingOpt.isPresent()) {
            Enrollment existing = existingOpt.get();
            if (existing.getStatus() == EnrollmentStatus.CANCELLED) {
                // Tái kích hoạt lại lượt ghi danh đã từng hủy
                existing.setStatus(EnrollmentStatus.ACTIVE);
                existing.setLastAccessedAt(Instant.now());
                Enrollment reactivated = enrollmentRepository.save(existing);
                saveEnrollmentCreatedOutboxEvent(reactivated, course.getTitle());
                log.info("Học viên id={} kích hoạt lại lượt ghi danh khóa học id={}", effectiveUserId, courseId);
                return EnrollmentResponse.from(reactivated, course.getTitle());
            }
            throw new DuplicateResourceException("học viên đã đăng ký khóa học này");
        }
        if (enrollmentRepository.existsByUserIdAndCourseId(effectiveUserId, courseId)) {
            throw new DuplicateResourceException("học viên đã đăng ký khóa học này");
        }

        // 5. Lưu snapshot khóa học hoặc đồng bộ lại nếu dữ liệu có thay đổi
        courseSnapshotRepository.findById(courseId).ifPresentOrElse(snapshot -> {
            boolean changed = false;
            if (course.getTitle() != null && !course.getTitle().equals(snapshot.getTitle())) {
                snapshot.setTitle(course.getTitle());
                changed = true;
            }
            if (course.getTotalLessons() != null && course.getTotalLessons() > 0
                    && !course.getTotalLessons().equals(snapshot.getTotalLessons())) {
                snapshot.setTotalLessons(course.getTotalLessons());
                changed = true;
            }
            if (changed) {
                snapshot.setSyncedAt(Instant.now());
                courseSnapshotRepository.save(snapshot);
            }
        }, () -> {
            CourseSnapshot snapshot = CourseSnapshot.builder()
                    .courseId(courseId)
                    .title(course.getTitle())
                    .totalLessons(course.getTotalLessons() != null && course.getTotalLessons() > 0 ? course.getTotalLessons() : 3)
                    .syncedAt(Instant.now())
                    .build();
            courseSnapshotRepository.save(snapshot);
        });

        // 6. Tạo bản ghi ghi danh
        Instant now = Instant.now();
        Enrollment enrollment = Enrollment.builder()
                .userId(effectiveUserId)
                .courseId(courseId)
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(BigDecimal.ZERO)
                .enrolledAt(now)
                .lastAccessedAt(now)
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        // 7. Ghi sự kiện vào bảng outbox_events (Transactional Outbox Pattern)
        saveEnrollmentCreatedOutboxEvent(savedEnrollment, course.getTitle());

        log.info("Học viên id={} ghi danh thành công khóa học id={}", effectiveUserId, courseId);
        return EnrollmentResponse.from(savedEnrollment, course.getTitle());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EnrollmentResponse> getMyCourses(Long currentUserId, Pageable pageable) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để xem danh sách khóa học của bạn");
        }

        Page<Enrollment> page = enrollmentRepository.findAllByUserId(currentUserId, pageable);

        List<EnrollmentResponse> responses = page.getContent().stream()
                .map(enrollment -> {
                    String title = courseClient.getCourseById(enrollment.getCourseId())
                            .map(CourseDto::getTitle)
                            .orElse("Khóa học #" + enrollment.getCourseId());
                    return EnrollmentResponse.from(enrollment, title);
                })
                .toList();

        return PageResponse.of(responses, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(Long id, Long currentUserId) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lượt ghi danh", "id", id));

        if (currentUserId != null && !enrollment.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập lượt ghi danh này");
        }

        String courseTitle = courseClient.getCourseById(enrollment.getCourseId())
                .map(CourseDto::getTitle)
                .orElse("Khóa học #" + enrollment.getCourseId());

        return EnrollmentResponse.from(enrollment, courseTitle);
    }

    @Override
    @Transactional
    public EnrollmentResponse cancelEnrollment(Long currentUserId, Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("lượt ghi danh", "id", enrollmentId));

        if (currentUserId != null && !enrollment.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền thao tác trên lượt ghi danh này");
        }

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        enrollment.setLastAccessedAt(Instant.now());
        Enrollment saved = enrollmentRepository.save(enrollment);

        String courseTitle = courseClient.getCourseById(enrollment.getCourseId())
                .map(CourseDto::getTitle)
                .orElse("Khóa học #" + enrollment.getCourseId());

        log.info("Học viên id={} đã hủy lượt ghi danh id={} khóa học id={}", enrollment.getUserId(), enrollmentId, enrollment.getCourseId());
        return EnrollmentResponse.from(saved, courseTitle);
    }

    @Override
    @Transactional
    public void unenrollCourse(Long currentUserId, Long courseId) {
        if (currentUserId == null || courseId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Thiếu userId hoặc courseId để hủy ghi danh");
        }

        enrollmentRepository.findByUserIdAndCourseId(currentUserId, courseId).ifPresent(enrollment -> {
            certificateRepository.findByEnrollmentId(enrollment.getId()).ifPresent(certificateRepository::delete);
            lessonProgressRepository.findAllByEnrollmentId(enrollment.getId()).forEach(lessonProgressRepository::delete);
            enrollmentRepository.delete(enrollment);
            log.info("Đã hủy và xóa sạch lượt ghi danh id={} của học viên id={} cho khóa học id={}", enrollment.getId(), currentUserId, courseId);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateResponse getCertificate(Long currentUserId, Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("lượt ghi danh", "id", enrollmentId));

        if (currentUserId != null && !enrollment.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập chứng chỉ này");
        }

        Certificate certificate = certificateRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("chứng chỉ", "enrollmentId", enrollmentId));

        String courseTitle = courseClient.getCourseById(enrollment.getCourseId())
                .map(CourseDto::getTitle)
                .orElse("Khóa học #" + enrollment.getCourseId());

        return CertificateResponse.from(certificate, enrollment.getUserId(), enrollment.getCourseId(), courseTitle);
    }

    private Long resolveUserId(Long currentUserId, Long requestUserId) {
        if (currentUserId != null) {
            return currentUserId;
        }
        if (requestUserId != null) {
            return requestUserId;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Thiếu thông tin ID người dùng (userId)");
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
                    .aggregateType("enrollment")
                    .eventType(event.eventType())
                    .payload(objectMapper.writeValueAsString(event))
                    .build();

            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Lỗi tuần tự hóa EnrollmentCreatedEvent sang JSON", e);
        }
    }
}
