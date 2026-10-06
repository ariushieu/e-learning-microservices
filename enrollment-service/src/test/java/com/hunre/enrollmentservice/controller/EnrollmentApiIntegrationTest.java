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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

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

    private String token(long user, String... roles) {
        return "Bearer " + Jwts.builder().subject(Long.toString(user))
                .claim("roles", roles.length == 0 ? List.of("ROLE_STUDENT") : List.of(roles))
                .expiration(Date.from(Instant.now().plusSeconds(120)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
