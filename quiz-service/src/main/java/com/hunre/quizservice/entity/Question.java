package com.hunre.quizservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private QuestionType type = QuestionType.SINGLE_CHOICE;

    @Builder.Default
    @Column(name = "score", precision = 5, scale = 2, nullable = false)
    private BigDecimal score = new BigDecimal("1.00");

    @Builder.Default
    @Column(name = "position", nullable = false)
    private Integer position = 0;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<AnswerOption> options = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.score == null) {
            this.score = new BigDecimal("1.00");
        }
        if (this.position == null) {
            this.position = 0;
        }
        if (this.type == null) {
            this.type = QuestionType.SINGLE_CHOICE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addOption(AnswerOption option) {
        options.add(option);
        option.setQuestion(this);
    }

    public void removeOption(AnswerOption option) {
        options.remove(option);
        option.setQuestion(null);
    }
}
