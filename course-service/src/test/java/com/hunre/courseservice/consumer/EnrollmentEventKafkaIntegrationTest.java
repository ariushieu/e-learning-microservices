package com.hunre.courseservice.consumer;

import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.courseservice.repository.ProcessedEventRepository;
import com.hunre.sharedcommon.event.EnrollmentCreatedEvent;
import com.hunre.sharedcommon.event.KafkaTopics;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.CannotCreateTransactionException;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.kafka.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:course_count_kafka;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.enable-auto-commit=false",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.listener.ack-mode=record",
        "elearning.kafka.retry.initial-interval=50ms",
        "elearning.kafka.retry.max-interval=50ms",
        "elearning.kafka.retry.max-elapsed-time=200ms"
})
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.ENROLLMENT_EVENTS, KafkaTopics.ENROLLMENT_EVENTS + ".DLT"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class EnrollmentEventKafkaIntegrationTest {
    @Autowired CourseRepository courses;
    @Autowired ProcessedEventRepository processed;
    @Autowired ObjectMapper mapper;
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired EmbeddedKafkaBroker broker;
    @MockitoSpyBean EnrollmentEventProcessor processor;

    @Test
    void realListenerRetriesDeduplicatesAndMovesBadEventsToDltWithoutBlocking() throws Exception {
        var course = courses.save(Course.builder().title("Kafka students").slug(UUID.randomUUID().toString()).instructorId(1L).build());
        var first = EnrollmentCreatedEvent.of(1L, 1L, course.getId(), course.getTitle());
        var second = EnrollmentCreatedEvent.of(2L, 2L, course.getId(), course.getTitle());
        var exhausted = EnrollmentCreatedEvent.of(3L, 3L, course.getId(), course.getTitle());
        doThrow(new CannotCreateTransactionException("temporary database outage"))
                .doCallRealMethod().when(processor).process(first);
        doThrow(new CannotCreateTransactionException("persistent database outage"))
                .when(processor).process(exhausted);

        Map<String, Object> config = new HashMap<>();
        config.put("bootstrap.servers", broker.getBrokersAsString());
        config.put("group.id", "count-dlt-proof-" + UUID.randomUUID());
        config.put("auto.offset.reset", "earliest");
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        try (var dlt = new KafkaConsumer<String, String>(config)) {
            dlt.subscribe(List.of(KafkaTopics.ENROLLMENT_EVENTS + ".DLT"));
            send("{broken");
            send(mapper.writeValueAsString(first));
            send(mapper.writeValueAsString(first));
            send(mapper.writeValueAsString(exhausted));
            send(mapper.writeValueAsString(second));
            await().atMost(Duration.ofSeconds(25)).untilAsserted(() -> {
                assertThat(courses.findById(course.getId()).orElseThrow().getStudentCount()).isEqualTo(2);
                assertThat(processed.existsById(second.eventId())).isTrue();
            });
            assertThat(processed.existsById(exhausted.eventId())).isFalse();
            verify(processor, atLeast(3)).process(first);
            verify(processor, atLeast(2)).process(exhausted);
            List<String> deadLetters = new ArrayList<>();
            long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            while (deadLetters.size() < 2 && System.nanoTime() < deadline) {
                dlt.poll(Duration.ofMillis(500)).forEach(record -> deadLetters.add(record.value()));
            }
            assertThat(deadLetters).containsExactlyInAnyOrder("{broken", mapper.writeValueAsString(exhausted));
        }
    }

    private void send(String payload) throws Exception {
        kafka.send(KafkaTopics.ENROLLMENT_EVENTS, "1", payload).get(10, TimeUnit.SECONDS);
    }
}
