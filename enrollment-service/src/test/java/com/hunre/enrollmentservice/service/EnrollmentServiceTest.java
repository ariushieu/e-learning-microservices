package com.hunre.enrollmentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hunre.enrollmentservice.client.AuthClient;
import com.hunre.enrollmentservice.client.CourseClient;
import com.hunre.enrollmentservice.client.CourseDto;
import com.hunre.enrollmentservice.client.UserDto;
import com.hunre.enrollmentservice.dto.request.EnrollCourseRequest;
import com.hunre.enrollmentservice.dto.response.EnrollmentResponse;
import com.hunre.enrollmentservice.entity.Enrollment;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import com.hunre.enrollmentservice.repository.CourseSnapshotRepository;
import com.hunre.enrollmentservice.repository.EnrollmentRepository;
import com.hunre.enrollmentservice.repository.OutboxEventRepository;
import com.hunre.enrollmentservice.service.impl.EnrollmentServiceImpl;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.DuplicateResourceException;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseSnapshotRepository courseSnapshotRepository;

    @Mock
    private com.hunre.enrollmentservice.repository.LessonProgressRepository lessonProgressRepository;

    @Mock
    private com.hunre.enrollmentservice.repository.CertificateRepository certificateRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private CourseClient courseClient;

    @Mock
    private AuthClient authClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    @Test
    @DisplayName("Ghi danh thành công với thông tin hợp lệ")
    void enroll_success() {
        Long userId = 1L;
        Long courseId = 100L;
        EnrollCourseRequest request = EnrollCourseRequest.builder()
                .courseId(courseId)
                .build();

        when(authClient.getUserById(userId)).thenReturn(Optional.of(UserDto.builder().id(userId).build()));
        when(courseClient.getCourseById(courseId)).thenReturn(Optional.of(CourseDto.builder()
                .id(courseId)
                .title("Lập trình Spring Boot")
                .status("PUBLISHED")
                .totalLessons(10)
                .build()));
        when(enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)).thenReturn(false);

        Enrollment saved = Enrollment.builder()
                .id(50L)
                .userId(userId)
                .courseId(courseId)
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(BigDecimal.ZERO)
                .enrolledAt(Instant.now())
                .build();
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(saved);

        EnrollmentResponse response = enrollmentService.enroll(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getCourseId()).isEqualTo(courseId);
        assertThat(response.getCourseTitle()).isEqualTo("Lập trình Spring Boot");
        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Ghi danh thất bại khi học viên đã đăng ký trước đó (trùng lặp)")
    void enroll_duplicate_throwsException() {
        Long userId = 1L;
        Long courseId = 100L;
        EnrollCourseRequest request = EnrollCourseRequest.builder().courseId(courseId).build();

        when(authClient.getUserById(userId)).thenReturn(Optional.of(UserDto.builder().id(userId).build()));
        when(courseClient.getCourseById(courseId)).thenReturn(Optional.of(CourseDto.builder()
                .id(courseId)
                .title("Lập trình Spring Boot")
                .status("PUBLISHED")
                .build()));
        when(enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.enroll(userId, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Ghi danh thất bại khi khóa học chưa PUBLISHED")
    void enroll_notPublished_throwsException() {
        Long userId = 1L;
        Long courseId = 100L;
        EnrollCourseRequest request = EnrollCourseRequest.builder().courseId(courseId).build();

        when(authClient.getUserById(userId)).thenReturn(Optional.of(UserDto.builder().id(userId).build()));
        when(courseClient.getCourseById(courseId)).thenReturn(Optional.of(CourseDto.builder()
                .id(courseId)
                .title("Khóa học nháp")
                .status("DRAFT")
                .build()));

        assertThatThrownBy(() -> enrollmentService.enroll(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa được xuất bản");
    }

    @Test
    @DisplayName("Ghi danh thất bại khi khóa học không tồn tại")
    void enroll_courseNotFound_throwsException() {
        Long userId = 1L;
        Long courseId = 999L;
        EnrollCourseRequest request = EnrollCourseRequest.builder().courseId(courseId).build();

        when(authClient.getUserById(userId)).thenReturn(Optional.of(UserDto.builder().id(userId).build()));
        when(courseClient.getCourseById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.enroll(userId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Lấy danh sách khóa học của học viên thành công")
    void getMyCourses_success() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .userId(userId)
                .courseId(10L)
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(BigDecimal.valueOf(50))
                .enrolledAt(Instant.now())
                .build();

        when(enrollmentRepository.findAllByUserId(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(enrollment), pageable, 1));
        when(courseClient.getCourseById(10L)).thenReturn(Optional.of(CourseDto.builder()
                .id(10L)
                .title("Java Core")
                .build()));

        PageResponse<EnrollmentResponse> page = enrollmentService.getMyCourses(userId, pageable);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).getCourseTitle()).isEqualTo("Java Core");
        assertThat(page.content().get(0).getProgressPercent()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    @DisplayName("Hủy đăng ký khóa học thành công")
    void cancelEnrollment_success() {
        Long userId = 1L;
        Long enrollmentId = 10L;
        Enrollment enrollment = Enrollment.builder()
                .id(enrollmentId)
                .userId(userId)
                .courseId(3L)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));
        when(courseClient.getCourseById(3L)).thenReturn(Optional.of(CourseDto.builder().id(3L).title("Docker").build()));

        EnrollmentResponse response = enrollmentService.cancelEnrollment(userId, enrollmentId);

        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("Hủy ghi danh sạch sẽ (unenroll) thành công")
    void unenrollCourse_success() {
        Long userId = 1L;
        Long courseId = 3L;
        Enrollment enrollment = Enrollment.builder().id(100L).userId(userId).courseId(courseId).build();

        when(enrollmentRepository.findByUserIdAndCourseId(userId, courseId)).thenReturn(Optional.of(enrollment));

        enrollmentService.unenrollCourse(userId, courseId);

        verify(enrollmentRepository).delete(enrollment);
    }

    @Test
    @DisplayName("Lấy chứng chỉ hoàn thành khóa học thành công")
    void getCertificate_success() {
        Long userId = 1L;
        Long enrollmentId = 20L;
        Enrollment enrollment = Enrollment.builder().id(enrollmentId).userId(userId).courseId(3L).build();
        com.hunre.enrollmentservice.entity.Certificate cert = com.hunre.enrollmentservice.entity.Certificate.builder()
                .id(1L)
                .enrollmentId(enrollmentId)
                .certificateCode("CERT-2026-C3-U1-TEST1234")
                .fileUrl("/cert.pdf")
                .issuedAt(Instant.now())
                .build();

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(certificateRepository.findByEnrollmentId(enrollmentId)).thenReturn(Optional.of(cert));
        when(courseClient.getCourseById(3L)).thenReturn(Optional.of(CourseDto.builder().id(3L).title("Docker").build()));

        com.hunre.enrollmentservice.dto.response.CertificateResponse response = enrollmentService.getCertificate(userId, enrollmentId);

        assertThat(response.certificateCode()).isEqualTo("CERT-2026-C3-U1-TEST1234");
        assertThat(response.courseTitle()).isEqualTo("Docker");
    }
}
