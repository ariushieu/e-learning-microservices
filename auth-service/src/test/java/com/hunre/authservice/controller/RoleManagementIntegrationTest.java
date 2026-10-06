package com.hunre.authservice.controller;

import com.hunre.authservice.domain.Role;
import com.hunre.authservice.domain.RoleCode;
import com.hunre.authservice.repository.RoleRepository;
import com.hunre.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:role_management;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "elearning.security.enabled=true",
        "elearning.security.jwt-secret=${jwt.secret}"
})
class RoleManagementIntegrationTest {

    @LocalServerPort
    private int port;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void seedAdmin() {
        jdbc.update("DELETE FROM refresh_tokens");
        jdbc.update("DELETE FROM user_roles");
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM roles");
        for (RoleCode code : RoleCode.values()) {
            roleRepository.save(Role.builder().code(code).name(code.name()).build());
        }
        // This audit column belongs to the SQL join table, not the JPA mapping.
        jdbc.execute("ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V3__seed_admin.sql"))
                .execute(dataSource);
    }

    @Test
    void adminCanGrantAndRemoveRolesAndNewTokensReflectDatabase() throws Exception {
        JsonNode admin = login("admin@elearning.hunre.edu.vn", "Admin@123456");
        String adminToken = admin.path("accessToken").asText();
        assertThat(jwtService.extractRoles(adminToken)).containsExactly("ROLE_ADMIN");

        JsonNode student = request("POST", "/api/auth/register", null, Map.of(
                "email", "role-test@example.com", "password", "Student@123456", "fullName", "Role Test"), 201);
        long id = student.path("id").asLong();
        String rolesPath = "/api/users/" + id + "/roles";
        JsonNode originalLogin = login("role-test@example.com", "Student@123456");
        String studentToken = originalLogin.path("accessToken").asText();
        assertThat(jwtService.extractRoles(studentToken)).containsExactly("ROLE_STUDENT");

        request("PATCH", rolesPath, null, Map.of("roles", new String[]{"ROLE_ADMIN"}), 401);
        request("PATCH", rolesPath, "invalid-token", Map.of("roles", new String[]{"ROLE_ADMIN"}), 401);
        request("PATCH", rolesPath, studentToken, Map.of("roles", new String[]{"ROLE_ADMIN"}), 403);
        request("GET", "/api/auth/me", null, null, 401);

        request("PATCH", rolesPath, adminToken,
                Map.of("roles", new String[]{"ROLE_STUDENT", "ROLE_INSTRUCTOR"}), 200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_roles WHERE user_id = ?", Integer.class, id))
                .isEqualTo(2);
        JsonNode refreshed = request("POST", "/api/auth/refresh-token", null,
                Map.of("refreshToken", originalLogin.path("refreshToken").asText()), 200);
        assertThat(jwtService.extractRoles(refreshed.path("accessToken").asText()))
                .containsExactlyInAnyOrder("ROLE_STUDENT", "ROLE_INSTRUCTOR");
        String instructorToken = login("role-test@example.com", "Student@123456").path("accessToken").asText();
        assertThat(jwtService.extractRoles(instructorToken))
                .containsExactlyInAnyOrder("ROLE_STUDENT", "ROLE_INSTRUCTOR");
        request("PATCH", rolesPath, instructorToken, Map.of("roles", new String[]{"ROLE_ADMIN"}), 403);

        // /me uses the verified caller's identity even if a different userId is supplied.
        JsonNode me = request("GET", "/api/auth/me?userId=" + admin.path("user").path("id").asLong(),
                instructorToken, null, 200);
        assertThat(me.path("id").asLong()).isEqualTo(id);

        request("PATCH", rolesPath, adminToken, Map.of("roles", new String[]{"ROLE_STUDENT"}), 200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_roles WHERE user_id = ?", Integer.class, id))
                .isEqualTo(1);
        JsonNode afterRemoval = request("POST", "/api/auth/refresh-token", null,
                Map.of("refreshToken", refreshed.path("refreshToken").asText()), 200);
        assertThat(jwtService.extractRoles(afterRemoval.path("accessToken").asText()))
                .containsExactly("ROLE_STUDENT");

        request("PATCH", "/api/users/" + admin.path("user").path("id").asLong() + "/roles",
                adminToken, Map.of("roles", new String[]{"ROLE_STUDENT"}), 422);
        request("PATCH", "/api/users/9223372036854775807/roles", adminToken,
                Map.of("roles", new String[]{"ROLE_STUDENT"}), 404);
    }

    private JsonNode login(String email, String password) throws Exception {
        return request("POST", "/api/auth/login", null, Map.of("email", email, "password", password), 200);
    }

    private JsonNode request(String method, String path, String token, Object body, int expectedStatus)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("%s %s: %s", method, path, response.body()).isEqualTo(expectedStatus);
        return objectMapper.readTree(response.body()).path("data");
    }
}
