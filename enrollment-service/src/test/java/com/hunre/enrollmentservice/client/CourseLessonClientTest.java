package com.hunre.enrollmentservice.client;

import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CourseLessonClientTest {
    private MockRestServiceServer server;
    private CourseLessonClient client;
    private MockHttpServletRequest incoming;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://course-service:8082");
        server = MockRestServiceServer.bindTo(builder).build();
        incoming = new MockHttpServletRequest();
        incoming.addHeader("Authorization", "Bearer student-token");
        @SuppressWarnings("unchecked")
        ObjectProvider<HttpServletRequest> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(incoming);
        client = new CourseLessonClient(builder.build(), provider);
    }

    @Test
    void acceptsLessonInRequestedCourse() {
        server.expect(requestTo("http://course-service:8082/api/lessons/101"))
                .andExpect(header("Authorization", "Bearer student-token"))
                .andRespond(withSuccess("""
                        {"success":true,"data":{"id":101,"courseId":10,"title":"Bài 1","resources":[]}}
                        """, MediaType.APPLICATION_JSON));
        assertThatCode(() -> client.validateLesson(10L, 101L)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void rejectsMissingLesson() {
        server.expect(requestTo("http://course-service:8082/api/lessons/101"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertError(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void rejectsLessonBelongingToAnotherCourse() {
        server.expect(requestTo("http://course-service:8082/api/lessons/101"))
                .andRespond(withSuccess("{\"success\":true,\"data\":{\"id\":101,\"courseId\":20}}",
                        MediaType.APPLICATION_JSON));
        assertError(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"success\":true,\"data\":null}",
            "{\"success\":false,\"data\":{\"id\":101,\"courseId\":10}}",
            "{\"success\":true,\"data\":{\"id\":102,\"courseId\":10}}",
            "{\"success\":true,\"data\":{\"id\":101}}",
            "not json"
    })
    void rejectsInvalidUpstreamResponse(String body) {
        server.expect(requestTo("http://course-service:8082/api/lessons/101"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertError(ErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    @Test
    void upstreamFailureIsNotReportedAsMissingLesson() {
        server.expect(requestTo("http://course-service:8082/api/lessons/101"))
                .andRespond(withServerError());
        assertError(ErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    @Test
    void timeoutFailsClosed() {
        server.expect(requestTo("http://course-service:8082/api/lessons/101"))
                .andRespond(withException(new SocketTimeoutException("Timed out")));
        assertError(ErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    private void assertError(ErrorCode expected) {
        assertThatThrownBy(() -> client.validateLesson(10L, 101L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.errorCode()).isEqualTo(expected));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "ARCHIVED"})
    void refusesEnrollmentEvenWhenCallerCanReadUnpublishedCourse(String status) {
        server.expect(requestTo("http://course-service:8082/api/courses/10"))
                .andExpect(header("Authorization", "Bearer student-token"))
                .andRespond(withSuccess("{\"success\":true,\"data\":{\"id\":10,\"status\":\"" + status + "\"}}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.requirePublishedCourse(10L)).isInstanceOfSatisfying(BusinessException.class,
                e -> org.assertj.core.api.Assertions.assertThat(e.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        server.verify();
    }

    @Test
    void verifiesPublishedCourseWithCallerToken() {
        server.expect(requestTo("http://course-service:8082/api/courses/10"))
                .andExpect(header("Authorization", "Bearer student-token"))
                .andRespond(withSuccess("{\"success\":true,\"data\":{\"id\":10,\"status\":\"PUBLISHED\"}}", MediaType.APPLICATION_JSON));
        client.requirePublishedCourse(10L);
        server.verify();
    }

    @Test
    void noTokenDoesNotSendAnonymousLessonProbe() {
        incoming.removeHeader("Authorization");
        assertError(ErrorCode.UNAUTHORIZED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "not json", "{\"success\":true,\"data\":{\"id\":99,\"status\":\"PUBLISHED\"}}"})
    void malformedCourseProbeCannotGrantEnrollment(String body) {
        server.expect(requestTo("http://course-service:8082/api/courses/10"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.requirePublishedCourse(10L)).isInstanceOfSatisfying(BusinessException.class,
                e -> org.assertj.core.api.Assertions.assertThat(e.errorCode()).isEqualTo(ErrorCode.EXTERNAL_SERVICE_ERROR));
        server.verify();
    }

    @Test
    void courseProbeTimeoutFailsClosed() {
        server.expect(requestTo("http://course-service:8082/api/courses/10"))
                .andRespond(withException(new SocketTimeoutException("Timed out")));
        assertThatThrownBy(() -> client.requirePublishedCourse(10L)).isInstanceOfSatisfying(BusinessException.class,
                e -> org.assertj.core.api.Assertions.assertThat(e.errorCode()).isEqualTo(ErrorCode.EXTERNAL_SERVICE_ERROR));
        server.verify();
    }
}
