package com.hunre.courseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "lesson_answers")
@Getter @Setter @NoArgsConstructor
public class LessonAnswer {
    /** Vai trò của người trả lời đối với khóa học, chốt lúc trả lời. */
    public enum AuthorRole { INSTRUCTOR, ADMIN, STUDENT }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "question_id", nullable = false)
    private Long questionId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "author_name", length = 150)
    private String authorName;
    @Enumerated(EnumType.STRING)
    @Column(name = "author_role", nullable = false, length = 20)
    private AuthorRole authorRole;
    @Column(nullable = false, length = 2000)
    private String content;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
