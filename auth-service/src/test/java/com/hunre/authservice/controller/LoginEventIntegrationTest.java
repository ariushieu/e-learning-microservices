package com.hunre.authservice.controller;

import com.hunre.authservice.repository.UserRepository;
import com.hunre.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:loginEvents;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "elearning.security.enabled=true", "elearning.security.jwt-secret=${jwt.secret}"
})
class LoginEventIntegrationTest {
    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @Autowired com.hunre.authservice.service.LoginEventService events;
    final HttpClient client = HttpClient.newHttpClient();
    static final String PASSWORD = "Session@123456";
    static final String EDGE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/130.0 Safari/537.36 Edg/130.0";

    @BeforeEach void prepare() throws Exception {
        jdbc.update("DELETE FROM refresh_tokens");
        jdbc.update("DELETE FROM user_roles");
        jdbc.update("DELETE FROM users");
        for (String email : new String[]{"a@example.com", "b@example.com"}) {
            request("POST", "/api/auth/register", null,
                    Map.of("email", "  " + email.toUpperCase(java.util.Locale.ROOT) + "  ", "password", PASSWORD,
                            "fullName", "Session Test"), EDGE, 201);
        }
    }

    @Test void failedAttemptsCommitOn401AndWarningAcknowledgesOnNextSuccess() throws Exception {
        for (int i = 0; i < 3; i++) badLogin("a@example.com");
        JsonNode session = login("a@example.com", EDGE);
        assertThat(warning(session)).isEqualTo(3);
        assertThat(session.path("user").path("failedLoginsSinceLastSuccess").asLong()).isEqualTo(3);
        JsonNode history = history(session, "");
        assertThat(history.path("totalElements").asLong()).isEqualTo(4);
        assertThat(history.path("content").get(0).path("success").asBoolean()).isTrue();
        for (int i = 1; i < 4; i++) assertThat(history.path("content").get(i).path("success").asBoolean()).isFalse();
        JsonNode rotated = refresh(session, EDGE, 200).path("data");
        assertThat(warning(rotated)).isEqualTo(3);
        assertThat(history(rotated, "").path("totalElements").asLong()).isEqualTo(4);
        assertThat(warning(login("a@example.com", EDGE))).isZero();
        badLogin("a@example.com");
        assertThat(warning(login("a@example.com", EDGE))).isEqualTo(1);
    }

    @Test void unknownAccountDoesNotCreateEventAndLockedCorrectPasswordIsNotSuccess() throws Exception {
        badLogin("absent@example.com");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM login_events", Long.class)).isZero();
        jdbc.update("UPDATE users SET status='LOCKED' WHERE email='a@example.com'");
        request("POST", "/api/auth/login", null, Map.of("email", "a@example.com", "password", PASSWORD), EDGE, 403);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM login_events", Long.class)).isZero();
        badLogin("a@example.com");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM login_events WHERE success=false", Long.class)).isEqualTo(1);
    }

    @Test void privacyPaginationOrderingAndValidation() throws Exception {
        badLogin("a@example.com");
        JsonNode a = login("a@example.com", EDGE);
        JsonNode b = login("b@example.com", "PostmanRuntime/7");
        assertThat(history(b, "?userId=" + users.findByEmail("a@example.com").orElseThrow().getId())
                .path("totalElements").asLong()).isEqualTo(1);
        JsonNode first = history(a, "?page=0&size=1&sort=createdAt,asc");
        assertThat(first.path("content").get(0).path("success").asBoolean()).isTrue();
        assertThat(first.path("totalPages").asInt()).isEqualTo(2);
        JsonNode row = first.path("content").get(0);
        assertThat(row.size()).isEqualTo(3);
        assertThat(row.path("device").asText()).isEqualTo("Edge trên Windows");
        assertThat(history(a, "?page=1&size=1").path("content").get(0).path("success").asBoolean()).isFalse();
        assertThat(history(a, "?page=20").path("content").size()).isZero();
        for (String query : new String[]{"?page=-1", "?size=0", "?size=101", "?page=2147483647&size=100", "?page=x"}) {
            request("GET", "/api/auth/login-events" + query, access(a), null, EDGE, 400);
        }
        for (String token : new String[]{null, "bad.token"}) {
            request("GET", "/api/auth/login-events", token, null, EDGE, 401);
        }
    }

    @Test void sessionStartSurvivesRepeatedRotation() throws Exception {
        JsonNode session = login("a@example.com", EDGE);
        long originalId = sid(session);
        String originalStart = list(session).get(0).path("startedAt").asText();
        for (int i = 0; i < 3; i++) {
            session = refresh(session, EDGE, 200).path("data");
            assertThat(sid(session)).isNotEqualTo(originalId);
            assertThat(list(session).get(0).path("startedAt").asText()).isEqualTo(originalStart);
        }
        assertThat(history(session, "").path("totalElements").asLong()).isEqualTo(1);
    }

    @Test void boundsUserAgentAndCascadesDeletedAccounts() throws Exception {
        JsonNode session = login("a@example.com", EDGE + "x".repeat(400));
        assertThat(jdbc.queryForObject("SELECT LENGTH(user_agent) FROM login_events", Integer.class)).isEqualTo(255);
        assertThat(history(session, "").path("content").get(0).path("device").asText()).isEqualTo("Edge trên Windows");
        jdbc.update("DELETE FROM refresh_tokens");
        jdbc.update("DELETE FROM user_roles");
        jdbc.update("DELETE FROM users");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM login_events", Long.class)).isZero();
    }

    @Test void retentionHidesOldRowsAndScheduledCleanupDeletesOnlyOldRows() throws Exception {
        badLogin("a@example.com");
        JsonNode session = login("a@example.com", EDGE);
        jdbc.update("UPDATE login_events SET created_at=TIMESTAMP '2000-01-01 00:00:00' WHERE success=false");
        assertThat(history(session, "").path("totalElements").asLong()).isEqualTo(1);
        assertThat(warning(session)).isZero();
        events.cleanup();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM login_events", Long.class)).isEqualTo(1);
    }

    @Test void concurrentFailuresAreAllRecordedBeforeSuccessfulLogin() throws Exception {
        var futures = new java.util.ArrayList<CompletableFuture<HttpResponse<String>>>();
        for (int i = 0; i < 6; i++) futures.add(client.sendAsync(build("POST", "/api/auth/login", null,
                Map.of("email", "a@example.com", "password", "wrong-password"), EDGE), HttpResponse.BodyHandlers.ofString()));
        for (var future : futures) assertThat(future.get(20, java.util.concurrent.TimeUnit.SECONDS).statusCode()).isEqualTo(401);
        assertThat(warning(login("a@example.com", EDGE))).isEqualTo(6);
    }

    private void badLogin(String email) throws Exception {
        request("POST", "/api/auth/login", null, Map.of("email", email, "password", "wrong-password"), EDGE, 401);
    }
    private JsonNode history(JsonNode session, String query) throws Exception {
        return request("GET", "/api/auth/login-events" + query, access(session), null, EDGE, 200).path("data");
    }
    private long warning(JsonNode session) throws Exception {
        return request("GET", "/api/auth/me", access(session), null, EDGE, 200)
                .path("data").path("failedLoginsSinceLastSuccess").asLong();
    }

    private JsonNode login(String email, String ua) throws Exception {
        return request("POST", "/api/auth/login", null, Map.of("email", email, "password", PASSWORD), ua, 200).path("data");
    }
    private JsonNode refresh(JsonNode session, String ua, int expected) throws Exception {
        return request("POST", "/api/auth/refresh-token", null,
                Map.of("refreshToken", session.path("refreshToken").asText()), ua, expected);
    }
    private JsonNode list(JsonNode session) throws Exception {
        return request("GET", "/api/auth/sessions", access(session), null, EDGE, 200).path("data");
    }
    private String access(JsonNode session) { return session.path("accessToken").asText(); }
    private long sid(JsonNode session) { return ((Number) jwt.parseToken(access(session)).get("sid")).longValue(); }
    private HttpRequest build(String method, String path, String token, Object body, String ua) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json").header("User-Agent", ua);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
    }
    private JsonNode request(String method, String path, String token, Object body, String ua, int status) throws Exception {
        var response = client.send(build(method, path, token, body, ua), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(method + " " + path).isEqualTo(status);
        return mapper.readTree(response.body());
    }
}
