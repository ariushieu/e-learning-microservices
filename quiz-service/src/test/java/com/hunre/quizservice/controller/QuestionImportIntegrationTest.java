package com.hunre.quizservice.controller;

import com.hunre.quizservice.entity.Quiz;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=quiz-import-test-secret-at-least-32-bytes",
        "app.outbox.publisher.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:quiz_import;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
class QuestionImportIntegrationTest {
    static final String SECRET = "quiz-import-test-secret-at-least-32-bytes";
    static final String HEADER = "type,content,score,explanation,option1,option2\r\n";
    static final String ROW = "SINGLE_CHOICE,Câu hỏi,1,Giải thích,*Đúng,Sai\r\n";
    @Autowired MockMvc mvc;
    @Autowired QuizRepository quizzes;
    @Autowired QuizAttemptRepository attempts;
    @Autowired OutboxEventRepository outbox;
    @Autowired QuestionRepository questions;
    @MockitoSpyBean com.hunre.quizservice.service.impl.QuestionServiceImpl questionService;
    Quiz quiz;

    @BeforeEach void seed() {
        outbox.deleteAll(); attempts.deleteAll(); quizzes.deleteAll();
        quiz = quizzes.saveAndFlush(Quiz.builder().createdBy(10L).courseId(100L).title("Import CSV").build());
    }
    String path() { return "/api/quizzes/" + quiz.getId() + "/questions/import"; }
    String token(long user, String role) {
        return "Bearer " + Jwts.builder().subject("" + user).claim("roles", List.of(role))
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
    ResultActions upload(byte[] bytes, long user, String role) throws Exception {
        return mvc.perform(multipart(path()).file(new MockMultipartFile("file", "questions.csv", "text/csv", bytes))
                .header("Authorization", token(user, role)));
    }
    ResultActions upload(String csv) throws Exception { return upload(csv.getBytes(StandardCharsets.UTF_8), 10, "ROLE_INSTRUCTOR"); }
    void rejected(String csv, int line) throws Exception {
        upload(csv).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].line").value(line));
        assertThat(questions.count()).isZero();
    }

    @Test void downloadedSampleImportsThreeTypesWithCorrectAnswersAndAppendsPositions() throws Exception {
        byte[] sample = mvc.perform(get(path() + "/template").header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk()).andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"mau-cau-hoi.csv\""))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(sample).startsWith((byte)0xef, (byte)0xbb, (byte)0xbf);
        upload(HEADER + ROW).andExpect(status().isCreated());
        upload(sample, 10, "ROLE_INSTRUCTOR").andExpect(status().isCreated()).andExpect(jsonPath("$.data.imported").value(3));
        mvc.perform(get("/api/quizzes/" + quiz.getId() + "/questions").header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[1].position").value(2))
                .andExpect(jsonPath("$.data[1].type").value("SINGLE_CHOICE"))
                .andExpect(jsonPath("$.data[2].type").value("MULTIPLE_CHOICE"))
                .andExpect(jsonPath("$.data[3].type").value("TRUE_FALSE"))
                .andExpect(jsonPath("$.data[1].options[0].isCorrect").value(true))
                .andExpect(jsonPath("$.data[1].options[0].content").value("Hà Nội"));
    }
    @Test void adminCanImport() throws Exception {
        upload((HEADER + ROW).getBytes(StandardCharsets.UTF_8), 99, "ROLE_ADMIN").andExpect(status().isCreated());
        assertThat(questions.count()).isEqualTo(1);
    }
    @ParameterizedTest @ValueSource(strings = {"ROLE_INSTRUCTOR", "ROLE_STUDENT"})
    void otherUsersCannotImportOrDownloadTemplate(String role) throws Exception {
        upload((HEADER + ROW).getBytes(StandardCharsets.UTF_8), 20, role).andExpect(status().isForbidden());
        mvc.perform(get(path() + "/template").header("Authorization", token(20, role))).andExpect(status().isForbidden());
        assertThat(questions.count()).isZero();
    }
    @Test void anonymousUnknownQuizAndMissingPartAreRejected() throws Exception {
        mvc.perform(multipart(path()).file("file", (HEADER + ROW).getBytes(StandardCharsets.UTF_8))).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/quizzes/999999/questions/import").file("file", (HEADER + ROW).getBytes(StandardCharsets.UTF_8))
                .header("Authorization", token(10, "ROLE_INSTRUCTOR"))).andExpect(status().isNotFound());
        mvc.perform(multipart(path()).header("Authorization", token(10, "ROLE_INSTRUCTOR")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].line").value(1));
    }
    @Test void invalidRowsReportAllLinesAndLeaveExistingQuestionsUnchanged() throws Exception {
        upload(HEADER + ROW).andExpect(status().isCreated());
        upload(HEADER + ROW.replace("*Đúng", "Đúng") + ROW + ROW.replace("1,Giải", "0,Giải"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].line").value(2)).andExpect(jsonPath("$.errors[1].line").value(4));
        assertThat(questions.count()).isEqualTo(1);
    }
    @Test void quotedMultilineCellsPreserveTextAndErrorsUsePhysicalStartLine() throws Exception {
        String row = "SINGLE_CHOICE,\"Nội dung, \"\"trích dẫn\"\"\r\nDòng tiếp\",1,\"Giải\r\nthích\",*Đúng,Sai\r\n";
        rejected(HEADER + row + ROW.replace("*Đúng", "Đúng"), 5);
        upload("\uFEFF" + HEADER + row).andExpect(status().isCreated());
        assertThat(questions.findAll().get(0).getContent()).isEqualTo("Nội dung, \"trích dẫn\"\r\nDòng tiếp");
    }
    @Test void excelSemicolonDelimiterWorks() throws Exception {
        upload("\uFEFF" + (HEADER + ROW).replace(',', ';')).andExpect(status().isCreated());
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "1000", "1.001", "NaN", "1e2", "", "1,5"})
    void rejectsInvalidScores(String score) throws Exception { rejected(HEADER + ROW.replace(",1,", ",\"" + score + "\","), 2); }
    @ParameterizedTest @ValueSource(strings = {"SINGLE_CHOICE,Q,1,,*A,*B", "MULTIPLE_CHOICE,Q,1,,A,B", "TRUE_FALSE,Q,1,,A,B", "UNKNOWN,Q,1,,*A,B", "SINGLE_CHOICE,,1,,*A,B", "SINGLE_CHOICE,Q,1,,*,B", "SINGLE_CHOICE,Q,1,,*A,"})
    void rejectsInvalidQuestions(String row) throws Exception { rejected(HEADER + row, 2); }
    @Test void rejectsTrueFalseWithThreeOptionsAndBlankMiddleOption() throws Exception {
        String header = HEADER.stripTrailing() + ",option3\r\n";
        rejected(header + "TRUE_FALSE,Q,1,,*A,B,C", 2);
        rejected(header + "MULTIPLE_CHOICE,Q,1,,*A,,C", 2);
    }
    @Test void rejectsOversizedCells() throws Exception {
        rejected(HEADER + ROW.replace("Câu hỏi", "a".repeat(10001)), 2);
        rejected(HEADER + ROW.replace("Giải thích", "a".repeat(10001)), 2);
        rejected(HEADER + ROW.replace("Đúng", "a".repeat(1001)), 2);
    }
    @Test void enforcesTwoHundredQuestionLimitWithoutPartialWrite() throws Exception {
        rejected(HEADER + ROW.repeat(201), 202);
        upload(HEADER + ROW.repeat(200)).andExpect(status().isCreated()).andExpect(jsonPath("$.data.imported").value(200));
        assertThat(questions.count()).isEqualTo(200);
    }
    @Test void rejectsOversizedEmptyInvalidEncodingAndMalformedCsv() throws Exception {
        upload(new byte[2 * 1024 * 1024], 10, "ROLE_INSTRUCTOR").andExpect(status().isBadRequest());
        upload(new byte[]{(byte)0xff}, 10, "ROLE_INSTRUCTOR").andExpect(status().isBadRequest());
        rejected("", 1); rejected(HEADER, 2); rejected("bad,header\r\n" + ROW, 1);
        rejected(HEADER + "SINGLE_CHOICE,\"unclosed", 2);
        rejected(HEADER + "SINGLE_CHOICE,Q,1,,*A,B,extra", 2);
        rejected(HEADER + "SINGLE_CHOICE,\"Q\"garbage,1,,*A,B", 2);
    }
    @Test void databaseFailureOnSecondQuestionRollsBackFirstQuestionAndOptions() throws Exception {
        var calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 2) throw new IllegalStateException("Injected persistence failure");
            return invocation.callRealMethod();
        }).when(questionService).addQuestion(anyLong(), any(), anyLong(), anyBoolean());
        upload(HEADER + ROW.repeat(2)).andExpect(status().is5xxServerError());
        assertThat(calls.get()).isEqualTo(2);
        assertThat(questions.count()).isZero();
    }
}
