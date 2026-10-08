package com.hunre.authservice.controller;

import com.hunre.authservice.domain.*;
import com.hunre.authservice.repository.*;
import com.hunre.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:user_management;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "elearning.security.enabled=true", "elearning.security.jwt-secret=${jwt.secret}"
})
class UserManagementIntegrationTest {
    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @MockitoSpyBean RefreshTokenRepository refreshTokens;
    private final HttpClient client = HttpClient.newHttpClient();
    private static final String PASSWORD = "Test@123456";
    private User admin, otherAdmin, student, instructor;
    private String adminToken;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM refresh_tokens"); jdbc.update("DELETE FROM user_roles");
        jdbc.update("DELETE FROM users"); jdbc.update("DELETE FROM roles");
        for (RoleCode code : RoleCode.values()) roles.save(Role.builder().code(code).name(code.name()).build());
        String hash = encoder.encode(PASSWORD);
        admin = create("admin@example.com", "Admin", hash, RoleCode.ROLE_ADMIN);
        otherAdmin = create("other.admin@example.com", "Other Admin", hash, RoleCode.ROLE_ADMIN, RoleCode.ROLE_STUDENT);
        student = create("qa.student@example.com", "Nguyễn Văn Quốc", hash, RoleCode.ROLE_STUDENT);
        instructor = create("instructor@example.com", "Instructor", hash, RoleCode.ROLE_INSTRUCTOR, RoleCode.ROLE_STUDENT);
        adminToken = jwt.generateAccessToken(admin);
    }

    private User create(String email, String name, String hash, RoleCode... codes) {
        Set<Role> assigned = new HashSet<>();
        for (RoleCode code : codes) assigned.add(roles.findByCode(code).orElseThrow());
        return users.save(User.builder().email(email).fullName(name).passwordHash(hash).status(UserStatus.ACTIVE)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).roles(assigned).build());
    }

    @Test void listHasBoundedStablePagesAndSafeDto() throws Exception {
        JsonNode page = get("", 200).path("data");
        assertThat(page.path("size").asInt()).isEqualTo(12);
        assertThat(page.path("totalElements").asInt()).isEqualTo(4);
        JsonNode first = page.path("content").get(0);
        assertThat(first.path("id").asLong()).isEqualTo(admin.getId());
        assertThat(first.has("email")).isTrue(); assertThat(first.has("fullName")).isTrue();
        assertThat(first.has("phone")).isTrue(); assertThat(first.has("status")).isTrue();
        assertThat(first.has("createdAt")).isTrue(); assertThat(first.has("roles")).isTrue();
        assertThat(page.toString()).doesNotContain("passwordHash", "tokenHash", "refreshToken");
        JsonNode second = get("?page=1&size=2", 200).path("data");
        assertThat(second.path("content").get(0).path("id").asLong()).isEqualTo(student.getId());
        assertThat(second.path("page").asInt()).isEqualTo(1);
        assertThat(second.path("totalPages").asInt()).isEqualTo(2);
        assertThat(second.path("last").asBoolean()).isTrue();
        assertThat(get("?page=50&size=2", 200).path("data").path("content").size()).isZero();
    }

    @Test void filtersCombineAndTreatLikeMetacharactersLiterally() throws Exception {
        assertThat(get("?keyword=%20QA.STUDENT%20", 200).path("data").path("totalElements").asInt()).isEqualTo(1);
        assertThat(get("?keyword=Qu%E1%BB%91c", 200).path("data").path("content").get(0).path("id").asLong()).isEqualTo(student.getId());
        assertThat(get("?role=ROLE_STUDENT", 200).path("data").path("totalElements").asInt()).isEqualTo(3);
        assertThat(get("?role=ROLE_INSTRUCTOR&keyword=instructor&status=ACTIVE", 200).path("data").path("totalElements").asInt()).isEqualTo(1);
        assertThat(get("?role=ROLE_ADMIN&keyword=qa.student", 200).path("data").path("content").size()).isZero();
        assertThat(get("?keyword=%25", 200).path("data").path("totalElements").asInt()).isZero();
        assertThat(get("?keyword=_", 200).path("data").path("totalElements").asInt()).isZero();
        create("literal@example.com", "100%_Done!", student.getPasswordHash(), RoleCode.ROLE_STUDENT);
        assertThat(get("?keyword=%25_Done!", 200).path("data").path("totalElements").asInt()).isEqualTo(1);
        status(student.getId(), "LOCKED", adminToken, 200);
        assertThat(get("?status=LOCKED", 200).path("data").path("content").get(0).path("id").asLong()).isEqualTo(student.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"email,asc", "email,desc", "fullName,asc", "fullName,desc", "createdAt,asc", "createdAt,desc"})
    void acceptsOnlyDocumentedSorts(String sort) throws Exception {
        assertThat(get("?sort=" + sort, 200).path("data").path("content").size()).isEqualTo(4);
    }

    @Test void sortDirectionChangesTheReturnedOrder() throws Exception {
        assertThat(get("?sort=email,asc", 200).path("data").path("content").get(0).path("email").asText()).isEqualTo(admin.getEmail());
        assertThat(get("?sort=email,desc", 200).path("data").path("content").get(0).path("email").asText()).isEqualTo(student.getEmail());
    }

    @ParameterizedTest
    @ValueSource(strings = {"sort=abcxyz", "sort=passwordHash", "sort=id", "sort=roles.code", "sort=email,invalid",
            "page=-1", "page=abc", "page=2147483647", "size=0", "size=101", "size=abc", "role=ADMIN", "status=DELETED"})
    void rejectsBadListParameters(String query) throws Exception { get("?" + query, 400); }

    @Test void bothEndpointsEnforceAuthenticationAndAdminRole() throws Exception {
        for (String token : new String[]{null, "invalid"}) {
            request("GET", "/api/users", token, null, 401);
            status(student.getId(), "LOCKED", token, 401);
        }
        for (User user : new User[]{student, instructor}) {
            String token = jwt.generateAccessToken(user);
            request("GET", "/api/users", token, null, 403);
            status(student.getId(), "LOCKED", token, 403);
        }
    }

    @Test void lockRevokesEverySessionUnlockDoesNotRestoreThemAndOldAccessExpiresNormally() throws Exception {
        JsonNode one = login(student, 200).path("data"), two = login(student, 200).path("data");
        JsonNode other = login(instructor, 200).path("data");
        assertThat(status(student.getId(), "LOCKED", adminToken, 200).path("data").path("status").asText()).isEqualTo("LOCKED");
        status(student.getId(), "LOCKED", adminToken, 200);
        login(student, 403); refresh(one, 401); refresh(two, 401); refresh(other, 200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE user_id=? AND revoked_at IS NULL", Integer.class, student.getId())).isZero();
        request("GET", "/api/auth/me", one.path("accessToken").asText(), null, 200);
        status(student.getId(), "ACTIVE", adminToken, 200);
        refresh(one, 401); refresh(two, 401); login(student, 200);
    }

    @Test void cannotLockSelfOrAnotherAdminEvenWithMultipleRoles() throws Exception {
        status(admin.getId(), "LOCKED", adminToken, 422);
        status(otherAdmin.getId(), "LOCKED", adminToken, 422);
        assertThat(users.findById(otherAdmin.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":null}", "{\"status\":\"\"}", "{\"status\":\"PENDING\"}", "{\"status\":\"locked\"}", "{\"status\":\"DELETED\"}"})
    void rejectsInvalidStatusBody(String json) throws Exception {
        JsonNode error = request("PATCH", "/api/users/"+student.getId()+"/status", adminToken, mapper.readTree(json), 400);
        assertThat(error.path("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(error.path("fieldErrors").get(0).path("field").asText()).isEqualTo("status");
        assertThat(users.findById(student.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test void missingAndMalformedIdsAreNotServerErrors() throws Exception {
        status(Long.MAX_VALUE, "LOCKED", adminToken, 404);
        request("PATCH", "/api/users/abc/status", adminToken, Map.of("status", "LOCKED"), 400);
    }

    @Test void tokenRevocationFailureRollsBackStatusAndKeepsSession() throws Exception {
        JsonNode session = login(student, 200).path("data");
        doThrow(new IllegalStateException("Simulated revocation failure")).when(refreshTokens).revokeAllUserTokens(eq(student.getId()), any());
        status(student.getId(), "LOCKED", adminToken, 500);
        assertThat(users.findById(student.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        refresh(session, 200);
    }

    @Test void concurrentLoginAndRefreshWaitForAccountLockAndCannotIssueNewTokens() throws Exception {
        JsonNode session = login(student, 200).path("data");
        CountDownLatch revoking = new CountDownLatch(1), proceed = new CountDownLatch(1);
        doAnswer(invocation -> {
            revoking.countDown();
            if (!proceed.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
            // Spring Data query methods are abstract; delegate to the original repository proxy.
            return mockingDetails(invocation.getMock()).getMockCreationSettings().getDefaultAnswer().answer(invocation);
        }).when(refreshTokens).revokeAllUserTokens(eq(student.getId()), any());
        CompletableFuture<Void> locking = CompletableFuture.runAsync(() -> unchecked(() -> status(student.getId(), "LOCKED", adminToken, 200)));
        try {
            assertThat(revoking.await(5, TimeUnit.SECONDS)).isTrue();
            CompletableFuture<Void> loggingIn = CompletableFuture.runAsync(() -> unchecked(() -> login(student, 403)));
            CompletableFuture<Void> refreshing = CompletableFuture.runAsync(() -> unchecked(() -> refresh(session, 401)));
            assertThatThrownBy(() -> loggingIn.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            assertThatThrownBy(() -> refreshing.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            proceed.countDown();
            CompletableFuture.allOf(locking, loggingIn, refreshing).get(10, TimeUnit.SECONDS);
        } finally { proceed.countDown(); locking.get(10, TimeUnit.SECONDS); }
    }

    interface Checked { void run() throws Exception; }
    private void unchecked(Checked action) { try { action.run(); } catch (Exception e) { throw new RuntimeException(e); } }
    private JsonNode get(String query, int code) throws Exception { return request("GET", "/api/users"+query, adminToken, null, code); }
    private JsonNode status(Long id, String status, String token, int code) throws Exception { return request("PATCH", "/api/users/"+id+"/status", token, Map.of("status",status), code); }
    private JsonNode login(User user, int code) throws Exception { return request("POST", "/api/auth/login", null, Map.of("email",user.getEmail(),"password",PASSWORD),code); }
    private void refresh(JsonNode session, int code) throws Exception { request("POST", "/api/auth/refresh-token", null, Map.of("refreshToken",session.path("refreshToken").asText()),code); }
    private JsonNode request(String method, String path, String token, Object body, int code) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json");
        if (token != null) builder.header("Authorization","Bearer "+token);
        var response = client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("%s %s: %s",method,path,response.body()).isEqualTo(code);
        return mapper.readTree(response.body());
    }
}
