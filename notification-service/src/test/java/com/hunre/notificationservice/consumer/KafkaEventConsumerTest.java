package com.hunre.notificationservice.consumer;

import com.hunre.notificationservice.repository.ProcessedEventRepository;
import com.hunre.sharedcommon.event.KafkaTopics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.CannotCreateTransactionException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Consumer phải phân biệt ba thứ: message hỏng (ném {@link InvalidEventException} để sang
 * .DLT ngay), sự kiện trùng (bỏ qua êm), và lỗi tạm thời (ném nguyên vẹn để được thử lại).
 * Nhầm một trong ba là mất sự kiện hoặc kẹt consumer.
 */
class KafkaEventConsumerTest {

    private static final String TOPIC = KafkaTopics.QUIZ_EVENTS;
    private static final String VALID = """
            {"eventId":"evt-1","eventType":"quiz.graded","userId":7}""";

    private EventProcessor eventProcessor;
    private ProcessedEventRepository processedEventRepository;
    private KafkaEventConsumer consumer;

    @BeforeEach
    void setUp() {
        eventProcessor = mock(EventProcessor.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        consumer = new KafkaEventConsumer(eventProcessor, processedEventRepository, JsonMapper.builder().build());
    }

    @Test
    @DisplayName("JSON sai cú pháp: ném InvalidEventException, không gọi xử lý")
    void malformedJsonIsInvalid() {
        assertThatThrownBy(() -> consumer.onMessage("{not json", TOPIC))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining(TOPIC);
        verifyNoInteractions(eventProcessor);
    }

    @Test
    @DisplayName("Thiếu eventType: ném InvalidEventException, không gọi xử lý")
    void missingEventTypeIsInvalid() {
        assertThatThrownBy(() -> consumer.onMessage("{\"eventId\":\"evt-1\"}", TOPIC))
                .isInstanceOf(InvalidEventException.class);
        verifyNoInteractions(eventProcessor);
    }

    @Test
    @DisplayName("Sự kiện đã có trong sổ: bỏ qua, không ném lỗi")
    void duplicateIsSkipped() {
        doThrow(new DataIntegrityViolationException("Duplicate entry 'evt-1'"))
                .when(eventProcessor).process(any(), any(), any(), any());
        when(processedEventRepository.existsById("evt-1")).thenReturn(true);

        assertThatCode(() -> consumer.onMessage(VALID, TOPIC)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Lỗi ràng buộc mà sổ không có sự kiện: là dữ liệu hỏng, không phải trùng")
    void constraintViolationWithoutLedgerEntryIsInvalid() {
        doThrow(new DataIntegrityViolationException("Column 'title' cannot be null"))
                .when(eventProcessor).process(any(), any(), any(), any());
        when(processedEventRepository.existsById("evt-1")).thenReturn(false);

        assertThatThrownBy(() -> consumer.onMessage(VALID, TOPIC))
                .isInstanceOf(InvalidEventException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Mất kết nối database: ném nguyên lỗi để được thử lại, không nuốt")
    void transientFailurePropagates() {
        CannotCreateTransactionException dbDown = new CannotCreateTransactionException("Connection refused");
        doThrow(dbDown).when(eventProcessor).process(any(), any(), any(), any());

        assertThatThrownBy(() -> consumer.onMessage(VALID, TOPIC)).isSameAs(dbDown);
    }

    @Test
    @DisplayName("Message hợp lệ: giao đúng eventId, eventType, topic cho EventProcessor")
    void validMessageIsProcessed() {
        consumer.onMessage(VALID, TOPIC);

        verify(eventProcessor).process("evt-1", "quiz.graded", TOPIC, VALID);
    }
}
