package com.hunre.authservice.controller;

import com.hunre.authservice.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:profile_management;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=${jwt.secret}"
})
class ProfileManagementIntegrationTest {
    @LocalServerPort
    private int port;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoSpyBean
    private PasswordEncoder passwordEncoder;
    @MockitoSpyBean
    private RefreshTokenRepository refreshTokens;

    private final HttpClient client = HttpClient.newHttpClient();
    private static final String OLD_PASSWORD = "Student@123456";
    private static final String NEW_PASSWORD = "Changed@123456";
    private JsonNode session;
    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("DELETE FROM refresh_tokens");
        jdbc.update("DELETE FROM user_roles");
        jdbc.update("DELETE FROM users");
        register("profile@example.com", "Original Name");
        session = login("profile@example.com", OLD_PASSWORD, 200).path("data");
        userId = session.path("user").path("id").asLong();
    }

    @Test
    void profileUsesTokenIdentityAndCannotChangeSensitiveFields() throws Exception {
        JsonNode other = register("other@example.com", "Other User").path("data");
        long otherId = other.path("id").asLong();
        JsonNode updated = request("PUT", "/api/auth/me?userId=" + otherId, accessToken(), Map.of(
                "fullName", "  Updated Name  ", "phone", " 0901234567 ", "userId", otherId,
                "email", "attacker@example.com", "roles", List.of("ROLE_ADMIN"),
                "passwordHash", "injected", "status", "LOCKED"), 200).path("data");
        assertThat(updated.path("id").asLong()).isEqualTo(userId);
        assertThat(updated.path("fullName").asText()).isEqualTo("Updated Name");
        assertThat(updated.path("phone").asText()).isEqualTo("0901234567");
        assertThat(updated.path("email").asText()).isEqualTo("profile@example.com");
        assertThat(updated.path("roles").toString()).isEqualTo("[\"ROLE_STUDENT\"]");
        assertThat(updated.path("status").asText()).isEqualTo("ACTIVE");
        assertThat(updated.has("passwordHash")).isFalse();
        JsonNode me = request("GET", "/api/auth/me", accessToken(), null, 200).path("data");
        assertThat(me.path("fullName").asText()).isEqualTo("Updated Name");
        assertThat(jdbc.queryForObject("SELECT full_name FROM users WHERE id = ?", String.class, otherId))
                .isEqualTo("Other User");
        login("profile@example.com", OLD_PASSWORD, 200);
    }

    @Test
    void omittedOrBlankPhoneClearsTheOptionalField() throws Exception {
        request("PUT", "/api/auth/me", accessToken(), Map.of("fullName", "Name", "phone", "0901234567"), 200);
        request("PUT", "/api/auth/me", accessToken(), Map.of("fullName", "Name"), 200);
        assertThat(jdbc.queryForObject("SELECT phone FROM users WHERE id = ?", String.class, userId)).isNull();
        request("PUT", "/api/auth/me", accessToken(), Map.of("fullName", "Name", "phone", "   "), 200);
        assertThat(jdbc.queryForObject("SELECT phone FROM users WHERE id = ?", String.class, userId)).isNull();
    }

    @Test
    void passwordChangeRevokesEverySessionButNotAnotherUsersTokens() throws Exception {
        JsonNode second = login("profile@example.com", OLD_PASSWORD, 200).path("data");
        register("other@example.com", "Other User");
        JsonNode other = login("other@example.com", OLD_PASSWORD, 200).path("data");
        request("POST", "/api/auth/change-password?userId=" + other.path("user").path("id").asLong(),
                accessToken(), Map.of("currentPassword", OLD_PASSWORD, "newPassword", NEW_PASSWORD,
                        "userId", other.path("user").path("id").asLong()), 200);
        login("profile@example.com", OLD_PASSWORD, 401);
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, userId);
        assertThat(hash).isNotEqualTo(NEW_PASSWORD);
        assertThat(passwordEncoder.matches(NEW_PASSWORD, hash)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL",
                Integer.class, userId)).isZero();
        refresh(session, 401);
        refresh(second, 401);
        refresh(other, 200);
        JsonNode newSession = login("profile@example.com", NEW_PASSWORD, 200).path("data");
        refresh(newSession, 200);
    }

    @Test
    void wrongCurrentPasswordReturnsFieldErrorAndPreservesCredentialsAndSessions() throws Exception {
        JsonNode error = request("POST", "/api/auth/change-password", accessToken(),
                Map.of("currentPassword", "Incorrect@123", "newPassword", NEW_PASSWORD), 400);
        assertValidation(error, "currentPassword");
        assertThat(error.toString()).doesNotContain("Incorrect@123", NEW_PASSWORD, "passwordHash");
        login("profile@example.com", OLD_PASSWORD, 200);
        login("profile@example.com", NEW_PASSWORD, 401);
        refresh(session, 200);
    }

    @Test
    void tokenRevocationFailureRollsBackThePasswordChange() throws Exception {
        doThrow(new IllegalStateException("Simulated revocation failure"))
                .when(refreshTokens).revokeAllUserTokens(eq(userId), any());
        request("POST", "/api/auth/change-password", accessToken(),
                Map.of("currentPassword", OLD_PASSWORD, "newPassword", NEW_PASSWORD), 500);
        login("profile@example.com", OLD_PASSWORD, 200);
        login("profile@example.com", NEW_PASSWORD, 401);
        refresh(session, 200);
    }

    @Test
    void refreshAndLoginCannotIssueTokensWhilePasswordChangeIsCommitting() throws Exception {
        CountDownLatch revoking = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        doAnswer(invocation -> {
            revoking.countDown();
            if (!proceed.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test timed out");
            return invocation.callRealMethod();
        }).when(passwordEncoder).encode(NEW_PASSWORD);

        CompletableFuture<Void> changing = CompletableFuture.runAsync(() -> {
            try {
                request("POST", "/api/auth/change-password", accessToken(),
                        Map.of("currentPassword", OLD_PASSWORD, "newPassword", NEW_PASSWORD), 200);
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        try {
            assertThat(revoking.await(5, TimeUnit.SECONDS)).isTrue();
            CompletableFuture<Void> refreshing = CompletableFuture.runAsync(() -> {
                try { refresh(session, 401); } catch (Exception e) { throw new RuntimeException(e); }
            });
            CompletableFuture<Void> loggingIn = CompletableFuture.runAsync(() -> {
                try { login("profile@example.com", OLD_PASSWORD, 401); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            // Both requests must wait for the password transaction's user lock.
            assertThatThrownBy(() -> refreshing.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            assertThatThrownBy(() -> loggingIn.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            proceed.countDown();
            CompletableFuture.allOf(changing, refreshing, loggingIn).get(10, TimeUnit.SECONDS);
            login("profile@example.com", NEW_PASSWORD, 200);
        } finally {
            proceed.countDown();
            changing.get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void bothMutationsRequireValidAuthentication() throws Exception {
        for (String token : new String[]{null, "invalid-token"}) {
            request("PUT", "/api/auth/me", token, Map.of("fullName", "Name"), 401);
            request("POST", "/api/auth/change-password", token,
                    Map.of("currentPassword", OLD_PASSWORD, "newPassword", NEW_PASSWORD), 401);
        }
    }

    @Test
    void deletedCallerCannotMutateAnAccount() throws Exception {
        jdbc.update("DELETE FROM refresh_tokens WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM users WHERE id = ?", userId);
        request("PUT", "/api/auth/me", accessToken(), Map.of("fullName", "Name"), 404);
        request("POST", "/api/auth/change-password", accessToken(),
                Map.of("currentPassword", OLD_PASSWORD, "newPassword", NEW_PASSWORD), 404);
    }

    static Stream<Object[]> invalidProfiles() {
        return Stream.of(
                new Object[]{Map.of(), "fullName"},
                new Object[]{Map.of("fullName", "   "), "fullName"},
                new Object[]{Map.of("fullName", "x".repeat(151)), "fullName"},
                new Object[]{Map.of("fullName", "Valid Name", "phone", "1".repeat(21)), "phone"});
    }

    @ParameterizedTest
    @MethodSource("invalidProfiles")
    void rejectsInvalidProfileWithoutSaving(Map<String, String> body, String field) throws Exception {
        assertValidation(request("PUT", "/api/auth/me", accessToken(), body, 400), field);
        assertThat(jdbc.queryForObject("SELECT full_name FROM users WHERE id = ?", String.class, userId))
                .isEqualTo("Original Name");
    }

    static Stream<Object[]> invalidPasswords() {
        return Stream.of(
                new Object[]{Map.of("newPassword", NEW_PASSWORD), "currentPassword"},
                new Object[]{Map.of("currentPassword", " ", "newPassword", NEW_PASSWORD), "currentPassword"},
                new Object[]{Map.of("currentPassword", OLD_PASSWORD), "newPassword"},
                new Object[]{Map.of("currentPassword", OLD_PASSWORD, "newPassword", "      "), "newPassword"},
                new Object[]{Map.of("currentPassword", OLD_PASSWORD, "newPassword", "12345"), "newPassword"},
                new Object[]{Map.of("currentPassword", OLD_PASSWORD, "newPassword", "x".repeat(51)), "newPassword"});
    }

    @ParameterizedTest
    @MethodSource("invalidPasswords")
    void rejectsInvalidPasswordWithoutRevokingTokens(Map<String, String> body, String field) throws Exception {
        assertValidation(request("POST", "/api/auth/change-password", accessToken(), body, 400), field);
        login("profile@example.com", OLD_PASSWORD, 200);
        refresh(session, 200);
    }

    private void assertValidation(JsonNode error, String field) {
        assertThat(error.path("success").asBoolean()).isFalse();
        assertThat(error.path("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(error.path("fieldErrors").valueStream().map(e -> e.path("field").asText()).toList())
                .contains(field);
    }

    private String accessToken() { return session.path("accessToken").asText(); }

    private JsonNode register(String email, String name) throws Exception {
        return request("POST", "/api/auth/register", null,
                Map.of("email", email, "password", OLD_PASSWORD, "fullName", name), 201);
    }

    private JsonNode login(String email, String password, int status) throws Exception {
        return request("POST", "/api/auth/login", null, Map.of("email", email, "password", password), status);
    }

    private void refresh(JsonNode login, int status) throws Exception {
        request("POST", "/api/auth/refresh-token", null,
                Map.of("refreshToken", login.path("refreshToken").asText()), status);
    }

    private JsonNode request(String method, String path, String token, Object body, int expected) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("%s %s: %s", method, path, response.body()).isEqualTo(expected);
        return mapper.readTree(response.body());
    }
}
