package com.hunre.quizservice.repository;

import com.hunre.quizservice.entity.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {

    List<AnswerOption> findByQuestionIdOrderByPositionAsc(Long questionId);

    /** Những phương án trong ids đã có ít nhất một bài làm chọn. */
    @Query("select distinct o.id from AttemptAnswer a join a.selectedOptions o where o.id in :ids")
    Set<Long> findPickedIds(@Param("ids") Collection<Long> ids);
}
