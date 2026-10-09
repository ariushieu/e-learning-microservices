package com.hunre.enrollmentservice.controller;

import com.hunre.enrollmentservice.entity.Enrollment;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
import com.hunre.enrollmentservice.entity.LessonProgress;
import com.hunre.enrollmentservice.entity.LessonProgressStatus;
import com.hunre.enrollmentservice.repository.CertificateRepository;
import com.hunre.enrollmentservice.repository.EnrollmentRepository;
import com.hunre.enrollmentservice.repository.LessonProgressRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Bảng xếp hạng khi không có Redis (cổng 1 không ai nghe): vẫn trả đúng kết quả, tính từ MySQL.
 * Đường đi qua Redis nằm ở {@code LeaderboardRedisIntegrationTest}.
 */
@SpringBootTest(properties = {"elearning.security.enabled=true", "app.outbox.publisher.enabled=false",
        "elearning.security.jwt-secret=" + LeaderboardIntegrationTest.SECRET,
        "spring.data.redis.port=1",
        "spring.datasource.url=jdbc:h2:mem:leaderboard;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"})
@AutoConfigureMockMvc
class LeaderboardIntegrationTest {
    static final String SECRET = "leaderboard-integration-test-secret-at-least-32-bytes";
    @Autowired MockMvc mvc;
    @Autowired EnrollmentRepository enrollments;
    @Autowired LessonProgressRepository progress;
    @Autowired CertificateRepository certificates;

    @BeforeEach
    void seed() {
        certificates.deleteAll(); progress.deleteAll(); enrollments.deleteAll();
        Instant now = Instant.now();
        Instant lastMonth = now.minus(Duration.ofDays(40));

        // An: xong khóa 10 (100) + 3 bài (30) = 130.
        var an = enroll(1, 10, "An", EnrollmentStatus.COMPLETED, now);
        lessons(an, 3, now);
        // Bình: 5 bài = 50. Lượt đã hủy ở khóa 20 có 4 bài, không tính.
        lessons(enroll(2, 10, "Bình", EnrollmentStatus.ACTIVE, null), 5, now);
        lessons(enroll(2, 20, "Bình", EnrollmentStatus.CANCELLED, null), 4, now);
        // Dũng: cũng 50, bằng Bình nên id lớn hơn đứng sau.
        lessons(enroll(4, 10, "Dũng", EnrollmentStatus.ACTIVE, null), 5, now);
        // Chi: 2 bài từ tháng trước = 20, không có điểm trong tuần này.
        lessons(enroll(3, 10, "Chi", EnrollmentStatus.ACTIVE, null), 2, lastMonth);
        // Em: ghi danh nhưng chưa học bài nào, không lên bảng.
        enroll(5, 10, "Em", EnrollmentStatus.ACTIVE, null);
    }

    @Test
    void allTimeRanksByPointsThenCoursesThenId() throws Exception {
        mvc.perform(get("/api/leaderboard").header("Authorization", token(3)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.period").value("ALL"))
                .andExpect(jsonPath("$.data.source").value("DATABASE_ONLY"))
                .andExpect(jsonPath("$.data.totalLearners").value(4))
                .andExpect(jsonPath("$.data.entries[0].name").value("An"))
                .andExpect(jsonPath("$.data.entries[0].points").value(130))
                .andExpect(jsonPath("$.data.entries[0].completedCourses").value(1))
                .andExpect(jsonPath("$.data.entries[0].completedLessons").value(3))
                .andExpect(jsonPath("$.data.entries[1].name").value("Bình"))
                .andExpect(jsonPath("$.data.entries[1].points").value(50))
                .andExpect(jsonPath("$.data.entries[2].name").value("Dũng"))
                .andExpect(jsonPath("$.data.entries[2].rank").value(3))
                .andExpect(jsonPath("$.data.entries[3].name").value("Chi"))
                .andExpect(jsonPath("$.data.me.rank").value(4))
                .andExpect(jsonPath("$.data.me.points").value(20));
    }

    @Test
    void weekOnlyCountsThisWeekAndLimitCutsTheList() throws Exception {
        mvc.perform(get("/api/leaderboard").param("period", "week").param("limit", "2").header("Authorization", token(3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("WEEK"))
                .andExpect(jsonPath("$.data.totalLearners").value(3))
                .andExpect(jsonPath("$.data.entries.length()").value(2))
                .andExpect(jsonPath("$.data.entries[1].name").value("Bình"))
                // Chi không có điểm tuần này nên không có hạng.
                .andExpect(jsonPath("$.data.me").doesNotExist());
    }

    @Test
    void rejectsAnonymousAndBadParameters() throws Exception {
        mvc.perform(get("/api/leaderboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/leaderboard").param("period", "year").header("Authorization", token(1)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/leaderboard").param("limit", "51").header("Authorization", token(1)))
                .andExpect(status().isBadRequest());
    }

    private Enrollment enroll(long userId, long courseId, String name, EnrollmentStatus status, Instant completedAt) {
        return enrollments.save(Enrollment.builder().userId(userId).courseId(courseId).learnerName(name)
                .status(status).completedAt(completedAt).build());
    }

    private void lessons(Enrollment enrollment, int count, Instant completedAt) {
        for (int i = 1; i <= count; i++) {
            progress.save(LessonProgress.builder().enrollment(enrollment).lessonId(enrollment.getCourseId() * 100 + i)
                    .status(LessonProgressStatus.COMPLETED).completedAt(completedAt).build());
        }
        progress.save(LessonProgress.builder().enrollment(enrollment).lessonId(enrollment.getCourseId() * 100 + 99)
                .status(LessonProgressStatus.IN_PROGRESS).build());
    }

    static String token(long userId) {
        return "Bearer " + Jwts.builder().subject(Long.toString(userId)).claim("roles", List.of("ROLE_STUDENT"))
                .claim("fullName", "Học viên " + userId).expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
