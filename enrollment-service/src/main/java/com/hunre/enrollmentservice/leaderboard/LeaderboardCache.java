package com.hunre.enrollmentservice.leaderboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bảng xếp hạng đã tính sẵn trong Redis. Mỗi khoảng thời gian dùng ba khóa:
 *
 * <ul>
 *   <li>{@code <key>}: sorted set, member là userId, score mã hóa cả thứ tự xếp hạng (xem
 *       {@link #score}). Top N là một lệnh {@code ZREVRANGE}, hạng của một người là {@code ZREVRANK}.</li>
 *   <li>{@code <key>:info}: hash userId → "số bài|số khóa|tên" để hiện chi tiết.</li>
 *   <li>{@code <key>:built}: đánh dấu đã tính, để phân biệt "chưa tính" với "tính rồi mà chưa ai có điểm".</li>
 * </ul>
 *
 * <p>Mọi lỗi Redis đều nuốt ở đây và trả về "không có": người gọi tự tính từ MySQL.
 */
@Component
public class LeaderboardCache {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardCache.class);

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public LeaderboardCache(StringRedisTemplate redis,
                            @Value("${elearning.leaderboard.cache-ttl:10m}") Duration ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    /** Một phần bảng xếp hạng đọc từ Redis. */
    public record Slice(List<LeaderboardEntry> top, LeaderboardEntry me, long total) {}

    public Optional<Slice> read(String key, int limit, Long userId) {
        try {
            if (!Boolean.TRUE.equals(redis.hasKey(key + ":built"))) return Optional.empty();
            Set<String> ids = redis.opsForZSet().reverseRange(key, 0, limit - 1L);
            List<String> members = ids == null ? List.of() : new ArrayList<>(ids);
            List<Object> infos = members.isEmpty() ? List.of()
                    : redis.opsForHash().multiGet(key + ":info", new ArrayList<>(members));
            List<LeaderboardEntry> top = new ArrayList<>(members.size());
            for (int i = 0; i < members.size(); i++) {
                LeaderboardEntry entry = decode(i + 1, members.get(i), infos.get(i));
                if (entry == null) return Optional.empty();
                top.add(entry);
            }
            LeaderboardEntry me = null;
            if (userId != null) {
                Long rank = redis.opsForZSet().reverseRank(key, userId.toString());
                if (rank != null) {
                    me = decode(rank.intValue() + 1, userId.toString(), redis.opsForHash().get(key + ":info", userId.toString()));
                    if (me == null) return Optional.empty();
                }
            }
            Long total = redis.opsForZSet().zCard(key);
            return Optional.of(new Slice(top, me, total == null ? 0 : total));
        } catch (DataAccessException ex) {
            log.warn("Không đọc được bảng xếp hạng {} từ Redis, tính từ MySQL: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    /** Ghi đè cả bảng trong một MULTI/EXEC, người đọc không thấy bảng ghi dở. */
    public boolean write(String key, List<LeaderboardEntry> entries) {
        Set<TypedTuple<String>> tuples = new HashSet<>();
        Map<String, String> infos = new HashMap<>();
        for (LeaderboardEntry e : entries) {
            tuples.add(new DefaultTypedTuple<>(e.userId().toString(), score(e)));
            infos.put(e.userId().toString(), e.completedLessons() + "|" + e.completedCourses() + "|" + e.name());
        }
        try {
            redis.execute(new SessionCallback<List<Object>>() {
                @Override
                @SuppressWarnings({"unchecked", "rawtypes"})
                public List<Object> execute(RedisOperations ops) {
                    ops.multi();
                    ops.delete(List.of(key, key + ":info", key + ":built"));
                    if (!tuples.isEmpty()) {
                        ops.opsForZSet().add(key, tuples);
                        ops.opsForHash().putAll(key + ":info", infos);
                        ops.expire(key, ttl);
                        ops.expire(key + ":info", ttl);
                    }
                    ops.opsForValue().set(key + ":built", Instant.now().toString(), ttl);
                    return ops.exec();
                }
            });
            return true;
        } catch (DataAccessException ex) {
            log.warn("Không ghi được bảng xếp hạng {} vào Redis: {}", key, ex.getMessage());
            return false;
        }
    }

    /**
     * Xóa bảng đang giữ sau khi transaction ghi tiến độ commit xong, để lần đọc sau tính lại từ
     * dữ liệu mới. Xóa trước khi commit thì người đọc chen giữa sẽ tính lại từ dữ liệu cũ.
     */
    public void evictAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict();
                }
            });
        } else {
            evict();
        }
    }

    void evict() {
        Instant now = Instant.now();
        List<String> keys = new ArrayList<>();
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            String key = period.redisKey(now);
            keys.addAll(List.of(key, key + ":info", key + ":built"));
        }
        try {
            redis.delete(keys);
        } catch (DataAccessException ex) {
            // Không xóa được thì bảng cũ tự hết hạn theo TTL.
            log.warn("Không xóa được bảng xếp hạng trong Redis: {}", ex.getMessage());
        }
    }

    /**
     * Score sắp đúng thứ tự của {@link LeaderboardQuery#ORDER} khi đọc từ cao xuống thấp: điểm,
     * rồi số khóa đã xong, rồi id nhỏ hơn đứng trước. Các phần không chồng lên nhau và tổng vẫn
     * dưới 2^53, nên double giữ chính xác.
     */
    static double score(LeaderboardEntry e) {
        long courses = Math.min(e.completedCourses(), 999);
        long idPart = 999_999 - Math.min(e.userId(), 999_999);
        return e.points() * 1_000_000_000d + courses * 1_000_000d + idPart;
    }

    private static LeaderboardEntry decode(int rank, String member, Object info) {
        if (!(info instanceof String text)) return null;
        String[] parts = text.split("\\|", 3);
        if (parts.length < 3) return null;
        int lessons = Integer.parseInt(parts[0]);
        int courses = Integer.parseInt(parts[1]);
        return new LeaderboardEntry(rank, Long.valueOf(member), parts[2],
                LeaderboardEntry.pointsFor(lessons, courses), lessons, courses);
    }
}
