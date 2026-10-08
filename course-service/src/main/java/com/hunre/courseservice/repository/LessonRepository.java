package com.hunre.courseservice.repository;

import com.hunre.courseservice.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("SELECT l.id FROM Lesson l WHERE l.course.id = :courseId ORDER BY l.id")
    List<Long> findIdsByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT l.course.id FROM Lesson l WHERE l.id = :id")
    Optional<Long> findCourseIdById(@Param("id") Long id);

    List<Lesson> findBySectionIdOrderByPositionAsc(Long sectionId);

    List<Lesson> findByCourseIdOrderByPositionAsc(Long courseId);

    int countByCourseId(Long courseId);

    @Query("SELECT COALESCE(SUM(l.durationSeconds), 0) FROM Lesson l WHERE l.course.id = :courseId")
    int sumDurationSecondsByCourseId(@Param("courseId") Long courseId);
}
