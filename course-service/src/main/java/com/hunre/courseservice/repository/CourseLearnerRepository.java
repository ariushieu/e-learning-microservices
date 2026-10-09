package com.hunre.courseservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CourseLearnerRepository {
    private final JdbcTemplate jdbc;

    public boolean exists(Long courseId, Long userId) {
        return userId != null && Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT COUNT(*) > 0 FROM course_learners WHERE course_id = ? AND user_id = ?",
                Boolean.class, courseId, userId));
    }

    public List<Long> findUserIds(Long courseId) {
        return jdbc.queryForList("SELECT user_id FROM course_learners WHERE course_id = ? ORDER BY user_id",
                Long.class, courseId);
    }

    // Khóa chính kép chặn cả hai eventId khác nhau của cùng một học viên.
    public int insertIfAbsent(Long courseId, Long userId) {
        return jdbc.update("INSERT IGNORE INTO course_learners (course_id, user_id) VALUES (?, ?)",
                courseId, userId);
    }
}
