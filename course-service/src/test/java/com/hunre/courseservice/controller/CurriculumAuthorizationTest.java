package com.hunre.courseservice.controller;

import com.hunre.courseservice.client.EnrollmentAccessClient;
import com.hunre.courseservice.entity.*;
import com.hunre.courseservice.event.CourseEventPublisher;
import com.hunre.courseservice.repository.*;
import com.hunre.courseservice.service.CourseService;
import com.hunre.courseservice.service.CurriculumService;
import com.hunre.courseservice.dto.request.*;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=course-security-integration-test-secret-at-least-32-bytes",
        "elearning.security.public-paths=GET:/api/courses/**,GET:/api/lessons/**"
})
@AutoConfigureMockMvc
@Transactional
class CurriculumAuthorizationTest {
    private static final String SECRET = "course-security-integration-test-secret-at-least-32-bytes";
    @Autowired MockMvc mvc;
    @Autowired CourseRepository courses;
    @Autowired SectionRepository sections;
    @Autowired LessonRepository lessons;
    @Autowired LessonResourceRepository resources;
    @Autowired EntityManager entityManager;
    @Autowired CurriculumService curriculum;
    @Autowired CourseService courseService;
    @MockitoBean EnrollmentAccessClient enrollmentAccessClient;
    @MockitoBean CourseEventPublisher publisher;
    Course course;
    Section section;
    Lesson lesson;
    LessonResource resource;

    @BeforeEach
    void seed() {
        course = courses.save(Course.builder().instructorId(50L).title("Security course")
                .slug(UUID.randomUUID().toString()).totalLessons(1).build());
        section = sections.save(Section.builder().course(course).title("Original section").build());
        lesson = lessons.save(Lesson.builder().course(course).section(section).title("Original lesson")
                .content("Protected text").contentUrl("https://example.test/private.mp4").build());
        resource = resources.save(LessonResource.builder().lesson(lesson).name("Private document")
                .fileUrl("https://example.test/private.pdf").build());
        entityManager.flush();
        entityManager.clear();
    }

    private String token(long userId, String role) {
        return "Bearer " + Jwts.builder().subject(Long.toString(userId)).claim("roles", List.of(role))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private MockHttpServletRequestBuilder mutation(int operation) {
        return switch (operation) {
            case 0 -> post("/api/courses/" + course.getId() + "/sections").content("{\"title\":\"New section\"}");
            case 1 -> put("/api/sections/" + section.getId()).content("{\"title\":\"Changed section\"}");
            case 2 -> delete("/api/sections/" + section.getId());
            case 3 -> post("/api/sections/" + section.getId() + "/lessons")
                    .content("{\"title\":\"New lesson\",\"content\":\"New content\",\"contentUrl\":\"https://example.test/new.mp4\"}");
            case 4 -> put("/api/lessons/" + lesson.getId())
                    .content("{\"title\":\"Changed lesson\",\"content\":\"Changed content\",\"contentUrl\":\"https://example.test/new.mp4\"}");
            case 5 -> delete("/api/lessons/" + lesson.getId());
            case 6 -> post("/api/lessons/" + lesson.getId() + "/resources")
                    .content("{\"name\":\"New document\",\"fileUrl\":\"https://example.test/new.pdf\"}");
            case 7 -> delete("/api/lessons/" + lesson.getId() + "/resources/" + resource.getId());
            default -> throw new IllegalArgumentException();
        };
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,2,3,4,5,6,7})
    void anotherInstructorCannotMutateAnyCurriculumResource(int operation) throws Exception {
        long sectionCount = sections.count(), lessonCount = lessons.count(), resourceCount = resources.count();
        mvc.perform(mutation(operation).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token(60, "ROLE_INSTRUCTOR")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        entityManager.clear();
        assertThat(sections.count()).isEqualTo(sectionCount);
        assertThat(lessons.count()).isEqualTo(lessonCount);
        assertThat(resources.count()).isEqualTo(resourceCount);
        assertThat(lessons.findById(lesson.getId()).orElseThrow().getTitle()).isEqualTo("Original lesson");
        assertThat(sections.findById(section.getId()).orElseThrow().getTitle()).isEqualTo("Original section");
        verifyNoInteractions(publisher);
    }

    @ParameterizedTest
    @CsvSource({"0,50,ROLE_INSTRUCTOR,201", "1,50,ROLE_INSTRUCTOR,200", "2,50,ROLE_INSTRUCTOR,200",
            "3,50,ROLE_INSTRUCTOR,201", "4,50,ROLE_INSTRUCTOR,200", "5,50,ROLE_INSTRUCTOR,200",
            "6,50,ROLE_INSTRUCTOR,201", "7,50,ROLE_INSTRUCTOR,200",
            "0,99,ROLE_ADMIN,201", "1,99,ROLE_ADMIN,200", "2,99,ROLE_ADMIN,200",
            "3,99,ROLE_ADMIN,201", "4,99,ROLE_ADMIN,200", "5,99,ROLE_ADMIN,200",
            "6,99,ROLE_ADMIN,201", "7,99,ROLE_ADMIN,200"})
    void ownerAndAdminCanMutate(int operation, long user, String role, int expectedStatus) throws Exception {
        mvc.perform(mutation(operation).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token(user, role)))
                .andExpect(status().is(expectedStatus)).andExpect(jsonPath("$.success").value(true));
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,2,3,4,5,6,7})
    void guestAndStudentCannotMutate(int operation) throws Exception {
        mvc.perform(mutation(operation).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
        mvc.perform(mutation(operation).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token(50, "ROLE_STUDENT"))).andExpect(status().isForbidden());
    }

    @Test
    void resourceMustBelongToTheLessonInThePath() throws Exception {
        mvc.perform(delete("/api/lessons/999999/resources/" + resource.getId())
                .header("Authorization", token(99, "ROLE_ADMIN"))).andExpect(status().isNotFound());
        assertThat(resources.existsById(resource.getId())).isTrue();
    }

    private void publish(boolean preview) {
        courses.findById(course.getId()).orElseThrow().setStatus(CourseStatus.PUBLISHED);
        lessons.findById(lesson.getId()).orElseThrow().setIsPreview(preview);
        entityManager.flush();
        entityManager.clear();
    }

    private void assertContent(String authorization, boolean visible) throws Exception {
        for (String path : List.of("/api/lessons/" + lesson.getId(), "/api/courses/" + course.getId() + "/curriculum")) {
            var request = get(path);
            if (authorization != null) request.header("Authorization", authorization);
            String prefix = path.contains("curriculum") ? "$.data[0].lessons[0]" : "$.data";
            var result = mvc.perform(request).andExpect(status().isOk());
            if (visible) {
                result.andExpect(jsonPath(prefix + ".content").value("Protected text"))
                        .andExpect(jsonPath(prefix + ".contentUrl").value("https://example.test/private.mp4"))
                        .andExpect(jsonPath(prefix + ".resources[0].fileUrl").value("https://example.test/private.pdf"));
            } else {
                result.andExpect(jsonPath(prefix + ".content").doesNotExist())
                        .andExpect(jsonPath(prefix + ".contentUrl").doesNotExist())
                        .andExpect(jsonPath(prefix + ".resources").isEmpty());
            }
        }
    }

    @Test
    void publishedPreviewIsPublicAndNeedsNoEnrollmentCall() throws Exception {
        publish(true);
        assertContent(null, true);
        verifyNoInteractions(enrollmentAccessClient);
    }

    @Test
    void protectedContentIsRedactedForGuestAndUnenrolledUser() throws Exception {
        publish(false);
        assertContent(null, false);
        assertContent(token(60, "ROLE_STUDENT"), false);
        assertContent("Bearer invalid-token", false);
    }

    @Test
    void enrolledStudentSeesContentAndResourceUrls() throws Exception {
        publish(false);
        when(enrollmentAccessClient.hasEnrollment(course.getId(), 60L)).thenReturn(true);
        assertContent(token(60, "ROLE_STUDENT"), true);
        verify(enrollmentAccessClient, times(2)).hasEnrollment(course.getId(), 60L);
    }

    @Test
    void enrollmentOutageKeepsCurriculumAvailableButProtectsRegularLessons() throws Exception {
        publish(false);
        Lesson preview = lessons.save(Lesson.builder().course(course).section(section).title("Preview lesson")
                .position(1).isPreview(true).content("Public text")
                .contentUrl("https://example.test/preview.mp4").build());
        resources.save(LessonResource.builder().lesson(preview).name("Preview document")
                .fileUrl("https://example.test/preview.pdf").build());
        entityManager.flush();
        entityManager.clear();
        when(enrollmentAccessClient.hasEnrollment(course.getId(), 60L))
                .thenThrow(new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "Enrollment unavailable"));

        for (String authorization : List.of("", token(60, "ROLE_STUDENT"))) {
            var request = get("/api/courses/" + course.getId() + "/curriculum");
            if (!authorization.isEmpty()) request.header("Authorization", authorization);
            mvc.perform(request).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].lessons.length()").value(2))
                    .andExpect(jsonPath("$.data[0].lessons[0].id").value(lesson.getId()))
                    .andExpect(jsonPath("$.data[0].lessons[0].title").value("Original lesson"))
                    .andExpect(jsonPath("$.data[0].lessons[0].content").doesNotExist())
                    .andExpect(jsonPath("$.data[0].lessons[0].contentUrl").doesNotExist())
                    .andExpect(jsonPath("$.data[0].lessons[0].resources").isEmpty())
                    .andExpect(jsonPath("$.data[0].lessons[1].content").value("Public text"))
                    .andExpect(jsonPath("$.data[0].lessons[1].contentUrl").value("https://example.test/preview.mp4"))
                    .andExpect(jsonPath("$.data[0].lessons[1].resources[0].fileUrl").value("https://example.test/preview.pdf"));
        }
        mvc.perform(get("/api/lessons/" + lesson.getId()).header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_ERROR"));
        mvc.perform(get("/api/lessons/" + preview.getId()).header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").value("Public text"));
        verify(enrollmentAccessClient, times(2)).hasEnrollment(course.getId(), 60L);
    }

    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"FORBIDDEN", "UNAUTHORIZED", "BUSINESS_RULE_VIOLATED"})
    void curriculumDoesNotSwallowOtherBusinessErrors(ErrorCode errorCode) throws Exception {
        publish(false);
        when(enrollmentAccessClient.hasEnrollment(course.getId(), 60L))
                .thenThrow(new BusinessException(errorCode, "Other business error"));
        mvc.perform(get("/api/courses/" + course.getId() + "/curriculum")
                        .header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().is(errorCode.httpStatus().value()))
                .andExpect(jsonPath("$.code").value(errorCode.name()));
    }

    @Test
    void ownerAndAdminSeeDraftContentWithoutEnrollment() throws Exception {
        assertContent(token(50, "ROLE_INSTRUCTOR"), true);
        assertContent(token(99, "ROLE_ADMIN"), true);
        verifyNoInteractions(enrollmentAccessClient);
    }

    private List<String> archivedReadPaths() {
        return List.of("/api/courses/" + course.getId(), "/api/courses/slug/" + course.getSlug(),
                "/api/courses/" + course.getId() + "/curriculum", "/api/lessons/" + lesson.getId());
    }

    @Test
    void archivedEnrollmentGrantsReadingButNeverOwnership() throws Exception {
        courses.findById(course.getId()).orElseThrow().setStatus(CourseStatus.ARCHIVED);
        when(enrollmentAccessClient.hasEnrollment(course.getId(), 60L)).thenReturn(true);
        assertContent(token(60, "ROLE_STUDENT"), true);
        mvc.perform(get("/api/courses/" + course.getId()).header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ARCHIVED"));
        mvc.perform(get("/api/courses/slug/" + course.getSlug()).header("Authorization", token(60, "ROLE_STUDENT")))
                .andExpect(status().isOk());
        verify(enrollmentAccessClient, times(4)).hasEnrollment(course.getId(), 60L);
        for (String auth : List.of("", token(60, "ROLE_STUDENT"))) {
            for (String path : List.of("/api/courses", "/api/courses?instructorId=50")) {
                mvc.perform(get(path).header("Authorization", auth))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
            }
        }
        mvc.perform(post("/api/courses/" + course.getId() + "/sections")
                        .header("Authorization", token(60, "ROLE_INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Not mine\"}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void archivedCourseIncludingPreviewIsHiddenWithoutEnrollment(boolean preview) throws Exception {
        courses.findById(course.getId()).orElseThrow().setStatus(CourseStatus.ARCHIVED);
        lessons.findById(lesson.getId()).orElseThrow().setIsPreview(preview);
        for (String path : archivedReadPaths()) {
            mvc.perform(get(path)).andExpect(status().isNotFound());
            mvc.perform(get(path).header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isNotFound());
        }
        verifyNoInteractions(enrollmentAccessClient);
        for (String path : archivedReadPaths()) {
            mvc.perform(get(path).header("Authorization", token(60, "ROLE_STUDENT")))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void archivedOutageDeniesStudentsButOwnerAndAdminStillRead() throws Exception {
        courses.findById(course.getId()).orElseThrow().setStatus(CourseStatus.ARCHIVED);
        when(enrollmentAccessClient.hasEnrollment(course.getId(), 60L))
                .thenThrow(new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "Enrollment unavailable"));
        for (String path : archivedReadPaths()) {
            mvc.perform(get(path).header("Authorization", token(60, "ROLE_STUDENT")))
                    .andExpect(status().isNotFound());
            mvc.perform(get(path).header("Authorization", token(50, "ROLE_INSTRUCTOR")))
                    .andExpect(status().isOk());
            mvc.perform(get(path).header("Authorization", token(99, "ROLE_ADMIN")))
                    .andExpect(status().isOk());
        }
        assertContent(token(50, "ROLE_INSTRUCTOR"), true);
        assertContent(token(99, "ROLE_ADMIN"), true);
        verify(enrollmentAccessClient, times(4)).hasEnrollment(course.getId(), 60L);
        verifyNoMoreInteractions(enrollmentAccessClient);
    }

    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"FORBIDDEN", "UNAUTHORIZED", "BUSINESS_RULE_VIOLATED"})
    void archivedReadsDoNotSwallowOtherBusinessErrors(ErrorCode errorCode) throws Exception {
        courses.findById(course.getId()).orElseThrow().setStatus(CourseStatus.ARCHIVED);
        when(enrollmentAccessClient.hasEnrollment(course.getId(), 60L))
                .thenThrow(new BusinessException(errorCode, "Other business error"));
        for (String path : archivedReadPaths()) {
            mvc.perform(get(path).header("Authorization", token(60, "ROLE_STUDENT")))
                    .andExpect(status().is(errorCode.httpStatus().value()))
                    .andExpect(jsonPath("$.code").value(errorCode.name()));
        }
    }

    @Test
    void draftStaysPrivateEvenForPreviouslyEnrolledStudent() throws Exception {
        when(enrollmentAccessClient.hasEnrollment(course.getId(), 60L)).thenReturn(true);
        for (String path : archivedReadPaths()) {
            mvc.perform(get(path).header("Authorization", token(60, "ROLE_STUDENT")))
                    .andExpect(status().isNotFound());
        }
        verifyNoInteractions(enrollmentAccessClient);
    }

    @Test
    void evenPreviewInDraftRemainsHiddenFromStranger() throws Exception {
        lessons.findById(lesson.getId()).orElseThrow().setIsPreview(true);
        for (String path : List.of("/api/lessons/" + lesson.getId(), "/api/courses/" + course.getId() + "/curriculum")) {
            mvc.perform(get(path)).andExpect(status().isNotFound());
            mvc.perform(get(path).header("Authorization", token(60, "ROLE_STUDENT"))).andExpect(status().isNotFound());
        }
        verifyNoInteractions(enrollmentAccessClient);
    }

    @Test
    void contentCanBeSavedAndReadAfterUpdate() throws Exception {
        mvc.perform(mutation(4).contentType(MediaType.APPLICATION_JSON).header("Authorization", token(50, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").value("Changed content"));
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/api/lessons/" + lesson.getId()).header("Authorization", token(50, "ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.content").value("Changed content"))
                .andExpect(jsonPath("$.data.contentUrl").value("https://example.test/new.mp4"));
    }

    @Test
    void instructorFilterDoesNotRevealDraftsToOtherUsersAndCombinesFilters() throws Exception {
        String path = "/api/courses?instructorId=50&keyword=Security&level=BEGINNER";
        mvc.perform(get(path)).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get(path).header("Authorization", token(60, "ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get(path).header("Authorization", token(50, "ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(get(path + "&categoryId=999999").header("Authorization", token(50, "ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/courses?instructorId=50&keyword=missing").header("Authorization", token(50, "ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/courses?instructorId=50&level=ADVANCED").header("Authorization", token(50, "ROLE_INSTRUCTOR")))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get(path).header("Authorization", token(99, "ROLE_ADMIN")))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(get("/api/courses?sort=madeUp")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/courses?instructorId=invalid")).andExpect(status().isBadRequest());
    }

    @Test
    void nullIdentityNeverBypassesOwnershipEvenWithAdminFlag() {
        for (boolean admin : List.of(false, true)) {
            assertThatThrownBy(() -> curriculum.deleteLesson(lesson.getId(), null, admin)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> curriculum.deleteSection(section.getId(), null, admin)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> curriculum.deleteResource(lesson.getId(), resource.getId(), null, admin)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> courseService.deleteCourse(course.getId(), null, admin)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> courseService.updateCourse(course.getId(), new UpdateCourseRequest(), null, admin)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> courseService.changeCourseStatus(course.getId(), new ChangeCourseStatusRequest(), null, admin)).isInstanceOf(BusinessException.class);
        }
    }
}
