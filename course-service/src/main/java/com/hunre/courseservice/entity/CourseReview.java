package com.hunre.courseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "course_reviews", uniqueConstraints = @UniqueConstraint(name = "uk_course_reviews_course_user", columnNames = {"course_id", "user_id"}))
@Getter @Setter @NoArgsConstructor
public class CourseReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "course_id", nullable = false)
    private Long courseId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Byte rating;
    @Column(length = 2000)
    private String comment;
    @Column(name = "author_name", length = 150)
    private String authorName;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
