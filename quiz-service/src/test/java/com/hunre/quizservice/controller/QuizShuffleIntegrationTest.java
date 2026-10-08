package com.hunre.quizservice.controller;

import com.hunre.quizservice.client.CourseOwnershipClient;
import com.hunre.quizservice.client.EnrollmentAccessClient;
import com.hunre.quizservice.entity.*;
import com.hunre.quizservice.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=quiz-shuffle-test-secret-at-least-32-bytes",
        "app.outbox.publisher.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:quiz_shuffle;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
class QuizShuffleIntegrationTest {
    static final String SECRET = "quiz-shuffle-test-secret-at-least-32-bytes";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired QuizRepository quizzes;
    @Autowired QuizAttemptRepository attempts;
    @Autowired OutboxEventRepository outbox;
    @MockitoBean EnrollmentAccessClient enrollment;
    @MockitoBean CourseOwnershipClient courses;
    Quiz quiz;

    @BeforeEach void seed() {
        outbox.deleteAll(); attempts.deleteAll(); quizzes.deleteAll();
        quiz = Quiz.builder().createdBy(10L).courseId(100L).title("Shuffle test")
                .maxAttempts(0).status(QuizStatus.PUBLISHED).shuffleQuestions(true).shuffleOptions(true).build();
        for (int i = 1; i <= 5; i++) {
            var q = Question.builder().content("Question " + i).position(i)
                    .type(i == 5 ? QuestionType.TRUE_FALSE : i == 4 ? QuestionType.MULTIPLE_CHOICE : QuestionType.SINGLE_CHOICE)
                    .explanation("Secret " + i).build();
            for (int j = 1; j <= (i == 5 ? 2 : 4); j++) {
                q.addOption(AnswerOption.builder().content(i == 5 ? (j == 1 ? "Đúng" : "Sai") : "Option " + i + "." + j)
                        .position(j).isCorrect(j == 1 || (i == 4 && j == 3)).build());
            }
            quiz.addQuestion(q);
        }
        quiz = quizzes.saveAndFlush(quiz);
        when(enrollment.hasEnrollment(anyLong(), anyLong(), any())).thenReturn(true);
    }
    String token(long user, String role) {
        return "Bearer " + Jwts.builder().subject("" + user).claim("roles", List.of(role))
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
    String base() { return "/api/quizzes/" + quiz.getId(); }
    JsonNode read(String path, long user, String role) throws Exception {
        return json.readTree(mvc.perform(get(path).header("Authorization", token(user, role)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("data");
    }
    JsonNode take(long user) throws Exception { return read(base() + "/take", user, "ROLE_STUDENT"); }
    JsonNode detail() throws Exception { return read(base(), 10, "ROLE_INSTRUCTOR"); }
    List<Long> ids(JsonNode nodes, String field) {
        var ids = new ArrayList<Long>(); nodes.forEach(n -> ids.add(n.get(field).asLong())); return ids;
    }
    List<List<Long>> order(JsonNode detail) {
        var result = new ArrayList<List<Long>>();
        for (var q : detail.get("questions")) {
            var row = new ArrayList<Long>(); row.add(q.get("id").asLong()); row.addAll(ids(q.get("options"), "id")); result.add(row);
        }
        return result;
    }
    long start(long user) throws Exception {
        return json.readTree(mvc.perform(post(base() + "/attempts").header("Authorization", token(user, "ROLE_STUDENT")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("data").get("id").asLong();
    }
    @Test void fiveReloadsAndResumeKeepWholeOrderAndHideAnswers() throws Exception {
        long id = start(20); var first = take(20);
        for (int i = 0; i < 5; i++) assertThat(take(20)).isEqualTo(first);
        assertThat(start(20)).isEqualTo(id);
        assertThat(take(20)).isEqualTo(first);
        assertThat(first.toString()).doesNotContain("isCorrect", "Secret", "correctOptionIds");
        for (var q : first.get("questions")) if (q.get("type").asString().equals("TRUE_FALSE")) {
            assertThat(q.get("options").get(0).get("content").asString()).isEqualTo("Đúng");
            assertThat(q.get("options").get(1).get("content").asString()).isEqualTo("Sai");
        }
    }
    @ParameterizedTest @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void togglesIndependentlyControlQuestionAndOptionOrder(boolean questionShuffle, boolean optionShuffle) throws Exception {
        quiz.setShuffleQuestions(questionShuffle); quiz.setShuffleOptions(optionShuffle); quizzes.saveAndFlush(quiz);
        var original = detail(); long id = start(20); var taken = take(20);
        assertThat(order(take(20))).isEqualTo(order(taken));
        var expectedQuestions = new ArrayList<JsonNode>(); original.get("questions").forEach(expectedQuestions::add);
        var random = new Random(id); if (questionShuffle) Collections.shuffle(expectedQuestions, random);
        assertThat(ids(taken.get("questions"), "id")).containsExactlyElementsOf(expectedQuestions.stream().map(q -> q.get("id").asLong()).toList());
        for (int i = 0; i < expectedQuestions.size(); i++) {
            var q = expectedQuestions.get(i); var optionIds = ids(q.get("options"), "id");
            if (optionShuffle && !q.get("type").asString().equals("TRUE_FALSE")) Collections.shuffle(optionIds, random);
            assertThat(ids(taken.get("questions").get(i).get("options"), "id")).containsExactlyElementsOf(optionIds);
        }
    }
    @Test void callerIdentityCannotBeOverriddenAndOtherAttemptDoesNotSeedLanding() throws Exception {
        var original = order(detail()); start(20);
        assertThat(order(take(21))).isEqualTo(original);
        assertThat(take(20)).isEqualTo(read(base()+"/take?userId=21&attemptId=999&seed=1",20,"ROLE_STUDENT"));
        mvc.perform(get(base()+"/take")).andExpect(status().isUnauthorized());
        quiz.setStatus(QuizStatus.DRAFT);quizzes.saveAndFlush(quiz);
        mvc.perform(get(base()+"/take").header("Authorization",token(20,"ROLE_STUDENT"))).andExpect(status().isUnprocessableContent());
    }
    @ParameterizedTest @EnumSource(value = AttemptStatus.class, names = {"SUBMITTED", "EXPIRED"})
    void closedAttemptsDoNotSeedTake(AttemptStatus status) throws Exception {
        long id=start(20);var attempt=attempts.findById(id).orElseThrow();attempt.setStatus(status);attempts.saveAndFlush(attempt);
        assertThat(order(take(20))).isEqualTo(order(detail()));
    }
    @Test void gradingByIdsScoresOneHundredAndNewAttemptUsesNewOrder() throws Exception {
        var original = detail(); long id = start(20); var before = order(take(20));
        var answers = new ArrayList<Map<String,Object>>();
        for (var q : original.get("questions")) {
            var correct = new ArrayList<Long>(); q.get("options").forEach(o -> {if(o.get("isCorrect").asBoolean()) correct.add(o.get("id").asLong());});
            answers.add(Map.of("questionId",q.get("id").asLong(),"selectedOptionIds",correct));
        }
        var result = json.readTree(mvc.perform(post("/api/attempts/"+id+"/submit").contentType("application/json")
                .header("Authorization",token(20,"ROLE_STUDENT")).content(json.writeValueAsString(Map.of("answers",answers))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.score").value(100))
                .andReturn().getResponse().getContentAsString()).get("data");
        assertThat(ids(result.get("questionResults"),"questionId")).isEqualTo(ids(original.get("questions"),"id"));
        var fetched=read("/api/attempts/"+id,20,"ROLE_STUDENT");
        for(int i=0;i<5;i++) assertThat(ids(fetched.get("questionResults").get(i).get("options"),"id"))
                .isEqualTo(ids(original.get("questions").get(i).get("options"),"id"));
        assertThat(start(20)).isNotEqualTo(id);assertThat(order(take(20))).isNotEqualTo(before);
    }
    @Test void takeDoesNotMutateAuthorStatsCsvOrDatabasePositions() throws Exception {
        var original=detail();start(20);
        var stats=read(base()+"/results",10,"ROLE_INSTRUCTOR");
        var csv=mvc.perform(get(base()+"/results/export").header("Authorization",token(10,"ROLE_INSTRUCTOR"))).andReturn().getResponse().getContentAsByteArray();
        take(20);take(20);
        assertThat(detail()).isEqualTo(original);
        assertThat(read(base()+"/results",10,"ROLE_INSTRUCTOR")).isEqualTo(stats);
        mvc.perform(get(base()+"/results/export").header("Authorization",token(10,"ROLE_INSTRUCTOR"))).andExpect(content().bytes(csv));
    }
    @Test void duplicatePositionsHaveAnIdTieBreakerBeforeShuffling() throws Exception {
        for (var question : quiz.getQuestions()) {
            question.setPosition(0);
            question.getOptions().forEach(option -> option.setPosition(0));
        }
        quizzes.saveAndFlush(quiz);
        assertThat(ids(detail().get("questions"), "id")).isSorted();
        start(20);
        var first = order(take(20));
        for (int i = 0; i < 5; i++) assertThat(order(take(20))).isEqualTo(first);
        assertThat(ids(detail().get("questions"), "id")).isSorted();
    }
    @Test void settingsRoundTripDefaultsFalseAndRequiresOwnership() throws Exception {
        mvc.perform(post("/api/quizzes").contentType("application/json").header("Authorization",token(10,"ROLE_INSTRUCTOR"))
                .content("{\"courseId\":100,\"title\":\"Default\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.shuffleOptions").value(false));
        for(boolean enabled:List.of(true,false)) {
            mvc.perform(put(base()).contentType("application/json").header("Authorization",token(10,"ROLE_INSTRUCTOR"))
                    .content("{\"title\":\"Shuffle test\",\"shuffleQuestions\":"+enabled+",\"shuffleOptions\":"+enabled+"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.shuffleOptions").value(enabled));
            assertThat(detail().get("shuffleOptions").asBoolean()).isEqualTo(enabled);
        }
        mvc.perform(put(base()).contentType("application/json").header("Authorization",token(11,"ROLE_INSTRUCTOR"))
                .content("{\"title\":\"Other\",\"shuffleOptions\":true}" )).andExpect(status().isForbidden());
        assertThat(detail().get("shuffleOptions").asBoolean()).isFalse();
    }
}
