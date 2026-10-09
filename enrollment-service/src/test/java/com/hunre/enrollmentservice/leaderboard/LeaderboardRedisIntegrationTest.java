package com.hunre.enrollmentservice.leaderboard;

import com.hunre.enrollmentservice.entity.Enrollment;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import com.hunre.enrollmentservice.repository.CertificateRepository;
import com.hunre.enrollmentservice.repository.EnrollmentRepository;
import com.hunre.enrollmentservice.repository.LessonProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Chạy với Redis thật nếu máy có (docker compose up -d), bỏ qua nếu không. Dùng database 15 để
 * không đụng bảng xếp hạng của stack demo ở database 0.
 */
@SpringBootTest(properties = {"app.outbox.publisher.enabled=false", "spring.data.redis.database=15",
        "spring.datasource.url=jdbc:h2:mem:leaderboard_redis;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"})
class LeaderboardRedisIntegrationTest {
    @Autowired LeaderboardService service;
    @Autowired LeaderboardCache cache;
    @Autowired StringRedisTemplate redis;
    @Autowired EnrollmentRepository enrollments;
    @Autowired LessonProgressRepository progress;
    @Autowired CertificateRepository certificates;

    @BeforeEach
    void requireRedis() {
        boolean up;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 6379), 300);
            up = true;
        } catch (Exception ex) {
            up = false;
        }
        assumeTrue(up, "Không có Redis ở localhost:6379");
        cache.evict();
        certificates.deleteAll(); progress.deleteAll(); enrollments.deleteAll();
    }

    @Test
    void readsFromRedisAfterTheFirstBuildAndRebuildsAfterEviction() {
        enrollments.save(Enrollment.builder().userId(7L).courseId(1L).learnerName("Bảy")
                .status(EnrollmentStatus.COMPLETED).completedAt(Instant.now()).build());
        enrollments.save(Enrollment.builder().userId(8L).courseId(1L).learnerName("Tám | có gạch")
                .status(EnrollmentStatus.COMPLETED).completedAt(Instant.now()).build());

        var first = service.get(LeaderboardPeriod.ALL, 10, 8L);
        assertThat(first.source()).isEqualTo(LeaderboardService.Source.DATABASE);
        assertThat(redis.opsForZSet().zCard("leaderboard:all")).isEqualTo(2);

        var second = service.get(LeaderboardPeriod.ALL, 10, 8L);
        assertThat(second.source()).isEqualTo(LeaderboardService.Source.REDIS);
        // Redis trả đúng như MySQL, kể cả thứ tự khi bằng điểm và tên có ký tự phân cách.
        assertThat(second.entries()).isEqualTo(first.entries());
        assertThat(second.me()).isEqualTo(first.me());
        assertThat(second.me().name()).isEqualTo("Tám | có gạch");
        assertThat(second.me().rank()).isEqualTo(2);

        enrollments.save(Enrollment.builder().userId(9L).courseId(1L).learnerName("Chín")
                .status(EnrollmentStatus.COMPLETED).completedAt(Instant.now()).build());
        cache.evictAfterCommit();
        var third = service.get(LeaderboardPeriod.ALL, 10, null);
        assertThat(third.source()).isEqualTo(LeaderboardService.Source.DATABASE);
        assertThat(third.totalLearners()).isEqualTo(3);
    }

    @Test
    void emptyBoardIsCachedToo() {
        assertThat(service.get(LeaderboardPeriod.WEEK, 10, 1L).source()).isEqualTo(LeaderboardService.Source.DATABASE);
        var cached = service.get(LeaderboardPeriod.WEEK, 10, 1L);
        assertThat(cached.source()).isEqualTo(LeaderboardService.Source.REDIS);
        assertThat(cached.entries()).isEmpty();
        assertThat(cached.me()).isNull();
    }
}
