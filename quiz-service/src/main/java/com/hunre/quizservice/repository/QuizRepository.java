package com.hunre.quizservice.repository;

import com.hunre.quizservice.entity.Quiz;
import com.hunre.quizservice.entity.QuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByCourseId(Long courseId);

    List<Quiz> findByCourseIdAndStatus(Long courseId, QuizStatus status);

    Optional<Quiz> findByLessonId(Long lessonId);

    Optional<Quiz> findByLessonIdAndStatus(Long lessonId, QuizStatus status);
}
