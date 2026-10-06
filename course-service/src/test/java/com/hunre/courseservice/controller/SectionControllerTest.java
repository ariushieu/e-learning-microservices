package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.request.CreateSectionRequest;
import com.hunre.courseservice.dto.request.UpdateSectionRequest;
import com.hunre.courseservice.dto.response.SectionResponse;
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

import java.util.List;
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
class SectionControllerTest {

    private MockMvc mockMvc;
    private AuthenticatedUser instructor;
    private AuthenticatedUser student;

    @Mock
    private CurriculumService curriculumService;

    @InjectMocks
    private SectionController sectionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(sectionController)
                .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        instructor = new AuthenticatedUser(1L, "teacher@hunre.edu.vn", "Thầy Tuấn", Set.of(Roles.INSTRUCTOR));
        student = new AuthenticatedUser(2L, "student@hunre.edu.vn", "Học viên A", Set.of(Roles.STUDENT));
    }

    @Test
    @DisplayName("GET /api/courses/{courseId}/curriculum thành công")
    void getCurriculum_success() throws Exception {
        SectionResponse section = SectionResponse.builder()
                .id(1L)
                .courseId(10L)
                .title("Chương 1")
                .position(1)
                .build();

        when(curriculumService.getCurriculumByCourseId(10L)).thenReturn(List.of(section));

        mockMvc.perform(get("/api/courses/10/curriculum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Chương 1"));
    }

    @Test
    @DisplayName("POST /api/sections thành công trả về 201 Created khi là Giảng viên")
    void createSection_success() throws Exception {
        SectionResponse section = SectionResponse.builder()
                .id(2L)
                .courseId(10L)
                .title("Chương 2: Cài đặt")
                .build();

        when(curriculumService.createSection(any(CreateSectionRequest.class))).thenReturn(section);

        String json = """
                {
                    "courseId": 10,
                    "title": "Chương 2: Cài đặt",
                    "position": 2
                }
                """;

        mockMvc.perform(post("/api/sections")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo chương học thành công"))
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    @DisplayName("POST /api/sections bị chặn 403 khi là Học viên")
    void createSection_whenStudent_returnsForbidden403() throws Exception {
        String json = """
                {
                    "courseId": 10,
                    "title": "Chương 2: Cài đặt"
                }
                """;

        mockMvc.perform(post("/api/sections")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("DELETE /api/sections/{id} thành công khi là Giảng viên")
    void deleteSection_success() throws Exception {
        mockMvc.perform(delete("/api/sections/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã xóa chương học"));

        verify(curriculumService).deleteSection(eq(1L));
    }

    @Test
    @DisplayName("DELETE /api/sections/{id} bị chặn 403 khi là Học viên")
    void deleteSection_whenStudent_returnsForbidden403() throws Exception {
        mockMvc.perform(delete("/api/sections/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
