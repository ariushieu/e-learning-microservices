package com.hunre.enrollmentservice.repository;

import com.hunre.enrollmentservice.entity.LessonProgress;
import com.hunre.enrollmentservice.entity.LessonProgressStatus;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    interface LessonCompletionCount {
        Long getLessonId();
        long getCompletedCount();
    }

    @Query("""
            select p.lessonId as lessonId,
                   sum(case when p.status = :completed then 1 else 0 end) as completedCount
            from LessonProgress p join p.enrollment e
            where e.courseId = :courseId and e.status <> :cancelled
            group by p.lessonId order by p.lessonId
            """)
    List<LessonCompletionCount> summarizeByCourseId(@Param("courseId") Long courseId,
            @Param("completed") LessonProgressStatus completed, @Param("cancelled") EnrollmentStatus cancelled);

    Optional<LessonProgress> findByEnrollmentIdAndLessonId(Long enrollmentId, Long lessonId);

    List<LessonProgress> findAllByEnrollmentId(Long enrollmentId);

    int countByEnrollmentIdAndStatus(Long enrollmentId, LessonProgressStatus status);

    void deleteAllByEnrollmentId(Long enrollmentId);
}
