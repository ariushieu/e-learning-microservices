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
        "spring.datasource.url=jdbc:h2:mem:sessions;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "elearning.security.enabled=true", "elearning.security.jwt-secret=${jwt.secret}"
})
class SessionIntegrationTest {
    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
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

    @Test void trimBeforeValidationAndSessionClaimsOnLoginAndRotation() throws Exception {
        JsonNode session = login("  A@EXAMPLE.COM  ", EDGE);
        assertThat(session.path("user").path("email").asText()).isEqualTo("a@example.com");
        long oldId = sid(session);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE id=? AND token_hash=?", Long.class,
                oldId, jwt.hashToken(session.path("refreshToken").asText()))).isEqualTo(1);
        JsonNode rotated = refresh(session, EDGE, 200).path("data");
        assertThat(sid(rotated)).isNotEqualTo(oldId);
        JsonNode list = list(rotated);
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0).path("id").asLong()).isEqualTo(sid(rotated));
        assertThat(list.get(0).path("current").asBoolean()).isTrue();
        refresh(session, EDGE, 401);
    }

    @Test void listsOnlyActiveOwnSessionsNewestFirstWithoutSensitiveFields() throws Exception {
        JsonNode edge = login("a@example.com", EDGE);
        JsonNode postman = login("a@example.com", "PostmanRuntime/7.43");
        JsonNode expired = login("a@example.com", "unknown");
        JsonNode revoked = login("a@example.com", "unknown");
        login("b@example.com", EDGE);
        jdbc.update("UPDATE refresh_tokens SET expires_at=TIMESTAMP '2000-01-01 00:00:00' WHERE id=?", sid(expired));
        jdbc.update("UPDATE refresh_tokens SET revoked_at=CURRENT_TIMESTAMP WHERE id=?", sid(revoked));
        JsonNode list = list(edge);
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).path("id").asLong()).isEqualTo(sid(postman));
        assertThat(list.get(0).path("device").asText()).isEqualTo("Thiết bị khác");
        assertThat(list.get(0).path("current").asBoolean()).isFalse();
        assertThat(list.get(1).path("device").asText()).isEqualTo("Edge trên Windows");
        assertThat(list.get(1).path("current").asBoolean()).isTrue();
        for (JsonNode row : list) {
            assertThat(row.size()).isEqualTo(4);
            assertThat(row.has("id") && row.has("device") && row.has("createdAt") && row.has("current")).isTrue();
        }
        assertThat(list.toString()).doesNotContain("ipAddress", "userAgent", "tokenHash", "refreshToken", "userId");
    }

    @Test void revokeOwnOtherAndCurrentSessionWhileAccessRemainsValid() throws Exception {
        JsonNode edge = login("a@example.com", EDGE);
        JsonNode other = login("a@example.com", "PostmanRuntime/7.43");
        JsonNode foreign = login("b@example.com", EDGE);
        request("DELETE", "/api/auth/sessions/" + sid(foreign), access(edge), null, EDGE, 404);
        request("DELETE", "/api/auth/sessions/999999999", access(edge), null, EDGE, 404);
        request("DELETE", "/api/auth/sessions/" + sid(other), access(edge), null, EDGE, 200);
        refresh(other, EDGE, 401);
        request("GET", "/api/auth/me", access(other), null, EDGE, 200);
        request("DELETE", "/api/auth/sessions/" + sid(other), access(edge), null, EDGE, 404);
        request("DELETE", "/api/auth/sessions/" + sid(edge), access(edge), null, EDGE, 200);
        refresh(edge, EDGE, 401);
        assertThat(list(edge).size()).isZero();
        assertThat(list(foreign).size()).isEqualTo(1);
    }

    @Test void revokeOthersPreservesCurrentAndDoesNotTouchAnotherUser() throws Exception {
        JsonNode edge = login("a@example.com", EDGE);
        JsonNode first = login("a@example.com", "node");
        JsonNode second = login("a@example.com", "node");
        JsonNode foreign = login("b@example.com", EDGE);
        request("POST", "/api/auth/sessions/revoke-others", access(edge), null, EDGE, 200);
        assertThat(list(edge).size()).isEqualTo(1);
        assertThat(list(edge).get(0).path("current").asBoolean()).isTrue();
        refresh(first, EDGE, 401);
        refresh(second, EDGE, 401);
        refresh(foreign, EDGE, 200);
        request("POST", "/api/auth/sessions/revoke-others", access(edge), null, EDGE, 200);
        refresh(edge, EDGE, 200);
    }

    @Test void noTokenAndInvalidTokenAreUnauthorizedOnEveryEndpoint() throws Exception {
        for (String access : new String[]{null, "invalid.jwt"}) {
            request("GET", "/api/auth/sessions", access, null, EDGE, 401);
            request("DELETE", "/api/auth/sessions/1", access, null, EDGE, 401);
            request("POST", "/api/auth/sessions/revoke-others", access, null, EDGE, 401);
        }
    }

    @Test void missingStaleAndForeignSidCannotRevokeOthers() throws Exception {
        JsonNode edge = login("a@example.com", EDGE);
        JsonNode foreign = login("b@example.com", EDGE);
        var user = users.findByEmail("a@example.com").orElseThrow();
        // A legacy token may list sessions, but cannot choose a current session.
        String legacy = jwt.generateAccessToken(user);
        assertThat(request("GET", "/api/auth/sessions", legacy, null, EDGE, 200)
                .path("data").get(0).path("current").asBoolean()).isFalse();
        for (String token : new String[]{legacy, jwt.generateAccessToken(user, sid(foreign)),
                jwt.generateAccessToken(user, 999999999L)}) {
            request("POST", "/api/auth/sessions/revoke-others", token, null, EDGE, 401);
        }
        JsonNode rotated = refresh(edge, EDGE, 200).path("data");
        request("POST", "/api/auth/sessions/revoke-others", access(edge), null, EDGE, 401);
        assertThat(list(rotated).size()).isEqualTo(1);
        refresh(rotated, EDGE, 200);
    }

    @Test void longUserAgentIsBoundedOnBothIssuancePaths() throws Exception {
        JsonNode session = login("a@example.com", EDGE + "x".repeat(400));
        assertThat(jdbc.queryForObject("SELECT LENGTH(user_agent) FROM refresh_tokens WHERE id=?", Integer.class, sid(session)))
                .isEqualTo(255);
        JsonNode rotated = refresh(session, "x".repeat(1000), 200).path("data");
        assertThat(jdbc.queryForObject("SELECT LENGTH(user_agent) FROM refresh_tokens WHERE id=?", Integer.class, sid(rotated)))
                .isEqualTo(255);
        assertThat(list(rotated).get(0).path("device").asText()).isEqualTo("Thiết bị khác");
    }

    @Test void revokeOthersSerializesWithRefreshAndRevokesItsReplacement() throws Exception {
        JsonNode current = login("a@example.com", EDGE);
        JsonNode other = login("a@example.com", EDGE);
        CompletableFuture<HttpResponse<String>> refresh = client.sendAsync(build("POST", "/api/auth/refresh-token", null,
                Map.of("refreshToken", other.path("refreshToken").asText()), EDGE), HttpResponse.BodyHandlers.ofString());
        request("POST", "/api/auth/sessions/revoke-others", access(current), null, EDGE, 200);
        HttpResponse<String> response = refresh.get(15, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(response.statusCode()).isIn(200, 401);
        if (response.statusCode() == 200) refresh(mapper.readTree(response.body()).path("data"), EDGE, 401);
        assertThat(list(current).size()).isEqualTo(1);
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
