package com.hunre.courseservice.repository;

import com.hunre.courseservice.entity.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
    Page<CourseReview> findByCourseId(Long courseId, Pageable pageable);
    Optional<CourseReview> findByCourseIdAndUserId(Long courseId, Long userId);
    Optional<CourseReview> findByIdAndCourseId(Long id, Long courseId);
}
