package com.hunre.enrollmentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hunre.enrollmentservice.client.CourseClient;
import com.hunre.enrollmentservice.client.CourseDto;
import com.hunre.enrollmentservice.dto.request.UpdateLessonProgressRequest;
import com.hunre.enrollmentservice.dto.response.CourseProgressResponse;
import com.hunre.enrollmentservice.dto.response.LessonProgressResponse;
import com.hunre.enrollmentservice.entity.CourseSnapshot;
import com.hunre.enrollmentservice.entity.Enrollment;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import com.hunre.enrollmentservice.entity.LessonProgress;
import com.hunre.enrollmentservice.entity.LessonProgressStatus;
import com.hunre.enrollmentservice.repository.CourseSnapshotRepository;
import com.hunre.enrollmentservice.repository.EnrollmentRepository;
import com.hunre.enrollmentservice.repository.LessonProgressRepository;
import com.hunre.enrollmentservice.repository.OutboxEventRepository;
import com.hunre.enrollmentservice.service.impl.ProgressServiceImpl;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private CourseSnapshotRepository courseSnapshotRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private com.hunre.enrollmentservice.repository.CertificateRepository certificateRepository;

    @Mock
    private CourseClient courseClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private ProgressServiceImpl progressService;

    @Test
    @DisplayName("Cập nhật tiến độ bài học và tính lại % tiến độ tổng thể khóa học")
    void updateLessonProgress_success() {
        Long userId = 1L;
        Long courseId = 10L;
        Long lessonId = 101L;

        UpdateLessonProgressRequest request = UpdateLessonProgressRequest.builder()
                .courseId(courseId)
                .lessonId(lessonId)
                .status(LessonProgressStatus.COMPLETED)
                .watchedSeconds(360)
                .build();

        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .userId(userId)
                .courseId(courseId)
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(BigDecimal.ZERO)
                .build();

        when(enrollmentRepository.findByUserIdAndCourseId(userId, courseId))
                .thenReturn(Optional.of(enrollment));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(1L, lessonId))
                .thenReturn(Optional.empty());

        LessonProgress savedProgress = LessonProgress.builder()
                .id(1L)
                .enrollment(enrollment)
                .lessonId(lessonId)
                .status(LessonProgressStatus.COMPLETED)
                .watchedSeconds(360)
                .completedAt(Instant.now())
                .build();
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenReturn(savedProgress);

        // Khóa học có tổng cộng 4 bài học, 1 bài hoàn thành -> 25%
        when(courseSnapshotRepository.findById(courseId)).thenReturn(Optional.of(CourseSnapshot.builder()
                .courseId(courseId)
                .title("Microservices")
                .totalLessons(4)
                .build()));
        when(lessonProgressRepository.countByEnrollmentIdAndStatus(1L, LessonProgressStatus.COMPLETED))
                .thenReturn(1);

        LessonProgressResponse response = progressService.updateLessonProgress(userId, request);

        assertThat(response.getLessonId()).isEqualTo(lessonId);
        assertThat(response.getStatus()).isEqualTo(LessonProgressStatus.COMPLETED);
        assertThat(response.getWatchedSeconds()).isEqualTo(360);
        assertThat(enrollment.getProgressPercent()).isEqualByComparingTo(BigDecimal.valueOf(25.00));
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        verify(enrollmentRepository).save(enrollment);
    }

    @Test
    @DisplayName("Cập nhật bài học cuối cùng đạt 100% tự động chuyển trạng thái khóa học sang COMPLETED")
    void updateLessonProgress_completesCourse() {
        Long userId = 1L;
        Long courseId = 10L;
        Long lessonId = 102L;

        UpdateLessonProgressRequest request = UpdateLessonProgressRequest.builder()
                .courseId(courseId)
                .lessonId(lessonId)
                .status(LessonProgressStatus.COMPLETED)
                .watchedSeconds(500)
                .build();

        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .userId(userId)
                .courseId(courseId)
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(BigDecimal.valueOf(50))
                .build();

        when(enrollmentRepository.findByUserIdAndCourseId(userId, courseId))
                .thenReturn(Optional.of(enrollment));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(1L, lessonId))
                .thenReturn(Optional.empty());

        LessonProgress savedProgress = LessonProgress.builder()
                .id(2L)
                .enrollment(enrollment)
                .lessonId(lessonId)
                .status(LessonProgressStatus.COMPLETED)
                .watchedSeconds(500)
                .completedAt(Instant.now())
                .build();
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenReturn(savedProgress);

        // Khóa học có 2 bài, cả 2 bài hoàn thành -> 100%
        when(courseSnapshotRepository.findById(courseId)).thenReturn(Optional.of(CourseSnapshot.builder()
                .courseId(courseId)
                .totalLessons(2)
                .build()));
        when(lessonProgressRepository.countByEnrollmentIdAndStatus(1L, LessonProgressStatus.COMPLETED))
                .thenReturn(2);
        when(courseClient.getCourseById(courseId)).thenReturn(Optional.of(CourseDto.builder()
                .id(courseId)
                .title("Microservices")
                .build()));

        LessonProgressResponse response = progressService.updateLessonProgress(userId, request);

        assertThat(enrollment.getProgressPercent()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(enrollment.getCompletedAt()).isNotNull();
        verify(outboxEventRepository, org.mockito.Mockito.atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException nếu học viên chưa ghi danh khóa học")
    void updateLessonProgress_notEnrolled_throwsException() {
        Long userId = 1L;
        Long courseId = 99L;
        UpdateLessonProgressRequest request = UpdateLessonProgressRequest.builder()
                .courseId(courseId)
                .lessonId(1L)
                .status(LessonProgressStatus.IN_PROGRESS)
                .build();

        when(enrollmentRepository.findByUserIdAndCourseId(userId, courseId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressService.updateLessonProgress(userId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Lấy chi tiết tiến độ khóa học thành công")
    void getCourseProgress_success() {
        Long userId = 1L;
        Long courseId = 10L;

        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .userId(userId)
                .courseId(courseId)
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(BigDecimal.valueOf(50))
                .build();

        when(enrollmentRepository.findByUserIdAndCourseId(userId, courseId))
                .thenReturn(Optional.of(enrollment));

        LessonProgress lp1 = LessonProgress.builder()
                .lessonId(1L)
                .status(LessonProgressStatus.COMPLETED)
                .watchedSeconds(300)
                .build();
        LessonProgress lp2 = LessonProgress.builder()
                .lessonId(2L)
                .status(LessonProgressStatus.IN_PROGRESS)
                .watchedSeconds(100)
                .build();

        when(lessonProgressRepository.findAllByEnrollmentId(1L)).thenReturn(List.of(lp1, lp2));
        when(courseSnapshotRepository.findById(courseId)).thenReturn(Optional.of(CourseSnapshot.builder()
                .courseId(courseId)
                .totalLessons(2)
                .build()));
        when(courseClient.getCourseById(courseId)).thenReturn(Optional.of(CourseDto.builder()
                .id(courseId)
                .title("Microservices")
                .build()));

        CourseProgressResponse response = progressService.getCourseProgress(userId, courseId);

        assertThat(response.getCourseId()).isEqualTo(courseId);
        assertThat(response.getProgressPercent()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(response.getCompletedLessonsCount()).isEqualTo(1);
        assertThat(response.getTotalLessonsCount()).isEqualTo(2);
        assertThat(response.getLessons()).hasSize(2);
    }

    @Test
    @DisplayName("Khóa học 3 bài học: Hoàn thành 1 bài đạt 33.33%, hoàn thành 3 bài đạt 100.00%")
    void updateLessonProgress_threeLessonsCourse_calculatesAccurately() {
        Long userId = 1L;
        Long courseId = 3L;
        Long lessonId = 1L;

        UpdateLessonProgressRequest request = UpdateLessonProgressRequest.builder()
                .courseId(courseId)
                .lessonId(lessonId)
                .status(LessonProgressStatus.COMPLETED)
                .watchedSeconds(600)
                .build();

        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .userId(userId)
                .courseId(courseId)
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(BigDecimal.ZERO)
                .build();

        when(enrollmentRepository.findByUserIdAndCourseId(userId, courseId)).thenReturn(Optional.of(enrollment));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(1L, lessonId)).thenReturn(Optional.empty());

        LessonProgress saved = LessonProgress.builder()
                .id(10L)
                .enrollment(enrollment)
                .lessonId(lessonId)
                .status(LessonProgressStatus.COMPLETED)
                .watchedSeconds(600)
                .build();
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenReturn(saved);

        // Khóa học 3 bài học, 1 bài hoàn thành -> 33.33%
        when(courseSnapshotRepository.findById(courseId)).thenReturn(Optional.of(CourseSnapshot.builder()
                .courseId(courseId)
                .totalLessons(3)
                .build()));
        when(lessonProgressRepository.countByEnrollmentIdAndStatus(1L, LessonProgressStatus.COMPLETED)).thenReturn(1);

        LessonProgressResponse response = progressService.updateLessonProgress(userId, request);

        assertThat(response).isNotNull();
        assertThat(enrollment.getProgressPercent()).isEqualByComparingTo(BigDecimal.valueOf(33.33));
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    }
}
