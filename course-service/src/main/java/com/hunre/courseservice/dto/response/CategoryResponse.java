package com.hunre.courseservice.dto.response;

import com.hunre.courseservice.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private Integer position;
    private Long parentId;
    private String parentName;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<CategoryResponse> subCategories = new ArrayList<>();

    public static CategoryResponse from(Category category) {
        if (category == null) {
            return null;
        }

        Long parentId = null;
        String parentName = null;
        if (category.getParent() != null) {
            parentId = category.getParent().getId();
            parentName = category.getParent().getName();
        }

        List<CategoryResponse> subs = new ArrayList<>();
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            for (Category sub : category.getSubCategories()) {
                subs.add(CategoryResponse.from(sub));
            }
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .position(category.getPosition())
                .parentId(parentId)
                .parentName(parentName)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .subCategories(subs)
                .build();
    }
}
