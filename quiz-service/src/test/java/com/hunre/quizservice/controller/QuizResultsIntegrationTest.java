package com.hunre.quizservice.controller;

import com.hunre.quizservice.client.EnrollmentAccessClient;
import com.hunre.quizservice.entity.*;
import com.hunre.quizservice.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=quiz-statistics-test-secret-at-least-32-bytes",
        "app.outbox.publisher.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:quiz_stats;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
class QuizResultsIntegrationTest {
    static final String SECRET = "quiz-statistics-test-secret-at-least-32-bytes";
    @Autowired MockMvc mvc;
    @Autowired QuizRepository quizzes;
    @Autowired QuizAttemptRepository attempts;
    @Autowired OutboxEventRepository outbox;
    @MockitoBean EnrollmentAccessClient enrollment;
    Quiz quiz;

    @BeforeEach void seed() {
        outbox.deleteAll(); attempts.deleteAll(); quizzes.deleteAll();
        quiz = Quiz.builder().createdBy(10L).courseId(100L).title("Kết quả lớp học")
                .status(QuizStatus.PUBLISHED).passScore(new BigDecimal("75")).maxAttempts(20).build();
        for (int i = 0; i < 2; i++) {
            var q = Question.builder().content("Câu " + (i + 1)).position(i).build();
            q.addOption(AnswerOption.builder().content("Đúng").isCorrect(true).build());
            q.addOption(AnswerOption.builder().content("Sai").build());
            quiz.addQuestion(q);
        }
        quiz = quizzes.saveAndFlush(quiz);
        when(enrollment.hasEnrollment(anyLong(), anyLong(), any())).thenReturn(true);
    }

    String token(long user, String role) {
        return "Bearer " + Jwts.builder().subject("" + user).claim("fullName", "Học viên " + user)
                .claim("roles", List.of(role)).expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
    String path() { return "/api/quizzes/" + quiz.getId() + "/results"; }

    @Test void csvExportsEveryLearnerWithBestScoreVietnamTimeAndUtf8Bom() throws Exception {
        attempt(20, 1, AttemptStatus.SUBMITTED, false, "Tên cũ", 100);
        attempt(20, 2, AttemptStatus.SUBMITTED, false, "Nguyễn Thị Ánh", 50);
        for (long id = 30; id < 51; id++) {
            attempt(id, 1, AttemptStatus.SUBMITTED, false, "Học viên " + id, 50);
        }
        attempt(10, 1, AttemptStatus.SUBMITTED, false, "Tác giả", 100);
        attempt(90, 1, AttemptStatus.SUBMITTED, true, "Admin preview", 100);
        attempt(91, 1, AttemptStatus.SUBMITTED, null, "Legacy", 100);
        attempt(92, 1, AttemptStatus.EXPIRED, false, "Expired", 0);
        attempt(93, 1, AttemptStatus.IN_PROGRESS, false, "Ongoing", 0);
        var bytes = mvc.perform(get(path() + "/export").param("page", "9").param("size", "1")
                        .accept("application/json").header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk()).andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"ket-qua-quiz-" + quiz.getId() + ".csv\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(bytes).startsWith((byte) 0xef, (byte) 0xbb, (byte) 0xbf);
        var csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(csv.lines().count()).isEqualTo(23); // header + all 22 learners, not a page
        assertThat(csv).contains("Nguyễn Thị Ánh,2,100.00,Đạt,08/10/2026 08:00:02\r\n",
                "Học viên 50,1,50.00,Chưa đạt,08/10/2026 08:00:01\r\n")
                .doesNotContain("Tên cũ", "Tác giả", "Admin preview", "Legacy", "Expired", "Ongoing", "correctOptionIds");
        mvc.perform(get(path() + "/export").header("Authorization", token(99, "ROLE_ADMIN")))
                .andExpect(status().isOk()).andExpect(content().bytes(bytes));
    }

    @ParameterizedTest
    @ValueSource(strings = {"=1+1", "+SUM(1,2)", "-1+1", "@SUM(A1)", "\t=1+1", "\r=1+1", "\n=1+1"})
    void csvNeutralizesFormulaPrefixesBeforeQuoting(String name) throws Exception {
        attempt(20, 1, AttemptStatus.SUBMITTED, false, name, 50);
        String escaped = "'" + name;
        if (name.contains(",") || name.contains("\r") || name.contains("\n")) escaped = "\"" + escaped + "\"";
        var csv = mvc.perform(get(path() + "/export").header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("\r\n" + escaped + ",1,50.00,Chưa đạt,");
    }

    @Test void csvQuotesCommasQuotesAndMultilineNamesAndKeepsStoredNameUnchanged() throws Exception {
        String name = "Nguyễn, \"Ánh\"\r\nLớp A";
        var saved = attempt(20, 1, AttemptStatus.SUBMITTED, false, name, 100);
        attempt(30, 1, AttemptStatus.SUBMITTED, false, null, 50);
        var csv = mvc.perform(get(path() + "/export").header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("\"Nguyễn, \"\"Ánh\"\"\r\nLớp A\",1,100.00,Đạt,", "Học viên #30,1,50.00,");
        assertThat(attempts.findById(saved.getId()).orElseThrow().getLearnerName()).isEqualTo(name);
    }

    @ParameterizedTest @ValueSource(strings = {"ROLE_STUDENT", "ROLE_INSTRUCTOR"})
    void csvRejectsOtherUsersWithoutAttachment(String role) throws Exception {
        mvc.perform(get(path() + "/export").param("userId", "10").param("isAdmin", "true")
                        .header("Authorization", token(20, role)))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test void csvRequiresLoginAndReturns404ForUnknownQuizAnd400ForInvalidId() throws Exception {
        mvc.perform(get(path() + "/export")).andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Content-Disposition"));
        mvc.perform(get("/api/quizzes/999999/results/export").header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isNotFound()).andExpect(header().doesNotExist("Content-Disposition"));
        mvc.perform(get("/api/quizzes/abc/results/export").header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isBadRequest());
    }

    @Test void emptyCsvStillContainsOnlyTheUtf8Header() throws Exception {
        mvc.perform(get(path() + "/export").accept("application/json").header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk()).andExpect(content().bytes(
                        "\uFEFFHọc viên,Lượt nộp,Điểm cao nhất,Kết quả,Nộp gần nhất (giờ Việt Nam)\r\n".getBytes(StandardCharsets.UTF_8)));
    }
    QuizAttempt attempt(long user, int no, AttemptStatus status, Boolean preview, String name, int score, boolean... correct) {
        var a = QuizAttempt.builder().quiz(quiz).userId(user).attemptNo(no).status(status).preview(preview)
                .learnerName(name).score(BigDecimal.valueOf(score)).passed(score >= 75)
                .submittedAt(Instant.parse("2026-10-08T01:00:00Z").plusSeconds(no)).build();
        for (int i = 0; i < correct.length; i++) {
            a.addAnswer(AttemptAnswer.builder().question(quiz.getQuestions().get(i))
                    .isCorrect(correct[i]).earnedScore(correct[i] ? BigDecimal.ONE : BigDecimal.ZERO).build());
        }
        return attempts.saveAndFlush(a);
    }

    @Test void requiredExampleAndPaginationUseWholePopulation() throws Exception {
        attempt(20, 1, AttemptStatus.SUBMITTED, false, "Sinh viên S", 50, true, false);
        attempt(30, 1, AttemptStatus.SUBMITTED, false, "Sinh viên B", 100, true, true);
        mvc.perform(get(path()).param("size", "1").header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.summary.submittedLearners").value(2))
                .andExpect(jsonPath("$.data.summary.submittedAttempts").value(2))
                .andExpect(jsonPath("$.data.summary.averageBestScore").value(75))
                .andExpect(jsonPath("$.data.summary.passRate").value(50))
                .andExpect(jsonPath("$.data.summary.questions[0].correctRate").value(100))
                .andExpect(jsonPath("$.data.summary.questions[1].correctRate").value(50))
                .andExpect(jsonPath("$.data.learners.content", hasSize(1)))
                .andExpect(jsonPath("$.data.learners.content[0].learnerName").value("Sinh viên S"))
                .andExpect(jsonPath("$.data.learners.content[0].lastSubmittedAt").value("2026-10-08T01:00:01Z"))
                .andExpect(jsonPath("$.data.learners.totalElements").value(2))
                .andExpect(jsonPath("$.data.learners.totalPages").value(2))
                .andExpect(content().string(not(containsString("correctOptionIds"))))
                .andExpect(content().string(not(containsString("email"))));
        mvc.perform(get(path()).param("size", "1").param("page", "1").header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.learners.content[0].learnerId").value(30));
    }

    @Test void retriesUseBestScoreButQuestionRatesUseAllGradedAnswers() throws Exception {
        attempt(20, 1, AttemptStatus.SUBMITTED, false, "Tên cũ", 0, false, false);
        attempt(20, 2, AttemptStatus.SUBMITTED, false, "Tên mới", 100, true, true);
        attempt(30, 1, AttemptStatus.SUBMITTED, false, null, 50, true, false);
        mvc.perform(get(path()).header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.summary.averageBestScore").value(75))
                .andExpect(jsonPath("$.data.summary.submittedAttempts").value(3))
                .andExpect(jsonPath("$.data.summary.passRate").value(50))
                .andExpect(jsonPath("$.data.summary.questions[0].correctRate").value(66.67))
                .andExpect(jsonPath("$.data.summary.questions[1].correctRate").value(33.33))
                .andExpect(jsonPath("$.data.learners.content[0].learnerName").value("Tên mới"))
                .andExpect(jsonPath("$.data.learners.content[0].submittedAttempts").value(2))
                .andExpect(jsonPath("$.data.learners.content[0].bestScore").value(100))
                .andExpect(jsonPath("$.data.learners.content[1].learnerName").value("Học viên #30"));
    }

    @Test void previewLegacyExpiredOngoingAndOtherQuizzesDoNotSkewScores() throws Exception {
        attempt(20, 1, AttemptStatus.SUBMITTED, false, "S", 50, true, false);
        attempt(20, 2, AttemptStatus.EXPIRED, false, "S", 0);
        attempt(20, 3, AttemptStatus.IN_PROGRESS, false, "S", 100);
        attempt(10, 1, AttemptStatus.SUBMITTED, false, "Owner legacy", 100, true, true);
        attempt(99, 1, AttemptStatus.SUBMITTED, true, "Admin preview", 100, true, true);
        attempt(99, 2, AttemptStatus.EXPIRED, true, "Admin preview", 0);
        attempt(40, 1, AttemptStatus.SUBMITTED, null, "Unknown legacy", 100, true, true);
        var other = quizzes.saveAndFlush(Quiz.builder().createdBy(10L).courseId(100L).title("Other").build());
        attempts.saveAndFlush(QuizAttempt.builder().quiz(other).userId(50L).preview(false)
                .status(AttemptStatus.SUBMITTED).score(BigDecimal.valueOf(100)).passed(true).build());
        mvc.perform(get(path()).header("Authorization", token(99, "ROLE_ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.summary.submittedLearners").value(1))
                .andExpect(jsonPath("$.data.summary.submittedAttempts").value(1))
                .andExpect(jsonPath("$.data.summary.expiredAttempts").value(1))
                .andExpect(jsonPath("$.data.summary.unclassifiedAttempts").value(1))
                .andExpect(jsonPath("$.data.summary.averageBestScore").value(50))
                .andExpect(jsonPath("$.data.summary.passRate").value(0))
                .andExpect(jsonPath("$.data.summary.questions[1].correctRate").value(0));
    }

    @ParameterizedTest @ValueSource(strings = {"ROLE_STUDENT", "ROLE_INSTRUCTOR"})
    void nonOwnerCannotReadResultsEvenWithSpoofedIdentity(String role) throws Exception {
        mvc.perform(get(path()).param("userId", "10").param("isAdmin", "true")
                        .header("Authorization", token(20, role)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test void noTokenIs401AndUnknownQuiz404() throws Exception {
        mvc.perform(get(path())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/quizzes/999999/results").header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isNotFound());
    }

    @Test void emptyAndBeyondLastPageReturnStableZeroStatistics() throws Exception {
        mvc.perform(get(path()).param("page", "9").header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.summary.averageBestScore").value(0))
                .andExpect(jsonPath("$.data.summary.passRate").value(0))
                .andExpect(jsonPath("$.data.summary.questions[0].gradedAnswers").value(0))
                .andExpect(jsonPath("$.data.learners.content", hasSize(0)))
                .andExpect(jsonPath("$.data.learners.totalElements").value(0));
    }

    @Test void unsupportedSortAndMalformedIdReturn400() throws Exception {
        mvc.perform(get(path()).param("sort", "secret,desc").header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/quizzes/abc/results").header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest @ValueSource(strings = {"ROLE_STUDENT", "ROLE_ADMIN", "ROLE_INSTRUCTOR"})
    void startRecordsTrustedNameAndPreviewAndResumeDoesNotReclassify(String role) throws Exception {
        long user = role.equals("ROLE_INSTRUCTOR") ? 10 : 20;
        mvc.perform(post("/api/quizzes/" + quiz.getId() + "/attempts").header("Authorization", token(user, role))
                        .param("learnerName", "spoof").param("preview", "false"))
                .andExpect(status().isCreated());
        var saved = attempts.findByQuizIdAndUserIdOrderByAttemptNoDesc(quiz.getId(), user).get(0);
        assertThat(saved.getLearnerName()).isEqualTo("Học viên " + user);
        assertThat(saved.getPreview()).isEqualTo(!role.equals("ROLE_STUDENT"));
        mvc.perform(post("/api/quizzes/" + quiz.getId() + "/attempts").header("Authorization", token(user, "ROLE_ADMIN")))
                .andExpect(status().isCreated());
        assertThat(attempts.findById(saved.getId()).orElseThrow().getPreview()).isEqualTo(saved.getPreview());
    }
}
