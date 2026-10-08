package com.hunre.courseservice.controller;

import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.repository.CourseRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:instructor_profile;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=profile-secret-key-at-least-32-characters-long",
        "elearning.security.public-paths=GET:/api/instructors/*,GET:/api/courses/**"
})
@AutoConfigureMockMvc
class InstructorProfileIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CourseRepository courses;

    private Course course(long instructorId, String name, CourseStatus status, int students, String average, int count) {
        return courses.saveAndFlush(Course.builder().instructorId(instructorId).instructorName(name)
                .title("Khóa " + UUID.randomUUID()).slug(UUID.randomUUID().toString()).status(status)
                .studentCount(students).ratingAvg(new BigDecimal(average)).ratingCount(count).build());
    }

    private String token(long id, String role) {
        return "Bearer " + Jwts.builder().subject(String.valueOf(id)).claim("roles", List.of(role))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor("profile-secret-key-at-least-32-characters-long".getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @AfterEach void cleanup() { courses.deleteAll(); }

    @Test void guestGetsWeightedStatisticsOnlyForPublishedCourses() throws Exception {
        course(50, "Tên cũ", CourseStatus.PUBLISHED, 2, "5", 2);
        course(50, "Giảng viên A", CourseStatus.PUBLISHED, 1, "2", 1);
        for (var status : List.of(CourseStatus.DRAFT, CourseStatus.PENDING_REVIEW, CourseStatus.ARCHIVED)) {
            course(50, "Tên riêng tư", status, 100, "1", 100);
        }
        course(51, "Giảng viên B", CourseStatus.PUBLISHED, 100, "1", 100);
        mvc.perform(get("/api/instructors/50"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Giảng viên A"))
                .andExpect(jsonPath("$.data.publishedCourses").value(2))
                .andExpect(jsonPath("$.data.totalStudents").value(3))
                .andExpect(jsonPath("$.data.ratingCount").value(3))
                .andExpect(jsonPath("$.data.ratingAvg").value(4.0))
                .andExpect(jsonPath("$.data.email").doesNotExist());
        mvc.perform(get("/api/courses?instructorId=50&size=1&sort=id,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].status").value("PUBLISHED"));
    }

    @Test void ownerAndAdminCannotIncludePrivateStatistics() throws Exception {
        course(50, "Công khai", CourseStatus.PUBLISHED, 3, "4.50", 2);
        course(50, "Riêng tư", CourseStatus.DRAFT, 99, "1", 99);
        for (var auth : List.of(token(50, "ROLE_INSTRUCTOR"), token(99, "ROLE_ADMIN"), token(60, "ROLE_STUDENT"))) {
            mvc.perform(get("/api/instructors/50").header("Authorization", auth))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.publishedCourses").value(1))
                    .andExpect(jsonPath("$.data.totalStudents").value(3))
                    .andExpect(jsonPath("$.data.ratingAvg").value(4.5))
                    .andExpect(jsonPath("$.data.ratingCount").value(2));
        }
    }

    @Test void noPublicCoursesAlwaysReturns404EvenForOwnerOrAdmin() throws Exception {
        course(50, "Ẩn", CourseStatus.DRAFT, 1, "5", 1);
        course(50, "Ẩn", CourseStatus.ARCHIVED, 1, "5", 1);
        for (String auth : List.of("", token(50, "ROLE_INSTRUCTOR"), token(99, "ROLE_ADMIN"))) {
            mvc.perform(get("/api/instructors/50").header("Authorization", auth))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }
        for (long id : new long[]{0, -1, Long.MAX_VALUE}) {
            mvc.perform(get("/api/instructors/" + id)).andExpect(status().isNotFound());
        }
    }

    @Test void zeroReviewsFallbackNameAndLargeStudentTotals() throws Exception {
        course(50, null, CourseStatus.PUBLISHED, 2_000_000_000, "0", 0);
        course(50, "  ", CourseStatus.PUBLISHED, 2_000_000_000, "0", 0);
        mvc.perform(get("/api/instructors/50")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Giảng viên HUNRE"))
                .andExpect(jsonPath("$.data.totalStudents").value(4_000_000_000L))
                .andExpect(jsonPath("$.data.ratingAvg").value(0))
                .andExpect(jsonPath("$.data.ratingCount").value(0));
    }

    @Test void fractionalWeightingAndInvalidRequests() throws Exception {
        course(50, "A", CourseStatus.PUBLISHED, 3, "4.33", 3);
        course(50, "A", CourseStatus.PUBLISHED, 1, "2", 1);
        course(50, "A", CourseStatus.PUBLISHED, 1, "0", 0);
        mvc.perform(get("/api/instructors/50"))
                .andExpect(jsonPath("$.data.ratingAvg").value(3.75))
                .andExpect(jsonPath("$.data.ratingCount").value(4));
        mvc.perform(get("/api/instructors/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/instructors/99999999999999999999")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/instructors/50").header("Authorization", "Bearer forged"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.publishedCourses").value(3));
        mvc.perform(post("/api/instructors/50")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/instructor/reviews")).andExpect(status().isUnauthorized());
    }
}
