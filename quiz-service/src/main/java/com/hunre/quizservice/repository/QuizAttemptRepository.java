package com.hunre.quizservice.repository;

import com.hunre.quizservice.entity.AttemptStatus;
import com.hunre.quizservice.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByQuizIdAndUserIdOrderByAttemptNoDesc(Long quizId, Long userId);

    Optional<QuizAttempt> findFirstByQuizIdAndUserIdAndStatus(Long quizId, Long userId, AttemptStatus status);

    long countByQuizIdAndUserId(Long quizId, Long userId);

    Optional<QuizAttempt> findByIdAndUserId(Long id, Long userId);
}
