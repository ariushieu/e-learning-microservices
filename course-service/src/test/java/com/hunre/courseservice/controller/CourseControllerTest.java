package com.hunre.courseservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hunre.courseservice.dto.request.ChangeCourseStatusRequest;
import com.hunre.courseservice.dto.request.CreateCourseRequest;
import com.hunre.courseservice.dto.request.UpdateCourseRequest;
import com.hunre.courseservice.dto.response.CourseResponse;
import com.hunre.courseservice.dto.response.CourseSummaryResponse;
import com.hunre.courseservice.entity.CourseLevel;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.service.CourseService;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.GlobalExceptionHandler;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AuthenticatedUser instructor;
    private AuthenticatedUser student;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private CourseController courseController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(courseController)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticatedUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        instructor = new AuthenticatedUser(50L, "teacher@hunre.edu.vn", "Thầy Tuấn", Set.of(Roles.INSTRUCTOR));
        student = new AuthenticatedUser(10L, "student@hunre.edu.vn", "Học viên A", Set.of(Roles.STUDENT));
    }

    @Test
    @DisplayName("GET /api/courses trả về danh sách phân trang chuẩn PageResponse")
    void getPublishedCourses_success() throws Exception {
        CourseSummaryResponse summary = CourseSummaryResponse.builder()
                .id(1L)
                .title("Spring Cloud Gateway")
                .slug("spring-cloud-gateway")
                .level(CourseLevel.INTERMEDIATE)
                .status(CourseStatus.PUBLISHED)
                .price(BigDecimal.valueOf(300000))
                .build();

        PageResponse<CourseSummaryResponse> pageResponse = PageResponse.of(List.of(summary), 0, 12, 1);

        when(courseService.getPublishedCourses(any(), any(), any(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Spring Cloud Gateway"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/courses/{id} thành công")
    void getCourseById_success() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id(1L)
                .title("Spring Boot")
                .slug("spring-boot")
                .price(BigDecimal.ZERO)
                .status(CourseStatus.PUBLISHED)
                .build();

        when(courseService.getCourseById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Spring Boot"));
    }

    @Test
    @DisplayName("GET /api/courses/{id} không tìm thấy trả về 404")
    void getCourseById_notFound() throws Exception {
        when(courseService.getCourseById(999L))
                .thenThrow(new ResourceNotFoundException("khóa học", "id", 999L));

        mockMvc.perform(get("/api/courses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy khóa học với id = 999"));
    }

    @Test
    @DisplayName("POST /api/courses thành công trả về 201 Created khi là Giảng viên")
    void createCourse_success() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id(10L)
                .title("Kafka For Beginners")
                .slug("kafka-for-beginners")
                .status(CourseStatus.DRAFT)
                .build();

        when(courseService.createCourse(any(CreateCourseRequest.class), eq(50L), any())).thenReturn(response);

        String json = """
                {
                    "categoryId": 1,
                    "title": "Kafka For Beginners",
                    "price": 200000
                }
                """;

        mockMvc.perform(post("/api/courses")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo khóa học thành công"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.title").value("Kafka For Beginners"));
    }

    @Test
    @DisplayName("POST /api/courses bị từ chối 403 Forbidden khi là Học viên")
    void createCourse_whenStudent_returnsForbidden403() throws Exception {
        String json = """
                {
                    "categoryId": 1,
                    "title": "Kafka For Beginners",
                    "price": 200000
                }
                """;

        mockMvc.perform(post("/api/courses")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /api/courses validation thất bại khi thiếu tiêu đề và danh mục")
    void createCourse_validationFailed() throws Exception {
        String invalidJson = """
                {
                    "title": ""
                }
                """;

        mockMvc.perform(post("/api/courses")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("PUT /api/courses/{id} thành công khi là Giảng viên")
    void updateCourse_success() throws Exception {
        CourseResponse response = CourseResponse.builder().id(1L).title("Updated").build();
        when(courseService.updateCourse(eq(1L), any(UpdateCourseRequest.class), eq(50L), eq(false)))
                .thenReturn(response);

        String json = """
                {
                    "categoryId": 1,
                    "title": "Updated"
                }
                """;

        mockMvc.perform(put("/api/courses/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("PATCH /api/courses/{id}/status thành công khi là Giảng viên")
    void changeCourseStatus_success() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id(1L)
                .status(CourseStatus.PUBLISHED)
                .publishedAt(Instant.now())
                .build();

        when(courseService.changeCourseStatus(eq(1L), any(ChangeCourseStatusRequest.class), eq(50L), eq(false)))
                .thenReturn(response);

        String json = """
                {
                    "status": "PUBLISHED"
                }
                """;

        mockMvc.perform(patch("/api/courses/1/status")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("DELETE /api/courses/{id} thành công trả về 200 khi là Giảng viên")
    void deleteCourse_success() throws Exception {
        mockMvc.perform(delete("/api/courses/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã xóa khóa học"));

        verify(courseService).deleteCourse(eq(1L), eq(50L), eq(false));
    }

    @Test
    @DisplayName("DELETE /api/courses/{id} bị chặn 403 khi là Học viên")
    void deleteCourse_whenStudent_returnsForbidden403() throws Exception {
        mockMvc.perform(delete("/api/courses/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
