package com.hunre.enrollmentservice.controller;

import com.hunre.enrollmentservice.dto.request.UpdateLessonProgressRequest;
import com.hunre.enrollmentservice.dto.response.CourseProgressResponse;
import com.hunre.enrollmentservice.dto.response.LessonProgressResponse;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import com.hunre.enrollmentservice.entity.LessonProgressStatus;
import com.hunre.enrollmentservice.service.ProgressService;
import com.hunre.sharedcommon.exception.GlobalExceptionHandler;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.AuthenticatedUserArgumentResolver;
import com.hunre.sharedcommon.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProgressControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private final AuthenticatedUser mockUser = new AuthenticatedUser(
            1L, "student@hunre.edu.vn", "Nguyễn Văn A", Set.of("ROLE_STUDENT"));

    @Mock
    private ProgressService progressService;

    @InjectMocks
    private ProgressController progressController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(progressController)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticatedUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/progress/lesson - Cập nhật tiến độ bài học thành công")
    void updateLessonProgress_success() throws Exception {
        UpdateLessonProgressRequest request = UpdateLessonProgressRequest.builder()
                .courseId(10L)
                .lessonId(101L)
                .status(LessonProgressStatus.COMPLETED)
                .watchedSeconds(300)
                .build();

        LessonProgressResponse response = LessonProgressResponse.builder()
                .lessonId(101L)
                .status(LessonProgressStatus.COMPLETED)
                .watchedSeconds(300)
                .build();

        when(progressService.updateLessonProgress(eq(1L), any(UpdateLessonProgressRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/progress/lesson")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, mockUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật tiến độ bài học thành công"))
                .andExpect(jsonPath("$.data.lessonId").value(101L))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/progress/course/{courseId} - Lấy chi tiết tiến độ khóa học")
    void getCourseProgress_success() throws Exception {
        LessonProgressResponse l1 = LessonProgressResponse.builder()
                .lessonId(1L)
                .status(LessonProgressStatus.COMPLETED)
                .watchedSeconds(300)
                .build();

        CourseProgressResponse response = CourseProgressResponse.builder()
                .courseId(10L)
                .enrollmentId(50L)
                .courseTitle("Spring Boot")
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(BigDecimal.valueOf(50))
                .completedLessonsCount(1)
                .totalLessonsCount(2)
                .lessons(List.of(l1))
                .build();

        when(progressService.getCourseProgress(eq(1L), eq(10L))).thenReturn(response);

        mockMvc.perform(get("/api/progress/course/10")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, mockUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseId").value(10L))
                .andExpect(jsonPath("$.data.progressPercent").value(50));
    }
}
