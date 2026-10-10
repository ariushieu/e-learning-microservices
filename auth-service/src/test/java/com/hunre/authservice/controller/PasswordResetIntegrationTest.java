package com.hunre.authservice.controller;

import com.hunre.authservice.domain.VerificationTokenType;
import com.hunre.authservice.outbox.OutboxEvent;
import com.hunre.authservice.outbox.OutboxEventRepository;
import com.hunre.authservice.repository.VerificationTokenRepository;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:password_reset;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=${jwt.secret}",
        // Cấu hình test thay hẳn application.properties chính; khai lại đúng danh sách công khai của nó.
        "elearning.security.public-paths=/actuator/**,/api/auth/login,/api/auth/register,/api/auth/refresh-token,"
                + "/api/auth/logout,/api/auth/forgot-password,/api/auth/reset-password"
})
class PasswordResetIntegrationTest {
    static final String GENERIC = "Nếu email này đã đăng ký, bạn sẽ nhận được liên kết đặt lại mật khẩu trong vài phút.";
    // Sinh mới mỗi lần chạy: không để chuỗi giống mật khẩu nằm cứng trong mã nguồn.
    final String oldPassword = randomPassword();
    final String newPassword = randomPassword();

    static String randomPassword() {
        return "T-" + java.util.UUID.randomUUID();
    }

    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newHttpClient();
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired OutboxEventRepository outbox;
    @Autowired VerificationTokenRepository tokens;
    @Autowired JwtService jwt;

    @BeforeEach
    void setUp() throws Exception {
        outbox.deleteAll();
        jdbc.update("DELETE FROM verification_tokens");
        jdbc.update("DELETE FROM refresh_tokens");
        jdbc.update("DELETE FROM login_events");
        jdbc.update("DELETE FROM user_roles");
        jdbc.update("DELETE FROM users");
        call("/api/auth/register", Map.of("email", "reset@example.com", "password", oldPassword, "fullName", "Người Quên"))
                .expect(201);
        outbox.deleteAll();
    }

    record Reply(int status, JsonNode body) {
        Reply expect(int expected) {
            assertThat(status).as(body.toString()).isEqualTo(expected);
            return this;
        }

        String message() {
            return body.path("message").asString();
        }
    }

    private Reply call(String path, Map<String, ?> body) throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),
                HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body().isBlank() ? mapper.createObjectNode() : mapper.readTree(response.body()));
    }

    private Reply forgot(String email) throws Exception {
        return call("/api/auth/forgot-password", Map.of("email", email));
    }

    private List<OutboxEvent> resetEvents() {
        return outbox.findAll().stream().filter(e -> e.getEventType().equals("user.password.reset.requested"))
                .sorted(java.util.Comparator.comparing(OutboxEvent::getId)).toList();
    }

    private String rawTokenFromEvent() {
        List<OutboxEvent> events = resetEvents();
        JsonNode payload = mapper.readTree(events.get(events.size() - 1).getPayload());
        return payload.path("token").asString();
    }

    @Test
    void knownEmailGetsAOneTimeTokenThatOnlyItsHashIsStored() throws Exception {
        assertThat(forgot("Reset@Example.com").expect(200).message()).isEqualTo(GENERIC);

        assertThat(resetEvents()).singleElement().satisfies(event -> {
            JsonNode payload = mapper.readTree(event.getPayload());
            assertThat(payload.path("email").asString()).isEqualTo("reset@example.com");
            assertThat(payload.path("fullName").asString()).isEqualTo("Người Quên");
            assertThat(payload.path("token").asString()).hasSizeGreaterThanOrEqualTo(43);
        });
        String raw = rawTokenFromEvent();
        var stored = tokens.findByTokenHashAndType(jwt.hashToken(raw), VerificationTokenType.PASSWORD_RESET);
        assertThat(stored).hasValueSatisfying(t -> {
            assertThat(t.getTokenHash()).isNotEqualTo(raw);
            assertThat(t.getExpiresAt()).isBetween(Instant.now().plus(Duration.ofMinutes(29)), Instant.now().plus(Duration.ofMinutes(31)));
        });
    }

    @Test
    void unknownOrLockedEmailLooksTheSameButSendsNothing() throws Exception {
        assertThat(forgot("nobody@example.com").expect(200).message()).isEqualTo(GENERIC);
        jdbc.update("UPDATE users SET status = 'LOCKED'");
        assertThat(forgot("reset@example.com").expect(200).message()).isEqualTo(GENERIC);
        assertThat(resetEvents()).isEmpty();
        assertThat(tokens.count()).isZero();
    }

    @Test
    void repeatedRequestsWithinAMinuteSendOneMailAndANewLinkRevokesTheOld() throws Exception {
        forgot("reset@example.com").expect(200);
        forgot("reset@example.com").expect(200);
        assertThat(resetEvents()).hasSize(1);
        String first = rawTokenFromEvent();

        // Hết thời gian chờ: link mới được gửi và link cũ thôi dùng được.
        jdbc.update("UPDATE verification_tokens SET created_at = ?", java.sql.Timestamp.from(Instant.now().minus(Duration.ofMinutes(2))));
        forgot("reset@example.com").expect(200);
        assertThat(resetEvents()).hasSize(2);
        call("/api/auth/reset-password", Map.of("token", first, "newPassword", newPassword))
                .expect(400);
        call("/api/auth/reset-password", Map.of("token", rawTokenFromEvent(), "newPassword", newPassword))
                .expect(200);
    }

    @Test
    void resetChangesThePasswordRevokesSessionsAndCannotBeReused() throws Exception {
        String refresh = call("/api/auth/login", Map.of("email", "reset@example.com", "password", oldPassword))
                .expect(200).body().path("data").path("refreshToken").asString();
        forgot("reset@example.com");
        String raw = rawTokenFromEvent();

        assertThat(call("/api/auth/reset-password", Map.of("token", raw, "newPassword", newPassword))
                .expect(200).message()).isEqualTo("Đã đặt lại mật khẩu. Hãy đăng nhập bằng mật khẩu mới.");
        call("/api/auth/login", Map.of("email", "reset@example.com", "password", oldPassword)).expect(401);
        call("/api/auth/login", Map.of("email", "reset@example.com", "password", newPassword)).expect(200);
        call("/api/auth/refresh-token", Map.of("refreshToken", refresh)).expect(401);

        assertThat(call("/api/auth/reset-password", Map.of("token", raw, "newPassword", randomPassword()))
                .expect(400).message()).contains("không hợp lệ hoặc đã hết hạn");
    }

    @Test
    void expiredWrongOrWeakInputIsRejected() throws Exception {
        forgot("reset@example.com");
        String raw = rawTokenFromEvent();
        call("/api/auth/reset-password", Map.of("token", raw, "newPassword", "123")).expect(400);
        call("/api/auth/reset-password", Map.of("token", "not-a-real-token", "newPassword", newPassword)).expect(400);
        jdbc.update("UPDATE verification_tokens SET expires_at = ?", java.sql.Timestamp.from(Instant.now().minusSeconds(1)));
        call("/api/auth/reset-password", Map.of("token", raw, "newPassword", newPassword)).expect(400);
        forgot("not-an-email").expect(400);
    }
}
