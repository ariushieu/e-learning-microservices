package com.hunre.enrollmentservice.leaderboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Đọc bảng xếp hạng theo kiểu cache-aside: có trong Redis thì trả luôn; chưa có thì tính từ
 * MySQL rồi cất vào Redis cho lần sau. Học viên hoàn thành bài hay khóa thì bảng bị xóa (xem
 * {@link LeaderboardCache#evictAfterCommit()}), lần đọc kế tiếp tính lại.
 */
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    /** Nguồn dữ liệu của câu trả lời, trả kèm để thấy được cache có hoạt động hay không. */
    public enum Source {
        /** Đọc thẳng từ Redis. */
        REDIS,
        /** Redis chưa có, vừa tính từ MySQL và cất vào Redis. */
        DATABASE,
        /** Redis không dùng được, tính từ MySQL và không cất được. */
        DATABASE_ONLY
    }

    public record Result(LeaderboardPeriod period, Instant since, Source source, long totalLearners,
                         List<LeaderboardEntry> entries, LeaderboardEntry me) {}

    private final LeaderboardQuery query;
    private final LeaderboardCache cache;

    public Result get(LeaderboardPeriod period, int limit, Long userId) {
        Instant now = Instant.now();
        String key = period.redisKey(now);
        Instant since = period.since(now);

        var cached = cache.read(key, limit, userId);
        if (cached.isPresent()) {
            var slice = cached.get();
            return new Result(period, since, Source.REDIS, slice.total(), slice.top(), slice.me());
        }

        List<LeaderboardEntry> all = query.rank(since);
        Source source = cache.write(key, all) ? Source.DATABASE : Source.DATABASE_ONLY;
        LeaderboardEntry me = userId == null ? null
                : all.stream().filter(e -> e.userId().equals(userId)).findFirst().orElse(null);
        return new Result(period, since, source, all.size(), all.subList(0, Math.min(limit, all.size())), me);
    }
}
