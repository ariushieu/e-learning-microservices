package com.hunre.quizservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "attempt_answers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attempt_answers_attempt_question", columnNames = {"attempt_id", "question_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Builder.Default
    @Column(name = "earned_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal earnedScore = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect = false;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "attempt_answer_options",
            joinColumns = @JoinColumn(name = "attempt_answer_id"),
            inverseJoinColumns = @JoinColumn(name = "option_id")
    )
    private Set<AnswerOption> selectedOptions = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (this.earnedScore == null) {
            this.earnedScore = BigDecimal.ZERO;
        }
    }
}
