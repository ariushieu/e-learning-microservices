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
        "spring.datasource.url=jdbc:h2:mem:quiz_draw;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
class QuizDrawIntegrationTest {
    static final String SECRET = "quiz-shuffle-test-secret-at-least-32-bytes";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired QuizRepository quizzes;
    @Autowired QuizAttemptRepository attempts;
    @Autowired OutboxEventRepository outbox;
    @Autowired QuestionRepository questionRepository;
    @MockitoBean EnrollmentAccessClient enrollment;
    @MockitoBean CourseOwnershipClient courses;
    Quiz quiz;

    @BeforeEach void seed() {
        outbox.deleteAll(); attempts.deleteAll(); questionRepository.deleteAll(); quizzes.deleteAll();
        quiz = Quiz.builder().createdBy(10L).courseId(100L).title("Shuffle test")
                .maxAttempts(0).status(QuizStatus.PUBLISHED).questionsPerAttempt(5).shuffleQuestions(true).shuffleOptions(true).build();
        for (int i = 1; i <= 12; i++) {
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
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    JsonNode submit(long id, JsonNode source) throws Exception {
        return json.readTree(mvc.perform(post("/api/attempts/" + id + "/submit")
                .header("Authorization", token(20, "ROLE_STUDENT")).contentType("application/json")
                .content(answers(source))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("data");
    }
    String answers(JsonNode source) {
        var answers = new ArrayList<Map<String, Object>>();
        for (var q : source.get("questions")) {
            var correct = new ArrayList<Long>();
            q.get("options").forEach(o -> { if (o.path("isCorrect").asBoolean(false)) correct.add(o.get("id").asLong()); });
            answers.add(Map.of("questionId", q.get("id").asLong(), "selectedOptionIds", correct));
        }
        return json.writeValueAsString(Map.of("answers", answers));
    }
    JsonNode selectedAnswers(JsonNode authored, JsonNode taken) {
        var copy = json.createObjectNode(); var selected = copy.putArray("questions");
        var ids = ids(taken.get("questions"), "id");
        authored.get("questions").forEach(q -> { if (ids.contains(q.get("id").asLong())) selected.add(q); });
        return copy;
    }
    @Test void drawIsStoredOnceSurvivesReloadSettingsAndAddedQuestions() throws Exception {
        long id = start(20); var first = take(20);
        assertThat(first.get("totalQuestions").asInt()).isEqualTo(5);
        assertThat(first.get("totalScore").decimalValue()).isEqualByComparingTo("5");
        for (int i=0;i<5;i++) assertThat(take(20)).isEqualTo(first);
        assertThat(start(20)).isEqualTo(id);
        assertThat(jdbc.queryForObject("select count(*) from attempt_questions where attempt_id=?", Long.class, id)).isEqualTo(5);
        quiz.setQuestionsPerAttempt(12); quizzes.saveAndFlush(quiz);
        mvc.perform(post(base()+"/questions").header("Authorization",token(10,"ROLE_INSTRUCTOR"))
                .contentType("application/json").content("""
                {"content":"Added later","type":"SINGLE_CHOICE","options":[{"content":"Yes","isCorrect":true},{"content":"No","isCorrect":false}]}
                """)).andExpect(status().isCreated());
        assertThat(order(take(20))).isEqualTo(order(first));
        assertThat(take(20).toString()).doesNotContain("isCorrect", "Secret", "Added later");
    }
    @Test void fullScoreSubsetResultsStatisticsAndNewDraw() throws Exception {
        var authored = detail(); long id=start(20); var first=take(20);
        var result=submit(id, selectedAnswers(authored, first));
        assertThat(result.get("score").decimalValue()).isEqualByComparingTo("100");
        assertThat(result.get("questionResults").size()).isEqualTo(5);
        var fetched=read("/api/attempts/"+id,20,"ROLE_STUDENT");
        assertThat(fetched.get("questionResults")).isEqualTo(result.get("questionResults"));
        var stats=read(base()+"/results",10,"ROLE_INSTRUCTOR");
        for(var q:stats.get("summary").get("questions")) {
            boolean seen=ids(first.get("questions"),"id").contains(q.get("questionId").asLong());
            assertThat(q.get("gradedAnswers").asLong()).isEqualTo(seen?1:0);
            assertThat(q.get("correctRate").asInt()).isEqualTo(seen?100:0);
        }
        long second=start(20); assertThat(second).isNotEqualTo(id);
        assertThat(new HashSet<>(ids(take(20).get("questions"),"id")))
                .isNotEqualTo(new HashSet<>(ids(first.get("questions"),"id")));
    }
    @Test void outsideDrawRejectedWithoutAnswersOrOutboxAndCanRetry() throws Exception {
        var authored=detail(); long id=start(20); var taken=take(20);
        mvc.perform(post("/api/attempts/"+id+"/submit").header("Authorization",token(20,"ROLE_STUDENT"))
                .contentType("application/json").content(answers(authored))).andExpect(status().isBadRequest());
        assertThat(outbox.count()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from attempt_answers",Long.class)).isZero();
        assertThat(attempts.findById(id).orElseThrow().getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(submit(id,selectedAnswers(authored,taken)).get("score").asInt()).isEqualTo(100);
    }
    @Test void deletedQuestionIsNotReplacedAndStaleAnswerIsIgnored() throws Exception {
        var authored=detail(); long id=start(20); var taken=take(20);
        long deleted=taken.get("questions").get(0).get("id").asLong();
        mvc.perform(delete(base()+"/questions/"+deleted).header("Authorization",token(10,"ROLE_INSTRUCTOR")))
                .andExpect(status().isOk());
        var remaining=take(20);
        assertThat(remaining.get("questions").size()).isEqualTo(4);
        assertThat(ids(remaining.get("questions"),"id")).isSubsetOf(ids(taken.get("questions"),"id"));
        assertThat(jdbc.queryForObject("select count(*) from attempt_questions where attempt_id=?", Long.class,id)).isEqualTo(5);
        var result=submit(id,selectedAnswers(authored,taken));
        assertThat(result.get("score").asInt()).isEqualTo(100);
        assertThat(result.get("questionResults").size()).isEqualTo(4);
    }
    @Test void editKeepsMembershipAndAllDeletedCannotProducePassingGrade() throws Exception {
        long id=start(20);var taken=take(20);var ids=ids(taken.get("questions"),"id");
        mvc.perform(put(base()+"/questions/"+ids.get(0)).header("Authorization",token(10,"ROLE_INSTRUCTOR"))
                .contentType("application/json").content("""
                {"content":"Edited question","type":"SINGLE_CHOICE","options":[{"content":"Yes","isCorrect":true},{"content":"No","isCorrect":false}]}
                """)).andExpect(status().isOk());
        assertThat(ids(take(20).get("questions"),"id")).containsExactlyInAnyOrderElementsOf(ids);
        for(long q:ids) mvc.perform(delete(base()+"/questions/"+q).header("Authorization",token(10,"ROLE_INSTRUCTOR"))).andExpect(status().isOk());
        mvc.perform(post("/api/attempts/"+id+"/submit").header("Authorization",token(20,"ROLE_STUDENT"))
                .contentType("application/json").content("{\"answers\":[]}")).andExpect(status().isUnprocessableContent());
        assertThat(outbox.count()).isZero();
    }
    @ParameterizedTest @CsvSource({"1,1","5,5","12,12","200,12"})
    void countBoundsAndNoShuffleUseAuthoredSubsetOrder(int count,int expected) throws Exception {
        quiz.setQuestionsPerAttempt(count);quiz.setShuffleQuestions(false);quiz.setShuffleOptions(false);quizzes.saveAndFlush(quiz);
        var original=detail();start(20);var taken=take(20);
        assertThat(taken.get("questions").size()).isEqualTo(expected);
        var selected=selectedAnswers(original,taken);
        assertThat(order(taken)).isEqualTo(order(selected));
    }
    @Test void nullTakesWholeBankAndResultsDoNotGrowAfterAddingQuestion() throws Exception {
        quiz.setQuestionsPerAttempt(null);quizzes.saveAndFlush(quiz);
        var authored=detail();long id=start(20);assertThat(take(20).get("questions").size()).isEqualTo(12);
        submit(id,authored);
        mvc.perform(post(base()+"/questions").header("Authorization",token(10,"ROLE_INSTRUCTOR"))
                .contentType("application/json").content("""
                {"content":"After submission","type":"TRUE_FALSE","options":[{"content":"True","isCorrect":true},{"content":"False","isCorrect":false}]}
                """)).andExpect(status().isCreated());
        assertThat(read("/api/attempts/"+id,20,"ROLE_STUDENT").get("questionResults").size()).isEqualTo(12);
    }
    @ParameterizedTest @CsvSource({"0","-1","201"})
    void invalidCountRejectedOnCreateAndUpdate(int count) throws Exception {
        String body="{\"courseId\":100,\"title\":\"Count\",\"questionsPerAttempt\":"+count+"}";
        mvc.perform(post("/api/quizzes").header("Authorization",token(10,"ROLE_INSTRUCTOR")).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        mvc.perform(put(base()).header("Authorization",token(10,"ROLE_INSTRUCTOR")).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        assertThat(detail().get("questionsPerAttempt").asInt()).isEqualTo(5);
    }
    @Test void deletionAfterPriorGradingPreservesPastResultAndFutureSubmission() throws Exception {
        quiz.setQuestionsPerAttempt(null);quizzes.saveAndFlush(quiz);
        var bank=detail();long old=start(20);submit(old,bank);long current=start(20);
        long deleted=bank.get("questions").get(0).get("id").asLong();
        mvc.perform(delete(base()+"/questions/"+deleted).header("Authorization",token(10,"ROLE_INSTRUCTOR"))).andExpect(status().isOk());
        assertThat(take(20).get("questions").size()).isEqualTo(11);
        assertThat(detail().get("questions").size()).isEqualTo(11);
        assertThat(read("/api/attempts/"+old,20,"ROLE_STUDENT").get("questionResults").size()).isEqualTo(12);
        assertThat(submit(current,bank).get("score").asInt()).isEqualTo(100);
        mvc.perform(delete(base()+"/questions/"+deleted).header("Authorization",token(10,"ROLE_INSTRUCTOR"))).andExpect(status().isNotFound());
    }

}
