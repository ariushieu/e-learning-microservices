package com.hunre.courseservice.repository;

import com.hunre.courseservice.entity.LessonAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LessonAnswerRepository extends JpaRepository<LessonAnswer, Long> {
    List<LessonAnswer> findByQuestionIdInOrderByCreatedAtAscIdAsc(Collection<Long> questionIds);

    Optional<LessonAnswer> findByIdAndQuestionId(Long id, Long questionId);

    long countByQuestionId(Long questionId);

    boolean existsByQuestionIdAndAuthorRoleIn(Long questionId, Collection<LessonAnswer.AuthorRole> roles);
}
