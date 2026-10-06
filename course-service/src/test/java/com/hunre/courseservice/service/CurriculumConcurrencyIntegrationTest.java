package com.hunre.courseservice.service;

import com.hunre.courseservice.consumer.EnrollmentEventConsumer;
import com.hunre.courseservice.dto.request.*;
import com.hunre.courseservice.entity.*;
import com.hunre.courseservice.repository.*;
import com.hunre.sharedcommon.event.EnrollmentCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:curriculum_concurrent;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class CurriculumConcurrencyIntegrationTest {
    @Autowired CourseRepository courses;
    @Autowired CategoryRepository categories;
    @Autowired SectionRepository sections;
    @Autowired LessonRepository lessons;
    @Autowired CurriculumService curriculum;
    @Autowired CourseService courseService;
    @Autowired EnrollmentEventConsumer consumer;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    Course course() {
        var category=categories.save(Category.builder().name("Concurrent").slug(UUID.randomUUID().toString()).build());
        return courses.save(Course.builder().category(category).instructorId(1L).title("Concurrent")
                .slug(UUID.randomUUID().toString()).status(CourseStatus.PUBLISHED).build());
    }

    Section section(Course course) {
        return sections.save(Section.builder().course(course).title("Concurrent section").build());
    }

    Long add(Long sectionId, int duration) {
        return curriculum.createLesson(sectionId, CreateLessonRequest.builder().title("Lesson")
                .durationSeconds(duration).build(),1L,false).getId();
    }

    void parallel(List<Callable<Void>> tasks) throws Exception {
        var pool=Executors.newFixedThreadPool(8);
        try {
            for(var result:pool.invokeAll(tasks,30,TimeUnit.SECONDS)) result.get();
        } finally {pool.shutdownNow();}
    }

    void assertStats(Long courseId, int count, int duration) {
        var stored=courses.findById(courseId).orElseThrow();
        assertThat(stored.getTotalLessons()).isEqualTo(count);
        assertThat(stored.getTotalDurationSeconds()).isEqualTo(duration);
        assertThat(lessons.countByCourseId(courseId)).isEqualTo(count);
        assertThat(lessons.sumDurationSecondsByCourseId(courseId)).isEqualTo(duration);
    }

    @Test
    void simultaneousLessonCreationAndCourseEditingPreserveAllStatsAndEvents() throws Exception {
        var course=course();var section=section(course);
        List<Callable<Void>> tasks=new ArrayList<>();
        for(int i=0;i<16;i++) {
            tasks.add(()->{add(section.getId(),30);return null;});
            tasks.add(()->{courseService.updateCourse(course.getId(),UpdateCourseRequest.builder()
                    .title("Edited").slug(course.getSlug()).categoryId(course.getCategory().getId()).build(),1L,false);return null;});
        }
        parallel(tasks);
        assertStats(course.getId(),16,480);
        String payload=jdbc.queryForObject("SELECT payload FROM outbox_events WHERE aggregate_id=? ORDER BY id DESC LIMIT 1",
                String.class,course.getId().toString());
        assertThat(mapper.readTree(payload).path("totalLessons").asInt()).isEqualTo(16);
    }

    @Test
    void concurrentEditsOfSameLessonUseLatestDuration() throws Exception {
        var course=course();var section=section(course);var lessonId=add(section.getId(),30);
        List<Callable<Void>> tasks=new ArrayList<>();
        for(int duration=40;duration<=160;duration+=10) {
            int next=duration;
            tasks.add(()->{curriculum.updateLesson(lessonId,UpdateLessonRequest.builder()
                    .title("Edited").durationSeconds(next).build(),1L,false);return null;});
        }
        parallel(tasks);
        assertStats(course.getId(),1,lessons.findById(lessonId).orElseThrow().getDurationSeconds());
    }

    @Test
    void sectionDeletionAndCreationInAnotherSectionKeepAccurateAggregates() throws Exception {
        var course=course();var removed=section(course);var kept=section(course);
        for(int i=0;i<8;i++) add(removed.getId(),30);
        List<Callable<Void>> tasks=new ArrayList<>();
        tasks.add(()->{curriculum.deleteSection(removed.getId(),1L,false);return null;});
        for(int i=0;i<8;i++) tasks.add(()->{add(kept.getId(),60);return null;});
        parallel(tasks);
        assertStats(course.getId(),8,480);
        assertThat(sections.findById(removed.getId())).isEmpty();
    }

    @Test
    void concurrentLessonDeletionDoesNotLoseDecrements() throws Exception {
        var course=course();var section=section(course);
        List<Callable<Void>> tasks=new ArrayList<>();
        for(int i=0;i<12;i++) {
            Long id=add(section.getId(),30);
            tasks.add(()->{curriculum.deleteLesson(id,1L,false);return null;});
        }
        parallel(tasks);
        assertStats(course.getId(),0,0);
    }

    @Test
    void enrollmentAndLessonCreationKeepIndependentCounters() throws Exception {
        var course=course();var section=section(course);
        List<Callable<Void>> tasks=new ArrayList<>();
        for(int i=1;i<=12;i++) {
            long user=i%3+1;
            String payload=mapper.writeValueAsString(EnrollmentCreatedEvent.of((long)i,user,course.getId(),"Concurrent"));
            tasks.add(()->{consumer.onMessage(payload);return null;});
            tasks.add(()->{add(section.getId(),30);return null;});
        }
        parallel(tasks);
        assertStats(course.getId(),12,360);
        assertThat(courses.findById(course.getId()).orElseThrow().getStudentCount()).isEqualTo(3);
    }
}
