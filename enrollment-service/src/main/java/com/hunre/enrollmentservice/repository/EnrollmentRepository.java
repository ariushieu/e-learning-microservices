package com.hunre.enrollmentservice.repository;

import com.hunre.enrollmentservice.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    Page<Enrollment> findAllByUserId(Long userId, Pageable pageable);

    List<Enrollment> findAllByUserId(Long userId);

    Optional<Enrollment> findByIdAndUserId(Long id, Long userId);
}
