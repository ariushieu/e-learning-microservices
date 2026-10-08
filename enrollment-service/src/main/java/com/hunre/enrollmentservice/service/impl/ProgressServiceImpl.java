package com.hunre.enrollmentservice.service.impl;

import com.hunre.enrollmentservice.client.CourseClient;
import com.hunre.enrollmentservice.client.CourseLessonClient;
import com.hunre.enrollmentservice.client.CourseDto;
import com.hunre.enrollmentservice.dto.request.UpdateLessonProgressRequest;
import com.hunre.enrollmentservice.dto.response.CourseProgressResponse;
import com.hunre.enrollmentservice.dto.response.LessonProgressResponse;
import com.hunre.enrollmentservice.entity.Certificate;
import com.hunre.enrollmentservice.entity.CourseSnapshot;
import com.hunre.enrollmentservice.entity.Enrollment;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import com.hunre.enrollmentservice.entity.LessonProgress;
import com.hunre.enrollmentservice.entity.LessonProgressStatus;
import com.hunre.enrollmentservice.entity.OutboxEvent;
import com.hunre.enrollmentservice.repository.CertificateRepository;
import com.hunre.enrollmentservice.repository.CourseSnapshotRepository;
import com.hunre.enrollmentservice.repository.EnrollmentRepository;
import com.hunre.enrollmentservice.repository.LessonProgressRepository;
import com.hunre.enrollmentservice.repository.OutboxEventRepository;
import com.hunre.enrollmentservice.service.ProgressService;
import com.hunre.enrollmentservice.service.CertificateDetailsService;
import com.hunre.sharedcommon.event.CertificateIssuedEvent;
import com.hunre.sharedcommon.event.EnrollmentCompletedEvent;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseSnapshotRepository courseSnapshotRepository;
    private final CertificateRepository certificateRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final CourseClient courseClient;
    private final CourseLessonClient courseLessonClient;
    private final CertificateDetailsService certificateDetails;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    @Transactional
    public LessonProgressResponse updateLessonProgress(Long currentUserId, UpdateLessonProgressRequest request) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Người dùng chưa được xác thực");
        }
        Long courseId = request.getCourseId();
        Long lessonId = request.getLessonId();

        // 1. Kiểm tra học viên đã ghi danh khóa học chưa
        Enrollment enrollment = enrollmentRepository.findForProgressUpdate(currentUserId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy lượt ghi danh của học viên %s cho khóa học %s"
                                .formatted(currentUserId, courseId)));

        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Lượt ghi danh khóa học này đã bị hủy, không thể cập nhật tiến độ");
        }

        // Chỉ ghi tiến độ sau khi xác minh bài học tồn tại và thuộc đúng khóa học.
        // Không dùng tổng số bài trong snapshot để suy đoán tính hợp lệ của lessonId.
        courseLessonClient.validateLesson(courseId, lessonId);

        return applyLessonProgress(enrollment, request, false);
    }

    @Override
    @Transactional
    public void completeLessonFromQuiz(Long userId, Long courseId, Long lessonId) {
        // Cùng khóa hàng với PUT/GET tiến độ: sự kiện và thao tác tay không cấp chứng chỉ hai lần.
        Enrollment enrollment = enrollmentRepository.findForProgressUpdate(userId, courseId).orElse(null);
        if (enrollment == null || enrollment.getStatus() != EnrollmentStatus.ACTIVE) return;
        CourseSnapshot snapshot = courseSnapshotRepository.findById(courseId).orElse(null);
        if (snapshot == null || snapshot.getLessonIds() == null || !snapshot.getLessonIds().contains(lessonId)
                || snapshot.getTotalLessons() == null || snapshot.getTotalLessons() <= 0) return;

        // Không gọi API cần JWT trên luồng Kafka. Danh tính và đề cương đều đến từ dữ liệu đã xác thực.
        applyLessonProgress(enrollment, UpdateLessonProgressRequest.builder()
                .courseId(courseId).lessonId(lessonId).status(LessonProgressStatus.COMPLETED).build(), true);
    }

    private LessonProgressResponse applyLessonProgress(Enrollment enrollment,
                                                       UpdateLessonProgressRequest request, boolean fromQuiz) {
        Long courseId = enrollment.getCourseId();
        Long lessonId = request.getLessonId();

        // 2. Tìm hoặc tạo mới bản ghi tiến độ bài học
        LessonProgress lessonProgress = lessonProgressRepository
                .findByEnrollmentIdAndLessonId(enrollment.getId(), lessonId)
                .orElseGet(() -> LessonProgress.builder()
                        .enrollment(enrollment)
                        .lessonId(lessonId)
                        .watchedSeconds(0)
                        .status(LessonProgressStatus.IN_PROGRESS)
                        .build());

        // Cập nhật số giây đã xem
        if (request.getWatchedSeconds() != null && request.getWatchedSeconds() > lessonProgress.getWatchedSeconds()) {
            lessonProgress.setWatchedSeconds(request.getWatchedSeconds());
        }

        // Cập nhật trạng thái
        if (request.getStatus() == LessonProgressStatus.COMPLETED) {
            lessonProgress.setStatus(LessonProgressStatus.COMPLETED);
            if (lessonProgress.getCompletedAt() == null) {
                lessonProgress.setCompletedAt(Instant.now());
            }
        } else if (request.getStatus() != null && lessonProgress.getStatus() != LessonProgressStatus.COMPLETED) {
            lessonProgress.setStatus(request.getStatus());
        }

        LessonProgress savedLessonProgress = lessonProgressRepository.save(lessonProgress);

        // 3. Tính lại % tiến độ tổng thể của khóa học
        int totalLessons = resolveTotalLessons(courseId);
        int completedLessons = lessonProgressRepository.countByEnrollmentIdAndStatus(
                enrollment.getId(), LessonProgressStatus.COMPLETED);

        BigDecimal progressPercent;
        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED || completedLessons >= totalLessons) {
            progressPercent = BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        } else {
            progressPercent = BigDecimal.valueOf(completedLessons)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalLessons), 2, RoundingMode.HALF_UP);
        }

        if (progressPercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            progressPercent = BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
            if (enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
                enrollment.setStatus(EnrollmentStatus.COMPLETED);
                enrollment.setCompletedAt(Instant.now());
                saveEnrollmentCompletedOutboxEvent(enrollment);
                issueCertificateIfAbsent(enrollment, fromQuiz);
            }
        }

        enrollment.setProgressPercent(progressPercent);
        enrollment.setLastAccessedAt(Instant.now());
        enrollmentRepository.save(enrollment);

        log.info("Cập nhật tiến độ: user={}, course={}, lesson={}, courseProgress={}%",
                enrollment.getUserId(), courseId, lessonId, progressPercent);

        return LessonProgressResponse.from(savedLessonProgress);
    }

    @Override
    @Transactional
    public CourseProgressResponse getCourseProgress(Long currentUserId, Long courseId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Người dùng chưa được xác thực");
        }

        // GET cũng có thể đồng bộ tiến độ/cấp chứng chỉ: dùng cùng khóa với PUT.
        Enrollment enrollment = enrollmentRepository.findForProgressUpdate(currentUserId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy lượt ghi danh của học viên %s cho khóa học %s"
                                .formatted(currentUserId, courseId)));

        List<LessonProgress> progresses = lessonProgressRepository.findAllByEnrollmentId(enrollment.getId());
        List<LessonProgressResponse> lessonResponses = progresses.stream()
                .map(LessonProgressResponse::from)
                .toList();

        int totalLessons = resolveTotalLessons(courseId);
        int completedCount = (int) progresses.stream()
                .filter(p -> p.getStatus() == LessonProgressStatus.COMPLETED)
                .count();

        // Chuẩn hóa và đồng bộ lại tiến độ của enrollment nếu có sự chênh lệch
        BigDecimal accuratePercent;
        // Chứng nhận hoàn thành đã đạt không bị thu hồi khi khóa học bổ sung bài mới.
        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED || completedCount >= totalLessons) {
            accuratePercent = BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        } else {
            accuratePercent = BigDecimal.valueOf(completedCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalLessons), 2, RoundingMode.HALF_UP);
        }

        if (enrollment.getProgressPercent() == null || enrollment.getProgressPercent().compareTo(accuratePercent) != 0) {
            enrollment.setProgressPercent(accuratePercent);
            if (accuratePercent.compareTo(BigDecimal.valueOf(100)) >= 0 && enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
                enrollment.setStatus(EnrollmentStatus.COMPLETED);
                enrollment.setCompletedAt(Instant.now());
                saveEnrollmentCompletedOutboxEvent(enrollment);
                issueCertificateIfAbsent(enrollment);
            }
            enrollmentRepository.save(enrollment);
        }

        String certificateCode = certificateRepository.findByEnrollmentId(enrollment.getId())
                .map(Certificate::getCertificateCode)
                .orElse(null);

        if (accuratePercent.compareTo(BigDecimal.valueOf(100)) >= 0 && certificateCode == null) {
            issueCertificateIfAbsent(enrollment);
            certificateCode = certificateRepository.findByEnrollmentId(enrollment.getId())
                    .map(Certificate::getCertificateCode)
                    .orElse(null);
        }

        String courseTitle = courseSnapshotRepository.findById(courseId)
                .map(CourseSnapshot::getTitle)
                .orElseGet(() -> courseClient.getCourseById(courseId)
                        .map(CourseDto::getTitle)
                        .orElse("Khóa học #" + courseId));

        return CourseProgressResponse.builder()
                .courseId(courseId)
                .enrollmentId(enrollment.getId())
                .courseTitle(courseTitle)
                .status(enrollment.getStatus())
                .progressPercent(enrollment.getProgressPercent())
                .completedLessonsCount(completedCount)
                .totalLessonsCount(totalLessons)
                .lastAccessedAt(enrollment.getLastAccessedAt())
                .certificateCode(certificateCode)
                .lessons(lessonResponses)
                .build();
    }

    private void issueCertificateIfAbsent(Enrollment enrollment) {
        issueCertificateIfAbsent(enrollment, false);
    }

    private void issueCertificateIfAbsent(Enrollment enrollment, boolean fromQuiz) {
        if (!certificateRepository.existsByEnrollmentId(enrollment.getId())) {
            // Mã công khai không nhúng ID người dùng/khóa học và không dễ đoán.
            String certCode = "CERT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

            Certificate certificate = Certificate.builder()
                    .enrollmentId(enrollment.getId())
                    .certificateCode(certCode)
                    .fileUrl("/certificates/" + certCode + ".pdf")
                    .issuedAt(Instant.now())
                    .build();

            String issuedCourseTitle = courseSnapshotRepository.findById(enrollment.getCourseId())
                    .map(CourseSnapshot::getTitle)
                    .orElseGet(() -> courseClient.getCourseById(enrollment.getCourseId())
                            .map(CourseDto::getTitle)
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khóa học")));
            if (fromQuiz) {
                // learnerName đã lấy từ JWT khi ghi danh. Ghi danh cũ có thể chưa có tên:
                // vẫn cấp mã, tên được bổ sung khi chính chủ mở chứng chỉ theo luồng hiện có.
                certificate.setLearnerName(enrollment.getLearnerName());
                certificate.setCourseTitle(issuedCourseTitle);
            } else {
                certificateDetails.captureIfMissing(certificate, enrollment.getUserId(), issuedCourseTitle);
            }

            Certificate savedCert = certificateRepository.save(certificate);
            if (savedCert == null) {
                savedCert = certificate;
            }

            saveCertificateIssuedOutboxEvent(savedCert, enrollment, issuedCourseTitle);
            log.info("Đã cấp chứng chỉ {} cho học viên id={} hoàn thành khóa học id={}",
                    certCode, enrollment.getUserId(), enrollment.getCourseId());
        }
    }

    private int resolveTotalLessons(Long courseId) {
        if (courseId == null) {
            throw new ResourceNotFoundException("Thiếu ID khóa học");
        }
        return courseSnapshotRepository.findById(courseId)
                .map(CourseSnapshot::getTotalLessons)
                .filter(t -> t != null && t > 0)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thông tin khóa học hoặc khóa học chưa có bài học: id=" + courseId));
    }

    private void saveEnrollmentCompletedOutboxEvent(Enrollment enrollment) {
        try {
            String courseTitle = courseSnapshotRepository.findById(enrollment.getCourseId())
                    .map(CourseSnapshot::getTitle)
                    .orElseGet(() -> courseClient.getCourseById(enrollment.getCourseId())
                            .map(CourseDto::getTitle)
                            .orElse("Khóa học #" + enrollment.getCourseId()));

            EnrollmentCompletedEvent event = EnrollmentCompletedEvent.of(
                    enrollment.getId(),
                    enrollment.getUserId(),
                    enrollment.getCourseId(),
                    courseTitle,
                    enrollment.getCompletedAt()
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
            log.error("Lỗi tuần tự hóa EnrollmentCompletedEvent sang JSON", e);
            throw new IllegalStateException("Lỗi tuần tự hóa sự kiện outbox: " + e.getMessage(), e);
        }
    }

    private void saveCertificateIssuedOutboxEvent(Certificate certificate, Enrollment enrollment, String courseTitle) {
        try {
            CertificateIssuedEvent event = CertificateIssuedEvent.of(
                    certificate.getId() != null ? certificate.getId() : 1L,
                    enrollment.getId(),
                    enrollment.getUserId(),
                    enrollment.getCourseId(),
                    courseTitle,
                    certificate.getCertificateCode(),
                    certificate.getFileUrl()
            );

            OutboxEvent outbox = OutboxEvent.builder()
                    .eventId(event.eventId())
                    .aggregateType("CERTIFICATE")
                    .aggregateId(String.valueOf(certificate.getId() != null ? certificate.getId() : enrollment.getId()))
                    .eventType(event.eventType())
                    .payload(objectMapper.writeValueAsString(event))
                    .build();

            outboxEventRepository.save(outbox);
        } catch (Exception e) {
            log.error("Lỗi tuần tự hóa CertificateIssuedEvent sang JSON", e);
            throw new IllegalStateException("Lỗi tuần tự hóa sự kiện outbox: " + e.getMessage(), e);
        }
    }
}
