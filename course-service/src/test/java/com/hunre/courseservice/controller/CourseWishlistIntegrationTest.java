package com.hunre.courseservice.controller;

import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.repository.CourseRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=" + CourseWishlistIntegrationTest.SECRET,
        "elearning.security.public-paths=GET:/api/courses/**"
})
@AutoConfigureMockMvc
class CourseWishlistIntegrationTest {
    static final String SECRET = "wishlist-test-secret-key-with-at-least-32-characters";
    @Autowired MockMvc mvc;
    @Autowired CourseRepository courses;
    @Autowired JdbcTemplate jdbc;
    Long first, second, draft;

    @BeforeEach void seed() {
        first = course("Java", CourseStatus.PUBLISHED);
        second = course("GIS", CourseStatus.PUBLISHED);
        draft = course("Nháp", CourseStatus.DRAFT);
    }

    @AfterEach void cleanup() {
        jdbc.update("DELETE FROM course_wishlist WHERE course_id IN (?, ?, ?)", first, second, draft);
        courses.deleteAllById(List.of(first, second, draft));
    }

    Long course(String title, CourseStatus status) {
        return courses.save(Course.builder().instructorId(50L).title(title)
                .slug(UUID.randomUUID().toString()).status(status).build()).getId();
    }

    String token(long user) {
        return "Bearer " + Jwts.builder().subject(Long.toString(user)).claim("roles", List.of("ROLE_STUDENT"))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    @Test void savesNewestFirstIdempotentlyAndPerUser() throws Exception {
        mvc.perform(put("/api/wishlist/" + first).header("Authorization", token(60))).andExpect(status().isOk());
        Thread.sleep(5);
        mvc.perform(put("/api/wishlist/" + second).header("Authorization", token(60))).andExpect(status().isOk());
        mvc.perform(put("/api/wishlist/" + second).header("Authorization", token(60))).andExpect(status().isOk());

        mvc.perform(get("/api/wishlist").header("Authorization", token(60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("GIS"))
                .andExpect(jsonPath("$.data[1].title").value("Java"));
        mvc.perform(get("/api/wishlist/ids").header("Authorization", token(61)))
                .andExpect(jsonPath("$.data.length()").value(0));

        mvc.perform(delete("/api/wishlist/" + second).header("Authorization", token(60))).andExpect(status().isOk());
        mvc.perform(delete("/api/wishlist/" + second).header("Authorization", token(60))).andExpect(status().isOk());
        mvc.perform(get("/api/wishlist/ids").header("Authorization", token(60)))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0]").value(first));
    }

    @Test void onlyPublishedCoursesAndOnlySignedInUsers() throws Exception {
        mvc.perform(put("/api/wishlist/" + draft).header("Authorization", token(60))).andExpect(status().isNotFound());
        mvc.perform(put("/api/wishlist/999999").header("Authorization", token(60))).andExpect(status().isNotFound());
        mvc.perform(get("/api/wishlist")).andExpect(status().isUnauthorized());

        // Lưu rồi khóa bị gỡ xuất bản: không hiện nữa, nhưng vẫn giữ để hiện lại khi xuất bản lại.
        mvc.perform(put("/api/wishlist/" + first).header("Authorization", token(60))).andExpect(status().isOk());
        var course = courses.findById(first).orElseThrow();
        course.setStatus(CourseStatus.ARCHIVED);
        courses.save(course);
        mvc.perform(get("/api/wishlist").header("Authorization", token(60))).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get("/api/wishlist/ids").header("Authorization", token(60))).andExpect(jsonPath("$.data.length()").value(1));
    }
}
