package com.hunre.courseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

/** Thông báo của giảng viên tới học viên; không sửa sau khi đăng vì đã gửi đi rồi. */
@Entity
@Table(name = "course_announcements")
@Getter @Setter @NoArgsConstructor
public class CourseAnnouncement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "course_id", nullable = false)
    private Long courseId;
    @Column(name = "author_id", nullable = false)
    private Long authorId;
    @Column(name = "author_name", length = 150)
    private String authorName;
    @Column(nullable = false, length = 150)
    private String title;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(name = "recipient_count", nullable = false)
    private int recipientCount;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
