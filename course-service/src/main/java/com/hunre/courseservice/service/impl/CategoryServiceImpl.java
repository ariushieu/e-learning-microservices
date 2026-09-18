package com.hunre.courseservice.service.impl;

import com.hunre.courseservice.dto.request.CreateCategoryRequest;
import com.hunre.courseservice.dto.request.UpdateCategoryRequest;
import com.hunre.courseservice.dto.response.CategoryResponse;
import com.hunre.courseservice.entity.Category;
import com.hunre.courseservice.repository.CategoryRepository;
import com.hunre.courseservice.service.CategoryService;
import com.hunre.courseservice.util.SlugUtils;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.DuplicateResourceException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import com.hunre.courseservice.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;

    @Override
    public List<CategoryResponse> getCategoryTree() {
        List<Category> rootCategories = categoryRepository.findByParentIsNullOrderByPositionAsc();
        return rootCategories.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Override
    public PageResponse<CategoryResponse> getCategories(Pageable pageable) {
        Page<Category> page = categoryRepository.findAll(pageable);
        List<CategoryResponse> content = page.getContent().stream()
                .map(CategoryResponse::from)
                .toList();

        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("danh mục", "id", id));
        return CategoryResponse.from(category);
    }

    @Override
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("danh mục", "slug", slug));
        return CategoryResponse.from(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String slug = resolveSlug(request.getName(), request.getSlug());

        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("danh mục", "slug", slug);
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("danh mục cha", "id", request.getParentId()));

            // Chỉ cho phép lồng tối đa 1 cấp (cha - con)
            if (parent.getParent() != null) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                        "Hệ thống chỉ hỗ trợ danh mục tối đa 1 cấp lồng nhau");
            }
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .slug(slug)
                .parent(parent)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .position(request.getPosition() != null ? request.getPosition() : 0)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return CategoryResponse.from(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("danh mục", "id", id));

        String slug = resolveSlug(request.getName(), request.getSlug());

        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new DuplicateResourceException("danh mục", "slug", slug);
        }

        Category parent = null;
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                        "Danh mục không thể tự làm cha của chính nó");
            }

            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("danh mục cha", "id", request.getParentId()));

            if (parent.getParent() != null) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                        "Hệ thống chỉ hỗ trợ danh mục tối đa 1 cấp lồng nhau");
            }

            // Nếu danh mục hiện tại đã có con thì không được biến thành danh mục con của danh mục khác
            if (categoryRepository.existsByParentId(id)) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                        "Danh mục đang chứa danh mục con không thể chuyển thành danh mục con");
            }
        }

        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setParent(parent);
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        category.setPosition(request.getPosition() != null ? request.getPosition() : 0);

        Category updatedCategory = categoryRepository.save(category);
        return CategoryResponse.from(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("danh mục", "id", id));

        if (categoryRepository.existsByParentId(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Không thể xóa danh mục đang có danh mục con");
        }

        if (courseRepository.existsByCategoryId(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED,
                    "Không thể xóa danh mục đang chứa khóa học");
        }

        categoryRepository.delete(category);
    }

    private String resolveSlug(String name, String providedSlug) {
        if (providedSlug != null && !providedSlug.isBlank()) {
            return SlugUtils.toSlug(providedSlug);
        }
        return SlugUtils.toSlug(name);
    }
}
