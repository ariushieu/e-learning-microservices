package com.hunre.courseservice.controller;

import com.hunre.courseservice.client.EnrollmentAccessClient;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=" + LessonQuestionIntegrationTest.SECRET,
        "elearning.security.public-paths=GET:/api/courses/**,GET:/api/lessons/**"
})
@AutoConfigureMockMvc
class LessonQuestionIntegrationTest {
    static final String SECRET = "lesson-question-test-secret-key-with-at-least-32-chars";
    static final long INSTRUCTOR = 50, LEARNER = 60, OTHER_LEARNER = 61, STRANGER = 70, ADMIN = 99;
    @Autowired MockMvc mvc;
    @Autowired CourseRepository courses;
    @Autowired SectionRepository sections;
    @Autowired LessonRepository lessons;
    @Autowired LessonQuestionRepository questions;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockitoBean EnrollmentAccessClient enrollmentAccess;
    Course course;
    Lesson lesson;

    @BeforeEach void seed() {
        course = courses.save(Course.builder().instructorId(INSTRUCTOR).title("Java cơ bản")
                .slug(UUID.randomUUID().toString()).status(CourseStatus.PUBLISHED).build());
        Section section = sections.save(Section.builder().course(course).title("Chương 1").build());
        lesson = lessons.save(Lesson.builder().course(course).section(section).title("Vòng lặp").build());
        when(enrollmentAccess.hasEnrollment(anyLong(), anyLong())).thenReturn(false);
        when(enrollmentAccess.hasEnrollment(eq(course.getId()), eq(LEARNER))).thenReturn(true);
        when(enrollmentAccess.hasEnrollment(eq(course.getId()), eq(OTHER_LEARNER))).thenReturn(true);
    }

    @AfterEach void cleanup() {
        jdbc.update("DELETE FROM outbox_events WHERE aggregate_type='COURSE' AND aggregate_id=?", String.valueOf(course.getId()));
        jdbc.update("DELETE FROM lesson_questions WHERE course_id=?", course.getId());
        lessons.deleteById(lesson.getId());
        jdbc.update("DELETE FROM sections WHERE course_id=?", course.getId());
        courses.deleteById(course.getId());
    }

    String token(long user, String role) {
        return "Bearer " + Jwts.builder().subject(Long.toString(user)).claim("roles", List.of(role))
                .claim("fullName", "Người " + user).expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    String base() { return "/api/lessons/" + lesson.getId() + "/questions"; }

    ResultActions postJson(String path, long user, String role, String content) throws Exception {
        return mvc.perform(post(path).header("Authorization", token(user, role)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("content", content))));
    }

    long ask(long user, String content) throws Exception {
        var body = postJson(base(), user, "ROLE_STUDENT", content).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("id").asLong();
    }

    List<String> events(String type) {
        return jdbc.queryForList("SELECT payload FROM outbox_events WHERE aggregate_id=? AND event_type=? ORDER BY id",
                String.class, String.valueOf(course.getId()), type);
    }

    @Test void learnerAsksInstructorAnswersAndEachSideIsNotified() throws Exception {
        long q = ask(LEARNER, "  Khi nào dùng for-each?  ");
        assertThat(events(EventTypes.LESSON_QUESTION_POSTED)).singleElement().satisfies(p -> {
            var e = json.readTree(p);
            assertThat(e.get("instructorId").asLong()).isEqualTo(INSTRUCTOR);
            assertThat(e.get("askerName").asString()).isEqualTo("Người 60");
            assertThat(e.get("lessonTitle").asString()).isEqualTo("Vòng lặp");
        });

        // Một học viên khác trả lời trước: chưa tính là giảng viên đã trả lời.
        postJson(base() + "/" + q + "/answers", OTHER_LEARNER, "ROLE_STUDENT", "Khi chỉ đọc phần tử")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.answers[0].authorRole").value("STUDENT"))
                .andExpect(jsonPath("$.data.instructorAnswered").value(false));
        postJson(base() + "/" + q + "/answers", INSTRUCTOR, "ROLE_INSTRUCTOR", "Đúng rồi, và không cần chỉ số")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.answers.length()").value(2))
                .andExpect(jsonPath("$.data.answers[1].authorRole").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.data.instructorAnswered").value(true));
        assertThat(events(EventTypes.LESSON_QUESTION_ANSWERED)).hasSize(2)
                .allSatisfy(p -> assertThat(json.readTree(p).get("askerId").asLong()).isEqualTo(LEARNER));

        // Người hỏi tự trả lời câu của mình thì không tự báo cho mình.
        postJson(base() + "/" + q + "/answers", LEARNER, "ROLE_STUDENT", "Cảm ơn ạ").andExpect(status().isCreated());
        assertThat(events(EventTypes.LESSON_QUESTION_ANSWERED)).hasSize(2);

        mvc.perform(get(base()).header("Authorization", token(LEARNER, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content").value("Khi nào dùng for-each?"))
                .andExpect(jsonPath("$.data.content[0].mine").value(true))
                .andExpect(jsonPath("$.data.content[0].answers[0].deletable").value(false))
                .andExpect(jsonPath("$.data.content[0].answers[2].mine").value(true));
    }

    @Test void onlyPeopleWhoCanReadTheLessonTakePart() throws Exception {
        mvc.perform(get(base())).andExpect(status().isUnauthorized());
        mvc.perform(get(base()).header("Authorization", token(STRANGER, "ROLE_STUDENT"))).andExpect(status().isForbidden());
        postJson(base(), STRANGER, "ROLE_STUDENT", "Cho em hỏi").andExpect(status().isForbidden());
        postJson(base(), 51, "ROLE_INSTRUCTOR", "Giảng viên khác").andExpect(status().isForbidden());
        postJson(base(), LEARNER, "ROLE_STUDENT", " ").andExpect(status().isBadRequest());
        postJson(base(), LEARNER, "ROLE_STUDENT", "x".repeat(2001)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/lessons/999999/questions").header("Authorization", token(LEARNER, "ROLE_STUDENT")))
                .andExpect(status().isNotFound());

        // Giảng viên tự hỏi trong khóa của mình thì không báo cho chính mình.
        postJson(base(), INSTRUCTOR, "ROLE_INSTRUCTOR", "Ghim: hỏi tại đây").andExpect(status().isCreated());
        assertThat(events(EventTypes.LESSON_QUESTION_POSTED)).isEmpty();
    }

    @Test void authorsDeleteTheirOwnAndManagersModerate() throws Exception {
        long q = ask(LEARNER, "Câu hỏi");
        String body = postJson(base() + "/" + q + "/answers", INSTRUCTOR, "ROLE_INSTRUCTOR", "Trả lời")
                .andReturn().getResponse().getContentAsString();
        long a = json.readTree(body).path("data").path("answers").get(0).path("id").asLong();

        mvc.perform(delete(base() + "/" + q + "/answers/" + a).header("Authorization", token(LEARNER, "ROLE_STUDENT")))
                .andExpect(status().isForbidden());
        mvc.perform(delete(base() + "/" + q + "/answers/" + a).header("Authorization", token(ADMIN, "ROLE_ADMIN")))
                .andExpect(status().isOk());
        var question = questions.findById(q).orElseThrow();
        assertThat(question.getAnswerCount()).isZero();
        assertThat(question.isInstructorAnswered()).isFalse();

        mvc.perform(delete(base() + "/" + q).header("Authorization", token(OTHER_LEARNER, "ROLE_STUDENT")))
                .andExpect(status().isForbidden());
        mvc.perform(delete(base() + "/" + q).header("Authorization", token(LEARNER, "ROLE_STUDENT")))
                .andExpect(status().isOk());
        assertThat(questions.findById(q)).isEmpty();
    }

    @Test void instructorInboxFiltersUnansweredAcrossOwnCourses() throws Exception {
        long open = ask(LEARNER, "Chưa ai trả lời");
        long done = ask(OTHER_LEARNER, "Đã trả lời");
        postJson(base() + "/" + done + "/answers", INSTRUCTOR, "ROLE_INSTRUCTOR", "Xong").andExpect(status().isCreated());

        mvc.perform(get("/api/instructor/questions").param("answered", "false").header("Authorization", token(INSTRUCTOR, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(open))
                .andExpect(jsonPath("$.data.content[0].lessonTitle").value("Vòng lặp"))
                .andExpect(jsonPath("$.data.content[0].courseTitle").value("Java cơ bản"))
                .andExpect(jsonPath("$.data.content[0].deletable").value(true));
        mvc.perform(get("/api/instructor/questions/unanswered-count").header("Authorization", token(INSTRUCTOR, "ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.count").value(1));
        mvc.perform(get("/api/instructor/questions").header("Authorization", token(51, "ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/instructor/questions").param("courseId", course.getId().toString())
                        .header("Authorization", token(51, "ROLE_INSTRUCTOR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/instructor/questions").header("Authorization", token(LEARNER, "ROLE_STUDENT")))
                .andExpect(status().isForbidden());
    }
}
