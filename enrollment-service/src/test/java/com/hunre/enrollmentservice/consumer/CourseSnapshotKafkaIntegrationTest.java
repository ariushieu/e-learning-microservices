package com.hunre.enrollmentservice.consumer;

import com.hunre.enrollmentservice.dto.request.EnrollCourseRequest;
import com.hunre.enrollmentservice.repository.CourseSnapshotRepository;
import com.hunre.enrollmentservice.repository.OutboxEventRepository;
import com.hunre.enrollmentservice.service.EnrollmentService;
import com.hunre.sharedcommon.event.CourseUpdatedEvent;
import com.hunre.sharedcommon.event.KafkaTopics;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/** Broker Kafka thật trong JVM + database H2: kiểm cả cấu hình listener và nghiệp vụ ghi danh. */
@SpringBootTest(properties = {
        "spring.kafka.enabled=true", "app.outbox.publisher.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:snapshot_kafka;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.kafka.consumer.group-id=enrollment-snapshot-test",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.enable-auto-commit=false",
        "spring.kafka.listener.ack-mode=record",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "elearning.kafka.retry.initial-interval=50ms", "elearning.kafka.retry.multiplier=2",
        "elearning.kafka.retry.max-interval=100ms", "elearning.kafka.retry.max-elapsed-time=350ms"
})
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.COURSE_EVENTS, KafkaTopics.COURSE_EVENTS + ".DLT"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class CourseSnapshotKafkaIntegrationTest {
    private static final String DLT = KafkaTopics.COURSE_EVENTS + ".DLT";
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.hunre.enrollmentservice.client.CourseLessonClient courseLessonClient;
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired ObjectMapper mapper;
    @Autowired CourseSnapshotRepository snapshots;
    @Autowired EnrollmentService enrollments;
    @Autowired OutboxEventRepository outbox;
    @Autowired EmbeddedKafkaBroker broker;
    @MockitoSpyBean CourseSnapshotConsumer consumer;

    @Test
    void publishDuplicateUpdateAndArchiveControlEnrollment() throws Exception {
        var published = CourseUpdatedEvent.of(301L, "Java", "java", "cover.png", 7L, "Teacher", 2, "PUBLISHED");
        send(published);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(snapshots.findById(301L)).isPresent());
        var enrolled = enrollments.enroll(901L, EnrollCourseRequest.builder().courseId(301L).build());
        assertThat(enrolled.getCourseTitle()).isEqualTo("Java");
        assertThat(outbox.findAll()).anyMatch(e -> "enrollment.created".equals(e.getEventType()));

        var firstSync = snapshots.findById(301L).orElseThrow().getSyncedAt();
        send(published);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(snapshots.findById(301L).orElseThrow().getSyncedAt()).isAfter(firstSync));
        assertThat(snapshots.findAll().stream().filter(s -> s.getCourseId().equals(301L))).hasSize(1);

        // Null phải xóa giá trị cũ; số bài và trạng thái phải được thay thế cùng snapshot.
        send(CourseUpdatedEvent.of(301L, "Java updated", "java-updated", null, null, null, 5, "ARCHIVED"));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var snapshot = snapshots.findById(301L).orElseThrow();
            assertThat(snapshot.getStatus()).isEqualTo("ARCHIVED");
            assertThat(snapshot.getTitle()).isEqualTo("Java updated");
            assertThat(snapshot.getTotalLessons()).isEqualTo(5);
            assertThat(snapshot.getSlug()).isEqualTo("java-updated");
            assertThat(snapshot.getThumbnailUrl()).isNull();
            assertThat(snapshot.getInstructorId()).isNull();
            assertThat(snapshot.getInstructorName()).isNull();
        });
        assertThatThrownBy(() -> enrollments.enroll(902L, EnrollCourseRequest.builder().courseId(301L).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void malformedMessageDoesNotBlockFollowingValidSnapshot() throws Exception {
        try (var deadLetters = deadLetterConsumer()) {
            kafka.send(KafkaTopics.COURSE_EVENTS, "302", "not-json").get(10, TimeUnit.SECONDS);
            kafka.send(KafkaTopics.COURSE_EVENTS, "302", "{\"eventType\":\"quiz.graded\"}").get(10, TimeUnit.SECONDS);
            send(CourseUpdatedEvent.of(302L, "Valid", "valid", null, 7L, null, 1, "PUBLISHED"));
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                    assertThat(snapshots.findById(302L)).isPresent());
            assertDeadLetter(deadLetters, "302", "not-json");
        }
    }

    private void send(CourseUpdatedEvent event) throws Exception {
        kafka.send(KafkaTopics.COURSE_EVENTS, event.courseId().toString(), mapper.writeValueAsString(event))
                .get(10, TimeUnit.SECONDS);
    }

    @Test
    void transientDatabaseFailureRetriesBeforeLaterSnapshot() throws Exception {
        var published = CourseUpdatedEvent.of(303L, "Retry", "retry", null, 7L, null, 1, "PUBLISHED");
        String payload = mapper.writeValueAsString(published);
        doThrow(new DataAccessResourceFailureException("simulated temporary database outage"))
                .doThrow(new DataAccessResourceFailureException("database still unavailable"))
                .doCallRealMethod().when(consumer).onMessage(payload);
        kafka.send(KafkaTopics.COURSE_EVENTS, "303", payload).get(10, TimeUnit.SECONDS);
        send(CourseUpdatedEvent.of(303L, "Retry", "retry", null, 7L, null, 1, "ARCHIVED"));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(snapshots.findById(303L)).isPresent();
            assertThat(snapshots.findById(303L).orElseThrow().getStatus()).isEqualTo("ARCHIVED");
            verify(consumer, atLeast(3)).onMessage(payload);
        });
    }

    @Test
    void oversizedTitleIsParkedAndNextCourseStillSynchronizes() throws Exception {
        var invalid = CourseUpdatedEvent.of(304L, "x".repeat(300), "oversized", null, 7L, null, 1, "PUBLISHED");
        String payload = mapper.writeValueAsString(invalid);
        try (var deadLetters = deadLetterConsumer()) {
            send(invalid);
            send(CourseUpdatedEvent.of(305L, "After oversized", "after-oversized", null, 7L, null, 1, "PUBLISHED"));
            assertDeadLetter(deadLetters, "304", payload);
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(snapshots.findById(305L)).isPresent());
            assertThat(snapshots.findById(304L)).isEmpty();
            verify(consumer, times(1)).onMessage(payload);
        }
    }

    @Test
    void databaseConstraintFailureGoesStraightToDeadLetter() throws Exception {
        var invalid = CourseUpdatedEvent.of(306L, "Constraint", "constraint", null, 7L, null, 1, "PUBLISHED");
        String payload = mapper.writeValueAsString(invalid);
        doThrow(new DataIntegrityViolationException("simulated permanent constraint violation"))
                .when(consumer).onMessage(payload);
        try (var deadLetters = deadLetterConsumer()) {
            send(invalid);
            send(CourseUpdatedEvent.of(307L, "After constraint", "after-constraint", null, 7L, null, 1, "PUBLISHED"));
            assertDeadLetter(deadLetters, "306", payload);
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(snapshots.findById(307L)).isPresent());
            verify(consumer, times(1)).onMessage(payload);
        }
    }

    @Test
    void exhaustedRetryBudgetParksMessageAndAllowsLaterSnapshot() throws Exception {
        var failing = CourseUpdatedEvent.of(308L, "Unavailable", "unavailable", null, 7L, null, 1, "PUBLISHED");
        String payload = mapper.writeValueAsString(failing);
        doThrow(new DataAccessResourceFailureException("simulated persistent outage"))
                .when(consumer).onMessage(payload);
        try (var deadLetters = deadLetterConsumer()) {
            send(failing);
            send(CourseUpdatedEvent.of(308L, "Recovered", "recovered", null, 7L, null, 1, "ARCHIVED"));
            assertDeadLetter(deadLetters, "308", payload);
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                    assertThat(snapshots.findById(308L)).hasValueSatisfying(snapshot ->
                            assertThat(snapshot.getStatus()).isEqualTo("ARCHIVED")));
            verify(consumer, atLeast(3)).onMessage(payload);
        }
    }

    private Consumer<String, String> deadLetterConsumer() {
        var observer = new DefaultKafkaConsumerFactory<>(KafkaTestUtils.consumerProps(
                "dlt-" + UUID.randomUUID(), "false", broker), new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        broker.consumeFromAnEmbeddedTopic(observer, true, DLT);
        return observer;
    }

    private void assertDeadLetter(Consumer<String, String> observer, String key, String payload) {
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(observer, DLT, Duration.ofSeconds(20));
        assertThat(record.key()).isEqualTo(key);
        assertThat(record.value()).isEqualTo(payload);
        assertThat(new String(record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC).value(), StandardCharsets.UTF_8))
                .isEqualTo(KafkaTopics.COURSE_EVENTS);
        assertThat(record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_OFFSET)).isNotNull();
        assertThat(record.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_FQCN)).isNotNull();
    }
}
