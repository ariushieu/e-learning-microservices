package com.hunre.courseservice.event;

import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.courseservice.repository.OutboxEventRepository;
import com.hunre.sharedcommon.event.KafkaTopics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class CourseOutboxIntegrationTest {
    @Autowired CourseRepository courses;
    @Autowired OutboxEventRepository outbox;
    @Autowired CourseEventPublisher publisher;
    @Autowired OutboxPublisherWorker worker;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.hunre.courseservice.service.CourseService courseService;
    @MockitoBean KafkaTemplate<String, String> kafka;

    @BeforeEach
    void prepare() {
        outbox.deleteAll();
        jdbc.update("DELETE FROM outbox_dispatch_lock");
        jdbc.update("INSERT INTO outbox_dispatch_lock (id) VALUES (1)");
        clearInvocations(kafka);
    }

    private Long change(CourseStatus status, Long id, boolean rollback) {
        return new TransactionTemplate(transactionManager).execute(tx -> {
            Course course = id == null ? courses.save(Course.builder().title("Outbox test")
                    .slug(UUID.randomUUID().toString()).instructorId(1L).build()) : courses.findById(id).orElseThrow();
            course.setStatus(status);
            publisher.publishCourseUpdated(course);
            if (rollback) tx.setRollbackOnly();
            return course.getId();
        });
    }

    @Test
    void rollbackLeavesNeitherCourseNorEvent() {
        Long id = change(CourseStatus.PUBLISHED, null, true);
        assertThat(courses.findById(id)).isEmpty();
        assertThat(outbox.count()).isZero();
        verifyNoInteractions(kafka);
    }

    @Test
    void kafkaFailureKeepsCommittedEventsAndRecoverySendsInOrder() {
        Long id = change(CourseStatus.PUBLISHED, null, false);
        change(CourseStatus.ARCHIVED, id, false);
        var rows = outbox.findTop50ByPublishedAtIsNullOrderByIdAsc();
        assertThat(rows).hasSize(2);
        assertThat(courses.findById(id).orElseThrow().getStatus()).isEqualTo(CourseStatus.ARCHIVED);
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka down")));
        worker.publishPendingEvents();
        assertThat(outbox.findTop50ByPublishedAtIsNullOrderByIdAsc()).hasSize(2);
        verify(kafka, times(1)).send(anyString(), anyString(), anyString());
        clearInvocations(kafka);
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        worker.publishPendingEvents();
        var order = inOrder(kafka);
        order.verify(kafka).send(KafkaTopics.COURSE_EVENTS, id.toString(), rows.get(0).getPayload());
        order.verify(kafka).send(KafkaTopics.COURSE_EVENTS, id.toString(), rows.get(1).getPayload());
        assertThat(outbox.findTop50ByPublishedAtIsNullOrderByIdAsc()).isEmpty();
        worker.publishPendingEvents();
        verifyNoMoreInteractions(kafka);
    }

    @Test
    void partialBatchMarksOnlyAcknowledgedEvents() {
        Long id = change(CourseStatus.PUBLISHED, null, false);
        change(CourseStatus.ARCHIVED, id, false);
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Broker lost")));
        worker.publishPendingEvents();
        assertThat(outbox.findTop50ByPublishedAtIsNullOrderByIdAsc()).singleElement()
                .satisfies(row -> assertThat(row.getPayload()).contains("ARCHIVED"));
    }

    @Test
    void rejectedOutboxInsertRollsBackCourseStatus() {
        Course course = courses.save(Course.builder().title("Atomic change").slug(UUID.randomUUID().toString())
                .instructorId(1L).status(CourseStatus.DRAFT).build());
        jdbc.execute("ALTER TABLE outbox_events ADD CONSTRAINT test_reject_outbox CHECK (aggregate_type <> 'COURSE')");
        try {
            var request = new com.hunre.courseservice.dto.request.ChangeCourseStatusRequest();
            request.setStatus(CourseStatus.PUBLISHED);
            assertThatThrownBy(() -> courseService.changeCourseStatus(course.getId(), request, 1L, false))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThat(courses.findById(course.getId()).orElseThrow().getStatus()).isEqualTo(CourseStatus.DRAFT);
            assertThat(outbox.count()).isZero();
        } finally {
            jdbc.execute("ALTER TABLE outbox_events DROP CONSTRAINT test_reject_outbox");
        }
    }
}
