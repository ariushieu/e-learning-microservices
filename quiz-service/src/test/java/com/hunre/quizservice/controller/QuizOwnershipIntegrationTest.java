package com.hunre.quizservice.controller;

import com.hunre.quizservice.client.CourseOwnershipClient;
import com.hunre.quizservice.entity.*;
import com.hunre.quizservice.repository.*;
import com.hunre.quizservice.service.QuestionService;
import com.hunre.quizservice.service.QuizService;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=quiz-ownership-integration-test-secret-at-least-32-bytes",
        "app.outbox.publisher.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:quiz_ownership;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
@Transactional
class QuizOwnershipIntegrationTest {
    private static final String SECRET = "quiz-ownership-integration-test-secret-at-least-32-bytes";
    private static final String QUESTION_BODY = """
            {"content":"Changed question","type":"SINGLE_CHOICE","explanation":"Secret explanation",
             "options":[{"content":"Correct","isCorrect":true},{"content":"Wrong","isCorrect":false}]}
            """;
    @Autowired MockMvc mvc;
    @Autowired QuizRepository quizzes;
    @Autowired QuestionRepository questions;
    @Autowired EntityManager em;
    @Autowired QuizService quizService;
    @Autowired QuestionService questionService;
    @MockitoBean CourseOwnershipClient courseClient;
    Quiz quiz;
    Question question;

    @BeforeEach
    void seed() {
        quiz = Quiz.builder().courseId(10L).createdBy(50L).title("Original quiz").build();
        question = Question.builder().content("Original question").type(QuestionType.SINGLE_CHOICE)
                .explanation("Secret explanation").build();
        question.addOption(AnswerOption.builder().content("Correct").isCorrect(true).position(1).build());
        question.addOption(AnswerOption.builder().content("Wrong").isCorrect(false).position(2).build());
        quiz.addQuestion(question);
        quiz = quizzes.saveAndFlush(quiz);
        em.clear();
    }

    private String token(long userId, String role) {
        return "Bearer " + Jwts.builder().subject(Long.toString(userId)).claim("roles", List.of(role))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private MockHttpServletRequestBuilder operation(int operation) {
        String path = "/api/quizzes/" + quiz.getId();
        return switch (operation) {
            case 0 -> put(path).content("{\"title\":\"Changed quiz\"}");
            case 1 -> patch(path + "/status").content("{\"status\":\"PUBLISHED\"}");
            case 2 -> patch(path + "/status").content("{\"status\":\"ARCHIVED\"}");
            case 3 -> delete(path);
            case 4 -> post(path + "/questions").content(QUESTION_BODY);
            case 5 -> put(path + "/questions/" + question.getId()).content(QUESTION_BODY);
            case 6 -> delete(path + "/questions/" + question.getId());
            case 7 -> get(path);
            case 8 -> get(path + "/questions");
            default -> throw new IllegalArgumentException();
        };
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,2,3,4,5,6,7,8})
    void anotherInstructorCannotManageOrReadAnswers(int operation) throws Exception {
        assertRejected(operation, token(60, "ROLE_INSTRUCTOR"), 403);
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,2,3,4,5,6,7,8})
    void studentCannotManageEvenIfTheyWereTheCreator(int operation) throws Exception {
        assertRejected(operation, token(50, "ROLE_STUDENT"), 403);
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,2,3,4,5,6,7,8})
    void missingTokenCannotBypassOwnership(int operation) throws Exception {
        assertRejected(operation, null, 401);
    }

    private void assertRejected(int operation, String token, int status) throws Exception {
        long quizCount = quizzes.count(), questionCount = questions.count();
        var request = operation(operation).contentType(MediaType.APPLICATION_JSON);
        if (token != null) request.header("Authorization", token);
        mvc.perform(request).andExpect(status().is(status));
        em.flush();
        em.clear();
        assertThat(quizzes.count()).isEqualTo(quizCount);
        assertThat(questions.count()).isEqualTo(questionCount);
        Quiz unchanged = quizzes.findById(quiz.getId()).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo("Original quiz");
        assertThat(unchanged.getStatus()).isEqualTo(QuizStatus.DRAFT);
        Question unchangedQuestion = questions.findById(question.getId()).orElseThrow();
        assertThat(unchangedQuestion.getContent()).isEqualTo("Original question");
        assertThat(unchangedQuestion.getOptions()).hasSize(2);
        verifyNoInteractions(courseClient);
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,2,3,4,5,6,7,8})
    void ownerCanManageAndReadAnswers(int operation) throws Exception {
        assertAllowed(operation, 50, "ROLE_INSTRUCTOR");
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,2,3,4,5,6,7,8})
    void adminCanManageAndReadAnswers(int operation) throws Exception {
        assertAllowed(operation, 99, "ROLE_ADMIN");
    }

    private void assertAllowed(int operation, long userId, String role) throws Exception {
        mvc.perform(operation(operation).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token(userId, role)))
                .andExpect(status().is(operation == 4 ? 201 : 200));
        em.flush();
        em.clear();
        switch (operation) {
            case 0 -> assertThat(quizzes.findById(quiz.getId()).orElseThrow().getTitle()).isEqualTo("Changed quiz");
            case 1 -> assertThat(quizzes.findById(quiz.getId()).orElseThrow().getStatus()).isEqualTo(QuizStatus.PUBLISHED);
            case 2 -> assertThat(quizzes.findById(quiz.getId()).orElseThrow().getStatus()).isEqualTo(QuizStatus.ARCHIVED);
            case 3 -> assertThat(quizzes.existsById(quiz.getId())).isFalse();
            case 4 -> assertThat(questions.findByQuizIdOrderByPositionAsc(quiz.getId())).hasSize(2);
            case 5 -> assertThat(questions.findById(question.getId()).orElseThrow().getContent()).isEqualTo("Changed question");
            case 6 -> assertThat(questions.existsById(question.getId())).isFalse();
            default -> { }
        }
        verifyNoInteractions(courseClient);
    }

    @ParameterizedTest
    @CsvSource({"50,ROLE_INSTRUCTOR,false", "99,ROLE_ADMIN,true"})
    void creationUsesTokenIdentityAndChecksCourse(long id, String role, boolean admin) throws Exception {
        String authorization = token(id, role);
        mvc.perform(post("/api/quizzes").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":10,\"title\":\"New quiz\",\"createdBy\":666}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.createdBy").value(id));
        verify(courseClient).requireCourseOwner(10L, id, admin, authorization);
    }

    @ParameterizedTest
    @CsvSource({"FORBIDDEN,403", "RESOURCE_NOT_FOUND,404", "EXTERNAL_SERVICE_ERROR,502", "UNAUTHORIZED,401"})
    void creationDoesNotPersistWhenCourseValidationFails(ErrorCode code, int status) throws Exception {
        String authorization = token(60, "ROLE_INSTRUCTOR");
        doThrow(new BusinessException(code, "Course validation failed")).when(courseClient)
                .requireCourseOwner(10L, 60L, false, authorization);
        long count = quizzes.count();
        mvc.perform(post("/api/quizzes").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":10,\"title\":\"New quiz\"}"))
                .andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code.name()));
        em.flush();
        assertThat(quizzes.count()).isEqualTo(count);
    }

    @Test
    void creationRequiresAuthenticationAndManagerRole() throws Exception {
        for (String authorization : new String[]{"", token(60, "ROLE_STUDENT")}) {
            mvc.perform(post("/api/quizzes").header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":10,\"title\":\"New quiz\"}"))
                    .andExpect(status().is(authorization.isEmpty() ? 401 : 403));
        }
        verifyNoInteractions(courseClient);
        assertThat(quizzes.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"50,ROLE_INSTRUCTOR,3", "60,ROLE_INSTRUCTOR,1", "99,ROLE_ADMIN,4", "50,ROLE_STUDENT,1"})
    void listingOnlyIncludesPublishedOrOwnQuizzes(long userId, String role, int expected) throws Exception {
        quizzes.save(Quiz.builder().courseId(10L).createdBy(50L).title("Archived").status(QuizStatus.ARCHIVED).build());
        quizzes.save(Quiz.builder().courseId(10L).createdBy(60L).title("Published").status(QuizStatus.PUBLISHED).build());
        quizzes.save(Quiz.builder().courseId(10L).createdBy(70L).title("Other draft").build());
        quizzes.save(Quiz.builder().courseId(20L).createdBy(50L).title("Different course").status(QuizStatus.PUBLISHED).build());
        mvc.perform(get("/api/quizzes").param("courseId", "10").header("Authorization", token(userId, role)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(expected));
        verifyNoInteractions(courseClient);
    }

    @Test
    void studentListingContainsNoDraftEvenWithForgedQueryIdentity() throws Exception {
        mvc.perform(get("/api/quizzes").param("courseId", "10").param("userId", "50").param("isAdmin", "true")
                        .header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/quizzes").param("courseId", "10")).andExpect(status().isUnauthorized());
    }

    @Test
    void cannotUseOwnQuizPathToChangeAnotherQuizzesQuestion() throws Exception {
        Quiz other = quizzes.saveAndFlush(Quiz.builder().courseId(10L).createdBy(60L).title("Other").build());
        mvc.perform(put("/api/quizzes/" + other.getId() + "/questions/" + question.getId())
                        .header("Authorization", token(60, "ROLE_INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(QUESTION_BODY))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/quizzes/" + other.getId() + "/questions/" + question.getId())
                        .header("Authorization", token(60, "ROLE_INSTRUCTOR")))
                .andExpect(status().isForbidden());
        assertThat(questions.findById(question.getId()).orElseThrow().getContent()).isEqualTo("Original question");
    }

    @Test
    void evenAdminCannotUseQuestionUnderWrongParent() throws Exception {
        Quiz other = quizzes.saveAndFlush(Quiz.builder().courseId(10L).createdBy(60L).title("Other").build());
        mvc.perform(put("/api/quizzes/" + other.getId() + "/questions/" + question.getId())
                        .header("Authorization", token(99, "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(QUESTION_BODY))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/quizzes/" + other.getId() + "/questions/" + question.getId())
                        .header("Authorization", token(99, "ROLE_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingResourcesAndMalformedIdsKeepExpectedHttpErrors() throws Exception {
        String authorization = token(50, "ROLE_INSTRUCTOR");
        for (String path : new String[]{"/api/quizzes/999999", "/api/quizzes/999999/questions"}) {
            mvc.perform(get(path).header("Authorization", authorization)).andExpect(status().isNotFound());
        }
        mvc.perform(put("/api/quizzes/999999").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Changed\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/quizzes/" + quiz.getId() + "/questions/999999")
                        .header("Authorization", authorization)).andExpect(status().isNotFound());
        mvc.perform(get("/api/quizzes/abc").header("Authorization", authorization)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/quizzes").param("courseId", "abc").header("Authorization", authorization))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/quizzes").header("Authorization", authorization)).andExpect(status().isBadRequest());
        verifyNoInteractions(courseClient);
    }

    @Test
    void draftsCannotBeTakenOrStartedByStudents() throws Exception {
        String authorization = token(60, "ROLE_STUDENT");
        mvc.perform(get("/api/quizzes/" + quiz.getId() + "/take").header("Authorization", authorization))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/quizzes/" + quiz.getId() + "/attempts").header("Authorization", authorization))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void publishedStudentViewStillHidesAnswers() throws Exception {
        Quiz published = quizzes.findById(quiz.getId()).orElseThrow();
        published.setStatus(QuizStatus.PUBLISHED);
        mvc.perform(get("/api/quizzes/" + quiz.getId() + "/take").header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].explanation").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].options[0].isCorrect").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nullIdentityCannotBypassServiceOwnershipEvenAsAdmin(boolean admin) {
        assertThatThrownBy(() -> quizService.deleteQuiz(quiz.getId(), null, admin))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThatThrownBy(() -> questionService.deleteQuestion(quiz.getId(), question.getId(), null, admin))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
