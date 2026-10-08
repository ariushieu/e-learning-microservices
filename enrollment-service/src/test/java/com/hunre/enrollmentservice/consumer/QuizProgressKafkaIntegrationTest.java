package com.hunre.enrollmentservice.consumer;

import com.hunre.enrollmentservice.entity.*;
import com.hunre.enrollmentservice.repository.*;
import com.hunre.sharedcommon.event.*;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.kafka.enabled=true", "app.outbox.publisher.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:quiz_progress_kafka;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "app.quiz-progress.group-id=enrollment-quiz-progress-test",
        "spring.kafka.consumer.auto-offset-reset=earliest", "spring.kafka.consumer.enable-auto-commit=false",
        "spring.kafka.listener.ack-mode=record",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "elearning.kafka.retry.initial-interval=50ms", "elearning.kafka.retry.multiplier=2",
        "elearning.kafka.retry.max-interval=100ms", "elearning.kafka.retry.max-elapsed-time=350ms"
})
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.COURSE_EVENTS, KafkaTopics.QUIZ_EVENTS,
        KafkaTopics.COURSE_EVENTS + ".DLT", KafkaTopics.QUIZ_EVENTS + ".DLT"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class QuizProgressKafkaIntegrationTest {
    private static final String DLT = KafkaTopics.QUIZ_EVENTS + ".DLT";
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired ObjectMapper mapper;
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired CourseSnapshotRepository snapshots;
    @Autowired EnrollmentRepository enrollments;
    @Autowired CertificateRepository certificates;
    @Autowired OutboxEventRepository outbox;
    @MockitoSpyBean QuizGradedConsumer consumer;

    @Test
    void courseSnapshotThenQuizEventCompletesThroughRealListeners() throws Exception {
        var enrollment = fixture(701L);
        var quiz = event(701L);
        send(mapper.writeValueAsString(quiz));
        send(mapper.writeValueAsString(quiz));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(enrollments.findById(enrollment.getId()).orElseThrow().getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
            verify(consumer, times(2)).onMessage(mapper.writeValueAsString(quiz));
        });
        assertThat(certificates.findByEnrollmentId(enrollment.getId())).isPresent();
        assertThat(outbox.findAll().stream().filter(e -> e.getAggregateType().equals("ENROLLMENT")
                && e.getAggregateId().equals(enrollment.getId().toString()))).hasSize(1);
    }

    @Test
    void temporaryDatabaseErrorRetriesBeforeAcknowledgingQuiz() throws Exception {
        var enrollment = fixture(702L);
        String payload = mapper.writeValueAsString(event(702L));
        doThrow(new DataAccessResourceFailureException("database temporarily offline"))
                .doThrow(new DataAccessResourceFailureException("database recovering"))
                .doCallRealMethod().when(consumer).onMessage(payload);
        send(payload);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(certificates.findByEnrollmentId(enrollment.getId())).isPresent();
            verify(consumer, atLeast(3)).onMessage(payload);
        });
    }

    @Test
    void malformedQuizGoesToDltAndDoesNotBlockNextLearner() throws Exception {
        var enrollment = fixture(703L);
        try (var observer = deadLetters()) {
            send("not-json");
            send(mapper.writeValueAsString(event(703L)));
            assertDeadLetter(observer, "not-json");
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                    assertThat(certificates.findByEnrollmentId(enrollment.getId())).isPresent());
            verify(consumer, times(1)).onMessage("not-json");
        }
    }

    @Test
    void exhaustedBudgetPreservesPayloadAndAllowsFollowingQuiz() throws Exception {
        fixture(704L);
        var next = fixture(705L);
        String payload = mapper.writeValueAsString(event(704L));
        doThrow(new DataAccessResourceFailureException("persistent database failure"))
                .when(consumer).onMessage(payload);
        try (var observer = deadLetters()) {
            send(payload);
            send(mapper.writeValueAsString(event(705L)));
            assertDeadLetter(observer, payload);
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                    assertThat(certificates.findByEnrollmentId(next.getId())).isPresent());
            verify(consumer, atLeast(3)).onMessage(payload);
        }
    }

    private Enrollment fixture(long courseId) throws Exception {
        var snapshot = CourseUpdatedEvent.of(courseId, "Course " + courseId, "course-" + courseId,
                null, 7L, "Instructor", 1, "PUBLISHED", List.of(courseId * 10));
        kafka.send(KafkaTopics.COURSE_EVENTS, Long.toString(courseId), mapper.writeValueAsString(snapshot))
                .get(10, TimeUnit.SECONDS);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(snapshots.findById(courseId)).hasValueSatisfying(saved ->
                        assertThat(saved.getLessonIds()).containsExactly(courseId * 10)));
        return enrollments.saveAndFlush(Enrollment.builder().courseId(courseId).userId(90L)
                .learnerName("Kafka learner").status(EnrollmentStatus.ACTIVE).progressPercent(BigDecimal.ZERO).build());
    }

    private QuizGradedEvent event(long courseId) {
        return QuizGradedEvent.of(courseId, 2L, courseId, 90L, "Quiz", BigDecimal.valueOf(100), true, courseId * 10);
    }

    private void send(String payload) throws Exception {
        kafka.send(KafkaTopics.QUIZ_EVENTS, "learner-90", payload).get(10, TimeUnit.SECONDS);
    }

    private Consumer<String, String> deadLetters() {
        var observer = new DefaultKafkaConsumerFactory<>(KafkaTestUtils.consumerProps(
                "quiz-dlt-" + UUID.randomUUID(), "false", broker), new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        broker.consumeFromAnEmbeddedTopic(observer, true, DLT);
        return observer;
    }

    private void assertDeadLetter(Consumer<String, String> observer, String payload) {
        var record = KafkaTestUtils.getSingleRecord(observer, DLT, Duration.ofSeconds(20));
        assertThat(record.key()).isEqualTo("learner-90");
        assertThat(record.value()).isEqualTo(payload);
        assertThat(new String(record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP).value(), StandardCharsets.UTF_8))
                .isEqualTo("enrollment-quiz-progress-test");
        assertThat(new String(record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC).value(), StandardCharsets.UTF_8))
                .isEqualTo(KafkaTopics.QUIZ_EVENTS);
        assertThat(record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_OFFSET)).isNotNull();
    }
}
