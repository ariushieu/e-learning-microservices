package com.hunre.courseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

/** Mỗi học viên chỉ được tính một lần trong lịch sử của một khóa học. */
@Entity
@Table(name = "course_learners")
@IdClass(CourseLearner.Key.class)
@Getter
@NoArgsConstructor
public class CourseLearner {
    @Id
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private Long courseId;
        private Long userId;
    }
}
