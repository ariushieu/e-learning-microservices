package com.hunre.courseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

/** Một khóa trong danh sách yêu thích của một người. Ghi/đọc qua CourseWishlistRepository (JDBC). */
@Entity
@Table(name = "course_wishlist", indexes = @Index(name = "idx_course_wishlist_course", columnList = "course_id"))
@IdClass(CourseWishlistItem.Key.class)
@Getter
@NoArgsConstructor
public class CourseWishlistItem {
    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private Long userId;
        private Long courseId;
    }
}
