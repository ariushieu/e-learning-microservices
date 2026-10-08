package com.hunre.quizservice.controller;

import com.hunre.quizservice.entity.*;
import com.hunre.quizservice.repository.*;
import com.hunre.sharedcommon.exception.BusinessException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** No test transaction: repository reads after HTTP errors must see committed state. */
@SpringBootTest(properties = {
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=quiz-result-integration-secret-at-least-32-bytes",
        "app.outbox.publisher.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:quiz_result;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
class QuizAttemptResultIntegrationTest {
    private static final String SECRET = "quiz-result-integration-secret-at-least-32-bytes";
    @Autowired MockMvc mvc;
    @Autowired QuizRepository quizzes;
    @Autowired QuizAttemptRepository attempts;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean OutboxEventRepository outbox;
    Quiz quiz;
    Question question;
    AnswerOption correct;

    @BeforeEach
    void seed() {
        outbox.deleteAll();
        attempts.deleteAll();
        quizzes.deleteAll();
        quiz = Quiz.builder().courseId(10L).createdBy(99L).title("Result access")
                .status(QuizStatus.PUBLISHED).timeLimitMinutes(1).maxAttempts(2).build();
        question = Question.builder().content("Question").explanation("secret explanation").build();
        correct = AnswerOption.builder().content("secret correct answer").isCorrect(true).build();
        question.addOption(correct);
        question.addOption(AnswerOption.builder().content("Wrong").build());
        quiz.addQuestion(question);
        quiz = quizzes.saveAndFlush(quiz);
    }

    private String token(long id, String role) {
        return "Bearer " + Jwts.builder().subject(Long.toString(id)).claim("roles", List.of(role))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private QuizAttempt attempt(AttemptStatus status, long ageSeconds) {
        return attempts.saveAndFlush(QuizAttempt.builder().quiz(quiz).userId(99L)
                .questionIds(new java.util.HashSet<>(java.util.Set.of(question.getId())))
                .status(status).startedAt(Instant.now().minusSeconds(ageSeconds)).build());
    }

    private String answers() {
        return "{\"answers\":[{\"questionId\":" + question.getId()
                + ",\"selectedOptionIds\":[" + correct.getId() + "]}]}";
    }

    @ParameterizedTest
    @EnumSource(value = AttemptStatus.class, names = {"IN_PROGRESS", "EXPIRED"})
    void unfinishedResultDoesNotExposeAnyAnswers(AttemptStatus state) throws Exception {
        var attempt = attempt(state, 120);
        mvc.perform(get("/api/attempts/" + attempt.getId()).header("Authorization", token(99, "ROLE_STUDENT")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(content().string(not(containsString("correctOptionIds"))))
                .andExpect(content().string(not(containsString("secret correct answer"))))
                .andExpect(content().string(not(containsString("secret explanation"))));
        assertThat(attempts.findById(attempt.getId()).orElseThrow().getStatus()).isEqualTo(state);
        assertThat(outbox.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_STUDENT", "ROLE_ADMIN"})
    void anotherUsersResultStillReturns404(String role) throws Exception {
        var attempt = attempt(AttemptStatus.IN_PROGRESS, 0);
        mvc.perform(get("/api/attempts/" + attempt.getId()).header("Authorization", token(100, role)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(post("/api/attempts/" + attempt.getId() + "/submit")
                        .header("Authorization", token(100, role)).contentType("application/json").content(answers()))
                .andExpect(status().isNotFound());
        assertThat(attempts.findById(attempt.getId()).orElseThrow().getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
    }

    @Test
    void resultAndSubmitRequireAuthentication() throws Exception {
        var attempt = attempt(AttemptStatus.IN_PROGRESS, 0);
        mvc.perform(get("/api/attempts/" + attempt.getId())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/attempts/" + attempt.getId() + "/submit")
                .contentType("application/json").content(answers())).andExpect(status().isUnauthorized());
    }

    @Test
    void submittedResultRetainsScoreAnswersAndExactlyOneEvent() throws Exception {
        var attempt = attempt(AttemptStatus.IN_PROGRESS, 0);
        String path = "/api/attempts/" + attempt.getId();
        mvc.perform(post(path + "/submit").header("Authorization", token(99, "ROLE_STUDENT"))
                        .contentType("application/json").content(answers()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.score").value(100));
        mvc.perform(get(path).header("Authorization", token(99, "ROLE_STUDENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.score").value(100))
                .andExpect(jsonPath("$.data.questionResults[0].correctOptionIds[0]").value(correct.getId()))
                .andExpect(jsonPath("$.data.questionResults[0].explanation").value("secret explanation"));
        mvc.perform(post(path + "/submit").header("Authorization", token(99, "ROLE_STUDENT"))
                        .contentType("application/json").content(answers()))
                .andExpect(status().isUnprocessableEntity());
        assertThat(outbox.count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from attempt_answers", Long.class)).isEqualTo(1);
        assertThat(attempts.findById(attempt.getId()).orElseThrow().getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
    }

    @Test
    void lateSubmissionCommitsExpiryAndHistoryEvenAfterErrorAndRetry() throws Exception {
        var attempt = attempt(AttemptStatus.IN_PROGRESS, 120);
        String path = "/api/attempts/" + attempt.getId();
        mvc.perform(post(path + "/submit").header("Authorization", token(99, "ROLE_STUDENT"))
                        .contentType("application/json").content(answers()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.data").doesNotExist());
        var saved = attempts.findById(attempt.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        assertThat(saved.getScore()).isZero();
        assertThat(saved.getPassed()).isFalse();
        assertThat(saved.getSubmittedAt()).isNotNull();
        assertThat(outbox.count()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from attempt_answers", Long.class)).isZero();
        mvc.perform(get("/api/quizzes/" + quiz.getId() + "/attempts").header("Authorization", token(99, "ROLE_STUDENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].status").value("EXPIRED"));
        mvc.perform(get(path).header("Authorization", token(99, "ROLE_STUDENT")))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(post(path + "/submit").header("Authorization", token(99, "ROLE_STUDENT"))
                        .contentType("application/json").content(answers()))
                .andExpect(status().isUnprocessableEntity());
        assertThat(attempts.findById(attempt.getId()).orElseThrow().getSubmittedAt()).isEqualTo(saved.getSubmittedAt());
        mvc.perform(post("/api/quizzes/" + quiz.getId() + "/attempts")
                        .header("Authorization", token(99, "ROLE_INSTRUCTOR")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.attemptNo").value(2));
    }

    @Test
    void failedOutboxWriteStillRollsBackGradingAndAnswers() throws Exception {
        var attempt = attempt(AttemptStatus.IN_PROGRESS, 0);
        doThrow(new BusinessException("Không lưu được sự kiện chấm điểm"))
                .when(outbox).save(any(OutboxEvent.class));
        mvc.perform(post("/api/attempts/" + attempt.getId() + "/submit")
                        .header("Authorization", token(99, "ROLE_STUDENT"))
                        .contentType("application/json").content(answers()))
                .andExpect(status().isUnprocessableEntity());
        var saved = attempts.findById(attempt.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(saved.getScore()).isNull();
        assertThat(saved.getSubmittedAt()).isNull();
        assertThat(outbox.count()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from attempt_answers", Long.class)).isZero();
    }
}
