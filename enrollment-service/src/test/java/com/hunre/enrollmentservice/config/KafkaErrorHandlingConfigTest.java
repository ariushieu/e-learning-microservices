package com.hunre.enrollmentservice.config;

import com.hunre.enrollmentservice.consumer.InvalidCourseEventException;
import com.hunre.sharedcommon.event.KafkaTopics;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class KafkaErrorHandlingConfigTest {
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void changingTransientExceptionTypeDoesNotRestartTheRetryBudget() {
        KafkaOperations template = mock(KafkaOperations.class);
        when(template.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));
        var handler = new KafkaErrorHandlingConfig().courseSnapshotErrorHandler(template,
                new KafkaRetryProperties(Duration.ofMillis(1), 2, Duration.ofMillis(10), Duration.ofMillis(30)));
        Consumer consumer = mock(Consumer.class);
        var container = mock(MessageListenerContainer.class);
        var record = new ConsumerRecord<>(KafkaTopics.COURSE_EVENTS, 0, 42L, "7", "{}");
        boolean recovered = false;
        for (int attempt = 0; attempt < 20; attempt++) {
            RuntimeException cause = attempt % 2 == 0
                    ? new DataAccessResourceFailureException("database unavailable")
                    : new CannotCreateTransactionException("connection unavailable");
            try {
                handler.handleRemaining(new ListenerExecutionFailedException("failed", cause),
                        List.of(record), consumer, container);
                recovered = true;
                break;
            } catch (RuntimeException retry) {
                assertThat(retry).hasMessageContaining("Record in retry");
            }
        }
        assertThat(recovered).isTrue();
        verify(template).send(any(ProducerRecord.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void failedDeadLetterPublishSeeksOriginalOffsetAndCanRecoverOnNextAttempt() {
        KafkaOperations template = mock(KafkaOperations.class);
        when(template.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("DLT unavailable")))
                .thenReturn(CompletableFuture.completedFuture(null));
        Consumer consumer = mock(Consumer.class);
        var container = mock(MessageListenerContainer.class);
        var handler = new KafkaErrorHandlingConfig().courseSnapshotErrorHandler(template,
                new KafkaRetryProperties(Duration.ofMillis(1), 2, Duration.ofMillis(10), Duration.ofMillis(30)));
        var record = new ConsumerRecord<>(KafkaTopics.COURSE_EVENTS, 2, 41L, "7", "not-json");
        var error = new ListenerExecutionFailedException("Listener failed",
                new InvalidCourseEventException("invalid", null));
        assertThatThrownBy(() -> handler.handleRemaining(error, List.of(record), consumer, container))
                .isInstanceOf(RuntimeException.class);
        verify(consumer).seek(new TopicPartition(KafkaTopics.COURSE_EVENTS, 2), 41L);
        verify(consumer, never()).commitSync(anyMap(), any(Duration.class));
        handler.handleRemaining(error, List.of(record), consumer, container);
        verify(template, times(2)).send(any(ProducerRecord.class));
    }
}
