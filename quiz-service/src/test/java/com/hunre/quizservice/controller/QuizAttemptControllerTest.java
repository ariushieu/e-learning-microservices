package com.hunre.quizservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hunre.quizservice.dto.QuizAttemptResponse;
import com.hunre.quizservice.dto.QuizResultResponse;
import com.hunre.quizservice.dto.SubmitAnswerItemRequest;
import com.hunre.quizservice.dto.SubmitQuizAttemptRequest;
import com.hunre.quizservice.entity.AttemptStatus;
import com.hunre.quizservice.service.QuizAttemptService;
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
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuizAttemptControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private QuizAttemptService quizAttemptService;

    @InjectMocks
    private QuizAttemptController quizAttemptController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(quizAttemptController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/quizzes/{quizId}/attempts: Bắt đầu làm bài thành công")
    void startAttempt_success() throws Exception {
        QuizAttemptResponse response = QuizAttemptResponse.builder()
                .id(100L)
                .quizId(1L)
                .quizTitle("Bài kiểm tra số 1")
                .userId(5L)
                .attemptNo(1)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .build();

        when(quizAttemptService.startAttempt(eq(1L), eq(5L))).thenReturn(response);

        mockMvc.perform(post("/api/quizzes/1/attempts")
                        .param("userId", "5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("POST /api/quizzes/attempts/{attemptId}/submit: Nộp bài thành công")
    void submitAttempt_success() throws Exception {
        SubmitQuizAttemptRequest request = SubmitQuizAttemptRequest.builder()
                .answers(List.of(
                        SubmitAnswerItemRequest.builder().questionId(10L).selectedOptionIds(Set.of(101L)).build()
                ))
                .build();

        QuizResultResponse resultResponse = QuizResultResponse.builder()
                .attemptId(100L)
                .quizId(1L)
                .quizTitle("Bài kiểm tra số 1")
                .userId(5L)
                .attemptNo(1)
                .score(new BigDecimal("100.00"))
                .passScore(new BigDecimal("50.00"))
                .passed(true)
                .startedAt(Instant.now())
                .submittedAt(Instant.now())
                .questionResults(List.of())
                .build();

        when(quizAttemptService.submitAttempt(eq(100L), eq(5L), any(SubmitQuizAttemptRequest.class)))
                .thenReturn(resultResponse);

        mockMvc.perform(post("/api/quizzes/attempts/100/submit")
                        .param("userId", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").value(100.00))
                .andExpect(jsonPath("$.data.passed").value(true));
    }
}
