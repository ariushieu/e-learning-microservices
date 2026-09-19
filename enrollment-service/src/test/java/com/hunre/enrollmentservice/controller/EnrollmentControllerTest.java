package com.hunre.enrollmentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hunre.enrollmentservice.dto.request.EnrollCourseRequest;
import com.hunre.enrollmentservice.dto.response.EnrollmentResponse;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import com.hunre.enrollmentservice.service.EnrollmentService;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.GlobalExceptionHandler;
import com.hunre.sharedcommon.security.AuthenticatedUserArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EnrollmentControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private EnrollmentController enrollmentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(enrollmentController)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticatedUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/enrollments - Đăng ký thành công trả về HTTP 201 và ApiResponse")
    void enroll_success() throws Exception {
        EnrollCourseRequest request = EnrollCourseRequest.builder()
                .courseId(10L)
                .userId(1L)
                .build();

        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(100L)
                .userId(1L)
                .courseId(10L)
                .courseTitle("Spring Boot Microservices")
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(BigDecimal.ZERO)
                .enrolledAt(Instant.now())
                .build();

        when(enrollmentService.enroll(any(), any(EnrollCourseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng ký khóa học thành công"))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.courseId").value(10L))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/enrollments - Dữ liệu không hợp lệ trả về HTTP 400")
    void enroll_invalid_returns400() throws Exception {
        EnrollCourseRequest request = EnrollCourseRequest.builder()
                .courseId(null) // ID khóa học null
                .build();

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("GET /api/enrollments/my-courses - Lấy danh sách khóa học của tôi")
    void getMyCourses_success() throws Exception {
        EnrollmentResponse item = EnrollmentResponse.builder()
                .id(1L)
                .userId(1L)
                .courseId(10L)
                .courseTitle("Java Core")
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(BigDecimal.valueOf(25))
                .build();

        PageResponse<EnrollmentResponse> page = PageResponse.of(List.of(item), 0, 10, 1);

        when(enrollmentService.getMyCourses(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/enrollments/my-courses")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].courseId").value(10L))
                .andExpect(jsonPath("$.data.content[0].courseTitle").value("Java Core"));
    }

    @Test
    @DisplayName("GET /api/enrollments/{id} - Lấy chi tiết lượt ghi danh")
    void getEnrollmentById_success() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(5L)
                .userId(1L)
                .courseId(10L)
                .courseTitle("Java Core")
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(BigDecimal.valueOf(100))
                .build();

        when(enrollmentService.getEnrollmentById(eq(5L), any())).thenReturn(response);

        mockMvc.perform(get("/api/enrollments/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(5L))
                .andExpect(jsonPath("$.data.progressPercent").value(100));
    }

    @Test
    @DisplayName("PATCH /api/enrollments/{id}/cancel - Hủy đăng ký khóa học")
    void cancelEnrollment_success() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(5L)
                .userId(1L)
                .courseId(10L)
                .courseTitle("Java Core")
                .status(EnrollmentStatus.CANCELLED)
                .progressPercent(BigDecimal.valueOf(50))
                .build();

        when(enrollmentService.cancelEnrollment(eq(1L), eq(5L))).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/enrollments/5/cancel")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("DELETE /api/enrollments/course/{courseId} - Reset hoặc hủy ghi danh khóa học")
    void unenrollCourse_success() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/enrollments/course/10")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã hủy ghi danh và đặt lại tiến độ khóa học thành công"));
    }

    @Test
    @DisplayName("GET /api/enrollments/{id}/certificate - Lấy chứng chỉ tốt nghiệp")
    void getCertificate_success() throws Exception {
        com.hunre.enrollmentservice.dto.response.CertificateResponse certResponse =
                com.hunre.enrollmentservice.dto.response.CertificateResponse.builder()
                        .id(1L)
                        .enrollmentId(5L)
                        .certificateCode("CERT-2026-C10-U1-TEST")
                        .fileUrl("/certificates/CERT-2026-C10-U1-TEST.pdf")
                        .issuedAt(Instant.now())
                        .build();

        when(enrollmentService.getCertificate(eq(1L), eq(5L))).thenReturn(certResponse);

        mockMvc.perform(get("/api/enrollments/5/certificate")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.certificateCode").value("CERT-2026-C10-U1-TEST"));
    }
}
