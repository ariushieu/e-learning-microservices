package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.request.CreateCategoryRequest;
import com.hunre.courseservice.dto.response.CategoryResponse;
import com.hunre.courseservice.service.CategoryService;
import com.hunre.sharedcommon.exception.GlobalExceptionHandler;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
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

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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
                .id(10L)
                .name("Lập trình Java")
                .slug("lap-trinh-java")
                .build();

        when(categoryService.getCategoryById(10L)).thenReturn(cat);

        mockMvc.perform(get("/api/categories/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("Lập trình Java"));
    }

    @Test
    @DisplayName("GET /api/categories/{id} không tìm thấy trả về HTTP 404 và ErrorResponse chuẩn")
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
    @DisplayName("POST /api/categories thành công trả về HTTP 201")
    void createCategory_success() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("DevOps")
                .slug("devops")
                .description("Khóa học DevOps")
                .position(2)
                .build();

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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo danh mục thành công"))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.name").value("DevOps"));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} thành công trả về HTTP 200 kèm message")
    void deleteCategory_success() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã xóa danh mục"));

        verify(categoryService).deleteCategory(eq(1L));
    }
}
