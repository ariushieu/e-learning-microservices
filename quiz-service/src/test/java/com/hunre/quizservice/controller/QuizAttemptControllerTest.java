package com.hunre.quizservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hunre.quizservice.dto.QuizAttemptResponse;
import com.hunre.quizservice.dto.QuizResultResponse;
import com.hunre.quizservice.dto.SubmitAnswerItemRequest;
import com.hunre.quizservice.dto.SubmitQuizAttemptRequest;
import com.hunre.quizservice.entity.AttemptStatus;
import com.hunre.quizservice.service.QuizAttemptService;
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
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuizAttemptControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AuthenticatedUser studentB;

    @Mock
    private QuizAttemptService quizAttemptService;

    @InjectMocks
    private QuizAttemptController quizAttemptController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(quizAttemptController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
        studentB = new AuthenticatedUser(
                4L, "student-b@hunre.edu.vn", "Student B", Set.of(Roles.STUDENT));
    }

    @Test
    @DisplayName("POST /api/quizzes/{quizId}/attempts: Bắt đầu làm bài thành công")
    void startAttempt_success() throws Exception {
        QuizAttemptResponse response = QuizAttemptResponse.builder()
                .id(100L)
                .quizId(1L)
                .quizTitle("Bài kiểm tra số 1")
                .userId(4L)
                .attemptNo(1)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .build();

        when(quizAttemptService.startAttempt(eq(1L), eq(4L))).thenReturn(response);

        mockMvc.perform(post("/api/quizzes/1/attempts")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, studentB)
                        .param("userId", "3"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        verify(quizAttemptService).startAttempt(1L, 4L);
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
                .userId(4L)
                .attemptNo(1)
                .score(new BigDecimal("100.00"))
                .passScore(new BigDecimal("50.00"))
                .passed(true)
                .startedAt(Instant.now())
                .submittedAt(Instant.now())
                .questionResults(List.of())
                .build();

        when(quizAttemptService.submitAttempt(eq(100L), eq(4L), any(SubmitQuizAttemptRequest.class)))
                .thenReturn(resultResponse);

        mockMvc.perform(post("/api/quizzes/attempts/100/submit")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, studentB)
                        .param("userId", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").value(100.00))
                .andExpect(jsonPath("$.data.passed").value(true));

        verify(quizAttemptService).submitAttempt(eq(100L), eq(4L), any(SubmitQuizAttemptRequest.class));
    }

    @Test
    @DisplayName("GET /api/quizzes/attempts/{attemptId}: Chỉ đọc kết quả của user trong token")
    void getAttemptResult_usesAuthenticatedUserId() throws Exception {
        QuizResultResponse response = QuizResultResponse.builder()
                .attemptId(100L)
                .quizId(1L)
                .userId(4L)
                .score(new BigDecimal("100.00"))
                .passed(true)
                .build();

        when(quizAttemptService.getAttemptResult(100L, 4L)).thenReturn(response);

        mockMvc.perform(get("/api/quizzes/attempts/100")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, studentB)
                        .param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(4L));

        verify(quizAttemptService).getAttemptResult(100L, 4L);
    }

    @Test
    @DisplayName("GET /api/quizzes/{quizId}/attempts/history: Chỉ đọc lịch sử của user trong token")
    void getMyAttempts_usesAuthenticatedUserId() throws Exception {
        QuizAttemptResponse response = QuizAttemptResponse.builder()
                .id(100L)
                .quizId(1L)
                .userId(4L)
                .attemptNo(1)
                .status(AttemptStatus.SUBMITTED)
                .build();

        when(quizAttemptService.getUserAttempts(1L, 4L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/quizzes/1/attempts/history")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, studentB)
                        .param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value(4L));

        verify(quizAttemptService).getUserAttempts(1L, 4L);
    }
}
