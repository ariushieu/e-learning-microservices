package com.hunre.courseservice.service;

import com.hunre.courseservice.dto.request.CreateCategoryRequest;
import com.hunre.courseservice.dto.request.UpdateCategoryRequest;
import com.hunre.courseservice.dto.response.CategoryResponse;
import com.hunre.courseservice.entity.Category;
import com.hunre.courseservice.repository.CategoryRepository;
import com.hunre.courseservice.service.impl.CategoryServiceImpl;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.DuplicateResourceException;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import com.hunre.courseservice.repository.CourseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    @DisplayName("Tạo danh mục gốc thành công")
    void createCategory_root_success() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Lập trình Web")
                .description("Các khóa học lập trình Web")
                .position(1)
                .build();

        when(categoryRepository.existsBySlug("lap-trinh-web")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(1L);
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Lập trình Web");
        assertThat(response.getSlug()).isEqualTo("lap-trinh-web");
        assertThat(response.getParentId()).isNull();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Tạo danh mục thất bại khi slug bị trùng")
    void createCategory_duplicateSlug_throwsDuplicateResourceException() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Lập trình Web")
                .slug("lap-trinh-web")
                .build();

        when(categoryRepository.existsBySlug("lap-trinh-web")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("lap-trinh-web");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo danh mục con thất bại khi danh mục cha không tồn tại")
    void createCategory_parentNotFound_throwsResourceNotFoundException() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Spring Boot")
                .parentId(999L)
                .build();

        when(categoryRepository.existsBySlug("spring-boot")).thenReturn(false);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Tạo danh mục con thất bại khi danh mục cha đã là con của danh mục khác (quá 1 cấp)")
    void createCategory_parentAlreadyHasParent_throwsBusinessException() {
        Category grandParent = Category.builder().id(1L).name("CNTT").build();
        Category parent = Category.builder().id(2L).name("Lập trình").parent(grandParent).build();

        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Java")
                .parentId(2L)
                .build();

        when(categoryRepository.existsBySlug("java")).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tối đa 1 cấp lồng nhau");
    }

    @Test
    @DisplayName("Tìm danh mục theo ID không tồn tại ném ResourceNotFoundException")
    void getCategoryById_notFound_throwsResourceNotFoundException() {
        when(categoryRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(123L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("123");
    }

    @Test
    @DisplayName("Xóa danh mục thất bại khi còn danh mục con")
    void deleteCategory_hasSubCategories_throwsBusinessException() {
        Category cat = Category.builder().id(1L).name("Gốc").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(categoryRepository.existsByParentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đang có danh mục con");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa danh mục thất bại khi còn khóa học trực thuộc")
    void deleteCategory_hasCourses_throwsBusinessException() {
        Category cat = Category.builder().id(1L).name("Gốc").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(categoryRepository.existsByParentId(1L)).thenReturn(false);
        when(courseRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đang chứa khóa học");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa danh mục thành công khi không có danh mục con và không có khóa học")
    void deleteCategory_success() {
        Category cat = Category.builder().id(1L).name("Gốc").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(categoryRepository.existsByParentId(1L)).thenReturn(false);
        when(courseRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(cat);
    }
}
