package com.hunre.enrollmentservice.repository;

import com.hunre.enrollmentservice.entity.LessonProgress;
import com.hunre.enrollmentservice.entity.LessonProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByEnrollmentIdAndLessonId(Long enrollmentId, Long lessonId);

    List<LessonProgress> findAllByEnrollmentId(Long enrollmentId);

    int countByEnrollmentIdAndStatus(Long enrollmentId, LessonProgressStatus status);

    void deleteAllByEnrollmentId(Long enrollmentId);
}
