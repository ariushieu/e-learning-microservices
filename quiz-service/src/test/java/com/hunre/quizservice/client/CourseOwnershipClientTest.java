package com.hunre.quizservice.client;

import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CourseOwnershipClientTest {
    private MockRestServiceServer server;
    private CourseOwnershipClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://course-service:8082");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CourseOwnershipClient(builder.build());
    }

    @ParameterizedTest
    @CsvSource({"50,false", "60,true"})
    void acceptsOwnerOrAdminAndForwardsOriginalToken(long userId, boolean admin) {
        server.expect(requestTo("http://course-service:8082/api/courses/10"))
                .andExpect(header("Authorization", "Bearer original-token"))
                .andRespond(withSuccess("""
                        {"success":true,"data":{"id":10,"instructorId":50,"status":"DRAFT","title":"Course"}}
                        """, MediaType.APPLICATION_JSON));
        assertThatCode(() -> client.requireCourseOwner(10L, userId, admin, "Bearer original-token"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void anotherInstructorIsForbidden() {
        server.expect(requestTo("http://course-service:8082/api/courses/10"))
                .andRespond(withSuccess("{\"success\":true,\"data\":{\"id\":10,\"instructorId\":60}}",
                        MediaType.APPLICATION_JSON));
        assertError(ErrorCode.FORBIDDEN);
    }

    @ParameterizedTest
    @CsvSource({"404,RESOURCE_NOT_FOUND", "403,FORBIDDEN", "401,UNAUTHORIZED", "500,EXTERNAL_SERVICE_ERROR",
            "503,EXTERNAL_SERVICE_ERROR", "429,EXTERNAL_SERVICE_ERROR"})
    void upstreamStatusIsMappedWithoutAllowingCreation(int status, ErrorCode code) {
        server.expect(requestTo("http://course-service:8082/api/courses/10"))
                .andRespond(withStatus(HttpStatus.valueOf(status)));
        assertError(code);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "not json", "{}", "{\"success\":true,\"data\":null}",
            "{\"success\":false,\"data\":{\"id\":10,\"instructorId\":50}}",
            "{\"success\":true,\"data\":{\"id\":11,\"instructorId\":50}}",
            "{\"success\":true,\"data\":{\"id\":10}}"})
    void malformedResponseFailsClosed(String body) {
        server.expect(requestTo("http://course-service:8082/api/courses/10"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertError(ErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    @Test
    void timeoutFailsClosed() {
        server.expect(requestTo("http://course-service:8082/api/courses/10"))
                .andRespond(withException(new SocketTimeoutException("Timed out")));
        assertError(ErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    @Test
    void adminStillNeedsAnExistingCourse() {
        server.expect(requestTo("http://course-service:8082/api/courses/10"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> client.requireCourseOwner(10L, 60L, true, "Bearer original-token"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        server.verify();
    }

    @Test
    void missingIdentityOrTokenIsRejectedBeforeCallingCourseService() {
        assertThatThrownBy(() -> client.requireCourseOwner(10L, null, true, "Bearer original-token"))
                .isInstanceOf(BusinessException.class);
        for (String token : new String[]{null, "", " "}) {
            assertThatThrownBy(() -> client.requireCourseOwner(10L, 50L, false, token))
                    .isInstanceOf(BusinessException.class);
        }
        server.verify();
    }

    private void assertError(ErrorCode expected) {
        assertThatThrownBy(() -> client.requireCourseOwner(10L, 50L, false, "Bearer original-token"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.errorCode()).isEqualTo(expected));
        server.verify();
    }
}
