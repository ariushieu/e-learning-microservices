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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=live-session-test-secret-key-with-at-least-32-chars",
        "elearning.security.public-paths=GET:/api/courses/**"
})
@AutoConfigureMockMvc
class LiveSessionIntegrationTest {
    static final String SECRET = "live-session-test-secret-key-with-at-least-32-chars";
    static final Pattern PEER_ID = Pattern.compile("\"peerId\":\"([0-9a-f-]{36})\"");
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
        jdbc.update("DELETE FROM live_session_attendance WHERE session_id IN (SELECT id FROM live_sessions WHERE course_id=?)", id);
        jdbc.update("DELETE FROM live_sessions WHERE course_id=?", id);
        jdbc.update("DELETE FROM course_learners WHERE course_id=?", id);
        courses.deleteById(id);
    }

    Long course(CourseStatus status) {
        return courses.save(Course.builder().instructorId(50L).title("Live course")
                .slug(UUID.randomUUID().toString()).status(status).build()).getId();
    }

    String token(long user, String role) {
        return "Bearer " + Jwts.builder().subject(Long.toString(user)).claim("roles", List.of(role))
                .claim("fullName", "Người dùng " + user).claim("email", "private@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    ResultActions call(String method, String url, long user, String role, String body) throws Exception {
        var request = request(org.springframework.http.HttpMethod.valueOf(method), url)
                .header("Authorization", token(user, role));
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(request);
    }

    String scheduleBody(Instant at) {
        return "{\"title\":\" Ôn tập chương 1 \",\"description\":\"Mang theo câu hỏi\",\"scheduledAt\":\"" + at
                + "\",\"durationMinutes\":60}";
    }

    Long schedule() throws Exception {
        var body = call("POST", "/api/courses/" + id + "/live-sessions", 50, "ROLE_INSTRUCTOR",
                scheduleBody(Instant.now().plus(1, ChronoUnit.HOURS)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("data").get("id").asLong();
    }

    List<String> eventTypes() {
        return jdbc.queryForList("SELECT event_type FROM outbox_events WHERE aggregate_id=? ORDER BY id",
                String.class, String.valueOf(id));
    }

    @Test void instructorSchedulesAndEveryLearnerIsNotified() throws Exception {
        call("POST", "/api/courses/" + id + "/live-sessions", 50, "ROLE_INSTRUCTOR",
                scheduleBody(Instant.now().plus(1, ChronoUnit.HOURS)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Ôn tập chương 1"))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.hostName").value("Người dùng 50"))
                .andExpect(jsonPath("$.data.canHost").value(true));

        var payloads = jdbc.queryForList("SELECT payload FROM outbox_events WHERE aggregate_id=? AND event_type=?",
                String.class, String.valueOf(id), EventTypes.LIVE_SESSION_SCHEDULED);
        assertThat(payloads).hasSize(1);
        var event = json.readTree(payloads.get(0));
        assertThat(event.get("courseTitle").asString()).isEqualTo("Live course");
        assertThat(event.get("durationMinutes").asInt()).isEqualTo(60);
        assertThat(event.get("recipientIds").toString()).isEqualTo("[60,61]");
    }

    @Test void onlyManagersScheduleAndOnlyLearnersOrManagersSeeTheSchedule() throws Exception {
        String body = scheduleBody(Instant.now().plus(1, ChronoUnit.HOURS));
        String url = "/api/courses/" + id + "/live-sessions";
        call("POST", url, 60, "ROLE_STUDENT", body).andExpect(status().isForbidden());
        call("POST", url, 51, "ROLE_INSTRUCTOR", body).andExpect(status().isForbidden());
        call("POST", url, 50, "ROLE_INSTRUCTOR", scheduleBody(Instant.now().minus(1, ChronoUnit.HOURS)))
                .andExpect(status().isBadRequest());
        call("POST", url, 50, "ROLE_INSTRUCTOR", body.replace("60}", "5}")).andExpect(status().isBadRequest());
        Long draft = course(CourseStatus.DRAFT);
        try {
            call("POST", "/api/courses/" + draft + "/live-sessions", 50, "ROLE_INSTRUCTOR", body)
                    .andExpect(status().isBadRequest());
        } finally {
            courses.deleteById(draft);
        }
        call("POST", url, 99, "ROLE_ADMIN", body).andExpect(status().isCreated());

        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        call("GET", url, 70, "ROLE_STUDENT", null).andExpect(status().isForbidden());
        call("GET", url, 60, "ROLE_STUDENT", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].canHost").value(false))
                .andExpect(jsonPath("$.data[0].attendeeCount").doesNotExist());
        call("GET", url, 50, "ROLE_INSTRUCTOR", null)
                .andExpect(jsonPath("$.data[0].attendeeCount").value(0));
    }

    @Test void lifecycleMovesForwardOnlyAndAnnouncesStartAndCancel() throws Exception {
        Long live = schedule();
        call("POST", "/api/live-sessions/" + live + "/start", 60, "ROLE_STUDENT", null).andExpect(status().isForbidden());
        call("POST", "/api/live-sessions/" + live + "/end", 50, "ROLE_INSTRUCTOR", null).andExpect(status().isUnprocessableContent());
        call("POST", "/api/live-sessions/" + live + "/start", 50, "ROLE_INSTRUCTOR", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("LIVE"));
        // Bấm lần nữa không báo học viên lần hai.
        call("POST", "/api/live-sessions/" + live + "/start", 50, "ROLE_INSTRUCTOR", null).andExpect(status().isOk());
        call("POST", "/api/live-sessions/" + live + "/cancel", 50, "ROLE_INSTRUCTOR", null)
                .andExpect(status().isUnprocessableContent());
        call("POST", "/api/live-sessions/" + live + "/end", 50, "ROLE_INSTRUCTOR", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ENDED"));
        call("POST", "/api/live-sessions/" + live + "/start", 50, "ROLE_INSTRUCTOR", null)
                .andExpect(status().isUnprocessableContent());

        Long cancelled = schedule();
        call("POST", "/api/live-sessions/" + cancelled + "/cancel", 50, "ROLE_INSTRUCTOR", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertThat(eventTypes()).containsExactly(EventTypes.LIVE_SESSION_SCHEDULED, EventTypes.LIVE_SESSION_STARTED,
                EventTypes.LIVE_SESSION_SCHEDULED, EventTypes.LIVE_SESSION_CANCELLED);
    }

    @Test void roomRelaysSignalsChatAndRecordsAttendance() throws Exception {
        Long session = schedule();
        String room = "/api/live-sessions/" + session + "/room";
        call("GET", room, 60, "ROLE_STUDENT", null).andExpect(status().isUnprocessableContent());
        call("POST", "/api/live-sessions/" + session + "/start", 50, "ROLE_INSTRUCTOR", null).andExpect(status().isOk());
        call("GET", room, 70, "ROLE_STUDENT", null).andExpect(status().isForbidden());

        MockHttpServletResponse student = call("GET", room + "?cam=false", 60, "ROLE_STUDENT", null)
                .andExpect(request().asyncStarted()).andReturn().getResponse();
        String studentPeer = peerId(student);
        MockHttpServletResponse teacher = call("GET", room, 50, "ROLE_INSTRUCTOR", null)
                .andExpect(request().asyncStarted()).andReturn().getResponse();
        String teacherPeer = peerId(teacher);

        assertThat(body(teacher)).contains("\"peerId\":\"" + studentPeer + "\"").contains("\"cam\":false");
        assertThat(body(student)).contains("event:peer-joined").contains("\"host\":true");

        call("POST", room + "/signal", 50, "ROLE_INSTRUCTOR", "{\"from\":\"" + teacherPeer
                + "\",\"messages\":[{\"to\":\"" + studentPeer + "\",\"type\":\"offer\",\"data\":{\"sdp\":\"v=0\"}}]}")
                .andExpect(status().isOk());
        assertThat(body(student)).contains("event:signal").contains("\"from\":\"" + teacherPeer + "\"")
                .contains("\"sdp\":\"v=0\"");

        // Không mạo danh được kết nối của người khác, học viên không tắt micro người khác được.
        call("POST", room + "/chat", 60, "ROLE_STUDENT", "{\"from\":\"" + teacherPeer + "\",\"text\":\"x\"}")
                .andExpect(status().isForbidden());
        call("POST", room + "/mute", 60, "ROLE_STUDENT", "{\"from\":\"" + studentPeer + "\",\"target\":\"" + teacherPeer + "\"}")
                .andExpect(status().isForbidden());
        call("POST", room + "/mute", 50, "ROLE_INSTRUCTOR", "{\"from\":\"" + teacherPeer + "\",\"target\":\"" + studentPeer + "\"}")
                .andExpect(status().isOk());
        assertThat(body(student)).contains("event:mute-request");

        call("POST", room + "/chat", 60, "ROLE_STUDENT", "{\"from\":\"" + studentPeer + "\",\"text\":\" Em chào cô \"}")
                .andExpect(status().isOk());
        assertThat(body(teacher)).contains("event:chat").contains("\"text\":\"Em chào cô\"");
        call("POST", room + "/state", 60, "ROLE_STUDENT", "{\"from\":\"" + studentPeer
                + "\",\"mic\":false,\"cam\":false,\"hand\":true,\"screen\":false}").andExpect(status().isOk());
        assertThat(body(teacher)).contains("event:peer-updated").contains("\"hand\":true");

        call("GET", "/api/live-sessions/" + session, 60, "ROLE_STUDENT", null)
                .andExpect(jsonPath("$.data.participantCount").value(2));
        call("POST", "/api/live-sessions/" + session + "/end", 50, "ROLE_INSTRUCTOR", null).andExpect(status().isOk());
        assertThat(body(student)).contains("event:ended");

        call("GET", "/api/live-sessions/" + session + "/attendance", 60, "ROLE_STUDENT", null).andExpect(status().isForbidden());
        call("GET", "/api/live-sessions/" + session + "/attendance", 50, "ROLE_INSTRUCTOR", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].userId").value(50))
                .andExpect(jsonPath("$.data[0].host").value(true))
                .andExpect(jsonPath("$.data[1].userId").value(60))
                .andExpect(jsonPath("$.data[1].lastLeftAt").isNotEmpty());
        call("GET", "/api/courses/" + id + "/live-sessions", 50, "ROLE_INSTRUCTOR", null)
                .andExpect(jsonPath("$.data[0].attendeeCount").value(1))
                .andExpect(jsonPath("$.data[0].participantCount").value(0));
    }

    static String body(MockHttpServletResponse response) throws Exception {
        return response.getContentAsString(StandardCharsets.UTF_8);
    }

    static String peerId(MockHttpServletResponse response) throws Exception {
        var matcher = PEER_ID.matcher(body(response));
        assertThat(matcher.find()).as("welcome event: %s", body(response)).isTrue();
        return matcher.group(1);
    }
}
