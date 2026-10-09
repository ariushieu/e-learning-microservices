package com.hunre.courseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

/** Câu hỏi của học viên trong một bài học. Số câu trả lời được tính lại mỗi lần thêm/xóa trả lời. */
@Entity
@Table(name = "lesson_questions")
@Getter @Setter @NoArgsConstructor
public class LessonQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;
    @Column(name = "course_id", nullable = false)
    private Long courseId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "author_name", length = 150)
    private String authorName;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(name = "answer_count", nullable = false)
    private int answerCount;
    /** Đã có giảng viên của khóa hoặc quản trị viên trả lời; dùng cho hộp "chưa trả lời" của giảng viên. */
    @Column(name = "instructor_answered", nullable = false)
    private boolean instructorAnswered;
    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
