package com.hunre.enrollmentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "course_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSnapshot {

    @Id
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "slug", length = 220)
    private String slug;

    @Column(name = "instructor_id")
    private Long instructorId;

    @Column(name = "total_lessons", nullable = false)
    @Builder.Default
    private Integer totalLessons = 0;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @PrePersist
    public void prePersist() {
        if (syncedAt == null) {
            syncedAt = Instant.now();
        }
        if (totalLessons == null) {
            totalLessons = 0;
        }
    }
}
