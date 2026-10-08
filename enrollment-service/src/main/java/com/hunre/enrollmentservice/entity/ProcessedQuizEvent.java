package com.hunre.enrollmentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Sổ khử trùng lặp bền vững qua restart và nhiều bản enrollment-service. */
@Entity
@Table(name = "processed_quiz_events")
@Getter
@NoArgsConstructor
public class ProcessedQuizEvent {
    @Id
    @Column(name = "event_id", length = 36, nullable = false)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
