package com.hunre.apigateway;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** HTTP đi qua gateway thật; hai backend giả lập ghi nhận đích, path và Authorization. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"elearning.rate-limit.enabled=false", "elearning.security.jwt-secret=" + EnrollmentRoutingIntegrationTest.SECRET})
class EnrollmentRoutingIntegrationTest {
    static final String SECRET = "enrollment-routing-integration-test-secret-at-least-32-bytes";
    private static final HttpServer ENROLLMENT = backend("enrollment");
    private static final HttpServer COURSE = backend("course");
    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("elearning.services.enrollment", () -> "http://localhost:" + ENROLLMENT.getAddress().getPort());
        registry.add("elearning.services.course", () -> "http://localhost:" + COURSE.getAddress().getPort());
    }

    @AfterAll
    static void stopBackends() {
        ENROLLMENT.stop(0);
        COURSE.stop(0);
    }

    @Test
    void progressPutReachesEnrollmentWithOriginalBodyAndToken() throws Exception {
        var response = request("PUT", "/api/lessons/101/progress", true);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("enrollment PUT /api/lessons/101/progress", "Bearer ", "\"courseId\":10");
    }

    @Test
    void publicLessonReadsStillReachCourse() throws Exception {
        var response = request("GET", "/api/lessons/101", false);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).startsWith("course GET /api/lessons/101");
    }

    @Test
    void progressWritesRequireAuthentication() throws Exception {
        assertThat(request("PUT", "/api/lessons/101/progress", false).statusCode()).isEqualTo(401);
    }

    @Test
    void normalizedEnrollmentAndProgressRoutesKeepQueryParameters() throws Exception {
        for (String path : List.of("/api/enrollments", "/api/progress?courseId=10")) {
            var response = request("GET", path, true);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).startsWith("enrollment GET " + path);
        }
        assertThat(request("DELETE", "/api/enrollments?courseId=10", true).body())
                .startsWith("enrollment DELETE /api/enrollments?courseId=10");
        assertThat(request("PATCH", "/api/enrollments/1/status", true).body())
                .startsWith("enrollment PATCH /api/enrollments/1/status");
    }

    private HttpResponse<String> request(String method, String path, boolean authenticated) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString("{\"courseId\":10}"));
        if (authenticated) {
            String token = Jwts.builder().subject("1").claim("roles", List.of("ROLE_STUDENT"))
                    .expiration(Date.from(Instant.now().plusSeconds(120)))
                    .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
            builder.header("Authorization", "Bearer " + token);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void certificateVerificationIsPublicButOtherCertificateAccessStaysPrivate() throws Exception {
        var response = request("GET", "/api/certificates/verify/CERT-test", false);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).startsWith("enrollment GET /api/certificates/verify/CERT-test null");
        for (String path : List.of("/api/certificates", "/api/certificates/1", "/api/enrollments/1/certificate")) {
            assertThat(request("GET", path, false).statusCode()).isEqualTo(401);
        }
        assertThat(request("POST", "/api/certificates/verify/CERT-test", false).statusCode()).isEqualTo(401);
    }

    private static HttpServer backend(String name) {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                byte[] body = (name + " " + exchange.getRequestMethod() + " " + exchange.getRequestURI()
                        + " " + exchange.getRequestHeaders().getFirst("Authorization") + " "
                        + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8))
                        .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
