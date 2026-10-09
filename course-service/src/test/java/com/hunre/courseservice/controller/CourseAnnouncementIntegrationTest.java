package com.hunre.courseservice.controller;

import com.hunre.courseservice.entity.*;
import com.hunre.courseservice.repository.*;
import com.hunre.sharedcommon.event.EventTypes;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=announcement-test-secret-key-with-at-least-32-chars",
        "elearning.security.public-paths=GET:/api/courses/**"
})
@AutoConfigureMockMvc
class CourseAnnouncementIntegrationTest {
    static final String SECRET = "announcement-test-secret-key-with-at-least-32-chars";
    @Autowired MockMvc mvc;
    @Autowired CourseRepository courses;
    @Autowired CourseLearnerRepository learners;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    Long id;

    @BeforeEach void seed() {
        id = course(CourseStatus.PUBLISHED);
        learners.insertIfAbsent(id, 60L);
        learners.insertIfAbsent(id, 61L);
    }

    @AfterEach void cleanup() {
        jdbc.update("DELETE FROM outbox_events WHERE aggregate_type='COURSE' AND aggregate_id=?", String.valueOf(id));
        jdbc.update("DELETE FROM course_announcements WHERE course_id=?", id);
        jdbc.update("DELETE FROM course_learners WHERE course_id=?", id);
        courses.deleteById(id);
    }

    Long course(CourseStatus status) {
        return courses.save(Course.builder().instructorId(50L).title("Announcement course")
                .slug(UUID.randomUUID().toString()).status(status).build()).getId();
    }

    String token(long user, String role) {
        return "Bearer " + Jwts.builder().subject(Long.toString(user)).claim("roles", List.of(role))
                .claim("fullName", "Người dùng " + user).claim("email", "private@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    String url(Long courseId) { return "/api/courses/" + courseId + "/announcements"; }

    org.springframework.test.web.servlet.ResultActions post(Long courseId, long user, String role, String body) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url(courseId))
                .header("Authorization", token(user, role))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test void instructorPostsToEveryLearnerThroughTheOutbox() throws Exception {
        post(id, 50, "ROLE_INSTRUCTOR", "{\"title\":\" Lịch thi \",\"content\":\" Thi vào\\n\\nthứ Hai \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Lịch thi"))
                .andExpect(jsonPath("$.data.content").value("Thi vào\n\nthứ Hai"))
                .andExpect(jsonPath("$.data.authorName").value("Người dùng 50"))
                .andExpect(jsonPath("$.data.recipientCount").value(2))
                .andExpect(jsonPath("$.message").value("Đã gửi thông báo tới 2 học viên"));

        var payloads = jdbc.queryForList("SELECT payload FROM outbox_events WHERE aggregate_id=? AND event_type=?",
                String.class, String.valueOf(id), EventTypes.COURSE_ANNOUNCEMENT_POSTED);
        assertThat(payloads).hasSize(1);
        var event = json.readTree(payloads.get(0));
        assertThat(event.get("courseTitle").asString()).isEqualTo("Announcement course");
        assertThat(event.get("preview").asString()).isEqualTo("Thi vào thứ Hai");
        assertThat(event.get("recipientIds").toString()).isEqualTo("[60,61]");
    }

    @Test void onlyManagersPostAndOnlyLearnersOrManagersRead() throws Exception {
        String body = "{\"title\":\"T\",\"content\":\"C\"}";
        post(id, 60, "ROLE_STUDENT", body).andExpect(status().isForbidden());
        post(id, 51, "ROLE_INSTRUCTOR", body).andExpect(status().isForbidden());
        post(id, 99, "ROLE_ADMIN", body).andExpect(status().isCreated());

        mvc.perform(get(url(id))).andExpect(status().isUnauthorized());
        mvc.perform(get(url(id)).header("Authorization", token(70, "ROLE_STUDENT"))).andExpect(status().isForbidden());
        mvc.perform(get(url(id)).header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("T"))
                // Học viên không cần biết lớp có bao nhiêu người.
                .andExpect(jsonPath("$.data.content[0].recipientCount").doesNotExist());
        mvc.perform(get(url(id)).header("Authorization", token(50, "ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.content[0].recipientCount").value(2));
    }

    @Test void draftCoursesAndBlankInputAreRejected() throws Exception {
        Long draft = course(CourseStatus.DRAFT);
        try {
            post(draft, 50, "ROLE_INSTRUCTOR", "{\"title\":\"T\",\"content\":\"C\"}").andExpect(status().isBadRequest());
        } finally {
            courses.deleteById(draft);
        }
        post(id, 50, "ROLE_INSTRUCTOR", "{\"title\":\"  \",\"content\":\"C\"}").andExpect(status().isBadRequest());
        post(id, 50, "ROLE_INSTRUCTOR", "{\"title\":\"T\",\"content\":\"" + "x".repeat(2001) + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test void courseWithoutLearnersSavesWithoutAnEvent() throws Exception {
        jdbc.update("DELETE FROM course_learners WHERE course_id=?", id);
        post(id, 50, "ROLE_INSTRUCTOR", "{\"title\":\"T\",\"content\":\"C\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recipientCount").value(0));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_events WHERE aggregate_id=? AND event_type=?",
                Integer.class, String.valueOf(id), EventTypes.COURSE_ANNOUNCEMENT_POSTED)).isZero();
    }

    @Test void managerDeletesFromTheCoursePage() throws Exception {
        post(id, 50, "ROLE_INSTRUCTOR", "{\"title\":\"T\",\"content\":\"C\"}").andExpect(status().isCreated());
        Long announcementId = jdbc.queryForObject("SELECT id FROM course_announcements WHERE course_id=?", Long.class, id);

        mvc.perform(delete(url(id) + "/" + announcementId).header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().isForbidden());
        mvc.perform(delete(url(id) + "/" + announcementId).header("Authorization", token(50, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk());
        mvc.perform(delete(url(id) + "/" + announcementId).header("Authorization", token(50, "ROLE_INSTRUCTOR")))
                .andExpect(status().isNotFound());
        mvc.perform(get(url(id)).header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}
