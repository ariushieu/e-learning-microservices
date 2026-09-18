package com.hunre.courseservice.repository;

import com.hunre.courseservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByParentId(Long parentId);

    List<Category> findByParentIsNullOrderByPositionAsc();

    List<Category> findByParentIdOrderByPositionAsc(Long parentId);
}
