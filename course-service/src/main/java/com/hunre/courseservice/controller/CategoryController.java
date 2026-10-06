package com.hunre.courseservice.controller;

import com.hunre.courseservice.dto.request.CreateCategoryRequest;
import com.hunre.courseservice.dto.request.UpdateCategoryRequest;
import com.hunre.courseservice.dto.response.CategoryResponse;
import com.hunre.courseservice.service.CategoryService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    public ApiResponse<List<CategoryResponse>> getCategoryTree() {
        return ApiResponse.ok(categoryService.getCategoryTree());
    }

    @GetMapping
    public ApiResponse<PageResponse<CategoryResponse>> getCategories(
            @PageableDefault(size = 20, sort = "position", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.ok(categoryService.getCategories(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ApiResponse.ok(categoryService.getCategoryById(id));
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        return ApiResponse.ok(categoryService.getCategoryBySlug(slug));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request,
            AuthenticatedUser user) {
        requireCategoryManager(user);
        return ApiResponse.ok(categoryService.createCategory(request), "Tạo danh mục thành công");
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request,
            AuthenticatedUser user) {
        requireCategoryManager(user);
        return ApiResponse.ok(categoryService.updateCategory(id, request), "Cập nhật danh mục thành công");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id, AuthenticatedUser user) {
        requireCategoryManager(user);
        categoryService.deleteCategory(id);
        return ApiResponse.message("Đã xóa danh mục");
    }

    private void requireCategoryManager(AuthenticatedUser user) {
        if (!user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Chỉ giảng viên hoặc quản trị viên mới có quyền quản lý danh mục");
        }
    }
}
