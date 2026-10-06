package com.hunre.quizservice.repository;

import com.hunre.quizservice.entity.Quiz;
import com.hunre.quizservice.entity.QuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByCourseId(Long courseId);

    @Query("select q from Quiz q where q.courseId = :courseId and (q.status = :status or q.createdBy = :userId)")
    List<Quiz> findVisibleByCourseId(@Param("courseId") Long courseId, @Param("userId") Long userId,
                                     @Param("status") QuizStatus status);

    List<Quiz> findByCourseIdAndStatus(Long courseId, QuizStatus status);

    Optional<Quiz> findByLessonId(Long lessonId);

    Optional<Quiz> findByLessonIdAndStatus(Long lessonId, QuizStatus status);
}
