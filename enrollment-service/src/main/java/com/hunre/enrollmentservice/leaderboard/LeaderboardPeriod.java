package com.hunre.enrollmentservice.leaderboard;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

/**
 * Khoảng thời gian tính điểm. Tuần tính theo giờ Việt Nam, bắt đầu 0 giờ thứ Hai: học lúc 6 giờ
 * sáng thứ Hai giờ Hà Nội vẫn là tuần mới, dù theo UTC lúc đó còn là Chủ nhật.
 */
public enum LeaderboardPeriod {
    ALL, WEEK;

    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Mốc bắt đầu tính điểm; cả lịch sử thì là EPOCH. */
    public Instant since(Instant now) {
        if (this == ALL) return Instant.EPOCH;
        LocalDate monday = now.atZone(ZONE).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return monday.atStartOfDay(ZONE).toInstant();
    }

    /** Khóa Redis. Mỗi tuần một khóa riêng, nên sang tuần mới là tự có bảng trống. */
    public String redisKey(Instant now) {
        if (this == ALL) return "leaderboard:all";
        LocalDate day = now.atZone(ZONE).toLocalDate();
        return "leaderboard:week:%d-W%02d".formatted(day.get(IsoFields.WEEK_BASED_YEAR), day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    public static LeaderboardPeriod parse(String value) {
        if (value == null || value.isBlank()) return ALL;
        return switch (value.trim().toLowerCase()) {
            case "all" -> ALL;
            case "week" -> WEEK;
            default -> throw new IllegalArgumentException(value);
        };
    }
}
