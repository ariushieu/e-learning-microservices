package com.hunre.courseservice.service;

import com.hunre.courseservice.dto.response.CourseSummaryResponse;
import com.hunre.courseservice.entity.*;
import com.hunre.courseservice.repository.CategoryRepository;
import com.hunre.courseservice.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CourseCategoryFilterIntegrationTest {
    @Autowired CourseService service;
    @Autowired CategoryRepository categories;
    @Autowired CourseRepository courses;

    Category parent, child, sibling;
    Course direct, nested, siblingCourse;

    @BeforeEach
    void seed() {
        parent = category(null);
        child = category(parent);
        sibling = category(parent);
        direct = course(parent, CourseStatus.PUBLISHED, CourseLevel.BEGINNER, 300);
        nested = course(child, CourseStatus.PUBLISHED, CourseLevel.BEGINNER, 100);
        siblingCourse = course(sibling, CourseStatus.PUBLISHED, CourseLevel.ADVANCED, 200);
        course(child, CourseStatus.DRAFT, CourseLevel.BEGINNER, 0);
        course(child, CourseStatus.ARCHIVED, CourseLevel.BEGINNER, 0);
        course(category(null), CourseStatus.PUBLISHED, CourseLevel.BEGINNER, 0);
    }

    Category category(Category parentCategory) {
        return categories.save(Category.builder().name("Catalog").slug(UUID.randomUUID().toString())
                .parent(parentCategory).build());
    }

    Course course(Category category, CourseStatus status, CourseLevel level, int price) {
        return courses.save(Course.builder().category(category).instructorId(1L).title("Catalog regression")
                .slug(UUID.randomUUID().toString()).status(status).level(level)
                .price(BigDecimal.valueOf(price)).build());
    }

    @Test
    void parentIncludesDirectAndBothChildrenButOnlyPublishedCourses() {
        var result = service.getCourses(null, parent.getId(), null, null, PageRequest.of(0, 20));
        assertThat(result.content()).extracting(CourseSummaryResponse::getId)
                .containsExactlyInAnyOrder(direct.getId(), nested.getId(), siblingCourse.getId());
        assertThat(result.totalElements()).isEqualTo(3);
    }

    @Test
    void childDoesNotIncludeParentOrSibling() {
        var result = service.getCourses(null, child.getId(), null, null, PageRequest.of(0, 20));
        assertThat(result.content()).extracting(CourseSummaryResponse::getId).containsExactly(nested.getId());
    }

    @Test
    void parentFilterComposesWithLevelKeywordInstructorSortAndPagination() {
        var sort = Sort.by("price").ascending().and(Sort.by("id").descending());
        var first = service.getCourses(1L, parent.getId(), CourseLevel.BEGINNER, "regression", PageRequest.of(0, 1, sort));
        var second = service.getCourses(1L, parent.getId(), CourseLevel.BEGINNER, "regression", PageRequest.of(1, 1, sort));
        assertThat(first.totalElements()).isEqualTo(2);
        assertThat(second.totalElements()).isEqualTo(2);
        assertThat(first.content()).extracting(CourseSummaryResponse::getId).containsExactly(nested.getId());
        assertThat(second.content()).extracting(CourseSummaryResponse::getId).containsExactly(direct.getId());
        assertThat(service.getCourses(2L, parent.getId(), null, null, PageRequest.of(0, 20)).content()).isEmpty();
        assertThat(service.getCourses(null, parent.getId(), null, "missing", PageRequest.of(0, 20)).content()).isEmpty();
    }

    @Test
    void unknownCategoryReturnsEmptyPage() {
        assertThat(service.getCourses(null, Long.MAX_VALUE, null, null, PageRequest.of(0, 20)).totalElements()).isZero();
    }
}
