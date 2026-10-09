package com.hunre.enrollmentservice.leaderboard;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeaderboardPeriodTest {

    @Test
    void weekStartsMondayMidnightInVietnam() {
        // 23:30 UTC Chủ nhật 11/10/2026 là 6:30 sáng thứ Hai 12/10 giờ Hà Nội: đã sang tuần mới.
        Instant now = Instant.parse("2026-10-11T23:30:00Z");
        assertThat(LeaderboardPeriod.WEEK.since(now)).isEqualTo(Instant.parse("2026-10-11T17:00:00Z"));
        assertThat(LeaderboardPeriod.WEEK.redisKey(now)).isEqualTo("leaderboard:week:2026-W42");
        assertThat(LeaderboardPeriod.WEEK.redisKey(Instant.parse("2026-10-11T16:59:59Z"))).isEqualTo("leaderboard:week:2026-W41");
    }

    @Test
    void allTimeStartsAtEpoch() {
        assertThat(LeaderboardPeriod.ALL.since(Instant.now())).isEqualTo(Instant.EPOCH);
        assertThat(LeaderboardPeriod.ALL.redisKey(Instant.now())).isEqualTo("leaderboard:all");
    }

    @Test
    void parsesQueryValues() {
        assertThat(LeaderboardPeriod.parse(null)).isEqualTo(LeaderboardPeriod.ALL);
        assertThat(LeaderboardPeriod.parse(" Week ")).isEqualTo(LeaderboardPeriod.WEEK);
        assertThatThrownBy(() -> LeaderboardPeriod.parse("year")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scoreKeepsTheSameOrderAsTheDatabase() {
        var high = new LeaderboardEntry(0, 9L, "a", 130, 3, 1);
        var tieMoreCourses = new LeaderboardEntry(0, 5L, "b", 100, 0, 1);
        var tieFewerCourses = new LeaderboardEntry(0, 1L, "c", 100, 10, 0);
        var sameSmallerId = new LeaderboardEntry(0, 2L, "d", 50, 5, 0);
        var sameLargerId = new LeaderboardEntry(0, 4L, "e", 50, 5, 0);
        assertThat(LeaderboardCache.score(high)).isGreaterThan(LeaderboardCache.score(tieMoreCourses));
        assertThat(LeaderboardCache.score(tieMoreCourses)).isGreaterThan(LeaderboardCache.score(tieFewerCourses));
        assertThat(LeaderboardCache.score(sameSmallerId)).isGreaterThan(LeaderboardCache.score(sameLargerId));
    }
}
