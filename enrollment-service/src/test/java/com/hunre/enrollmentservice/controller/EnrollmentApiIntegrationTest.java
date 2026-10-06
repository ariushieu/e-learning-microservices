package com.hunre.enrollmentservice.controller;

import com.hunre.enrollmentservice.client.CourseLessonClient;
import com.hunre.enrollmentservice.entity.CourseSnapshot;
import com.hunre.enrollmentservice.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** JWT filter + controller + service + database; chỉ giả lập lời gọi sang course-service. */
@SpringBootTest(properties = {"elearning.security.enabled=true", "app.outbox.publisher.enabled=false",
        "elearning.security.jwt-secret=" + EnrollmentApiIntegrationTest.SECRET,
        "spring.datasource.url=jdbc:h2:mem:enrollment_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"})
@AutoConfigureMockMvc
class EnrollmentApiIntegrationTest {
    static final String SECRET = "enrollment-api-integration-test-secret-at-least-32-bytes";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired CourseSnapshotRepository snapshots;
    @Autowired EnrollmentRepository enrollments;
    @Autowired LessonProgressRepository progress;
    @Autowired CertificateRepository certificates;
    @Autowired OutboxEventRepository outbox;
    @MockitoBean CourseLessonClient lessons;

    @BeforeEach
    void prepare() {
        certificates.deleteAll(); progress.deleteAll(); enrollments.deleteAll(); outbox.deleteAll(); snapshots.deleteAll();
        snapshots.save(CourseSnapshot.builder().courseId(10L).title("Java").totalLessons(2).status("PUBLISHED").build());
    }

    @Test
    void lifecycleUsesTokenIdentityAndPathLessonId() throws Exception {
        long id = enroll(1);
        mvc.perform(get("/api/enrollments").param("userId", "999").header("Authorization", token(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].userId").value(1));
        mvc.perform(get("/api/enrollments").header("Authorization", token(2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isEmpty());
        mvc.perform(put("/api/lessons/101/progress").header("Authorization", token(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":10,\"lessonId\":999,\"userId\":999,\"status\":\"COMPLETED\",\"watchedSeconds\":30}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.lessonId").value(101));
        verify(lessons).validateLesson(10L, 101L);
        mvc.perform(get("/api/progress").param("courseId", "10").header("Authorization", token(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.progressPercent").value(50));
        mvc.perform(patch("/api/enrollments/{id}/status", id).header("Authorization", token(1))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        mvc.perform(delete("/api/enrollments").param("courseId", "10").header("Authorization", token(1)))
                .andExpect(status().isOk());
        assertThat(enrollments.count()).isZero();
        assertThat(progress.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void stalePublishedSnapshotCannotCreateOrReactivateEnrollment(boolean cancelled) throws Exception {
        if (cancelled) {
            long id = enroll(1);
            mvc.perform(patch("/api/enrollments/{id}/status", id).header("Authorization", token(1))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                    .andExpect(status().isOk());
        }
        long before = outbox.count();
        doThrow(new com.hunre.sharedcommon.exception.ResourceNotFoundException("khóa học", "id", 10L))
                .when(lessons).requirePublishedCourse(10L);
        mvc.perform(post("/api/enrollments").header("Authorization", token(1))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":10}"))
                .andExpect(status().isNotFound());
        assertThat(outbox.count()).isEqualTo(before);
        if (cancelled) assertThat(enrollments.findAll()).singleElement()
                .satisfies(row -> assertThat(row.getStatus().name()).isEqualTo("CANCELLED"));
        else assertThat(enrollments.count()).isZero();
    }

    @Test
    void courseOutageDoesNotCreateEnrollmentOrOutbox() throws Exception {
        doThrow(new com.hunre.sharedcommon.exception.BusinessException(
                com.hunre.sharedcommon.exception.ErrorCode.EXTERNAL_SERVICE_ERROR, "Course unavailable"))
                .when(lessons).requirePublishedCourse(10L);
        mvc.perform(post("/api/enrollments").header("Authorization", token(1))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":10}"))
                .andExpect(status().isBadGateway());
        assertThat(enrollments.count()).isZero();
        assertThat(outbox.count()).isZero();
    }

    @Test
    void ownershipAndCompletionCannotBeForged() throws Exception {
        long id = enroll(1);
        mvc.perform(patch("/api/enrollments/{id}/status", id).header("Authorization", token(2))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isForbidden());
        for (String state : List.of("ACTIVE", "COMPLETED")) {
            mvc.perform(patch("/api/enrollments/{id}/status", id).header("Authorization", token(1))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + state + "\"}"))
                    .andExpect(status().isUnprocessableEntity());
        }
        assertThat(certificates.count()).isZero();
    }

    @Test
    void invalidInputAndMissingResourcesReturnClientErrors() throws Exception {
        enroll(1);
        for (String body : List.of("{}", "{\"status\":null}", "{\"status\":\"UNKNOWN\"}")) {
            mvc.perform(patch("/api/enrollments/1/status").header("Authorization", token(1))
                            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        for (String path : List.of("/api/progress", "/api/progress?courseId=abc", "/api/progress?courseId=-1",
                "/api/enrollments?sort=doesNotExist")) {
            mvc.perform(get(path).header("Authorization", token(1))).andExpect(status().isBadRequest());
        }
        mvc.perform(delete("/api/enrollments").header("Authorization", token(1))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/enrollments/999999").header("Authorization", token(1))).andExpect(status().isNotFound());
        mvc.perform(post("/api/enrollments").header("Authorization", token(1)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":10}")).andExpect(status().isConflict());
        for (String body : List.of("{}", "{\"courseId\":10,\"status\":\"COMPLETED\",\"watchedSeconds\":-1}")) {
            mvc.perform(put("/api/lessons/101/progress").header("Authorization", token(1))
                            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/lessons/0/progress").header("Authorization", token(1))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":10,\"status\":\"COMPLETED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void writesRequireAuthentication() throws Exception {
        mvc.perform(get("/api/enrollments")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/lessons/101/progress").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":10,\"status\":\"COMPLETED\"}"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_INSTRUCTOR", "ROLE_ADMIN"})
    void everyAuthenticatedRoleCanLearnButCannotManageOtherUsers(String role) throws Exception {
        long studentEnrollment = enroll(1);
        String learner = token(3, role);
        var result = mvc.perform(post("/api/enrollments").header("Authorization", learner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":10}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.userId").value(3)).andReturn();
        long ownEnrollment = mapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
        mvc.perform(put("/api/lessons/101/progress").header("Authorization", learner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":10,\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/enrollments/{id}/status", studentEnrollment).header("Authorization", learner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/enrollments/{id}/status", ownEnrollment).header("Authorization", learner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/enrollments?courseId=10").header("Authorization", learner))
                .andExpect(status().isOk());
        mvc.perform(get("/api/enrollments/{id}", studentEnrollment).header("Authorization", token(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userId").value(1));
    }

    private long enroll(long user) throws Exception {
        var result = mvc.perform(post("/api/enrollments").header("Authorization", token(user))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":10}"))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    @Test
    void completedLessonAndCertificateSurviveLateOrRepeatedProgressUpdates() throws Exception {
        long id = enroll(1);
        putProgress(101, "COMPLETED", 60);
        var firstCompletedAt = progress.findByEnrollmentIdAndLessonId(id, 101L).orElseThrow().getCompletedAt();
        putProgress(101, "IN_PROGRESS", 10);
        assertThat(progress.findByEnrollmentIdAndLessonId(id, 101L).orElseThrow().getStatus().name())
                .isEqualTo("COMPLETED");
        mvc.perform(get("/api/progress?courseId=10").header("Authorization", token(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.progressPercent").value(50));

        putProgress(102, "COMPLETED", 60);
        var certificate = certificates.findByEnrollmentId(id).orElseThrow();
        var courseCompletedAt = enrollments.findById(id).orElseThrow().getCompletedAt();
        long events = outbox.count();
        putProgress(101, "IN_PROGRESS", 90);
        putProgress(101, "IN_PROGRESS", 5);
        putProgress(102, "COMPLETED", 1);

        var lesson = progress.findByEnrollmentIdAndLessonId(id, 101L).orElseThrow();
        assertThat(lesson.getStatus().name()).isEqualTo("COMPLETED");
        assertThat(lesson.getWatchedSeconds()).isEqualTo(90);
        assertThat(lesson.getCompletedAt()).isEqualTo(firstCompletedAt);
        assertThat(enrollments.findById(id).orElseThrow().getCompletedAt()).isEqualTo(courseCompletedAt);
        assertThat(outbox.count()).isEqualTo(events);
        assertThat(certificates.count()).isEqualTo(1);
        mvc.perform(get("/api/progress?courseId=10").header("Authorization", token(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.progressPercent").value(100))
                .andExpect(jsonPath("$.data.certificateCode").value(certificate.getCertificateCode()));
    }

    @Test
    void addingLessonsDoesNotRevokeAnAlreadyCompletedEnrollment() throws Exception {
        long id = enroll(1);
        putProgress(101, "COMPLETED", 60);
        putProgress(102, "COMPLETED", 60);
        var certificate = certificates.findByEnrollmentId(id).orElseThrow();
        var completedAt = enrollments.findById(id).orElseThrow().getCompletedAt();
        long events = outbox.count();
        var snapshot = snapshots.findById(10L).orElseThrow();
        snapshot.setTotalLessons(3);
        snapshots.save(snapshot);
        putProgress(101, "IN_PROGRESS", 120);
        mvc.perform(get("/api/progress?courseId=10").header("Authorization", token(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.progressPercent").value(100))
                .andExpect(jsonPath("$.data.totalLessonsCount").value(3))
                .andExpect(jsonPath("$.data.certificateCode").value(certificate.getCertificateCode()));
        assertThat(enrollments.findById(id).orElseThrow().getCompletedAt()).isEqualTo(completedAt);
        assertThat(outbox.count()).isEqualTo(events);
    }

    @Test
    void concurrentFirstUpdatesAreSerializedAndDoNotLoseCompletionOrWatchTime() throws Exception {
        long id = enroll(1);
        var snapshot = snapshots.findById(10L).orElseThrow();
        snapshot.setTotalLessons(1);
        snapshots.save(snapshot);
        CountDownLatch completing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch lateRequestStarted = new CountDownLatch(1);
        doAnswer(invocation -> {
            completing.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return null;
        }).doNothing().when(lessons).validateLesson(10L, 101L);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var complete = executor.submit(() -> { putProgress(101, "COMPLETED", 90); return null; });
            assertThat(completing.await(5, TimeUnit.SECONDS)).isTrue();
            var late = executor.submit(() -> {
                lateRequestStarted.countDown();
                putProgress(101, "IN_PROGRESS", 10);
                return null;
            });
            assertThat(lateRequestStarted.await(5, TimeUnit.SECONDS)).isTrue();
            // Request thứ hai phải đợi transaction hoàn thành thay vì đọc trạng thái cũ.
            assertThatThrownBy(() -> late.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();
            complete.get(5, TimeUnit.SECONDS);
            late.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(progress.findAllByEnrollmentId(id)).singleElement().satisfies(lesson -> {
            assertThat(lesson.getStatus().name()).isEqualTo("COMPLETED");
            assertThat(lesson.getWatchedSeconds()).isEqualTo(90);
        });
        assertThat(enrollments.findById(id).orElseThrow().getStatus().name()).isEqualTo("COMPLETED");
        assertThat(certificates.count()).isEqualTo(1);
        assertThat(outbox.findAll()).filteredOn(event -> "enrollment.completed".equals(event.getEventType())).hasSize(1);
        assertThat(outbox.findAll()).filteredOn(event -> "certificate.issued".equals(event.getEventType())).hasSize(1);
    }

    private void putProgress(long lessonId, String state, int watchedSeconds) throws Exception {
        mvc.perform(put("/api/lessons/{id}/progress", lessonId).header("Authorization", token(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":10,\"status\":\"" + state + "\",\"watchedSeconds\":" + watchedSeconds + "}"))
                .andExpect(status().isOk());
    }

    private String token(long user, String... roles) {
        return "Bearer " + Jwts.builder().subject(Long.toString(user))
                .claim("roles", roles.length == 0 ? List.of("ROLE_STUDENT") : List.of(roles))
                .expiration(Date.from(Instant.now().plusSeconds(120)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
