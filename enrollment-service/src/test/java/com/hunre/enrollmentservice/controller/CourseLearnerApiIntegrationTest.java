package com.hunre.enrollmentservice.controller;

import com.hunre.enrollmentservice.client.CourseLessonClient;
import com.hunre.enrollmentservice.entity.CourseSnapshot;
import com.hunre.enrollmentservice.entity.Enrollment;
import com.hunre.enrollmentservice.entity.EnrollmentStatus;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real JWT, MVC, JPA and progress writes; only the course HTTP dependency is stubbed. */
@SpringBootTest(properties = {"elearning.security.enabled=true", "app.outbox.publisher.enabled=false",
        "elearning.security.jwt-secret=" + CourseLearnerApiIntegrationTest.SECRET,
        "spring.datasource.url=jdbc:h2:mem:course_learners;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"})
@AutoConfigureMockMvc
class CourseLearnerApiIntegrationTest {
    static final String SECRET = "course-learner-api-integration-test-secret-at-least-32-bytes";
    static final String PATH = "/api/courses/10/learners";
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
        snapshots.save(CourseSnapshot.builder().courseId(10L).instructorId(100L)
                .title("Java").totalLessons(2).status("PUBLISHED").build());
        snapshots.save(CourseSnapshot.builder().courseId(20L).instructorId(200L)
                .title("Other course").totalLessons(2).status("PUBLISHED").build());
    }

    @Test
    void instructorSeesOnlyOwnCourseWithLiveProgressAndCertificate() throws Exception {
        long student = enroll(1, "Nguyễn Văn S", "ROLE_STUDENT");
        long colleague = enroll(200, "Giảng viên B", "ROLE_INSTRUCTOR");
        enrollments.save(Enrollment.builder().userId(2L).courseId(20L).learnerName("Hidden").build());
        complete(101);
        mvc.perform(get(PATH).param("sort", "progressPercent,desc").header("Authorization", owner()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.content[0].enrollmentId").value(student))
                .andExpect(jsonPath("$.data.content[0].learnerName").value("Nguyễn Văn S"))
                .andExpect(jsonPath("$.data.content[0].progressPercent").value(50))
                .andExpect(jsonPath("$.data.content[1].enrollmentId").value(colleague))
                .andExpect(jsonPath("$.data.content[1].progressPercent").value(0));
        complete(102);
        var response = mvc.perform(get(PATH).param("status", "COMPLETED").header("Authorization", owner()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].progressPercent").value(100))
                .andExpect(jsonPath("$.data.content[0].completedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].lastAccessedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].certificateCode")
                        .value(certificates.findByEnrollmentId(student).orElseThrow().getCertificateCode())).andReturn();
        var row = mapper.readTree(response.getResponse().getContentAsString()).path("data").path("content").get(0);
        assertThat(row.propertyNames()).containsExactlyInAnyOrder("enrollmentId", "userId", "learnerName", "status",
                "progressPercent", "enrolledAt", "lastAccessedAt", "completedAt", "certificateCode");
    }

    @Test
    void serviceRejectsAnonymousAndInvalidTokensDespitePublicGatewayPrefix() throws Exception {
        enroll(1, "S", "ROLE_STUDENT");
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
    }

    @Test
    void studentsAndOtherInstructorsCannotReadOrForgeOwnerIdentity() throws Exception {
        for (String token : List.of(token(1, "S", "ROLE_STUDENT"), token(200, "B", "ROLE_INSTRUCTOR"),
                token(100, "Former instructor", "ROLE_STUDENT"))) {
            mvc.perform(get(PATH).param("instructorId", "100").param("userId", "100")
                            .header("Authorization", token)).andExpect(status().isForbidden());
        }
        mvc.perform(get(PATH).header("Authorization", token(300, "Admin", "ROLE_ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    void missingSnapshotIs404AndArchivedCourseRemainsReadableToOwner() throws Exception {
        mvc.perform(get("/api/courses/999/learners").header("Authorization", owner())).andExpect(status().isNotFound());
        var snapshot = snapshots.findById(10L).orElseThrow();
        snapshot.setStatus("ARCHIVED"); snapshots.save(snapshot);
        mvc.perform(get(PATH).header("Authorization", owner()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void namesComeFromJwtOnEnrollmentAndReactivationOnly() throws Exception {
        long id = enroll(1, "  Nguyễn Văn S  ", "ROLE_STUDENT");
        assertThat(enrollments.findById(id).orElseThrow().getLearnerName()).isEqualTo("Nguyễn Văn S");
        mvc.perform(get(PATH).param("learnerName", "Forged").header("Authorization", owner()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].learnerName").value("Nguyễn Văn S"));
        mvc.perform(patch("/api/enrollments/{id}/status", id).header("Authorization", token(1, "S", "ROLE_STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());
        long sameId = enroll(1, "Tên mới của S", "ROLE_STUDENT");
        assertThat(sameId).isEqualTo(id);
        assertThat(enrollments.findById(id).orElseThrow().getLearnerName()).isEqualTo("Tên mới của S");
        assertThat(outbox.count()).isEqualTo(2);
    }

    @Test
    void legacyNamesRemainNullAndReadsNeverBackfillFromInstructorToken() throws Exception {
        var legacy = enrollments.save(Enrollment.builder().userId(5L).courseId(10L).build());
        mvc.perform(get(PATH).header("Authorization", owner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].learnerName").isEmpty())
                .andExpect(jsonPath("$.data.content[0].userId").value(5))
                .andExpect(jsonPath("$.data.content[0].certificateCode").isEmpty());
        assertThat(enrollments.findById(legacy.getId()).orElseThrow().getLearnerName()).isNull();
    }

    @Test
    void filteringSortingAndPaginationKeepTiesStableAndTotalsCorrect() throws Exception {
        var when = Instant.parse("2026-10-01T00:00:00Z");
        var first = enrollments.save(Enrollment.builder().userId(1L).courseId(10L).enrolledAt(when)
                .status(EnrollmentStatus.ACTIVE).progressPercent(BigDecimal.valueOf(50)).build());
        var second = enrollments.save(Enrollment.builder().userId(2L).courseId(10L).enrolledAt(when)
                .status(EnrollmentStatus.CANCELLED).build());
        mvc.perform(get(PATH).param("size", "1").header("Authorization", owner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].enrollmentId").value(second.getId()))
                .andExpect(jsonPath("$.data.totalElements").value(2)).andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.first").value(true)).andExpect(jsonPath("$.data.last").value(false));
        mvc.perform(get(PATH).param("size", "1").param("page", "1").header("Authorization", owner()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].enrollmentId").value(first.getId()))
                .andExpect(jsonPath("$.data.last").value(true));
        mvc.perform(get(PATH).param("status", "CANCELLED").header("Authorization", owner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1)).andExpect(jsonPath("$.data.content[0].status").value("CANCELLED"));
        mvc.perform(get(PATH).param("status", "ACTIVE").header("Authorization", owner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].enrollmentId").value(first.getId()));
        mvc.perform(get(PATH).param("status", "COMPLETED").header("Authorization", owner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get(PATH).param("sort", "progressPercent,asc").header("Authorization", owner()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].enrollmentId").value(second.getId()));
        mvc.perform(get(PATH).param("page", "99").header("Authorization", owner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty()).andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"status=UNKNOWN", "page=-1", "page=abc", "size=0", "size=101", "size=abc",
            "sort=learnerName", "sort=userId", "sort=courseId", "sort=progressPercent,desc&sort=createdAt"})
    void invalidQueriesAre400(String query) throws Exception {
        mvc.perform(get(PATH + "?" + query).header("Authorization", owner())).andExpect(status().isBadRequest());
    }

    @Test
    void invalidCourseIdIs400() throws Exception {
        for (String id : List.of("0", "-1", "abc")) {
            mvc.perform(get("/api/courses/" + id + "/learners").header("Authorization", owner()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void missingOrInvalidJwtNameCannotWriteEnrollment() throws Exception {
        for (String name : new String[] {null, " ", "x".repeat(151)}) {
            mvc.perform(post("/api/enrollments").header("Authorization", token(1, name, "ROLE_STUDENT"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":10,\"learnerName\":\"Forged\"}"))
                    .andExpect(status().isUnauthorized());
        }
        assertThat(enrollments.count()).isZero();
        assertThat(outbox.count()).isZero();
    }

    private long enroll(long id, String name, String role) throws Exception {
        var response = mvc.perform(post("/api/enrollments").header("Authorization", token(id, name, role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":10,\"learnerName\":\"Forged\",\"fullName\":\"Forged\"}"))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(response.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private void complete(long lesson) throws Exception {
        mvc.perform(put("/api/lessons/{id}/progress", lesson).header("Authorization", token(1, "Nguyễn Văn S", "ROLE_STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":10,\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());
    }

    private String owner() { return token(100, "Giảng viên A", "ROLE_INSTRUCTOR"); }

    private String token(long id, String name, String role) {
        return "Bearer " + Jwts.builder().subject(Long.toString(id)).claim("fullName", name)
                .claim("email", "private@example.com").claim("roles", List.of(role))
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
