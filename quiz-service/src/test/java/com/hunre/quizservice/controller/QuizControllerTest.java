package com.hunre.quizservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hunre.quizservice.dto.CreateQuizRequest;
import com.hunre.quizservice.dto.QuizResponse;
import com.hunre.quizservice.entity.QuizStatus;
import com.hunre.quizservice.service.QuizService;
import com.hunre.quizservice.dto.QuizDetailResponse;
import com.hunre.sharedcommon.exception.GlobalExceptionHandler;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.AuthenticatedUserArgumentResolver;
import com.hunre.sharedcommon.security.JwtAuthenticationFilter;
import com.hunre.sharedcommon.security.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AuthenticatedUser instructor;
    private AuthenticatedUser student;

    @Mock
    private QuizService quizService;

    @InjectMocks
    private QuizController quizController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(quizController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
        instructor = new AuthenticatedUser(
                1L, "teacher@hunre.edu.vn", "Thay Giao", Set.of(Roles.INSTRUCTOR));
        student = new AuthenticatedUser(
                10L, "student@hunre.edu.vn", "Nguyen Van A", Set.of(Roles.STUDENT));
    }

    @Test
    @DisplayName("POST /api/quizzes: Tạo quiz thành công trả về ApiResponse với HTTP 201")
    void createQuiz_success() throws Exception {
        CreateQuizRequest request = CreateQuizRequest.builder()
                .courseId(10L)
                .title("Kiểm tra chương 1")
                .description("Mô tả")
                .timeLimitMinutes(30)
                .passScore(new BigDecimal("60.00"))
                .maxAttempts(3)
                .createdBy(1L)
                .build();

        QuizResponse response = QuizResponse.builder()
                .id(1L)
                .courseId(10L)
                .title("Kiểm tra chương 1")
                .status(QuizStatus.DRAFT)
                .createdBy(1L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(quizService.createQuiz(any(CreateQuizRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/quizzes")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo bài kiểm tra thành công"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.title").value("Kiểm tra chương 1"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("POST /api/quizzes: Dữ liệu không hợp lệ trả về lỗi VALIDATION_FAILED HTTP 400")
    void createQuiz_validationFailed() throws Exception {
        CreateQuizRequest invalidRequest = CreateQuizRequest.builder()
                .title("") // Trống tiêu đề
                .build();

        mockMvc.perform(post("/api/quizzes")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("GET /api/quizzes/{id}: Học viên (ROLE_STUDENT) bị chặn 403 FORBIDDEN")
    void getQuizDetail_forbiddenForStudent() throws Exception {
        mockMvc.perform(get("/api/quizzes/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /api/quizzes/{id}: Giảng viên (ROLE_INSTRUCTOR) được xem chi tiết bài kiểm tra kèm đáp án")
    void getQuizDetail_successForInstructor() throws Exception {
        QuizDetailResponse detail = QuizDetailResponse.builder()
                .id(1L)
                .title("Kiểm tra chương 1")
                .questions(Collections.emptyList())
                .build();

        when(quizService.getQuizDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/quizzes/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/quizzes/{id}: Quản trị viên (ROLE_ADMIN) được xem chi tiết bài kiểm tra kèm đáp án")
    void getQuizDetail_successForAdmin() throws Exception {
        AuthenticatedUser admin = new AuthenticatedUser(
                2L, "admin@hunre.edu.vn", "Admin", Set.of(Roles.ADMIN));

        QuizDetailResponse detail = QuizDetailResponse.builder()
                .id(1L)
                .title("Kiểm tra chương 1")
                .questions(Collections.emptyList())
                .build();

        when(quizService.getQuizDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/quizzes/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("Các endpoint quản lý quiz: Học viên (ROLE_STUDENT) đều bị chặn 403 FORBIDDEN")
    void quizManagementEndpoints_forbiddenForStudent() throws Exception {
        CreateQuizRequest createRequest = CreateQuizRequest.builder()
                .courseId(10L)
                .title("Kiểm tra chương 1")
                .timeLimitMinutes(30)
                .passScore(new BigDecimal("60.00"))
                .maxAttempts(3)
                .createdBy(1L)
                .build();

        String updateRequest = """
                {
                  "title": "Kiểm tra chương 1 - cập nhật",
                  "timeLimitMinutes": 45,
                  "passScore": 60,
                  "maxAttempts": 3
                }
                """;

        mockMvc.perform(post("/api/quizzes")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(put("/api/quizzes/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/quizzes/1/publish")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/quizzes/1/archive")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(delete("/api/quizzes/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(quizService);
    }
}
