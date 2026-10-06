package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.request.CreateLessonRequest;
import com.hunre.courseservice.dto.request.CreateLessonResourceRequest;
import com.hunre.courseservice.dto.request.UpdateLessonRequest;
import com.hunre.courseservice.dto.response.LessonResourceResponse;
import com.hunre.courseservice.dto.response.LessonResponse;
import com.hunre.courseservice.entity.LessonType;
import com.hunre.courseservice.service.CurriculumService;
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

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LessonControllerTest {

    private MockMvc mockMvc;
    private AuthenticatedUser instructor;
    private AuthenticatedUser student;

    @Mock
    private CurriculumService curriculumService;

    @InjectMocks
    private LessonController lessonController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(lessonController)
                .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        instructor = new AuthenticatedUser(1L, "teacher@hunre.edu.vn", "Thầy Tuấn", Set.of(Roles.INSTRUCTOR));
        student = new AuthenticatedUser(2L, "student@hunre.edu.vn", "Học viên A", Set.of(Roles.STUDENT));
    }

    @Test
    @DisplayName("GET /api/lessons/{id} thành công")
    void getLessonById_success() throws Exception {
        LessonResponse lesson = LessonResponse.builder()
                .id(100L)
                .title("Bài 1: Giới thiệu")
                .type(LessonType.VIDEO)
                .durationSeconds(240)
                .isPreview(true)
                .build();

        when(curriculumService.getLessonById(100L)).thenReturn(lesson);

        mockMvc.perform(get("/api/lessons/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.title").value("Bài 1: Giới thiệu"))
                .andExpect(jsonPath("$.data.isPreview").value(true));
    }

    @Test
    @DisplayName("POST /api/lessons thành công trả về 201 Created khi là Giảng viên")
    void createLesson_success() throws Exception {
        LessonResponse lesson = LessonResponse.builder()
                .id(101L)
                .title("Bài 2: Hướng dẫn code")
                .type(LessonType.VIDEO)
                .build();

        when(curriculumService.createLesson(any(CreateLessonRequest.class))).thenReturn(lesson);

        String json = """
                {
                    "sectionId": 1,
                    "title": "Bài 2: Hướng dẫn code",
                    "type": "VIDEO",
                    "durationSeconds": 300,
                    "isPreview": false
                }
                """;

        mockMvc.perform(post("/api/lessons")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo bài học thành công"))
                .andExpect(jsonPath("$.data.id").value(101));
    }

    @Test
    @DisplayName("POST /api/lessons bị chặn 403 khi là Học viên")
    void createLesson_whenStudent_returnsForbidden403() throws Exception {
        String json = """
                {
                    "sectionId": 1,
                    "title": "Bài 2"
                }
                """;

        mockMvc.perform(post("/api/lessons")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /api/lessons/{lessonId}/resources thành công khi là Giảng viên")
    void addResource_success() throws Exception {
        LessonResourceResponse resource = LessonResourceResponse.builder()
                .id(50L)
                .lessonId(100L)
                .name("Tài liệu tham khảo")
                .fileUrl("https://storage.elearning.com/ref.pdf")
                .build();

        when(curriculumService.addResource(eq(100L), any(CreateLessonResourceRequest.class))).thenReturn(resource);

        String json = """
                {
                    "name": "Tài liệu tham khảo",
                    "fileUrl": "https://storage.elearning.com/ref.pdf"
                }
                """;

        mockMvc.perform(post("/api/lessons/100/resources")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đính kèm tài liệu thành công"))
                .andExpect(jsonPath("$.data.id").value(50));
    }

    @Test
    @DisplayName("DELETE /api/lessons/{id} thành công khi là Giảng viên")
    void deleteLesson_success() throws Exception {
        mockMvc.perform(delete("/api/lessons/100")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã xóa bài học"));

        verify(curriculumService).deleteLesson(eq(100L));
    }

    @Test
    @DisplayName("DELETE /api/lessons/{id} bị chặn 403 khi là Học viên")
    void deleteLesson_whenStudent_returnsForbidden403() throws Exception {
        mockMvc.perform(delete("/api/lessons/100")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
