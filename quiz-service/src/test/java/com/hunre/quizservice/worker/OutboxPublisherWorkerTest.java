package com.hunre.quizservice.worker;

import com.hunre.quizservice.entity.OutboxEvent;
import com.hunre.quizservice.repository.OutboxEventRepository;
import com.hunre.sharedcommon.event.KafkaTopics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherWorkerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisherWorker worker;

    @BeforeEach
    void setUp() {
        worker = new OutboxPublisherWorker(outboxEventRepository, new ObjectMapper());
        ReflectionTestUtils.setField(worker, "kafkaTemplate", kafkaTemplate);
    }

    @Test
    void kafkaDisabled_keepsPendingEvent() {
        ReflectionTestUtils.setField(worker, "kafkaTemplate", null);

        worker.publishPendingEvents();

        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void kafkaAcknowledges_marksEventPublished() {
        OutboxEvent event = pendingEvent(1L, 42L);
        when(outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        @SuppressWarnings("unchecked")
        SendResult<String, String> result = mock(SendResult.class);
        when(kafkaTemplate.send(KafkaTopics.QUIZ_EVENTS, "42", event.getPayload()))
                .thenReturn(CompletableFuture.completedFuture(result));

        worker.publishPendingEvents();

        verify(kafkaTemplate).send(KafkaTopics.QUIZ_EVENTS, "42", event.getPayload());
        verify(outboxEventRepository).save(event);
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    void kafkaFails_leavesEventPendingAndStopsBatch() {
        OutboxEvent first = pendingEvent(1L, 42L);
        OutboxEvent second = pendingEvent(2L, 43L);
        when(outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(first, second));
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        when(kafkaTemplate.send(KafkaTopics.QUIZ_EVENTS, "42", first.getPayload()))
                .thenReturn(failed);

        worker.publishPendingEvents();

        assertThat(first.getPublishedAt()).isNull();
        verify(outboxEventRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(KafkaTopics.QUIZ_EVENTS, "43", second.getPayload());
    }

    @Test
    void failedEventIsRetriedOnNextRun() {
        OutboxEvent event = pendingEvent(1L, 42L);
        when(outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        @SuppressWarnings("unchecked")
        SendResult<String, String> result = mock(SendResult.class);
        when(kafkaTemplate.send(KafkaTopics.QUIZ_EVENTS, "42", event.getPayload()))
                .thenReturn(failed, CompletableFuture.completedFuture(result));

        worker.publishPendingEvents();
        assertThat(event.getPublishedAt()).isNull();

        worker.publishPendingEvents();
        assertThat(event.getPublishedAt()).isNotNull();
        verify(outboxEventRepository).save(event);
    }

    private OutboxEvent pendingEvent(Long id, Long userId) {
        return OutboxEvent.builder()
                .id(id)
                .eventId("event-" + id)
                .aggregateType("QUIZ_ATTEMPT")
                .aggregateId(String.valueOf(id))
                .eventType("quiz.graded")
                .payload("{\"eventId\":\"event-" + id + "\",\"userId\":" + userId + "}")
                .build();
    }
}
