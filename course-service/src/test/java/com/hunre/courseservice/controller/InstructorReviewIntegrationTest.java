package com.hunre.courseservice.controller;

import com.hunre.courseservice.entity.*;
import com.hunre.courseservice.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:inbox;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "elearning.security.enabled=true", "elearning.security.jwt-secret=inbox-secret-key-at-least-32-characters-long",
        "elearning.security.public-paths=GET:/api/courses/**"})
@AutoConfigureMockMvc
class InstructorReviewIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CourseRepository courses;
    @Autowired CourseReviewRepository reviews;
    @Autowired CourseLearnerRepository learners;
    @Autowired JdbcTemplate jdbc;
    Long a1, a2, b, r1, r2, rb;
    String token(long id, String role) {
        return "Bearer "+Jwts.builder().subject(""+id).claim("roles", List.of(role)).expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor("inbox-secret-key-at-least-32-characters-long".getBytes(StandardCharsets.UTF_8))).compact();
    }
    Long course(long owner, CourseStatus status) {
        return courses.save(Course.builder().instructorId(owner).title("Khóa "+UUID.randomUUID()).slug(UUID.randomUUID().toString()).status(status).build()).getId();
    }
    Long review(Long course) {
        var r=new CourseReview();r.setCourseId(course);r.setUserId(60L);r.setRating((byte)5);r.setAuthorName("Học viên S");r.setComment("Góp ý");
        learners.insertIfAbsent(course,60L);return reviews.saveAndFlush(r).getId();
    }
    @BeforeEach void seed() {
        a1=course(50,CourseStatus.PUBLISHED);a2=course(50,CourseStatus.DRAFT);b=course(51,CourseStatus.ARCHIVED);
        r1=review(a1);r2=review(a2);rb=review(b);
        // Cùng thời gian tạo để kiểm khóa sắp xếp phụ theo ID.
        jdbc.update("UPDATE course_reviews SET created_at='2026-01-01 00:00:00'");
    }
    @AfterEach void cleanup() {
        for(Long c:List.of(a1,a2,b)){jdbc.update("DELETE FROM course_reviews WHERE course_id=?",c);jdbc.update("DELETE FROM course_learners WHERE course_id=?",c);courses.deleteById(c);}
    }
    @Test void ownershipPaginationAndStableOrderIncludeOwnDrafts() throws Exception {
        mvc.perform(get("/api/instructor/reviews?replied=false&size=1").header("Authorization",token(50,"ROLE_INSTRUCTOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.unrepliedCount").value(2))
                .andExpect(jsonPath("$.data.reviews.totalElements").value(2)).andExpect(jsonPath("$.data.reviews.totalPages").value(2))
                .andExpect(jsonPath("$.data.reviews.content[0].review.id").value(r2))
                .andExpect(jsonPath("$.data.reviews.content[0].courseId").value(a2))
                .andExpect(jsonPath("$.data.courses.length()").value(2));
        mvc.perform(get("/api/instructor/reviews?size=1&page=1").header("Authorization",token(50,"ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.reviews.content[0].review.id").value(r1));
        mvc.perform(get("/api/instructor/reviews").header("Authorization",token(51,"ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.reviews.totalElements").value(1)).andExpect(jsonPath("$.data.reviews.content[0].review.id").value(rb));
        mvc.perform(get("/api/instructor/reviews").header("Authorization",token(99,"ROLE_ADMIN")))
                .andExpect(jsonPath("$.data.reviews.totalElements").value(3)).andExpect(jsonPath("$.data.unrepliedCount").value(3));
    }
    @Test void roleAndCourseChecksProtectRowsCountsAndOptions() throws Exception {
        mvc.perform(get("/api/instructor/reviews")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/instructor/reviews").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
        for(String role:List.of("ROLE_STUDENT","ROLE_UNKNOWN")) mvc.perform(get("/api/instructor/reviews").header("Authorization",token(50,role))).andExpect(status().isForbidden());
        mvc.perform(get("/api/instructor/reviews?courseId="+a1).header("Authorization",token(51,"ROLE_INSTRUCTOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/instructor/reviews?courseId=9223372036854775807").header("Authorization",token(50,"ROLE_INSTRUCTOR"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/instructor/reviews?courseId="+b).header("Authorization",token(99,"ROLE_ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.unrepliedCount").value(1));
    }
    @Test void replyFiltersCountAndPublicLabelsUpdateWithoutLeakingIdentity() throws Exception {
        String path="/api/courses/"+a1+"/reviews/"+r1+"/reply";
        mvc.perform(put(path).header("Authorization",token(99,"ROLE_ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Admin phản hồi\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.replyAuthorRole").value("ADMIN"));
        mvc.perform(get("/api/courses/"+a1+"/reviews")).andExpect(jsonPath("$.data.content[0].replyAuthorRole").value("ADMIN"))
                .andExpect(jsonPath("$.data.content[0].repliedBy").doesNotExist());
        mvc.perform(get("/api/courses/"+a1+"/reviews/me").header("Authorization",token(60,"ROLE_STUDENT")))
                .andExpect(jsonPath("$.data.review.replyAuthorRole").value("ADMIN"));
        for(String filter:List.of("true","false")) mvc.perform(get("/api/instructor/reviews?replied="+filter).header("Authorization",token(50,"ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.unrepliedCount").value(1)).andExpect(jsonPath("$.data.reviews.totalElements").value(1));
        mvc.perform(get("/api/instructor/reviews?courseId="+a1+"&replied=true").header("Authorization",token(50,"ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.unrepliedCount").value(0)).andExpect(jsonPath("$.data.reviews.content[0].review.replyAuthorRole").value("ADMIN"));
        mvc.perform(put(path).header("Authorization",token(50,"ROLE_INSTRUCTOR")).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Giảng viên phản hồi\"}"))
                .andExpect(jsonPath("$.data.replyAuthorRole").value("INSTRUCTOR"));
        mvc.perform(delete(path).header("Authorization",token(50,"ROLE_INSTRUCTOR"))).andExpect(status().isOk());
        mvc.perform(get("/api/instructor/reviews").header("Authorization",token(50,"ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.unrepliedCount").value(2));
        mvc.perform(get("/api/courses/"+a1+"/reviews")).andExpect(jsonPath("$.data.content[0].replyAuthorRole").isEmpty());
    }
    @Test void invalidFiltersAndEmptyPagesAreHandled() throws Exception {
        for(String query:List.of("replied=invalid","courseId=abc","sort=comment,desc")) mvc.perform(get("/api/instructor/reviews?"+query).header("Authorization",token(50,"ROLE_INSTRUCTOR"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/instructor/reviews?page=100").header("Authorization",token(50,"ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.reviews.content").isEmpty()).andExpect(jsonPath("$.data.unrepliedCount").value(2));
        mvc.perform(get("/api/instructor/reviews").header("Authorization",token(52,"ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.reviews.content").isEmpty()).andExpect(jsonPath("$.data.courses").isEmpty()).andExpect(jsonPath("$.data.unrepliedCount").value(0));
    }
}
