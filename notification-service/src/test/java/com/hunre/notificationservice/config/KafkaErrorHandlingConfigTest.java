package com.hunre.notificationservice.config;

import com.hunre.notificationservice.consumer.InvalidEventException;
import com.hunre.sharedcommon.event.KafkaTopics;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.transaction.CannotCreateTransactionException;
import tools.jackson.core.exc.StreamReadException;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm cách phân loại lỗi của {@link KafkaErrorHandlingConfig} mà không cần Kafka thật:
 * gọi thẳng {@code DefaultErrorHandler} như listener container vẫn gọi khi listener ném lỗi,
 * rồi xem nó gửi sang .DLT hay tua lại để thử lại.
 */
class KafkaErrorHandlingConfigTest {

    private static final String TOPIC = KafkaTopics.QUIZ_EVENTS;

    @SuppressWarnings("rawtypes")
    private KafkaOperations template;
    private Consumer<?, ?> consumer;
    private MessageListenerContainer container;
    private DefaultErrorHandler handler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        template = mock(KafkaOperations.class);
        when(template.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));
        consumer = mock(Consumer.class);
        container = mock(MessageListenerContainer.class);

        // Thử lại 50ms một lần, tối đa 10 phút: đủ để chắc lần lỗi đầu chưa bị bỏ cuộc.
        KafkaRetryProperties retry = new KafkaRetryProperties(
                Duration.ofMillis(50), 1.0, Duration.ofMillis(50), Duration.ofMinutes(10));
        handler = new KafkaErrorHandlingConfig().kafkaErrorHandler(template, retry);
    }

    @Test
    @DisplayName("Message hỏng: sang <topic>.DLT ngay lần đầu, giữ nguyên nội dung")
    @SuppressWarnings("unchecked")
    void invalidEventGoesStraightToDeadLetter() {
        ConsumerRecord<String, String> record = record("{not json");

        handler.handleRemaining(wrap(new InvalidEventException("hỏng")), List.of(record), consumer, container);

        ArgumentCaptor<ProducerRecord<Object, Object>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(template).send(sent.capture());
        assertThat(sent.getValue().topic()).isEqualTo(TOPIC + ".DLT");
        assertThat(sent.getValue().partition()).as("để Kafka tự chọn partition").isNull();
        assertThat(sent.getValue().value()).isEqualTo("{not json");
        verify(consumer, never()).seek(any(TopicPartition.class), anyLong());
    }

    @Test
    @DisplayName("JSON không khớp lớp sự kiện (lỗi Jackson): cũng sang .DLT ngay")
    @SuppressWarnings("unchecked")
    void jacksonFailureGoesStraightToDeadLetter() {
        handler.handleRemaining(wrap(new StreamReadException(null, "Unexpected character")),
                List.of(record("{}")), consumer, container);

        verify(template).send(any(ProducerRecord.class));
    }

    @Test
    @DisplayName("Mất kết nối database: không gửi .DLT, tua lại để đọc message đó lần nữa")
    @SuppressWarnings("unchecked")
    void transientFailureIsRetried() {
        ConsumerRecord<String, String> record = record("{\"eventId\":\"evt-1\"}");

        // Ném lại lỗi sau khi tua là cách DefaultErrorHandler báo cho container "chưa xong,
        // đừng commit offset". Container bắt lỗi này, không phải lỗi của test.
        assertThatThrownBy(() -> handler.handleRemaining(
                wrap(new CannotCreateTransactionException("Connection refused")),
                List.of(record), consumer, container))
                .hasMessageContaining("Record in retry");

        verify(template, never()).send(any(ProducerRecord.class));
        verify(consumer).seek(eq(new TopicPartition(TOPIC, 2)), eq(41L));
    }

    private ConsumerRecord<String, String> record(String value) {
        return new ConsumerRecord<>(TOPIC, 2, 41L, "7", value);
    }

    /** Listener container luôn bọc lỗi của listener trong lớp này trước khi giao cho handler. */
    private Exception wrap(Exception cause) {
        return new ListenerExecutionFailedException("Listener failed", cause);
    }
}
