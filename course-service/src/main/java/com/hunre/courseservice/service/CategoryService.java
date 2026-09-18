package com.hunre.courseservice.service;

import com.hunre.courseservice.dto.request.CreateCategoryRequest;
import com.hunre.courseservice.dto.request.UpdateCategoryRequest;
import com.hunre.courseservice.dto.response.CategoryResponse;
import com.hunre.sharedcommon.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getCategoryTree();

    PageResponse<CategoryResponse> getCategories(Pageable pageable);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse getCategoryBySlug(String slug);

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(Long id, UpdateCategoryRequest request);

    void deleteCategory(Long id);
}
