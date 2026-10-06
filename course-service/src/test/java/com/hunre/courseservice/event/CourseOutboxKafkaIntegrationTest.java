package com.hunre.courseservice.event;

import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.courseservice.repository.OutboxEventRepository;
import com.hunre.sharedcommon.event.KafkaTopics;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Lưu trong H2, giả lập một lần mất kết nối rồi gửi lại tới broker Kafka thật trong JVM. */
@SpringBootTest(properties = {
        "spring.kafka.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:course_outbox_kafka;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
@EmbeddedKafka(partitions = 1, topics = KafkaTopics.COURSE_EVENTS, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class CourseOutboxKafkaIntegrationTest {
    @Autowired CourseRepository courses;
    @Autowired OutboxEventRepository outbox;
    @Autowired CourseEventPublisher publisher;
    @Autowired OutboxPublisherWorker worker;
    @Autowired PlatformTransactionManager txManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired EmbeddedKafkaBroker broker;
    @MockitoSpyBean KafkaTemplate<String, String> kafka;

    @Test
    void failedSendIsRetriedVerbatimToRealBroker() {
        jdbc.update("INSERT INTO outbox_dispatch_lock (id) VALUES (1)");
        new TransactionTemplate(txManager).executeWithoutResult(tx -> {
            var course = courses.save(Course.builder().title("Recover").slug(UUID.randomUUID().toString())
                    .instructorId(7L).status(CourseStatus.ARCHIVED).build());
            publisher.publishCourseUpdated(course);
        });
        var original = outbox.findTop50ByPublishedAtIsNullOrderByIdAsc().get(0);
        doReturn(CompletableFuture.failedFuture(new IllegalStateException("Connection lost")))
                .doCallRealMethod().when(kafka).send(anyString(), anyString(), anyString());
        worker.publishPendingEvents();
        assertThat(outbox.findById(original.getId()).orElseThrow().getPublishedAt()).isNull();
        worker.publishPendingEvents();
        assertThat(outbox.findById(original.getId()).orElseThrow().getPublishedAt()).isNotNull();
        Map<String, Object> config = new HashMap<>();
        config.put("bootstrap.servers", broker.getBrokersAsString());
        config.put("group.id", "outbox-proof-" + UUID.randomUUID());
        config.put("auto.offset.reset", "earliest");
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        try (var consumer = new KafkaConsumer<String, String>(config)) {
            consumer.subscribe(List.of(KafkaTopics.COURSE_EVENTS));
            long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            while (System.nanoTime() < deadline) {
                for (var record : consumer.poll(Duration.ofMillis(500))) {
                    assertThat(record.key()).isEqualTo(original.getAggregateId());
                    assertThat(record.value()).isEqualTo(original.getPayload());
                    return;
                }
            }
            fail("Không nhận được sự kiện đã gửi lại từ broker");
        }
    }
}
