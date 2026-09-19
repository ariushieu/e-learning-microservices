package com.hunre.quizservice.controller;

import com.hunre.quizservice.dto.AnswerOptionResponse;
import com.hunre.quizservice.dto.QuestionResponse;
import com.hunre.quizservice.entity.QuestionType;
import com.hunre.quizservice.service.QuestionService;
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
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    private static final String VALID_QUESTION_BODY = """
            {
              "content": "2 + 2 = ?",
              "type": "SINGLE_CHOICE",
              "score": 1,
              "position": 1,
              "explanation": "Giải thích bí mật",
              "options": [
                {"content": "3", "isCorrect": false, "position": 1},
                {"content": "4", "isCorrect": true, "position": 2}
              ]
            }
            """;

    private MockMvc mockMvc;
    private AuthenticatedUser student;
    private AuthenticatedUser instructor;

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private QuestionController questionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(questionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
                .build();
        student = new AuthenticatedUser(
                10L, "student@hunre.edu.vn", "Nguyen Van A", Set.of(Roles.STUDENT));
        instructor = new AuthenticatedUser(
                1L, "teacher@hunre.edu.vn", "Thay Giao", Set.of(Roles.INSTRUCTOR));
    }

    @Test
    @DisplayName("GET /api/quizzes/{quizId}/questions: Học viên bị chặn 403 và service không được gọi")
    void getQuestions_forbiddenForStudent() throws Exception {
        mockMvc.perform(get("/api/quizzes/1/questions")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(questionService);
    }

    @Test
    @DisplayName("GET /api/quizzes/{quizId}/questions: Giảng viên được xem câu hỏi kèm đáp án")
    void getQuestions_successForInstructor() throws Exception {
        when(questionService.getQuestionsByQuiz(1L)).thenReturn(questionResponses());

        mockMvc.perform(get("/api/quizzes/1/questions")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].explanation").value("Giải thích bí mật"))
                .andExpect(jsonPath("$.data[0].options[1].isCorrect").value(true));
    }

    @Test
    @DisplayName("GET /api/quizzes/{quizId}/questions: Quản trị viên được xem câu hỏi kèm đáp án")
    void getQuestions_successForAdmin() throws Exception {
        AuthenticatedUser admin = new AuthenticatedUser(
                2L, "admin@hunre.edu.vn", "Admin", Set.of(Roles.ADMIN));
        when(questionService.getQuestionsByQuiz(1L)).thenReturn(questionResponses());

        mockMvc.perform(get("/api/quizzes/1/questions")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10L));
    }

    @Test
    @DisplayName("Các endpoint sửa câu hỏi: Học viên đều bị chặn 403 FORBIDDEN")
    void questionManagementEndpoints_forbiddenForStudent() throws Exception {
        mockMvc.perform(post("/api/quizzes/1/questions")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_QUESTION_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(put("/api/quizzes/1/questions/10")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_QUESTION_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(delete("/api/quizzes/1/questions/10")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(questionService);
    }

    private List<QuestionResponse> questionResponses() {
        return List.of(QuestionResponse.builder()
                .id(10L)
                .content("2 + 2 = ?")
                .type(QuestionType.SINGLE_CHOICE)
                .score(BigDecimal.ONE)
                .position(1)
                .explanation("Giải thích bí mật")
                .options(List.of(
                        AnswerOptionResponse.builder()
                                .id(100L)
                                .content("3")
                                .isCorrect(false)
                                .position(1)
                                .build(),
                        AnswerOptionResponse.builder()
                                .id(101L)
                                .content("4")
                                .isCorrect(true)
                                .position(2)
                                .build()))
                .build());
    }
}
