package com.hunre.courseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * Một dòng điểm danh buổi học trực tuyến. Đọc ghi bằng SQL ở LiveAttendanceRepository (cộng dồn
 * nguyên tử); entity này để Hibernate kiểm bảng khớp migration và dựng bảng khi test.
 */
@Entity
@Table(name = "live_session_attendance")
@IdClass(LiveAttendance.Key.class)
@Getter
@NoArgsConstructor
public class LiveAttendance {
    @Id
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(nullable = false)
    private boolean host;

    @Column(name = "first_joined_at", nullable = false)
    private Instant firstJoinedAt;

    @Column(name = "last_left_at")
    private Instant lastLeftAt;

    @Column(name = "join_count", nullable = false)
    private int joinCount = 1;

    @Column(name = "attended_seconds", nullable = false)
    private long attendedSeconds;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private Long sessionId;
        private Long userId;
    }
}
