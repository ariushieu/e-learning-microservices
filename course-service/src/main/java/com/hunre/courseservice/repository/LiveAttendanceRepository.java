package com.hunre.courseservice.repository;

import com.hunre.courseservice.dto.response.LiveAttendanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Sổ điểm danh buổi học trực tuyến, ghi mỗi khi một người vào hoặc rời phòng. */
@Repository
@RequiredArgsConstructor
public class LiveAttendanceRepository {
    private final JdbcTemplate jdbc;

    public void recordJoin(Long sessionId, Long userId, String fullName, boolean host, Instant at) {
        if (updateJoin(sessionId, userId, fullName) > 0) return;
        try {
            jdbc.update("""
                    INSERT INTO live_session_attendance
                        (session_id, user_id, full_name, host, first_joined_at, join_count, attended_seconds)
                    VALUES (?, ?, ?, ?, ?, 1, 0)""", sessionId, userId, fullName, host, Timestamp.from(at));
        } catch (DuplicateKeyException e) {
            // Cùng người mở hai tab cùng lúc: tab kia vừa chèn xong, chỉ cần cộng lượt vào.
            updateJoin(sessionId, userId, fullName);
        }
    }

    public void recordLeave(Long sessionId, Long userId, long seconds, Instant at) {
        jdbc.update("""
                UPDATE live_session_attendance SET attended_seconds = attended_seconds + ?, last_left_at = ?
                WHERE session_id = ? AND user_id = ?""", Math.max(seconds, 0), Timestamp.from(at), sessionId, userId);
    }

    public List<LiveAttendanceResponse> findBySession(Long sessionId) {
        return jdbc.query("""
                SELECT user_id, full_name, host, first_joined_at, last_left_at, join_count, attended_seconds
                FROM live_session_attendance WHERE session_id = ? ORDER BY host DESC, first_joined_at, user_id""",
                (rs, i) -> new LiveAttendanceResponse(rs.getLong("user_id"), rs.getString("full_name"),
                        rs.getBoolean("host"), rs.getTimestamp("first_joined_at").toInstant(),
                        rs.getTimestamp("last_left_at") == null ? null : rs.getTimestamp("last_left_at").toInstant(),
                        rs.getInt("join_count"), rs.getLong("attended_seconds")), sessionId);
    }

    /** Số học viên (không tính giảng viên) đã vào từng buổi. */
    public Map<Long, Integer> countLearners(Collection<Long> sessionIds) {
        Map<Long, Integer> counts = new HashMap<>();
        if (sessionIds.isEmpty()) return counts;
        String marks = String.join(",", java.util.Collections.nCopies(sessionIds.size(), "?"));
        jdbc.query("SELECT session_id, COUNT(*) AS n FROM live_session_attendance WHERE host = 0 AND session_id IN ("
                        + marks + ") GROUP BY session_id",
                rs -> { counts.put(rs.getLong("session_id"), rs.getInt("n")); }, sessionIds.toArray());
        return counts;
    }

    private int updateJoin(Long sessionId, Long userId, String fullName) {
        return jdbc.update("""
                UPDATE live_session_attendance SET join_count = join_count + 1, full_name = COALESCE(?, full_name)
                WHERE session_id = ? AND user_id = ?""", fullName, sessionId, userId);
    }
}
