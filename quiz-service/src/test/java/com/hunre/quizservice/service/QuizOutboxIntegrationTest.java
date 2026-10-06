package com.hunre.quizservice.service;

import com.hunre.quizservice.dto.SubmitAnswerItemRequest;
import com.hunre.quizservice.dto.SubmitQuizAttemptRequest;
import com.hunre.quizservice.entity.AnswerOption;
import com.hunre.quizservice.entity.AttemptStatus;
import com.hunre.quizservice.entity.OutboxEvent;
import com.hunre.quizservice.entity.Question;
import com.hunre.quizservice.entity.Quiz;
import com.hunre.quizservice.entity.QuizStatus;
import com.hunre.quizservice.repository.OutboxEventRepository;
import com.hunre.quizservice.repository.QuizAttemptRepository;
import com.hunre.quizservice.repository.QuizRepository;
import com.hunre.sharedcommon.event.EventTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.outbox.publisher.enabled=false")
class QuizOutboxIntegrationTest {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private QuizAttemptService quizAttemptService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void submittingAttemptCommitsResultAndPendingOutboxEventWithoutKafka() {
        Quiz quiz = Quiz.builder()
                .courseId(12L)
                .title("Outbox test quiz")
                .createdBy(7L)
                .status(QuizStatus.PUBLISHED)
                .build();
        Question question = Question.builder().content("2 + 2 = ?").build();
        AnswerOption correct = AnswerOption.builder().content("4").isCorrect(true).build();
        question.addOption(correct);
        quiz.addQuestion(question);
        quiz = quizRepository.saveAndFlush(quiz);

        Long attemptId = quizAttemptService.startAttempt(quiz.getId(), 99L).getId();
        SubmitQuizAttemptRequest request = SubmitQuizAttemptRequest.builder()
                .answers(List.of(SubmitAnswerItemRequest.builder()
                        .questionId(question.getId())
                        .selectedOptionIds(Set.of(correct.getId()))
                        .build()))
                .build();

        var result = quizAttemptService.submitAttempt(attemptId, 99L, request);

        assertThat(result.getScore()).isEqualByComparingTo("100.00");
        var savedAttempt = quizAttemptRepository.findById(attemptId).orElseThrow();
        assertThat(savedAttempt.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(savedAttempt.getPassed()).isTrue();

        List<OutboxEvent> pending = outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        assertThat(pending).hasSize(1);
        OutboxEvent event = pending.get(0);
        assertThat(event.getAggregateType()).isEqualTo("QUIZ_ATTEMPT");
        assertThat(event.getAggregateId()).isEqualTo(attemptId.toString());
        assertThat(event.getEventType()).isEqualTo(EventTypes.QUIZ_GRADED);
        JsonNode payload = objectMapper.readTree(event.getPayload());
        // H2's JSON column can wrap a bound String as a JSON string.
        if (payload.isTextual()) {
            payload = objectMapper.readTree(payload.asString());
        }
        assertThat(payload.get("attemptId").asLong()).isEqualTo(attemptId);
        assertThat(payload.get("quizId").asLong()).isEqualTo(quiz.getId());
        assertThat(payload.get("userId").asLong()).isEqualTo(99L);
        assertThat(payload.get("passed").asBoolean()).isTrue();
    }
}
