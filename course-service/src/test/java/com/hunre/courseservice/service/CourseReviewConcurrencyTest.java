package com.hunre.courseservice.service;

import com.hunre.courseservice.dto.request.*;
import com.hunre.courseservice.entity.*;
import com.hunre.courseservice.repository.*;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:review_concurrent;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class CourseReviewConcurrencyTest {
    @Autowired CourseReviewService service;
    @Autowired CourseService courseService;
    @Autowired CourseRepository courses;
    @Autowired CategoryRepository categories;
    @Autowired CourseLearnerRepository learners;
    @Autowired JdbcTemplate jdbc;
    Long id, categoryId;

    @BeforeEach void seed() {
        categoryId=categories.save(Category.builder().name("Review").slug(UUID.randomUUID().toString()).build()).getId();
        id=courses.save(Course.builder().category(categories.findById(categoryId).orElseThrow()).instructorId(50L)
                .title("Concurrent").slug(UUID.randomUUID().toString()).status(CourseStatus.PUBLISHED).build()).getId();
        for(long user=1;user<=12;user++) learners.insertIfAbsent(id,user);
    }
    AuthenticatedUser user(long id) { return new AuthenticatedUser(id,"private@example.com","Learner",Set.of(Roles.STUDENT)); }
    void parallel(List<Callable<Void>> tasks) throws Exception {
        var pool=Executors.newFixedThreadPool(8);
        var start=new CountDownLatch(1);
        try {
            var futures=tasks.stream().map(task->pool.submit(()->{start.await();return task.call();})).toList();
            start.countDown();
            for(var future:futures) future.get(30,TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
    }
    void stats(int count,String avg) {
        assertThat(courses.findById(id).orElseThrow().getRatingCount()).isEqualTo(count);
        assertThat(courses.findById(id).orElseThrow().getRatingAvg()).isEqualByComparingTo(avg);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM course_reviews WHERE course_id=?",Integer.class,id)).isEqualTo(count);
    }
    @Test void differentLearnersAndCourseEditsDoNotLoseRatings() throws Exception {
        List<Callable<Void>> tasks=new ArrayList<>();
        for(long i=1;i<=12;i++) {
            final long learner=i;
            tasks.add(()->{service.save(id,new SaveCourseReviewRequest(BigDecimal.valueOf(learner%2==0?5:3),"Good"),user(learner));return null;});
            tasks.add(()->{courseService.updateCourse(id,UpdateCourseRequest.builder().categoryId(categoryId).title("Edited").build(),50L,false);return null;});
        }
        parallel(tasks); stats(12,"4.00");
    }
    @Test void sameLearnerConcurrentUpsertsCreateOneReview() throws Exception {
        List<Callable<Void>> tasks=new ArrayList<>();
        for(int i=0;i<12;i++) tasks.add(()->{service.save(id,new SaveCourseReviewRequest(BigDecimal.valueOf(5),null),user(1));return null;});
        parallel(tasks); stats(1,"5.00");
    }
    @Test void concurrentDeletesAndUpdatesKeepExactAverage() throws Exception {
        for(long i=1;i<=12;i++) service.save(id,new SaveCourseReviewRequest(BigDecimal.valueOf(5),null),user(i));
        List<Callable<Void>> tasks=new ArrayList<>();
        for(long i=1;i<=12;i++) {
            final long learner=i;
            tasks.add(()->{if(learner%2==0)service.delete(id,user(learner));else service.save(id,new SaveCourseReviewRequest(BigDecimal.valueOf(3),null),user(learner));return null;});
        }
        parallel(tasks); stats(6,"3.00");
    }

    @Test void adminRemovalsAndLearnerEditsKeepExactAverage() throws Exception {
        var admin = new AuthenticatedUser(99L, "admin@example.com", "Admin", Set.of(Roles.ADMIN));
        Map<Long, Long> reviewIds = new HashMap<>();
        for (long i=1; i<=12; i++) {
            reviewIds.put(i, service.save(id, new SaveCourseReviewRequest(BigDecimal.valueOf(5), null), user(i)).id());
        }
        List<Callable<Void>> tasks = new ArrayList<>();
        for (long i=1; i<=12; i++) {
            final long learner = i;
            tasks.add(() -> {
                if (learner % 2 == 0) service.removeByAdmin(id, reviewIds.get(learner), admin);
                else service.save(id, new SaveCourseReviewRequest(BigDecimal.valueOf(3), null), user(learner));
                return null;
            });
        }
        parallel(tasks);
        stats(6, "3.00");
    }

    @Test void adminAndAuthorDeletingSameReviewReturnOneSuccessAndOneNotFound() throws Exception {
        var admin = new AuthenticatedUser(99L, "admin@example.com", "Admin", Set.of(Roles.ADMIN));
        Long reviewId = service.save(id, new SaveCourseReviewRequest(BigDecimal.valueOf(5), null), user(1)).id();
        var successes = new java.util.concurrent.atomic.AtomicInteger();
        var missing = new java.util.concurrent.atomic.AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (boolean moderator : List.of(true, false)) {
            tasks.add(() -> {
                try {
                    if (moderator) service.removeByAdmin(id, reviewId, admin);
                    else service.delete(id, user(1));
                    successes.incrementAndGet();
                } catch (com.hunre.sharedcommon.exception.ResourceNotFoundException expected) {
                    missing.incrementAndGet();
                }
                return null;
            });
        }
        parallel(tasks);
        assertThat(successes.get()).isEqualTo(1);
        assertThat(missing.get()).isEqualTo(1);
        stats(0, "0.00");
    }
}
