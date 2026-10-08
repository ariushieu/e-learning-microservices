package com.hunre.enrollmentservice.controller;

import com.hunre.enrollmentservice.client.CourseLessonClient;
import com.hunre.enrollmentservice.entity.*;
import com.hunre.enrollmentservice.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"elearning.security.enabled=true", "app.outbox.publisher.enabled=false",
        "elearning.security.jwt-secret=" + CourseLearnerSummaryIntegrationTest.SECRET,
        "spring.datasource.url=jdbc:h2:mem:course_summary;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"})
@AutoConfigureMockMvc
class CourseLearnerSummaryIntegrationTest {
    static final String SECRET = "course-summary-integration-test-secret-at-least-32-bytes";
    static final String PATH = "/api/courses/10/learners/summary";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired CourseSnapshotRepository snapshots;
    @Autowired EnrollmentRepository enrollments;
    @Autowired LessonProgressRepository progress;
    @Autowired CertificateRepository certificates;
    @Autowired OutboxEventRepository outbox;
    @Autowired EntityManagerFactory entityManagerFactory;
    @MockitoBean CourseLessonClient lessons;

    @BeforeEach
    void prepare() {
        certificates.deleteAll(); progress.deleteAll(); enrollments.deleteAll(); outbox.deleteAll(); snapshots.deleteAll();
        snapshots.save(CourseSnapshot.builder().courseId(10L).instructorId(100L)
                .title("Two lessons").totalLessons(2).status("PUBLISHED").build());
        snapshots.save(CourseSnapshot.builder().courseId(20L).instructorId(200L)
                .title("Other course").totalLessons(2).status("PUBLISHED").build());
    }

    @Test
    void realProgressWritesMatchAssignmentAndIgnoreCancelledProgress() throws Exception {
        enroll(1); enroll(2); enroll(3);
        long cancelled = enroll(4);
        complete(1, 101); complete(1, 102);
        complete(2, 101); complete(4, 101);
        mvc.perform(patch("/api/enrollments/{id}/status", cancelled)
                        .header("Authorization", student(4)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());
        // Neither another course's progress nor its certificate may leak into this summary.
        var other = seed(99, 20, EnrollmentStatus.COMPLETED, "100");
        seedProgress(other, 101, LessonProgressStatus.COMPLETED);
        certificates.save(Certificate.builder().enrollmentId(other.getId()).certificateCode("OTHER").build());

        var response = mvc.perform(get(PATH).header("Authorization", owner())
                        .param("status", "CANCELLED").param("page", "99").param("size", "1"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.active").value(2))
                .andExpect(jsonPath("$.data.completed").value(1))
                .andExpect(jsonPath("$.data.cancelled").value(1))
                .andExpect(jsonPath("$.data.averageProgress").value(50))
                .andExpect(jsonPath("$.data.completionRate").value(33.33))
                .andExpect(jsonPath("$.data.certificatesIssued").value(1))
                .andExpect(jsonPath("$.data.lessons.length()").value(2))
                .andExpect(jsonPath("$.data.lessons[0].lessonId").value(101))
                .andExpect(jsonPath("$.data.lessons[0].completedCount").value(2))
                .andExpect(jsonPath("$.data.lessons[0].completionRate").value(66.67))
                .andExpect(jsonPath("$.data.lessons[1].lessonId").value(102))
                .andExpect(jsonPath("$.data.lessons[1].completedCount").value(1))
                .andExpect(jsonPath("$.data.lessons[1].completionRate").value(33.33)).andReturn();
        var data = mapper.readTree(response.getResponse().getContentAsString()).path("data");
        assertThat(data.propertyNames()).containsExactlyInAnyOrder("active", "completed", "cancelled",
                "averageProgress", "completionRate", "certificatesIssued", "lessons");
        assertThat(data.path("lessons").get(0).propertyNames())
                .containsExactlyInAnyOrder("lessonId", "completedCount", "completionRate");
    }

    @Test
    void emptyAndCancelledOnlyCoursesReturnFiniteZeroRates() throws Exception {
        for (int cancelled = 0; cancelled <= 1; cancelled++) {
            mvc.perform(get(PATH).header("Authorization", owner())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.active").value(0))
                    .andExpect(jsonPath("$.data.completed").value(0))
                    .andExpect(jsonPath("$.data.cancelled").value(cancelled))
                    .andExpect(jsonPath("$.data.averageProgress").value(0))
                    .andExpect(jsonPath("$.data.completionRate").value(0))
                    .andExpect(jsonPath("$.data.certificatesIssued").value(0))
                    .andExpect(jsonPath("$.data.lessons").isEmpty());
            if (cancelled == 0) seedProgress(seed(1, 10, EnrollmentStatus.CANCELLED, "50"),
                    101, LessonProgressStatus.COMPLETED);
        }
        verifyNoInteractions(lessons);
    }

    @Test
    void unfinishedLessonsDoNotCountAndProgressUsesDecimalRounding() throws Exception {
        var first = seed(1, 10, EnrollmentStatus.ACTIVE, "33.33");
        seed(2, 10, EnrollmentStatus.ACTIVE, "0");
        seedProgress(first, 103, LessonProgressStatus.IN_PROGRESS);
        mvc.perform(get(PATH).header("Authorization", owner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageProgress").value(16.67))
                .andExpect(jsonPath("$.data.completionRate").value(0))
                .andExpect(jsonPath("$.data.lessons[0].lessonId").value(103))
                .andExpect(jsonPath("$.data.lessons[0].completedCount").value(0))
                .andExpect(jsonPath("$.data.lessons[0].completionRate").value(0));
    }

    @Test
    void certificatesCountIssuedRecordsInsteadOfCompletedEnrollments() throws Exception {
        seed(1, 10, EnrollmentStatus.COMPLETED, "100");
        mvc.perform(get(PATH).header("Authorization", owner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(1))
                .andExpect(jsonPath("$.data.completionRate").value(100))
                .andExpect(jsonPath("$.data.certificatesIssued").value(0));
    }

    @Test
    void anonymousAndInvalidTokensAreRejectedByEnrollmentService() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlySnapshotOwnerWithInstructorRoleOrAdminCanRead() throws Exception {
        for (String auth : List.of(student(1), token(200, "ROLE_INSTRUCTOR"), student(100))) {
            mvc.perform(get(PATH).header("Authorization", auth).param("instructorId", "100").param("userId", "100"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get(PATH).header("Authorization", token(300, "ROLE_ADMIN"))).andExpect(status().isOk());
        var snapshot = snapshots.findById(10L).orElseThrow();
        snapshot.setInstructorId(200L); snapshots.save(snapshot);
        mvc.perform(get(PATH).header("Authorization", owner())).andExpect(status().isForbidden());
        mvc.perform(get(PATH).header("Authorization", token(200, "ROLE_INSTRUCTOR"))).andExpect(status().isOk());
    }

    @Test
    void missingSnapshotIs404AndArchivedCourseRemainsReadable() throws Exception {
        mvc.perform(get("/api/courses/999/learners/summary").header("Authorization", owner()))
                .andExpect(status().isNotFound());
        var snapshot = snapshots.findById(10L).orElseThrow();
        snapshot.setStatus("ARCHIVED"); snapshots.save(snapshot);
        mvc.perform(get(PATH).header("Authorization", owner())).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void invalidCourseIdsAre400(String id) throws Exception {
        mvc.perform(get("/api/courses/" + id + "/learners/summary").header("Authorization", owner()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void summaryUsesConstantQueriesWithoutLoadingLearnerEntities() throws Exception {
        for (int i = 1; i <= 30; i++) {
            seedProgress(seed(i, 10, EnrollmentStatus.ACTIVE, "50"), 101, LessonProgressStatus.COMPLETED);
        }
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            mvc.perform(get(PATH).header("Authorization", owner())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.active").value(30))
                    .andExpect(jsonPath("$.data.lessons[0].completedCount").value(30));
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(4); // snapshot + three aggregates
            for (Class<?> entity : List.of(Enrollment.class, LessonProgress.class, Certificate.class)) {
                assertThat(statistics.getEntityStatistics(entity.getName()).getLoadCount()).isZero();
            }
            verifyNoInteractions(lessons);
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    private Enrollment seed(long userId, long courseId, EnrollmentStatus status, String percent) {
        return enrollments.save(Enrollment.builder().userId(userId).courseId(courseId).status(status)
                .progressPercent(new BigDecimal(percent)).build());
    }

    private void seedProgress(Enrollment enrollment, long lessonId, LessonProgressStatus status) {
        progress.save(LessonProgress.builder().enrollment(enrollment).lessonId(lessonId).status(status).build());
    }

    private long enroll(long userId) throws Exception {
        var response = mvc.perform(post("/api/enrollments").header("Authorization", student(userId))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":10}"))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(response.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private void complete(long userId, long lessonId) throws Exception {
        mvc.perform(put("/api/lessons/{id}/progress", lessonId).header("Authorization", student(userId))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":10,\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());
    }

    private String owner() { return token(100, "ROLE_INSTRUCTOR"); }
    private String student(long id) { return token(id, "ROLE_STUDENT"); }
    private String token(long id, String role) {
        return "Bearer " + Jwts.builder().subject(Long.toString(id)).claim("fullName", "Learner " + id)
                .claim("roles", List.of(role)).expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
