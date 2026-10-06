package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.request.CreateCategoryRequest;
import com.hunre.courseservice.dto.response.CategoryResponse;
import com.hunre.courseservice.service.CategoryService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
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
class CategoryControllerTest {

    private MockMvc mockMvc;
    private AuthenticatedUser instructor;
    private AuthenticatedUser student;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(categoryController)
                .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        instructor = new AuthenticatedUser(1L, "teacher@hunre.edu.vn", "Thầy Tuấn", Set.of(Roles.INSTRUCTOR));
        student = new AuthenticatedUser(2L, "student@hunre.edu.vn", "Học viên A", Set.of(Roles.STUDENT));
    }

    @Test
    @DisplayName("GET /api/categories/tree trả về ApiResponse chuẩn kèm danh sách cây")
    void getCategoryTree_success() throws Exception {
        CategoryResponse cat = CategoryResponse.builder()
                .id(1L)
                .name("CNTT")
                .slug("cntt")
                .position(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryService.getCategoryTree()).thenReturn(List.of(cat));

        mockMvc.perform(get("/api/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("CNTT"))
                .andExpect(jsonPath("$.data[0].slug").value("cntt"));
    }

    @Test
    @DisplayName("GET /api/categories/{id} thành công")
    void getCategoryById_success() throws Exception {
        CategoryResponse cat = CategoryResponse.builder()
                .id(1L)
                .name("CNTT")
                .slug("cntt")
                .build();

        when(categoryService.getCategoryById(1L)).thenReturn(cat);

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("CNTT"));
    }

    @Test
    @DisplayName("GET /api/categories/{id} thất bại khi id không tồn tại trả về HTTP 404")
    void getCategoryById_notFound() throws Exception {
        when(categoryService.getCategoryById(999L))
                .thenThrow(new ResourceNotFoundException("danh mục", "id", 999L));

        mockMvc.perform(get("/api/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy danh mục với id = 999"));
    }

    @Test
    @DisplayName("POST /api/categories thành công trả về HTTP 201 khi là Giảng viên")
    void createCategory_success() throws Exception {
        CategoryResponse response = CategoryResponse.builder()
                .id(5L)
                .name("DevOps")
                .slug("devops")
                .build();

        when(categoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(response);

        String jsonRequest = """
                {
                    "name": "DevOps",
                    "slug": "devops",
                    "description": "Khóa học DevOps",
                    "position": 2
                }
                """;

        mockMvc.perform(post("/api/categories")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo danh mục thành công"))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.name").value("DevOps"));
    }

    @Test
    @DisplayName("POST /api/categories bị chặn 403 Forbidden khi là Học viên")
    void createCategory_whenStudent_returnsForbidden403() throws Exception {
        String jsonRequest = """
                {
                    "name": "DevOps",
                    "slug": "devops"
                }
                """;

        mockMvc.perform(post("/api/categories")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /api/categories thất bại khi dữ liệu không hợp lệ (tên để trống)")
    void createCategory_validationFailed() throws Exception {
        String invalidJsonRequest = """
                {
                    "name": ""
                }
                """;

        mockMvc.perform(post("/api/categories")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} thành công trả về HTTP 200 khi là Giảng viên")
    void deleteCategory_success() throws Exception {
        mockMvc.perform(delete("/api/categories/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, instructor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã xóa danh mục"));

        verify(categoryService).deleteCategory(eq(1L));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} bị chặn 403 khi là Học viên")
    void deleteCategory_whenStudent_returnsForbidden403() throws Exception {
        mockMvc.perform(delete("/api/categories/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ATTRIBUTE, student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
