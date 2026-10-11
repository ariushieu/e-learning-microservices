package com.hunre.courseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

/** Một buổi học trực tuyến của khóa học. Phòng học (ai đang ở trong) chỉ nằm trong bộ nhớ, xem LiveRoomRegistry. */
@Entity
@Table(name = "live_sessions")
@Getter @Setter @NoArgsConstructor
public class LiveSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "course_id", nullable = false)
    private Long courseId;
    @Column(name = "host_id", nullable = false)
    private Long hostId;
    @Column(name = "host_name", length = 150)
    private String hostName;
    @Column(nullable = false, length = 150)
    private String title;
    @Column(length = 2000)
    private String description;
    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LiveSessionStatus status = LiveSessionStatus.SCHEDULED;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "ended_at")
    private Instant endedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
