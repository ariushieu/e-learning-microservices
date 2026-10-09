package com.hunre.quizservice.controller;

import com.hunre.quizservice.client.EnrollmentAccessClient;
import com.hunre.quizservice.entity.*;
import com.hunre.quizservice.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Sửa câu hỏi đã có học viên trả lời: phương án đã được chọn phải giữ nguyên id. */
@SpringBootTest(properties = {
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=" + QuestionEditAfterAnswersIntegrationTest.SECRET,
        "app.outbox.publisher.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:quiz_edit;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
class QuestionEditAfterAnswersIntegrationTest {
    static final String SECRET = "quiz-question-edit-test-secret-at-least-32-bytes";
    @Autowired MockMvc mvc;
    @Autowired QuizRepository quizzes;
    @Autowired QuizAttemptRepository attempts;
    @Autowired OutboxEventRepository outbox;
    @MockitoBean EnrollmentAccessClient enrollment;
    Quiz quiz;
    Question question;
    AnswerOption right;
    AnswerOption wrong;

    @BeforeEach void seed() {
        outbox.deleteAll(); attempts.deleteAll(); quizzes.deleteAll();
        quiz = Quiz.builder().createdBy(10L).courseId(100L).title("Sửa sau khi có bài làm")
                .status(QuizStatus.PUBLISHED).passScore(new BigDecimal("50")).maxAttempts(3).build();
        var q = Question.builder().content("2 + 2 = ?").type(QuestionType.SINGLE_CHOICE).position(1).build();
        q.addOption(AnswerOption.builder().content("4").isCorrect(true).position(1).build());
        q.addOption(AnswerOption.builder().content("5").position(2).build());
        quiz.addQuestion(q);
        quiz = quizzes.saveAndFlush(quiz);
        question = quiz.getQuestions().get(0);
        right = question.getOptions().get(0);
        wrong = question.getOptions().get(1);
        var a = QuizAttempt.builder().quiz(quiz).userId(20L).attemptNo(1).status(AttemptStatus.SUBMITTED)
                .preview(false).learnerName("S").score(BigDecimal.ZERO).passed(false).submittedAt(Instant.now()).build();
        a.addAnswer(AttemptAnswer.builder().question(question).isCorrect(false).earnedScore(BigDecimal.ZERO)
                .selectedOptions(new java.util.HashSet<>(Set.of(wrong))).build());
        attempts.saveAndFlush(a);
    }

    String token(long user, String role) {
        return "Bearer " + Jwts.builder().subject("" + user).claim("fullName", "GV " + user)
                .claim("roles", List.of(role)).expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    String path() { return "/api/quizzes/" + quiz.getId() + "/questions/" + question.getId(); }

    String body(String type, String options) {
        return "{\"content\":\"2 + 2 bằng mấy?\",\"type\":\"" + type + "\",\"score\":1,\"options\":[" + options + "]}";
    }

    String opt(Long id, String content, boolean correct, int position) {
        return "{" + (id == null ? "" : "\"id\":" + id + ",") + "\"content\":\"" + content
                + "\",\"isCorrect\":" + correct + ",\"position\":" + position + "}";
    }

    org.springframework.test.web.servlet.ResultActions edit(String body) throws Exception {
        return mvc.perform(put(path()).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", token(10, "ROLE_INSTRUCTOR")));
    }

    @Test void editingWithIdsKeepsChosenOptionsAndAddsNewOnes() throws Exception {
        edit(body("SINGLE_CHOICE", String.join(",", opt(right.getId(), "Bốn", true, 1),
                opt(wrong.getId(), "Năm", false, 2), opt(null, "Sáu", false, 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("2 + 2 bằng mấy?"))
                .andExpect(jsonPath("$.data.options.length()").value(3))
                .andExpect(jsonPath("$.data.options[0].id").value(right.getId()))
                .andExpect(jsonPath("$.data.options[0].content").value("Bốn"))
                .andExpect(jsonPath("$.data.options[1].id").value(wrong.getId()))
                .andExpect(jsonPath("$.data.options[1].content").value("Năm"))
                .andExpect(jsonPath("$.data.options[2].content").value("Sáu"));
    }

    @Test void unpickedOptionsCanBeRemovedButPickedOnesAreProtected() throws Exception {
        edit(body("SINGLE_CHOICE", String.join(",", opt(right.getId(), "4", true, 1),
                opt(wrong.getId(), "5", false, 2), opt(null, "6", false, 3)))).andExpect(status().isOk());
        // Bỏ "6" (chưa ai chọn): được.
        edit(body("SINGLE_CHOICE", String.join(",", opt(right.getId(), "4", true, 1), opt(wrong.getId(), "5", false, 2))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.options.length()").value(2));
        // Bỏ "5" (đã có học viên chọn): bị từ chối rõ ràng, không phải lỗi 500.
        edit(body("SINGLE_CHOICE", String.join(",", opt(right.getId(), "4", true, 1), opt(null, "3", false, 2))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("\"5\" vì đã có học viên chọn")));
        // Client cũ không gửi id: muốn thay cả bộ phương án cũng bị chặn như vậy.
        edit(body("SINGLE_CHOICE", String.join(",", opt(null, "4", true, 1), opt(null, "5", false, 2))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test void optionIdFromAnotherQuestionIsRejected() throws Exception {
        edit(body("SINGLE_CHOICE", String.join(",", opt(right.getId(), "4", true, 1), opt(999_999L, "5", false, 2))))
                .andExpect(status().isBadRequest());
        edit(body("SINGLE_CHOICE", String.join(",", opt(right.getId(), "4", true, 1), opt(right.getId(), "5", false, 2))))
                .andExpect(status().isBadRequest());
    }
}
