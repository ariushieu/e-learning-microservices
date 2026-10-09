package com.hunre.enrollmentservice.controller;

import com.hunre.enrollmentservice.entity.*;
import com.hunre.enrollmentservice.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"elearning.security.enabled=true", "app.outbox.publisher.enabled=false",
        "elearning.security.jwt-secret=" + InstructorAnalyticsIntegrationTest.SECRET, "spring.data.redis.port=1",
        "spring.datasource.url=jdbc:h2:mem:analytics;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"})
@AutoConfigureMockMvc
class InstructorAnalyticsIntegrationTest {
    static final String SECRET = "instructor-analytics-test-secret-at-least-32-bytes";
    @Autowired MockMvc mvc;
    @Autowired CourseSnapshotRepository snapshots;
    @Autowired EnrollmentRepository enrollments;
    @Autowired LessonProgressRepository progress;
    @Autowired CertificateRepository certificates;
    @Autowired OutboxEventRepository outbox;

    @BeforeEach
    void seed() {
        certificates.deleteAll(); progress.deleteAll(); enrollments.deleteAll(); outbox.deleteAll(); snapshots.deleteAll();
        snapshots.save(CourseSnapshot.builder().courseId(10L).instructorId(100L).title("Java").totalLessons(2).status("PUBLISHED").build());
        snapshots.save(CourseSnapshot.builder().courseId(11L).instructorId(100L).title("GIS").totalLessons(2).status("PUBLISHED").build());
        snapshots.save(CourseSnapshot.builder().courseId(20L).instructorId(200L).title("Khóa khác").totalLessons(2).status("PUBLISHED").build());

        Instant now = Instant.now();
        Instant longAgo = now.minus(Duration.ofDays(60));
        // Java: một người xong (100%), một người đang học (50%), một người đã hủy.
        var done = enroll(1, 10, EnrollmentStatus.COMPLETED, "100", now, now);
        lesson(done, 1, now);
        lesson(done, 2, now);
        enroll(2, 10, EnrollmentStatus.ACTIVE, "50", longAgo, null);
        enroll(3, 10, EnrollmentStatus.CANCELLED, "0", now, null);
        // GIS: người 1 học thêm khóa này, chưa bắt đầu.
        enroll(1, 11, EnrollmentStatus.ACTIVE, "0", now, null);
        // Khóa của giảng viên khác: không được tính.
        enroll(4, 20, EnrollmentStatus.COMPLETED, "100", now, now);
    }

    @Test
    void instructorSeesOnlyOwnCoursesWithDailyBuckets() throws Exception {
        String today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString();
        mvc.perform(get("/api/instructor/analytics").param("days", "7").header("Authorization", token(100, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.days").value(7))
                .andExpect(jsonPath("$.data.totals.courses").value(2))
                .andExpect(jsonPath("$.data.totals.learners").value(2))
                .andExpect(jsonPath("$.data.totals.enrollments").value(3))
                .andExpect(jsonPath("$.data.totals.completed").value(1))
                .andExpect(jsonPath("$.data.totals.completionRate").value(33.33))
                .andExpect(jsonPath("$.data.totals.averageProgress").value(50.0))
                // Lượt hủy hôm nay vẫn là một lượt ghi danh mới trong kỳ.
                .andExpect(jsonPath("$.data.totals.newEnrollments").value(3))
                .andExpect(jsonPath("$.data.totals.activeLearners").value(1))
                .andExpect(jsonPath("$.data.totals.lessonsCompleted").value(2))
                .andExpect(jsonPath("$.data.daily.length()").value(7))
                .andExpect(jsonPath("$.data.daily[6].date").value(today))
                .andExpect(jsonPath("$.data.daily[6].enrollments").value(3))
                .andExpect(jsonPath("$.data.daily[6].completions").value(1))
                .andExpect(jsonPath("$.data.daily[6].lessonsCompleted").value(2))
                .andExpect(jsonPath("$.data.courses[0].title").value("Java"))
                .andExpect(jsonPath("$.data.courses[0].enrollments").value(2))
                .andExpect(jsonPath("$.data.courses[0].cancelled").value(1))
                .andExpect(jsonPath("$.data.courses[0].completionRate").value(50.0))
                .andExpect(jsonPath("$.data.courses[1].title").value("GIS"));
    }

    @Test
    void adminSeesEverythingAndOthersAreRejected() throws Exception {
        mvc.perform(get("/api/instructor/analytics").header("Authorization", token(1, "ROLE_ADMIN")))
                .andExpect(jsonPath("$.data.totals.courses").value(3))
                .andExpect(jsonPath("$.data.daily.length()").value(30));
        mvc.perform(get("/api/instructor/analytics").header("Authorization", token(300, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totals.courses").value(0))
                .andExpect(jsonPath("$.data.courses.length()").value(0));
        mvc.perform(get("/api/instructor/analytics").header("Authorization", token(1, "ROLE_STUDENT")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/instructor/analytics").param("days", "200").header("Authorization", token(100, "ROLE_INSTRUCTOR")))
                .andExpect(status().isBadRequest());
    }

    private Enrollment enroll(long user, long course, EnrollmentStatus status, String progressPercent, Instant enrolledAt, Instant completedAt) {
        return enrollments.save(Enrollment.builder().userId(user).courseId(course).learnerName("Học viên " + user)
                .status(status).progressPercent(new BigDecimal(progressPercent)).enrolledAt(enrolledAt)
                .completedAt(completedAt).lastAccessedAt(enrolledAt).build());
    }

    private void lesson(Enrollment e, long lessonId, Instant at) {
        progress.save(LessonProgress.builder().enrollment(e).lessonId(lessonId).status(LessonProgressStatus.COMPLETED).completedAt(at).build());
    }

    static String token(long userId, String role) {
        return "Bearer " + Jwts.builder().subject(Long.toString(userId)).claim("roles", List.of(role))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
