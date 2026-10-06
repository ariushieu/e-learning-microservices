package com.hunre.courseservice.consumer;

import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.courseservice.repository.ProcessedEventRepository;
import com.hunre.courseservice.service.CourseService;
import com.hunre.sharedcommon.event.EnrollmentCreatedEvent;
import com.hunre.sharedcommon.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:course_count;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class EnrollmentEventIntegrationTest {
    @Autowired CourseRepository courses;
    @Autowired ProcessedEventRepository processed;
    @Autowired EnrollmentEventConsumer consumer;
    @Autowired CourseService service;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    Course course() {
        return courses.save(Course.builder().title("Count test").slug(UUID.randomUUID().toString())
                .instructorId(1L).status(CourseStatus.PUBLISHED).build());
    }

    String event(Long courseId, long student) {
        return mapper.writeValueAsString(EnrollmentCreatedEvent.of(student, student, courseId, "Count test"));
    }

    @Test
    void countsTwoStudentsAndIgnoresReplayInApiResponse() {
        var course = course();
        var first = event(course.getId(), 1L);
        consumer.onMessage(first);
        consumer.onMessage(event(course.getId(), 2L));
        consumer.onMessage(first);
        assertThat(service.getCourseById(course.getId()).getStudentCount()).isEqualTo(2);
        assertThat(service.getCourseBySlug(course.getSlug()).getStudentCount()).isEqualTo(2);
    }

    @Test
    void countsArchivedCoursesAndPreventsDeletingDraftWithStudents() {
        var course = course();
        course.setStatus(CourseStatus.ARCHIVED);
        courses.save(course);
        consumer.onMessage(event(course.getId(), 1L));
        course.setStatus(CourseStatus.DRAFT);
        courses.save(course);
        assertThatThrownBy(() -> service.deleteCourse(course.getId(), 1L, false))
                .isInstanceOf(BusinessException.class).hasMessageContaining("học viên");
        assertThat(courses.findById(course.getId()).orElseThrow().getStudentCount()).isEqualTo(1);
    }

    @Test
    void missingCourseRollsBackReceiptAndAllowsReplayAfterRepair() {
        var course = course();
        long missing = course.getId() + 99999;
        var event = EnrollmentCreatedEvent.of(1L, 1L, missing, "Missing");
        assertThatThrownBy(() -> consumer.onMessage(mapper.writeValueAsString(event)))
                .isInstanceOf(InvalidEventException.class);
        assertThat(processed.existsById(event.eventId())).isFalse();
        var repaired = new EnrollmentCreatedEvent(event.eventId(), event.occurredAt(), 1L, 1L, course.getId(), "Repaired");
        consumer.onMessage(mapper.writeValueAsString(repaired));
        assertThat(courses.findById(course.getId()).orElseThrow().getStudentCount()).isEqualTo(1);
    }

    @Test
    void databaseFailureRollsBackReceiptAndCount() {
        var course = course();
        var event = EnrollmentCreatedEvent.of(1L, 1L, course.getId(), "Overflow");
        jdbc.update("UPDATE courses SET student_count = 2147483647 WHERE id = ?", course.getId());
        assertThatThrownBy(() -> consumer.onMessage(mapper.writeValueAsString(event))).isInstanceOf(InvalidEventException.class);
        assertThat(processed.existsById(event.eventId())).isFalse();
        assertThat(courses.findById(course.getId()).orElseThrow().getStudentCount()).isEqualTo(Integer.MAX_VALUE);
        jdbc.update("UPDATE courses SET student_count = 0 WHERE id = ?", course.getId());
        consumer.onMessage(mapper.writeValueAsString(event));
        assertThat(courses.findById(course.getId()).orElseThrow().getStudentCount()).isEqualTo(1);
    }

    @Test
    void simultaneousDeliveriesDoNotLoseIncrementsOrCountDuplicates() throws Exception {
        var course = course();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            String payload = event(course.getId(), i);
            for (int j = 0; j < 2; j++) tasks.add(() -> { consumer.onMessage(payload); return null; });
        }
        var pool = Executors.newFixedThreadPool(4);
        try {
            for (var result : pool.invokeAll(tasks)) result.get(10, TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
        assertThat(courses.findById(course.getId()).orElseThrow().getStudentCount()).isEqualTo(8);
    }

    @Test
    void editingPreviouslyLoadedCourseCannotOverwriteNewCount() throws Exception {
        var course = course();
        new TransactionTemplate(txManager).executeWithoutResult(tx -> {
            var stale = courses.findById(course.getId()).orElseThrow();
            var executor = Executors.newSingleThreadExecutor();
            try {
                executor.submit(() -> consumer.onMessage(event(course.getId(), 1L))).get(10, TimeUnit.SECONDS);
            } catch (Exception ex) { throw new IllegalStateException(ex); }
            finally { executor.shutdownNow(); }
            stale.setTitle("Edited after enrollment");
            courses.saveAndFlush(stale);
        });
        assertThat(courses.findById(course.getId()).orElseThrow().getStudentCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "{broken", "{}", "{\"eventType\":42}",
            "{\"eventType\":\"enrollment.created\",\"eventId\":\"\"}"})
    void malformedPayloadIsRejected(String payload) {
        assertThatThrownBy(() -> consumer.onMessage(payload)).isInstanceOf(InvalidEventException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", "\"1\"", "9223372036854775808", "null"})
    void invalidCourseIdIsNotCoerced(String id) {
        String payload = event(course().getId(), 1L).replaceFirst("\"courseId\":\\d+", "\"courseId\":" + id);
        assertThatThrownBy(() -> consumer.onMessage(payload)).isInstanceOf(InvalidEventException.class);
    }

    @Test
    void unrelatedEventDoesNotIncrement() {
        var course = course();
        consumer.onMessage(event(course.getId(), 1L).replace("enrollment.created", "enrollment.completed"));
        assertThat(courses.findById(course.getId()).orElseThrow().getStudentCount()).isZero();
    }

    @Test
    void deletionRechecksCountWhenEnrollmentCommitsAfterInitialRead() {
        var course = course();
        course.setStatus(CourseStatus.DRAFT);
        courses.save(course);
        assertThatThrownBy(() -> new TransactionTemplate(txManager).executeWithoutResult(tx -> {
            // Mô phỏng request xóa đã đọc số 0, rồi consumer commit trước lệnh DELETE.
            assertThat(courses.findById(course.getId()).orElseThrow().getStudentCount()).isZero();
            var executor = Executors.newSingleThreadExecutor();
            try {
                executor.submit(() -> consumer.onMessage(event(course.getId(), 1L))).get(10, TimeUnit.SECONDS);
            } catch (Exception ex) { throw new IllegalStateException(ex); }
            finally { executor.shutdownNow(); }
            service.deleteCourse(course.getId(), 1L, false);
        })).isInstanceOf(BusinessException.class);
        assertThat(courses.findById(course.getId()).orElseThrow().getStudentCount()).isEqualTo(1);
    }

    @Test
    void deletionRechecksStatusWhenCourseWasPublishedAfterInitialRead() {
        var course = course();
        course.setStatus(CourseStatus.DRAFT);
        courses.save(course);
        assertThatThrownBy(() -> new TransactionTemplate(txManager).executeWithoutResult(tx -> {
            assertThat(courses.findById(course.getId()).orElseThrow().getStatus()).isEqualTo(CourseStatus.DRAFT);
            var executor = Executors.newSingleThreadExecutor();
            try {
                executor.submit(() -> jdbc.update("UPDATE courses SET status = 'PUBLISHED' WHERE id = ?", course.getId()))
                        .get(10, TimeUnit.SECONDS);
            } catch (Exception ex) { throw new IllegalStateException(ex); }
            finally { executor.shutdownNow(); }
            service.deleteCourse(course.getId(), 1L, false);
        })).isInstanceOf(BusinessException.class);
        assertThat(courses.findById(course.getId()).orElseThrow().getStatus()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    void emptyDraftStillDeletesSuccessfully() {
        var course = course();
        course.setStatus(CourseStatus.DRAFT);
        courses.save(course);
        service.deleteCourse(course.getId(), 1L, false);
        assertThat(courses.findById(course.getId())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-date", "null"})
    void invalidTimestampIsRejected(String timestamp) {
        var course = course();
        String payload = event(course.getId(), 1L).replaceFirst("\"occurredAt\":\"[^\"]*\"",
                "\"occurredAt\":\"" + timestamp + "\"");
        assertThatThrownBy(() -> consumer.onMessage(payload)).isInstanceOf(InvalidEventException.class);
        assertThat(courses.findById(course.getId()).orElseThrow().getStudentCount()).isZero();
    }
}
