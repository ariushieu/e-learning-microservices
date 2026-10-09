package com.hunre.courseservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** Danh sách yêu thích chỉ là cặp (người dùng, khóa học), không cần entity riêng. */
@Repository
@RequiredArgsConstructor
public class CourseWishlistRepository {
    private final JdbcTemplate jdbc;

    /** Mới lưu trước. */
    public List<Long> findCourseIds(Long userId) {
        return jdbc.queryForList("SELECT course_id FROM course_wishlist WHERE user_id = ? ORDER BY created_at DESC, course_id DESC",
                Long.class, userId);
    }

    /** Thêm hai lần không lỗi, không tạo dòng thứ hai. */
    public void add(Long userId, Long courseId) {
        jdbc.update("INSERT IGNORE INTO course_wishlist (user_id, course_id, created_at) VALUES (?, ?, ?)",
                userId, courseId, Timestamp.from(Instant.now()));
    }

    public void remove(Long userId, Long courseId) {
        jdbc.update("DELETE FROM course_wishlist WHERE user_id = ? AND course_id = ?", userId, courseId);
    }
}
