package com.hunre.courseservice.repository;

import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    // Khóa cha trước khi cập nhật khóa học hoặc số liệu chương/bài.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Course c WHERE c.id = :id")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);

    // Giữ khóa tồn tại cho đến khi ghi sổ học viên và cập nhật số đếm xong.
    @Query(value = "SELECT id FROM courses WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Long> lockForLearnerUpdate(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE courses SET student_count = student_count + 1 WHERE id = :id", nativeQuery = true)
    int incrementStudentCount(@Param("id") Long id);

    // Kiểm tra ngay tại lệnh ghi, không dựa vào entity có thể đã đọc trước consumer.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM courses WHERE id = :id AND status = 'DRAFT' AND student_count = 0", nativeQuery = true)
    int deleteEmptyDraft(@Param("id") Long id);

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByCategoryId(Long categoryId);

    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    Page<Course> findByInstructorId(Long instructorId, Pageable pageable);

    Page<Course> findByInstructorIdAndStatus(Long instructorId, CourseStatus status, Pageable pageable);
}
