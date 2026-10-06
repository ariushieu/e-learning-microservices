package com.hunre.enrollmentservice.worker;

import com.hunre.enrollmentservice.entity.OutboxEvent;
import com.hunre.enrollmentservice.repository.OutboxEventRepository;
import com.hunre.sharedcommon.event.KafkaTopics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    private ObjectMapper objectMapper;
    private OutboxPublisherWorker worker;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        worker = new OutboxPublisherWorker(outboxEventRepository, objectMapper);
        ReflectionTestUtils.setField(worker, "kafkaTemplate", kafkaTemplate);
    }

    @Test
    @DisplayName("Bỏ qua quét outbox nếu KafkaTemplate là null (chưa bật Kafka)")
    void publishPendingEvents_shouldDoNothing_whenKafkaTemplateIsNull() {
        ReflectionTestUtils.setField(worker, "kafkaTemplate", null);

        worker.publishPendingEvents();

        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    @DisplayName("Không làm gì nếu không có sự kiện outbox nào chưa gửi")
    void publishPendingEvents_shouldDoNothing_whenNoPendingEvents() {
        when(outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of());

        worker.publishPendingEvents();

        verify(outboxEventRepository).findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Gửi sự kiện thành công và cập nhật published_at vào cơ sở dữ liệu")
    void publishPendingEvents_shouldPublishAndMarkPublished_whenPendingEventsExist() {
        OutboxEvent event = OutboxEvent.builder()
                .id(1L)
                .eventId("test-uuid-1")
                .aggregateType("ENROLLMENT")
                .aggregateId("10")
                .eventType("enrollment.created")
                .payload("{\"eventId\":\"test-uuid-1\",\"userId\":42,\"courseId\":100}")
                .createdAt(Instant.now())
                .build();

        when(outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        SendResult<String, String> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send(eq(KafkaTopics.ENROLLMENT_EVENTS), eq("42"), eq(event.getPayload())))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        worker.publishPendingEvents();

        verify(kafkaTemplate).send(KafkaTopics.ENROLLMENT_EVENTS, "42", event.getPayload());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent savedEvent = captor.getValue();
        assertThat(savedEvent.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Dùng aggregateId làm khóa nếu payload không có userId")
    void publishPendingEvents_shouldFallbackToAggregateId_whenPayloadHasNoUserId() {
        OutboxEvent event = OutboxEvent.builder()
                .id(2L)
                .eventId("test-uuid-2")
                .aggregateType("CERTIFICATE")
                .aggregateId("99")
                .eventType("certificate.issued")
                .payload("{\"eventId\":\"test-uuid-2\",\"certificateCode\":\"CERT-123\"}")
                .createdAt(Instant.now())
                .build();

        when(outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        SendResult<String, String> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send(eq(KafkaTopics.ENROLLMENT_EVENTS), eq("99"), eq(event.getPayload())))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        worker.publishPendingEvents();

        verify(kafkaTemplate).send(KafkaTopics.ENROLLMENT_EVENTS, "99", event.getPayload());
        verify(outboxEventRepository).save(event);
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Nếu gửi Kafka thất bại, không cập nhật published_at và tạm dừng đợt gửi")
    void publishPendingEvents_shouldNotMarkPublishedAndStopBatch_whenKafkaSendFails() {
        OutboxEvent event1 = OutboxEvent.builder()
                .id(1L)
                .eventId("test-uuid-1")
                .aggregateType("ENROLLMENT")
                .aggregateId("10")
                .eventType("enrollment.created")
                .payload("{\"userId\":42}")
                .build();

        OutboxEvent event2 = OutboxEvent.builder()
                .id(2L)
                .eventId("test-uuid-2")
                .aggregateType("ENROLLMENT")
                .aggregateId("11")
                .eventType("enrollment.created")
                .payload("{\"userId\":43}")
                .build();

        when(outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event1, event2));

        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka Broker Unavailable"));

        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

        worker.publishPendingEvents();

        // Sự kiện 1 gửi thất bại nên không cập nhật published_at và không gọi save
        assertThat(event1.getPublishedAt()).isNull();
        verify(outboxEventRepository, never()).save(any());

        // Đợt gửi dừng lại nên sự kiện 2 không bị gửi tiếp
        verify(kafkaTemplate, never()).send(eq(KafkaTopics.ENROLLMENT_EVENTS), eq("43"), anyString());
    }
}
