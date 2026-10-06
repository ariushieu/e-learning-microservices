package com.hunre.courseservice.controller;

import com.hunre.courseservice.entity.Course;
import com.hunre.courseservice.entity.CourseStatus;
import com.hunre.courseservice.entity.Lesson;
import com.hunre.courseservice.entity.Section;
import com.hunre.courseservice.repository.CourseRepository;
import com.hunre.courseservice.repository.LessonRepository;
import com.hunre.courseservice.repository.SectionRepository;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Client HTTP thật: mock EnrollmentAccessClient sẽ không phát hiện cấu hình URL cũ. */
@SpringBootTest(properties = {
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=" + EnrollmentContentAccessIntegrationTest.SECRET,
        "elearning.security.public-paths=GET:/api/courses/**,GET:/api/lessons/**"
})
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class EnrollmentContentAccessIntegrationTest {
    static final String SECRET = "course-enrollment-content-regression-secret-at-least-32-bytes";
    private static final List<String> PATHS = new CopyOnWriteArrayList<>();
    private static final List<String> TOKENS = new CopyOnWriteArrayList<>();
    private static volatile String enrollmentPage;
    private static volatile int enrollmentStatus;
    private static final HttpServer ENROLLMENT = enrollmentServer();

    @Autowired MockMvc mvc;
    @Autowired Environment environment;
    @Autowired CourseRepository courses;
    @Autowired SectionRepository sections;
    @Autowired LessonRepository lessons;
    @Autowired EntityManager entityManager;
    private Course course;
    private Lesson lesson;

    @DynamicPropertySource
    static void enrollmentAddress(DynamicPropertyRegistry registry) {
        // Giữ list-path từ application.properties để test bắt được lỗi cấu hình thật.
        registry.add("course.enrollment.base-url", () -> "http://127.0.0.1:" + ENROLLMENT.getAddress().getPort());
    }

    @BeforeEach
    void prepare() {
        PATHS.clear();
        TOKENS.clear();
        enrollmentStatus = 200;
        course = courses.save(Course.builder().instructorId(50L).title("Enrolled access")
                .slug(UUID.randomUUID().toString()).status(CourseStatus.PUBLISHED).totalLessons(1).build());
        Section section = sections.save(Section.builder().course(course).title("Section").build());
        lesson = lessons.save(Lesson.builder().course(course).section(section).title("Protected lesson")
                .content("Protected text").contentUrl("https://example.test/private.mp4").isPreview(false).build());
        entityManager.flush();
        entityManager.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "COMPLETED"})
    void enrolledStudentReadsLessonAndCurriculumThroughConfiguredClient(String status) throws Exception {
        enrollmentPage = page("{\"userId\":60,\"courseId\":" + course.getId() + ",\"status\":\"" + status + "\"}");
        String token = studentToken();
        mvc.perform(get("/api/lessons/{id}", lesson.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Protected text"))
                .andExpect(jsonPath("$.data.contentUrl").value("https://example.test/private.mp4"));
        mvc.perform(get("/api/courses/{id}/curriculum", course.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lessons[0].content").value("Protected text"))
                .andExpect(jsonPath("$.data[0].lessons[0].contentUrl").value("https://example.test/private.mp4"));
        assertThat(PATHS).containsExactly(
                "/api/enrollments?page=0&size=100&sort=id,asc",
                "/api/enrollments?page=0&size=100&sort=id,asc");
        assertThat(TOKENS).containsExactly(token, token);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CANCELLED", "NONE"})
    void cancelledOrMissingEnrollmentDoesNotRevealProtectedContent(String status) throws Exception {
        enrollmentPage = page("NONE".equals(status) ? "" :
                "{\"userId\":60,\"courseId\":" + course.getId() + ",\"status\":\"CANCELLED\"}");
        String token = studentToken();
        mvc.perform(get("/api/lessons/{id}", lesson.getId()).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").doesNotExist());
        mvc.perform(get("/api/courses/{id}/curriculum", course.getId()).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].lessons[0].content").doesNotExist());
        assertThat(PATHS).hasSize(2).allMatch(path -> path.startsWith("/api/enrollments?"));
    }

    @Test
    void productionDefaultUsesTheSameEndpointAsTheTestedClient() throws Exception {
        Properties production = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/application.properties"))) {
            production.load(reader);
        }
        String productionDefault = new PropertySourcesPropertyResolver(new MutablePropertySources())
                .resolveRequiredPlaceholders(production.getProperty("course.enrollment.list-path"));
        assertThat(productionDefault).isEqualTo(environment.getRequiredProperty("course.enrollment.list-path"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "COMPLETED"})
    void archivedCourseKeepsContentAndRechecksEnrollmentOnEachRequest(String status) throws Exception {
        courses.findById(course.getId()).orElseThrow().setStatus(CourseStatus.ARCHIVED);
        enrollmentPage = page("{\"userId\":60,\"courseId\":" + course.getId() + ",\"status\":\"" + status + "\"}");
        String token = studentToken();
        mvc.perform(get("/api/courses/{id}/curriculum", course.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lessons[0].content").value("Protected text"));
        mvc.perform(get("/api/lessons/{id}", lesson.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentUrl").value("https://example.test/private.mp4"));
        assertThat(PATHS).hasSize(2);
        assertThat(TOKENS).containsExactly(token, token);

        // Hủy ghi danh phải mất quyền ngay ở request kế tiếp, không dùng lại kết quả cũ.
        enrollmentPage = page("{\"userId\":60,\"courseId\":" + course.getId() + ",\"status\":\"CANCELLED\"}");
        for (String path : List.of("/api/courses/" + course.getId() + "/curriculum",
                "/api/lessons/" + lesson.getId(), "/api/courses/" + course.getId(),
                "/api/courses/slug/" + course.getSlug())) {
            mvc.perform(get(path).header("Authorization", token)).andExpect(status().isNotFound());
        }
        assertThat(PATHS).hasSize(6);
    }

    private static String page(String enrollment) {
        return "{\"success\":true,\"data\":{\"page\":0,\"last\":true,\"content\":[" + enrollment + "]}}";
    }

    @ParameterizedTest
    @CsvSource({"60,ACTIVE,wrong-course", "999,ACTIVE,same-course", "60,CANCELLED,same-course"})
    void archivedReadsRejectUnrelatedOrCancelledEnrollment(long userId, String status, String target) throws Exception {
        courses.findById(course.getId()).orElseThrow().setStatus(CourseStatus.ARCHIVED);
        long enrolledCourseId = "wrong-course".equals(target) ? course.getId() + 1000 : course.getId();
        enrollmentPage = page("{\"userId\":" + userId + ",\"courseId\":" + enrolledCourseId
                + ",\"status\":\"" + status + "\"}");
        assertArchivedReadsNotFound();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 429, 500, 503})
    void archivedReadsFailClosedWithRealHttpErrors(int responseStatus) throws Exception {
        courses.findById(course.getId()).orElseThrow().setStatus(CourseStatus.ARCHIVED);
        enrollmentPage = page("{\"userId\":60,\"courseId\":" + course.getId() + ",\"status\":\"ACTIVE\"}");
        enrollmentStatus = responseStatus;
        assertArchivedReadsNotFound();
    }

    @Test
    void archivedReadsFailClosedWithMalformedHttpResponse() throws Exception {
        courses.findById(course.getId()).orElseThrow().setStatus(CourseStatus.ARCHIVED);
        enrollmentPage = "not json";
        assertArchivedReadsNotFound();
    }

    private void assertArchivedReadsNotFound() throws Exception {
        String token = studentToken();
        for (String path : List.of("/api/courses/" + course.getId(), "/api/courses/slug/" + course.getSlug(),
                "/api/courses/" + course.getId() + "/curriculum", "/api/lessons/" + lesson.getId())) {
            mvc.perform(get(path).header("Authorization", token)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
        assertThat(PATHS).hasSize(4);
        assertThat(TOKENS).containsOnly(token);
    }

    private String studentToken() {
        return "Bearer " + Jwts.builder().subject("60").claim("roles", List.of("ROLE_STUDENT"))
                .expiration(Date.from(Instant.now().plusSeconds(120)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private static HttpServer enrollmentServer() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                PATHS.add(exchange.getRequestURI().toString());
                TOKENS.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
                // HttpServer khớp theo prefix, phải kiểm path chính xác để URL cũ thật sự trả 404.
                boolean currentPath = "/api/enrollments".equals(exchange.getRequestURI().getPath());
                byte[] body = (currentPath ? enrollmentPage : "{}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(currentPath ? enrollmentStatus : 404, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @AfterAll
    static void stopEnrollment() {
        ENROLLMENT.stop(0);
    }
}
