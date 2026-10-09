package com.hunre.courseservice.repository;

import com.hunre.courseservice.entity.LessonQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface LessonQuestionRepository extends JpaRepository<LessonQuestion, Long> {
    Page<LessonQuestion> findByLessonIdOrderByLastActivityAtDescIdDesc(Long lessonId, Pageable pageable);

    Optional<LessonQuestion> findByIdAndLessonId(Long id, Long lessonId);

    /** Hộp câu hỏi của giảng viên; instructorId null là quản trị viên, xem mọi khóa. */
    @Query("""
            SELECT q FROM LessonQuestion q
            WHERE (:instructorId IS NULL OR q.courseId IN (SELECT c.id FROM Course c WHERE c.instructorId = :instructorId))
              AND (:answered IS NULL OR q.instructorAnswered = :answered)
              AND (:courseId IS NULL OR q.courseId = :courseId)
            ORDER BY q.lastActivityAt DESC, q.id DESC""")
    Page<LessonQuestion> findInbox(@Param("instructorId") Long instructorId, @Param("answered") Boolean answered,
                                   @Param("courseId") Long courseId, Pageable pageable);

    @Query("""
            SELECT COUNT(q) FROM LessonQuestion q
            WHERE (:instructorId IS NULL OR q.courseId IN (SELECT c.id FROM Course c WHERE c.instructorId = :instructorId))
              AND q.instructorAnswered = false""")
    long countUnanswered(@Param("instructorId") Long instructorId);
}
