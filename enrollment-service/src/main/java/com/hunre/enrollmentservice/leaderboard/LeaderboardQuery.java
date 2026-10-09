package com.hunre.enrollmentservice.leaderboard;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tính bảng xếp hạng thẳng từ MySQL. Đây là nguồn sự thật; Redis chỉ giữ bản đã tính sẵn. */
@Component
@RequiredArgsConstructor
public class LeaderboardQuery {

    /** Điểm cao trước; bằng điểm thì ai xong nhiều khóa hơn trước, rồi tới tài khoản tạo trước (id nhỏ hơn). */
    static final Comparator<LeaderboardEntry> ORDER = Comparator
            .comparingInt(LeaderboardEntry::points).reversed()
            .thenComparing(Comparator.comparingInt(LeaderboardEntry::completedCourses).reversed())
            .thenComparing(LeaderboardEntry::userId);

    private final JdbcTemplate jdbc;

    /** Mọi học viên có điểm từ {@code since}, đã xếp hạng từ 1. */
    public List<LeaderboardEntry> rank(Instant since) {
        Timestamp from = Timestamp.from(since);
        Map<Long, int[]> counts = new HashMap<>();
        jdbc.query("""
                SELECT e.user_id, COUNT(*) FROM lesson_progress lp
                JOIN enrollments e ON e.id = lp.enrollment_id
                WHERE e.status <> 'CANCELLED' AND lp.status = 'COMPLETED' AND lp.completed_at >= ?
                GROUP BY e.user_id""",
                rs -> { counts.computeIfAbsent(rs.getLong(1), k -> new int[2])[0] = rs.getInt(2); }, from);
        jdbc.query("""
                SELECT user_id, COUNT(*) FROM enrollments
                WHERE status = 'COMPLETED' AND completed_at >= ?
                GROUP BY user_id""",
                rs -> { counts.computeIfAbsent(rs.getLong(1), k -> new int[2])[1] = rs.getInt(2); }, from);

        // Tên lưu theo từng lượt ghi danh; lấy tên ở lượt mới nhất vì người dùng có thể đã đổi tên.
        Map<Long, String> names = new HashMap<>();
        jdbc.query("SELECT user_id, learner_name FROM enrollments WHERE learner_name IS NOT NULL ORDER BY id",
                rs -> { names.put(rs.getLong(1), rs.getString(2)); });

        List<LeaderboardEntry> entries = new ArrayList<>();
        counts.forEach((userId, c) -> entries.add(new LeaderboardEntry(0, userId,
                names.getOrDefault(userId, "Học viên #" + userId), LeaderboardEntry.pointsFor(c[0], c[1]), c[0], c[1])));
        entries.sort(ORDER);
        List<LeaderboardEntry> ranked = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) ranked.add(entries.get(i).withRank(i + 1));
        return ranked;
    }
}
