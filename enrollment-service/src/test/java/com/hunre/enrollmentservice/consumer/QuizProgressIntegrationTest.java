package com.hunre.enrollmentservice.consumer;

import com.hunre.enrollmentservice.client.CourseLessonClient;
import com.hunre.enrollmentservice.dto.request.UpdateLessonProgressRequest;
import com.hunre.enrollmentservice.entity.*;
import com.hunre.enrollmentservice.repository.*;
import com.hunre.enrollmentservice.service.ProgressService;
import com.hunre.sharedcommon.event.QuizGradedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Transaction thật: không mock repository tiến độ, chứng chỉ hay sổ khử trùng lặp. */
@SpringBootTest(properties = {
        "app.outbox.publisher.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:quiz_progress;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
class QuizProgressIntegrationTest {
    @Autowired QuizGradedConsumer consumer;
    @Autowired ObjectMapper mapper;
    @Autowired EnrollmentRepository enrollments;
    @Autowired CourseSnapshotRepository snapshots;
    @Autowired LessonProgressRepository lessons;
    @Autowired CertificateRepository certificates;
    @Autowired ProgressService progress;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean OutboxEventRepository outbox;
    @MockitoBean CourseLessonClient courseLessons;
    private Enrollment enrollment;

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM processed_quiz_events");
        outbox.deleteAll();
        certificates.deleteAll();
        lessons.deleteAll();
        enrollments.deleteAll();
        snapshots.deleteAll();
        snapshots.saveAndFlush(CourseSnapshot.builder().courseId(30L).title("Khóa sự kiện")
                .slug("khoa-su-kien").totalLessons(2).lessonIds(List.of(301L, 302L)).build());
        enrollment = enrollments.saveAndFlush(Enrollment.builder().courseId(30L).userId(90L)
                .learnerName("Học viên S").status(EnrollmentStatus.ACTIVE).progressPercent(new BigDecimal("50.00")).build());
        lessons.saveAndFlush(LessonProgress.builder().enrollment(enrollment).lessonId(301L)
                .status(LessonProgressStatus.COMPLETED).watchedSeconds(80).completedAt(Instant.now()).build());
        clearInvocations(outbox, courseLessons);
    }

    @Test
    void passingLastLessonCompletesCourseCertificateAndOutboxWithoutHttpRequest() {
        receive(event(true, 302L));
        assertCompleted();
        var certificate = certificates.findByEnrollmentId(enrollment.getId()).orElseThrow();
        assertThat(certificate.getLearnerName()).isEqualTo("Học viên S");
        assertThat(certificate.getCourseTitle()).isEqualTo("Khóa sự kiện");
        assertThat(certificate.getCertificateCode()).startsWith("CERT-");
        assertThat(ledgerCount()).isEqualTo(1);
        verifyNoInteractions(courseLessons);
    }

    @ParameterizedTest
    @ValueSource(strings = {"failed", "unlinked", "author", "cancelled", "completed", "wrong-lesson", "missing-snapshot", "legacy-snapshot", "empty-curriculum"})
    void ineligibleEventsAreAcknowledgedWithoutChangingProgress(String scenario) {
        var event = event(true, 302L);
        switch (scenario) {
            case "failed" -> event = event(false, 302L);
            case "unlinked" -> event = event(true, null);
            case "author" -> event = QuizGradedEvent.of(1L, 2L, 30L, 7L, "Preview", BigDecimal.TEN, true, 302L);
            case "cancelled", "completed" -> {
                enrollment.setStatus(EnrollmentStatus.valueOf(scenario.toUpperCase()));
                enrollments.saveAndFlush(enrollment);
            }
            case "wrong-lesson" -> event = event(true, 999L);
            case "missing-snapshot" -> snapshots.deleteAll();
            case "legacy-snapshot", "empty-curriculum" -> {
                var snapshot = snapshots.findById(30L).orElseThrow();
                snapshot.setLessonIds(scenario.equals("legacy-snapshot") ? null : List.of());
                snapshots.saveAndFlush(snapshot);
            }
        }
        receive(event);
        assertThat(lessons.count()).isEqualTo(1);
        assertThat(certificates.count()).isZero();
        assertThat(outbox.count()).isZero();
        assertThat(ledgerCount()).isEqualTo(1);
        assertThat(enrollments.findById(enrollment.getId()).orElseThrow().getProgressPercent()).isEqualByComparingTo("50");
        verifyNoInteractions(courseLessons);
    }

    @Test
    void duplicateEventPreservesCompletionTimeCertificateAndOutbox() {
        var event = event(true, 302L);
        receive(event);
        var completedAt = lessons.findByEnrollmentIdAndLessonId(enrollment.getId(), 302L).orElseThrow().getCompletedAt();
        var code = certificates.findByEnrollmentId(enrollment.getId()).orElseThrow().getCertificateCode();
        receive(event);
        assertCompleted();
        assertThat(ledgerCount()).isEqualTo(1);
        assertThat(lessons.findByEnrollmentIdAndLessonId(enrollment.getId(), 302L).orElseThrow().getCompletedAt()).isEqualTo(completedAt);
        assertThat(certificates.findByEnrollmentId(enrollment.getId()).orElseThrow().getCertificateCode()).isEqualTo(code);
    }

    @Test
    void duplicateIgnoredBeforeEnrollmentCannotCompleteALaterEnrollment() {
        var event = QuizGradedEvent.of(1L, 2L, 30L, 91L, "Quiz", BigDecimal.TEN, true, 302L);
        receive(event);
        var later = enrollments.saveAndFlush(Enrollment.builder().courseId(30L).userId(91L)
                .learnerName("Later student").status(EnrollmentStatus.ACTIVE).progressPercent(BigDecimal.ZERO).build());
        receive(event);
        assertThat(lessons.findAllByEnrollmentId(later.getId())).isEmpty();
        assertThat(ledgerCount()).isEqualTo(1);
    }

    @Test
    void retryAfterOutboxFailureRollsBackLedgerProgressAndCertificateTogether() {
        var event = event(true, 302L);
        doAnswer(invocation -> {
            OutboxEvent pending = invocation.getArgument(0);
            if (pending.getEventType().equals("certificate.issued")) {
                throw new DataAccessResourceFailureException("temporary outage after certificate INSERT");
            }
            return invocation.callRealMethod();
        }).when(outbox).save(any(OutboxEvent.class));
        assertThatThrownBy(() -> receive(event)).isInstanceOf(IllegalStateException.class);
        assertThat(ledgerCount()).isZero();
        assertThat(lessons.count()).isEqualTo(1);
        assertThat(certificates.count()).isZero();
        assertThat(outbox.count()).isZero();
        assertThat(enrollments.findById(enrollment.getId()).orElseThrow().getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        reset(outbox);
        receive(event);
        assertCompleted();
        assertThat(ledgerCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void simultaneousDeliveriesProduceOneCertificate(boolean sameEvent) throws Exception {
        var first = event(true, 302L);
        var second = sameEvent ? first : event(true, 302L);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var futures = List.of(first, second).stream().map(event -> executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                receive(event);
                return null;
            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (var future : futures) future.get(10, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
        assertCompleted();
        assertThat(ledgerCount()).isEqualTo(sameEvent ? 1 : 2);
    }

    @Test
    void completedLessonNeverLosesWatchedSecondsOrCompletionTime() {
        var before = lessons.findByEnrollmentIdAndLessonId(enrollment.getId(), 301L).orElseThrow();
        receive(event(true, 301L));
        var after = lessons.findByEnrollmentIdAndLessonId(enrollment.getId(), 301L).orElseThrow();
        assertThat(after.getWatchedSeconds()).isEqualTo(80);
        assertThat(after.getCompletedAt()).isEqualTo(before.getCompletedAt());
        assertThat(after.getStatus()).isEqualTo(LessonProgressStatus.COMPLETED);
        assertThat(certificates.count()).isZero();
    }

    @Test
    void manualProgressAfterQuizCannotDowngradeTheLesson() {
        receive(event(true, 302L));
        progress.updateLessonProgress(90L, UpdateLessonProgressRequest.builder().courseId(30L).lessonId(302L)
                .status(LessonProgressStatus.IN_PROGRESS).watchedSeconds(15).build());
        assertCompleted();
        assertThat(lessons.findByEnrollmentIdAndLessonId(enrollment.getId(), 302L).orElseThrow().getWatchedSeconds()).isEqualTo(15);
    }

    @Test
    void legacyEnrollmentWithoutNameStillReceivesCertificateWithoutInventedIdentity() {
        enrollment.setLearnerName(null);
        enrollments.saveAndFlush(enrollment);
        receive(event(true, 302L));
        assertCompleted();
        assertThat(certificates.findByEnrollmentId(enrollment.getId()).orElseThrow().getLearnerName()).isNull();
    }

    private QuizGradedEvent event(boolean passed, Long lessonId) {
        return QuizGradedEvent.of(1L, 2L, 30L, 90L, "Quiz", new BigDecimal("100.00"), passed, lessonId);
    }

    private void receive(QuizGradedEvent event) {
        consumer.onMessage(mapper.writeValueAsString(event));
    }

    private int ledgerCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM processed_quiz_events", Integer.class);
    }

    private void assertCompleted() {
        var saved = enrollments.findById(enrollment.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(saved.getProgressPercent()).isEqualByComparingTo("100");
        assertThat(saved.getCompletedAt()).isNotNull();
        assertThat(lessons.findByEnrollmentIdAndLessonId(enrollment.getId(), 302L).orElseThrow().getStatus())
                .isEqualTo(LessonProgressStatus.COMPLETED);
        assertThat(certificates.count()).isEqualTo(1);
        assertThat(outbox.findAll()).extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("enrollment.completed", "certificate.issued");
    }
}
