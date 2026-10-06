package com.hunre.quizservice.controller;

import com.hunre.quizservice.entity.*;
import com.hunre.quizservice.repository.*;
import com.hunre.quizservice.service.QuizAttemptService;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** JWT, controller, service, SQL và HTTP client thật; chỉ giả lập server ghi danh. */
@SpringBootTest(properties = {
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=quiz-enrollment-integration-secret-at-least-32-bytes",
        "app.outbox.publisher.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:quiz_enrollment;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
class QuizEnrollmentIntegrationTest {
    private static final String SECRET = "quiz-enrollment-integration-secret-at-least-32-bytes";
    private static final List<String> tokens = new CopyOnWriteArrayList<>();
    private static volatile String body;
    private static volatile int upstreamStatus;
    private static final HttpServer upstream = startServer();

    @Autowired MockMvc mvc;
    @Autowired QuizRepository quizzes;
    @Autowired QuizAttemptRepository attempts;
    @Autowired QuizAttemptService service;
    Quiz quiz;

    static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/enrollments", exchange -> {
                tokens.add(exchange.getRequestHeaders().getFirst("Authorization"));
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                try {
                    exchange.sendResponseHeaders(upstreamStatus, bytes.length);
                    exchange.getResponseBody().write(bytes);
                } finally { exchange.close(); }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("quiz.enrollment.base-url", () -> "http://127.0.0.1:" + upstream.getAddress().getPort());
    }

    @AfterAll
    static void shutdown() {
        upstream.stop(0);
    }

    @BeforeEach
    void seed() {
        attempts.deleteAll();
        quizzes.deleteAll();
        tokens.clear();
        upstreamStatus = 200;
        body = "{\"success\":true,\"data\":{\"page\":0,\"last\":true,\"content\":[]}}";
        quiz = Quiz.builder().courseId(10L).createdBy(50L).title("Enrollment quiz")
                .status(QuizStatus.PUBLISHED).timeLimitMinutes(5).build();
        Question question = Question.builder().content("Question").build();
        question.addOption(AnswerOption.builder().content("Correct").isCorrect(true).build());
        question.addOption(AnswerOption.builder().content("Wrong").isCorrect(false).build());
        quiz.addQuestion(question);
        quiz = quizzes.saveAndFlush(quiz);
    }

    private String token(long id, String role) {
        return "Bearer " + Jwts.builder().subject(Long.toString(id)).claim("roles", List.of(role))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private String path() { return "/api/quizzes/" + quiz.getId() + "/attempts"; }

    private void enrollment(long userId, long courseId, String status) {
        body = """
                {"success":true,"data":{"page":0,"last":true,"content":[
                {"userId":%d,"courseId":%d,"status":"%s"}]}}
                """.formatted(userId, courseId, status);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "COMPLETED"})
    void enrolledStudentStartsAndResumesSameAttempt(String status) throws Exception {
        enrollment(60, 10, status);
        String authorization = token(60, "ROLE_STUDENT");
        mvc.perform(post(path()).header("Authorization", authorization))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.userId").value(60));
        QuizAttempt first = attempts.findAll().get(0);
        mvc.perform(post(path()).header("Authorization", authorization))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value(first.getId()));
        assertThat(attempts.count()).isEqualTo(1);
        assertThat(tokens).containsExactly(authorization, authorization);
    }

    @Test
    void unenrolledStudentCannotForgeIdentityOrAdminFlag() throws Exception {
        mvc.perform(post(path()).header("Authorization", token(60, "ROLE_STUDENT"))
                        .param("userId", "50").param("isAdmin", "true"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(attempts.count()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"60,10,CANCELLED", "60,11,ACTIVE", "70,10,ACTIVE", "60,10,UNKNOWN"})
    void onlyThisUsersEligibleEnrollmentInThisCourseGrantsAccess(long userId, long courseId, String status) throws Exception {
        enrollment(userId, courseId, status);
        mvc.perform(post(path()).header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().isForbidden());
        assertThat(attempts.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {302,404,429,500,503})
    void upstreamFailureReturns502WithoutWritingAnAttempt(int code) throws Exception {
        upstreamStatus = code;
        mvc.perform(post(path()).header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_ERROR"));
        assertThat(attempts.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not json", "{}", "{\"success\":false}"})
    void invalidResponseReturns502WithoutWritingAnAttempt(String invalidBody) throws Exception {
        body = invalidBody;
        mvc.perform(post(path()).header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().isBadGateway());
        assertThat(attempts.count()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"50,ROLE_INSTRUCTOR", "99,ROLE_ADMIN"})
    void creatorAndAdminCanPreviewWithoutCallingEnrollment(long id, String role) throws Exception {
        upstreamStatus = 503;
        mvc.perform(post(path()).header("Authorization", token(id, role)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.userId").value(id));
        assertThat(attempts.count()).isEqualTo(1);
        assertThat(tokens).isEmpty();
    }

    @Test
    void otherInstructorStillNeedsEnrollment() throws Exception {
        mvc.perform(post(path()).header("Authorization", token(60, "ROLE_INSTRUCTOR")))
                .andExpect(status().isForbidden());
        assertThat(attempts.count()).isZero();
        assertThat(tokens).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {200,503})
    void deniedOrUnavailableEnrollmentCannotResumeOrExpireAnOldAttempt(int upstreamCode) throws Exception {
        upstreamStatus = upstreamCode;
        QuizAttempt old = attempts.saveAndFlush(QuizAttempt.builder().quiz(quiz).userId(60L).attemptNo(1)
                .status(AttemptStatus.IN_PROGRESS).startedAt(Instant.now().minusSeconds(3600)).build());
        mvc.perform(post(path()).header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().is(upstreamCode == 200 ? 403 : 502));
        assertThat(attempts.count()).isEqualTo(1);
        QuizAttempt unchanged = attempts.findById(old.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(unchanged.getSubmittedAt()).isNull();
    }

    @Test
    void authorizedStudentCanReplaceExpiredAttempt() throws Exception {
        enrollment(60, 10, "ACTIVE");
        QuizAttempt old = attempts.saveAndFlush(QuizAttempt.builder().quiz(quiz).userId(60L).attemptNo(1)
                .status(AttemptStatus.IN_PROGRESS).startedAt(Instant.now().minusSeconds(3600)).build());
        mvc.perform(post(path()).header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.attemptNo").value(2));
        assertThat(attempts.count()).isEqualTo(2);
        assertThat(attempts.findById(old.getId()).orElseThrow().getStatus()).isEqualTo(AttemptStatus.EXPIRED);
    }

    @Test
    void attemptLimitStillAppliesToEnrolledStudents() throws Exception {
        enrollment(60, 10, "ACTIVE");
        quiz.setMaxAttempts(1);
        quizzes.saveAndFlush(quiz);
        attempts.saveAndFlush(QuizAttempt.builder().quiz(quiz).userId(60L).attemptNo(1)
                .status(AttemptStatus.SUBMITTED).startedAt(Instant.now()).build());
        mvc.perform(post(path()).header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().isUnprocessableEntity());
        assertThat(attempts.count()).isEqualTo(1);
    }

    @Test
    void missingTokenAndUnknownQuizDoNotCallEnrollment() throws Exception {
        mvc.perform(post(path())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/quizzes/999999/attempts").header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().isNotFound());
        assertThat(tokens).isEmpty();
        assertThat(attempts.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false,true})
    void nullIdentityCannotBypassAsAdmin(boolean admin) {
        assertThatThrownBy(() -> service.startAttempt(quiz.getId(), null, admin, null))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThat(attempts.count()).isZero();
        assertThat(tokens).isEmpty();
    }
}
