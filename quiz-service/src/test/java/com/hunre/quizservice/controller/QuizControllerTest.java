package com.hunre.quizservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hunre.quizservice.dto.CreateQuizRequest;
import com.hunre.quizservice.dto.QuizResponse;
import com.hunre.quizservice.entity.QuizStatus;
import com.hunre.quizservice.service.QuizService;
import com.hunre.sharedcommon.exception.GlobalExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private QuizService quizService;

    @InjectMocks
    private QuizController quizController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(quizController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
