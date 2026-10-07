package com.hunre.courseservice.controller;

import com.hunre.courseservice.entity.*;
import com.hunre.courseservice.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=review-test-secret-key-with-at-least-32-characters",
        "elearning.security.public-paths=GET:/api/courses/**"
})
@AutoConfigureMockMvc
class CourseReviewIntegrationTest {
    static final String SECRET = "review-test-secret-key-with-at-least-32-characters";
    @Autowired MockMvc mvc;
    @Autowired CourseRepository courses;
    @Autowired CourseLearnerRepository learners;
    @Autowired CourseReviewRepository reviews;
    @Autowired JdbcTemplate jdbc;
    Long id;

    @BeforeEach void seed() {
        id = courses.save(Course.builder().instructorId(50L).title("Review course")
                .slug(UUID.randomUUID().toString()).status(CourseStatus.PUBLISHED).build()).getId();
        learners.insertIfAbsent(id, 60L);
        learners.insertIfAbsent(id, 61L);
    }
    @AfterEach void cleanup() {
        jdbc.update("DELETE FROM course_reviews WHERE course_id=?", id);
        jdbc.update("DELETE FROM course_learners WHERE course_id=?", id);
        courses.deleteById(id);
    }
    String token(long user, String role) {
        return "Bearer " + Jwts.builder().subject(Long.toString(user)).claim("roles", List.of(role))
                .claim("fullName", "Học viên " + user).claim("email", "private@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
    String url() { return "/api/courses/" + id + "/reviews"; }
    void save(long user, int rating) throws Exception {
        mvc.perform(put(url()+"/me").header("Authorization", token(user, "ROLE_STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":"+rating+",\"comment\":\" Hữu ích \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.comment").value("Hữu ích"));
    }
    void stats(int count, String avg) {
        var course=courses.findById(id).orElseThrow();
        assertThat(course.getRatingCount()).isEqualTo(count);
        assertThat(course.getRatingAvg()).isEqualByComparingTo(avg);
    }
    @Test void lifecycleRecomputesStatsAndReturnsNoPrivateFields() throws Exception {
        save(60,5); stats(1,"5.00");
        save(61,3); stats(2,"4.00");
        save(60,1); stats(2,"2.00");
        mvc.perform(get(url()+"?size=1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].authorName").value("Học viên 61"))
                .andExpect(jsonPath("$.data.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].userId").doesNotExist());
        mvc.perform(get(url()+"?size=1&page=1")).andExpect(jsonPath("$.data.content[0].rating").value(1));
        mvc.perform(delete(url()+"/me").header("Authorization",token(60,"ROLE_STUDENT"))).andExpect(status().isOk());
        stats(1,"3.00");
        mvc.perform(delete(url()+"/me").header("Authorization",token(61,"ROLE_STUDENT"))).andExpect(status().isOk());
        stats(0,"0.00");
        mvc.perform(delete(url()+"/me").header("Authorization",token(61,"ROLE_STUDENT"))).andExpect(status().isNotFound());
    }
    @Test void membershipAndIdentityComeFromServer() throws Exception {
        save(60,5);
        mvc.perform(put(url()+"/me?userId=60").header("Authorization",token(62,"ROLE_STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":1,\"userId\":60}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(url()+"/me?userId=60").header("Authorization",token(61,"ROLE_STUDENT")))
                .andExpect(status().isNotFound());
        mvc.perform(put(url()+"/me").header("Authorization",token(61,"ROLE_STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":3,\"userId\":60,\"authorName\":\"Forged\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.authorName").value("Học viên 61"));
        stats(2,"4.00");
    }
    @ParameterizedTest @ValueSource(strings={"ROLE_STUDENT","ROLE_INSTRUCTOR","ROLE_ADMIN","ROLE_UNKNOWN"})
    void noRoleBypassesMembership(String role) throws Exception {
        mvc.perform(put(url()+"/me").header("Authorization",token(99,role))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}"))
                .andExpect(status().isForbidden());
        stats(0,"0.00");
    }
    @Test void privateReadRequiresVerifiedTokenEvenOnPublicPath() throws Exception {
        mvc.perform(get(url()+"/me")).andExpect(status().isUnauthorized());
        mvc.perform(get(url()+"/me").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
        mvc.perform(get(url()+"/me").header("Authorization",token(60,"ROLE_STUDENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.canReview").value(true))
                .andExpect(jsonPath("$.data.review").isEmpty());
        save(60,4);
        mvc.perform(get(url()+"/me").header("Authorization",token(60,"ROLE_STUDENT")))
                .andExpect(jsonPath("$.data.review.rating").value(4));
        mvc.perform(get(url()+"/me").header("Authorization",token(62,"ROLE_STUDENT")))
                .andExpect(jsonPath("$.data.canReview").value(false)).andExpect(jsonPath("$.data.review").isEmpty());
        mvc.perform(put(url()+"/me").contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete(url()+"/me")).andExpect(status().isUnauthorized());
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"rating\":null}","{\"rating\":0}","{\"rating\":6}","{\"rating\":-1}","{\"rating\":2.5}","{\"rating\":\"abc\"}"})
    void invalidRatingNeverWrites(String body) throws Exception {
        mvc.perform(put(url()+"/me").header("Authorization",token(60,"ROLE_STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        stats(0,"0.00");
    }
    @Test void commentLimitAndPlainTextArePreserved() throws Exception {
        String body="{\"rating\":5,\"comment\":\""+"x".repeat(2001)+"\"}";
        mvc.perform(put(url()+"/me").header("Authorization",token(60,"ROLE_STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(put(url()+"/me").header("Authorization",token(60,"ROLE_STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5,\"comment\":\"<script>alert(1)</script>\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.comment").value("<script>alert(1)</script>"));
    }
    @Test void draftDoesNotLeakReviewsAndArchivedAllowsHistoricalLearnerWrites() throws Exception {
        save(60,5);
        jdbc.update("UPDATE courses SET status='DRAFT' WHERE id=?",id);
        mvc.perform(get(url())).andExpect(status().isNotFound());
        mvc.perform(get(url()).header("Authorization",token(60,"ROLE_STUDENT"))).andExpect(status().isNotFound());
        mvc.perform(get(url()).header("Authorization",token(50,"ROLE_INSTRUCTOR"))).andExpect(status().isOk());
        mvc.perform(put(url()+"/me").header("Authorization",token(60,"ROLE_STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":1}")).andExpect(status().isNotFound());
        stats(1,"5.00");
        jdbc.update("UPDATE courses SET status='ARCHIVED' WHERE id=?",id);
        save(60,4);
        mvc.perform(get(url())).andExpect(status().isNotFound());
        stats(1,"4.00");
    }
    @Test void missingCourseAndWrongCourseFail() throws Exception {
        mvc.perform(get("/api/courses/9223372036854775807/reviews")).andExpect(status().isNotFound());
        mvc.perform(put("/api/courses/9223372036854775807/reviews/me").header("Authorization",token(60,"ROLE_STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}")).andExpect(status().isNotFound());
        var other=courses.save(Course.builder().instructorId(50L).title("Other").slug(UUID.randomUUID().toString()).status(CourseStatus.PUBLISHED).build());
        try {
            mvc.perform(put("/api/courses/"+other.getId()+"/reviews/me").header("Authorization",token(60,"ROLE_STUDENT"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}")).andExpect(status().isForbidden());
        } finally { courses.deleteById(other.getId()); }
    }

    @Test void roundingAndMaximumCommentLengthAreExact() throws Exception {
        learners.insertIfAbsent(id,62L);
        save(60,5); save(61,5); save(62,4); stats(3,"4.67");
        mvc.perform(put(url()+"/me").header("Authorization",token(60,"ROLE_STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5,\"comment\":\""+"x".repeat(2000)+"\"}"))
                .andExpect(status().isOk());
        stats(3,"4.67");
        mvc.perform(get(url()+"?sort=unknown,desc")).andExpect(status().isBadRequest());
    }
}
