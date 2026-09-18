package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.request.ChangeCourseStatusRequest;
import com.hunre.courseservice.dto.request.CreateCourseRequest;
import com.hunre.courseservice.dto.response.CourseResponse;
import com.hunre.courseservice.dto.response.CourseSummaryResponse;
import com.hunre.courseservice.entity.CourseLevel;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.service.CourseService;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.GlobalExceptionHandler;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private CourseController courseController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(courseController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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
    @DisplayName("POST /api/courses thành công trả về 201 Created")
    void createCourse_success() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id(10L)
                .title("Kafka For Beginners")
                .slug("kafka-for-beginners")
                .status(CourseStatus.DRAFT)
                .build();

        when(courseService.createCourse(any(CreateCourseRequest.class))).thenReturn(response);

        String json = """
                {
                    "categoryId": 1,
                    "instructorId": 50,
                    "title": "Kafka For Beginners",
                    "price": 200000
                }
                """;

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo khóa học thành công"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.title").value("Kafka For Beginners"));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("PATCH /api/courses/{id}/status thành công")
    void changeCourseStatus_success() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id(1L)
                .status(CourseStatus.PUBLISHED)
                .publishedAt(Instant.now())
                .build();

        when(courseService.changeCourseStatus(eq(1L), any(ChangeCourseStatusRequest.class)))
                .thenReturn(response);

        String json = """
                {
                    "status": "PUBLISHED"
                }
                """;

        mockMvc.perform(patch("/api/courses/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("DELETE /api/courses/{id} thành công trả về 200")
    void deleteCourse_success() throws Exception {
        mockMvc.perform(delete("/api/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã xóa khóa học"));

        verify(courseService).deleteCourse(eq(1L));
    }
}
