package com.hunre.enrollmentservice.repository;

import com.hunre.enrollmentservice.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    // Serialize progress transactions, including first writes with no lesson_progress row yet.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Enrollment e where e.userId = :userId and e.courseId = :courseId")
    Optional<Enrollment> findForProgressUpdate(@Param("userId") Long userId, @Param("courseId") Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    Page<Enrollment> findAllByUserId(Long userId, Pageable pageable);

    List<Enrollment> findAllByUserId(Long userId);

    Optional<Enrollment> findByIdAndUserId(Long id, Long userId);
}
