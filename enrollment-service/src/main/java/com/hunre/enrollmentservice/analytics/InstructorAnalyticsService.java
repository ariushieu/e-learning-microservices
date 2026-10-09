package com.hunre.enrollmentservice.analytics;

import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Số liệu học tập trên các khóa của một giảng viên (quản trị viên: mọi khóa), tính từ dữ liệu
 * ghi danh và tiến độ của chính enrollment-service. Danh sách khóa và giảng viên lấy từ
 * {@code course_snapshots}, bản sao khóa học đồng bộ qua Kafka.
 *
 * <p>Ngày tính theo giờ Việt Nam. Lượt ghi danh đã hủy không tính vào học viên hay tỉ lệ hoàn thành.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorAnalyticsService {
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final NamedParameterJdbcTemplate jdbc;

    public record Totals(int courses, int learners, int enrollments, int completed, BigDecimal completionRate,
                         BigDecimal averageProgress, int newEnrollments, int activeLearners, int lessonsCompleted) {}

    public record Day(LocalDate date, int enrollments, int completions, int lessonsCompleted) {}

    public record CourseStats(Long courseId, String title, int enrollments, int cancelled, int completed,
                              BigDecimal completionRate, BigDecimal averageProgress, int newEnrollments) {}

    public record Result(int days, Instant since, Totals totals, List<Day> daily, List<CourseStats> courses) {}

    private record Row(Long courseId, Long userId, String status, BigDecimal progress,
                       Instant enrolledAt, Instant completedAt, Instant lastAccessedAt) {
        boolean cancelled() { return "CANCELLED".equals(status); }
    }

    public Result get(int days, AuthenticatedUser user) {
        if (user == null || user.userId() == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn cần đăng nhập");
        if (!user.hasAnyRole(Roles.INSTRUCTOR, Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ giảng viên hoặc quản trị viên xem được thống kê");
        }
        LocalDate today = LocalDate.now(ZONE);
        LocalDate firstDay = today.minusDays(days - 1L);
        Instant since = firstDay.atStartOfDay(ZONE).toInstant();

        var params = new MapSqlParameterSource();
        String filter = "";
        if (!user.hasRole(Roles.ADMIN)) {
            filter = " WHERE instructor_id = :instructorId";
            params.addValue("instructorId", user.userId());
        }
        Map<Long, String> titles = new LinkedHashMap<>();
        jdbc.query("SELECT course_id, title FROM course_snapshots" + filter + " ORDER BY course_id", params,
                rs -> { titles.put(rs.getLong(1), rs.getString(2)); });

        Map<LocalDate, int[]> byDay = new LinkedHashMap<>();
        for (LocalDate d = firstDay; !d.isAfter(today); d = d.plusDays(1)) byDay.put(d, new int[3]);
        if (titles.isEmpty()) {
            return new Result(days, since, new Totals(0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0),
                    days(byDay), List.of());
        }

        var ids = new MapSqlParameterSource("ids", titles.keySet()).addValue("since", Timestamp.from(since));
        List<Row> rows = jdbc.query("""
                SELECT course_id, user_id, status, progress_percent, enrolled_at, completed_at, last_accessed_at
                FROM enrollments WHERE course_id IN (:ids)""", ids, (rs, i) -> new Row(rs.getLong(1), rs.getLong(2),
                rs.getString(3), rs.getBigDecimal(4), instant(rs.getTimestamp(5)), instant(rs.getTimestamp(6)),
                instant(rs.getTimestamp(7))));
        List<Instant> lessons = jdbc.query("""
                SELECT lp.completed_at FROM lesson_progress lp JOIN enrollments e ON e.id = lp.enrollment_id
                WHERE e.course_id IN (:ids) AND lp.status = 'COMPLETED' AND lp.completed_at >= :since""",
                ids, (rs, i) -> instant(rs.getTimestamp(1)));

        for (Row r : rows) {
            bump(byDay, r.enrolledAt(), 0);
            if (!r.cancelled()) bump(byDay, r.completedAt(), 1);
        }
        lessons.forEach(t -> bump(byDay, t, 2));

        List<Row> live = rows.stream().filter(r -> !r.cancelled()).toList();
        Set<Long> learners = new HashSet<>();
        Set<Long> active = new HashSet<>();
        for (Row r : live) {
            learners.add(r.userId());
            if (r.lastAccessedAt() != null && !r.lastAccessedAt().isBefore(since)) active.add(r.userId());
        }
        int completed = (int) live.stream().filter(r -> "COMPLETED".equals(r.status())).count();
        int newEnrollments = (int) rows.stream().filter(r -> r.enrolledAt() != null && !r.enrolledAt().isBefore(since)).count();
        Totals totals = new Totals(titles.size(), learners.size(), live.size(), completed, rate(completed, live.size()),
                average(live), newEnrollments, active.size(), lessons.size());

        List<CourseStats> courses = new ArrayList<>();
        titles.forEach((courseId, title) -> {
            List<Row> all = rows.stream().filter(r -> r.courseId().equals(courseId)).toList();
            List<Row> courseLive = all.stream().filter(r -> !r.cancelled()).toList();
            int done = (int) courseLive.stream().filter(r -> "COMPLETED".equals(r.status())).count();
            courses.add(new CourseStats(courseId, title, courseLive.size(), all.size() - courseLive.size(), done,
                    rate(done, courseLive.size()), average(courseLive),
                    (int) all.stream().filter(r -> r.enrolledAt() != null && !r.enrolledAt().isBefore(since)).count()));
        });
        courses.sort(Comparator.comparingInt(CourseStats::enrollments).reversed().thenComparing(CourseStats::courseId));
        return new Result(days, since, totals, days(byDay), courses);
    }

    private static void bump(Map<LocalDate, int[]> byDay, Instant at, int index) {
        if (at == null) return;
        int[] counts = byDay.get(at.atZone(ZONE).toLocalDate());
        if (counts != null) counts[index]++;
    }

    private static List<Day> days(Map<LocalDate, int[]> byDay) {
        return byDay.entrySet().stream().map(e -> new Day(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2])).toList();
    }

    /** Phần trăm, hai chữ số thập phân. */
    private static BigDecimal rate(int part, int whole) {
        if (whole == 0) return BigDecimal.ZERO.setScale(2);
        return BigDecimal.valueOf(part * 100L).divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal average(List<Row> rows) {
        if (rows.isEmpty()) return BigDecimal.ZERO.setScale(2);
        BigDecimal sum = rows.stream().map(r -> r.progress() == null ? BigDecimal.ZERO : r.progress())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP);
    }

    private static Instant instant(Timestamp t) {
        return t == null ? null : t.toInstant();
    }
}
