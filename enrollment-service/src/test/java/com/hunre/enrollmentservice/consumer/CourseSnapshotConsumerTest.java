package com.hunre.enrollmentservice.consumer;

import com.hunre.enrollmentservice.entity.CourseSnapshot;
import com.hunre.enrollmentservice.repository.CourseSnapshotRepository;
import com.hunre.sharedcommon.event.CourseUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseSnapshotConsumerTest {
    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final CourseSnapshotRepository repository = mock(CourseSnapshotRepository.class);
    private final CourseSnapshotConsumer consumer = new CourseSnapshotConsumer(mapper, repository);

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "null", "[]", "{}",
            "{\"eventType\":\"course.updated\"}", "{\"eventType\":\"course.updated\",\"courseId\":\"abc\"}"})
    void invalidMessagesAreRejectedForDeadLetterWithoutWriting(String payload) {
        assertThatThrownBy(() -> consumer.onMessage(payload)).isInstanceOf(InvalidCourseEventException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void unrelatedEventIsIgnored() {
        assertThatCode(() -> consumer.onMessage("{\"eventType\":\"quiz.graded\"}")).doesNotThrowAnyException();
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"courseId", "title", "slug", "status", "totalLessons", "eventId", "occurredAt"})
    void missingRequiredFieldsDoNotOverwriteExistingSnapshot(String field) {
        var node = mapper.valueToTree(event());
        ((tools.jackson.databind.node.ObjectNode) node).remove(field);
        assertThatThrownBy(() -> consumer.onMessage(mapper.writeValueAsString(node)))
                .isInstanceOf(InvalidCourseEventException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void databaseFailureEscapesForKafkaRetry() {
        var failure = new DataAccessResourceFailureException("database unavailable");
        when(repository.save(any(CourseSnapshot.class))).thenThrow(failure);
        assertThatThrownBy(() -> consumer.onMessage(mapper.writeValueAsString(event())))
                .isSameAs(failure);
    }

    @Test
    void mapsAllFieldsAndAcceptsFutureFields() {
        var node = (tools.jackson.databind.node.ObjectNode) mapper.valueToTree(event());
        node.put("futureField", "ignored");
        var before = java.time.Instant.now();
        consumer.onMessage(mapper.writeValueAsString(node));
        var capture = org.mockito.ArgumentCaptor.forClass(CourseSnapshot.class);
        verify(repository).save(capture.capture());
        var snapshot = capture.getValue();
        assertThat(snapshot.getCourseId()).isEqualTo(3L);
        assertThat(snapshot.getTitle()).isEqualTo("Java");
        assertThat(snapshot.getSlug()).isEqualTo("java");
        assertThat(snapshot.getThumbnailUrl()).isEqualTo("https://example.com/java.png");
        assertThat(snapshot.getInstructorId()).isEqualTo(7L);
        assertThat(snapshot.getInstructorName()).isEqualTo("Teacher");
        assertThat(snapshot.getTotalLessons()).isEqualTo(12);
        assertThat(snapshot.getStatus()).isEqualTo("PUBLISHED");
        assertThat(snapshot.getSyncedAt()).isBetween(before, java.time.Instant.now());
    }

    private CourseUpdatedEvent event() {
        return CourseUpdatedEvent.of(3L, "Java", "java", "https://example.com/java.png",
                7L, "Teacher", 12, "PUBLISHED");
    }
}
