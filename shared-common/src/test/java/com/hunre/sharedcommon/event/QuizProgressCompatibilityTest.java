package com.hunre.sharedcommon.event;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizProgressCompatibilityTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void oldQuizPayloadWithoutLessonIdStillDeserializes() {
        var event = QuizGradedEvent.of(1L, 2L, 3L, 4L, "Quiz", new BigDecimal("85.50"), true);
        var tree = mapper.valueToTree(event).deepCopy();
        ((tools.jackson.databind.node.ObjectNode) tree).remove("lessonId");
        var read = mapper.treeToValue(tree, QuizGradedEvent.class);
        assertThat(read.lessonId()).isNull();
        assertThat(read.score()).isEqualByComparingTo("85.50");
        assertThat(read.passed()).isTrue();
    }

    @Test
    void lessonIdAndFutureFieldsDoNotBreakQuizConsumers() {
        var event = QuizGradedEvent.of(1L, 2L, 3L, 4L, "Quiz", BigDecimal.TEN, false, 31L);
        var tree = (tools.jackson.databind.node.ObjectNode) mapper.valueToTree(event);
        tree.put("futureField", "ignored");
        assertThat(mapper.treeToValue(tree, QuizGradedEvent.class)).isEqualTo(event);
    }

    @Test
    void unknownAndEmptyCurriculumRemainDistinct() {
        var old = CourseUpdatedEvent.of(3L, "Course", "course", null, 7L, null, 0, "PUBLISHED");
        var tree = (tools.jackson.databind.node.ObjectNode) mapper.valueToTree(old);
        tree.remove("lessonIds");
        assertThat(mapper.treeToValue(tree, CourseUpdatedEvent.class).lessonIds()).isNull();
        var empty = CourseUpdatedEvent.of(3L, "Course", "course", null, 7L, null, 0, "PUBLISHED", List.of());
        assertThat(mapper.readValue(mapper.writeValueAsString(empty), CourseUpdatedEvent.class).lessonIds()).isEmpty();
    }
}
