package com.hunre.quizservice.repository;

import com.hunre.quizservice.entity.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {

    List<AnswerOption> findByQuestionIdOrderByPositionAsc(Long questionId);
}
