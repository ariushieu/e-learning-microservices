package com.hunre.courseservice.client;

import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnrollmentAccessClientTest {
    HttpServer server;
    ExecutorService executor;
    MockHttpServletRequest incoming;
    EnrollmentAccessClient client;
    List<String> calls = new CopyOnWriteArrayList<>();
    List<String> tokens = new CopyOnWriteArrayList<>();
    volatile int responseStatus = 200;
    volatile long delay;
    volatile String firstBody;
    volatile String nextBody;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void start() throws Exception {
        incoming = new MockHttpServletRequest();
        incoming.addHeader("Authorization", "Bearer original-caller-token");
        ObjectProvider<HttpServletRequest> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(incoming);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/api/enrollments/my-courses", exchange -> {
            calls.add(exchange.getRequestURI().toString());
            tokens.add(exchange.getRequestHeaders().getFirst("Authorization"));
            if (delay > 0) {
                try { Thread.sleep(delay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            String body = exchange.getRequestURI().getQuery().contains("page=0") ? firstBody : nextBody;
            if (body == null) body = "{}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + server.getAddress().getPort() + "/redirect");
            try {
                exchange.sendResponseHeaders(responseStatus, bytes.length);
                exchange.getResponseBody().write(bytes);
            } finally { exchange.close(); }
        });
        server.start();
        client = new EnrollmentAccessClient(provider, JsonMapper.builder().build(),
                "http://127.0.0.1:" + server.getAddress().getPort(), "/api/enrollments/my-courses", 500);
    }

    @AfterEach
    void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    private String page(int page, boolean last, long userId, long courseId, String status) {
        return """
                {"success":true,"data":{"page":%d,"last":%s,"content":[
                {"userId":%d,"courseId":%d,"status":"%s"}]}}
                """.formatted(page, last, userId, courseId, status);
    }

    @ParameterizedTest
    @CsvSource({"ACTIVE,true", "COMPLETED,true", "CANCELLED,false", "UNKNOWN,false"})
    void onlyActiveOrCompletedEnrollmentsGrantAccess(String status, boolean expected) {
        firstBody = page(0, true, 60, 10, status);
        assertThat(client.hasEnrollment(10L, 60L)).isEqualTo(expected);
        assertThat(tokens).containsExactly("Bearer original-caller-token");
    }

    @Test
    void findsEnrollmentOnSecondPageAndForwardsTheSameToken() {
        firstBody = page(0, false, 60, 11, "ACTIVE");
        nextBody = page(1, true, 60, 10, "ACTIVE");
        assertThat(client.hasEnrollment(10L, 60L)).isTrue();
        assertThat(calls).hasSize(2);
        assertThat(calls.get(1)).contains("page=1", "size=100", "sort=id,asc");
        assertThat(tokens).containsOnly("Bearer original-caller-token");
    }

    @Test
    void anotherUsersEnrollmentCannotGrantAccess() {
        firstBody = page(0, true, 999, 10, "ACTIVE");
        assertThat(client.hasEnrollment(10L, 60L)).isFalse();
    }

    @Test
    void noEnrollmentOrNoTokenDoesNotGrantAccess() {
        firstBody = "{\"success\":true,\"data\":{\"page\":0,\"last\":true,\"content\":[]}}";
        assertThat(client.hasEnrollment(10L, 60L)).isFalse();
        incoming.removeHeader("Authorization");
        assertThat(client.hasEnrollment(10L, 60L)).isFalse();
        assertThat(calls).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    void rejectedTokenDoesNotGrantAccess(int status) {
        responseStatus = status;
        assertThat(client.hasEnrollment(10L, 60L)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 404, 429, 500, 503})
    void dependencyErrorsAndRedirectsFailClosed(int status) {
        responseStatus = status;
        firstBody = page(0, true, 60, 10, "ACTIVE");
        assertUnavailable();
        assertThat(calls).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "not json", "{}", "{\"success\":true,\"data\":{\"content\":[]}}",
            "{\"success\":true,\"data\":{\"page\":0,\"last\":false,\"content\":[]}}"})
    void malformedResponseDoesNotGrantAccess(String body) {
        firstBody = body;
        assertUnavailable();
    }

    @Test
    void slowDependencyHasABoundedWaitAndDoesNotGrantAccess() {
        firstBody = page(0, true, 60, 10, "ACTIVE");
        delay = 1500;
        long started = System.nanoTime();
        assertUnavailable();
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofMillis(1400));
    }

    @Test
    void connectionRefusedDoesNotGrantAccess() {
        server.stop(0);
        assertUnavailable();
    }

    private void assertUnavailable() {
        assertThatThrownBy(() -> client.hasEnrollment(10L, 60L)).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.EXTERNAL_SERVICE_ERROR));
    }
}
