package com.hunre.enrollmentservice.consumer;

import com.hunre.sharedcommon.event.QuizGradedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuizGradedConsumerTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final QuizProgressProcessor processor = mock(QuizProgressProcessor.class);
    private final QuizGradedConsumer consumer = new QuizGradedConsumer(mapper, processor);

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "[]", "null", "{}", "{\"eventType\":\"quiz.graded\"}"})
    void malformedPayloadIsPermanentFailure(String payload) {
        assertThatThrownBy(() -> consumer.onMessage(payload)).isInstanceOf(InvalidQuizEventException.class);
        verifyNoInteractions(processor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"eventId", "occurredAt", "attemptId", "quizId", "courseId", "userId", "passed"})
    void requiredFieldsCannotBeMissing(String field) {
        var tree = (tools.jackson.databind.node.ObjectNode) mapper.valueToTree(event());
        tree.remove(field);
        assertThatThrownBy(() -> consumer.onMessage(tree.toString())).isInstanceOf(InvalidQuizEventException.class);
        verifyNoInteractions(processor);
    }

    @Test
    void unknownEventTypeIsIgnored() {
        consumer.onMessage("{\"eventType\":\"quiz.future-event\"}");
        verifyNoInteractions(processor);
    }

    @Test
    void transientAndBusinessConstraintErrorsAreNotMisreportedAsDuplicateEvents() {
        var event = event();
        doThrow(new DataAccessResourceFailureException("MySQL offline")).when(processor).process(event);
        assertThatThrownBy(() -> consumer.onMessage(mapper.writeValueAsString(event)))
                .isInstanceOf(DataAccessResourceFailureException.class);
        doThrow(new DataIntegrityViolationException("invalid certificate")).when(processor).process(event);
        assertThatThrownBy(() -> consumer.onMessage(mapper.writeValueAsString(event)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private QuizGradedEvent event() {
        return QuizGradedEvent.of(1L, 2L, 3L, 4L, "Quiz", BigDecimal.TEN, true, 31L);
    }
}
