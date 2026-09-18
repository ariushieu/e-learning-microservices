package com.hunre.authservice.security;

import com.hunre.authservice.domain.Role;
import com.hunre.authservice.domain.RoleCode;
import com.hunre.authservice.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("elearning-microservices-auth-service-jwt-secret-key-for-local-dev-only-32bytes");
        properties.setAccessTokenExpirationMs(60000); // 1 minute
        properties.setRefreshTokenExpirationMs(600000); // 10 minutes
        jwtService = new JwtService(properties);
    }

    @Test
    @DisplayName("Tạo access token và trích xuất đúng thông tin user")
    void generateAccessToken_and_extractClaims() {
        Role role = Role.builder().code(RoleCode.ROLE_STUDENT).name("Học viên").build();
        User user = User.builder()
                .id(100L)
                .email("student@hunre.edu.vn")
                .fullName("Nguyen Van A")
                .roles(Collections.singleton(role))
                .build();

        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(100L);
        assertThat(jwtService.extractEmail(token)).isEqualTo("student@hunre.edu.vn");

        List<String> roles = jwtService.extractRoles(token);
        assertThat(roles).containsExactly("ROLE_STUDENT");
    }

    @Test
    @DisplayName("Token không hợp lệ trả về false khi validate")
    void validateToken_invalidToken_returnsFalse() {
        assertThat(jwtService.validateToken("invalid.jwt.token")).isFalse();
        assertThat(jwtService.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("Băm SHA-256 trả về chuỗi hex 64 ký tự chuẩn xác và có tính tất định")
    void hashToken_returnsDeterministic64CharHex() {
        String rawToken = "my-secret-refresh-token-12345";
        String hash1 = jwtService.hashToken(rawToken);
        String hash2 = jwtService.hashToken(rawToken);

        assertThat(hash1).hasSize(64);
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).matches("^[a-f0-9]{64}$");
    }

    @Test
    @DisplayName("Sinh refresh token ngẫu nhiên và đủ độ dài an toàn")
    void generateRefreshToken_returnsSecureString() {
        String token1 = jwtService.generateRefreshToken();
        String token2 = jwtService.generateRefreshToken();

        assertThat(token1).isNotBlank();
        assertThat(token2).isNotBlank();
        assertThat(token1).isNotEqualTo(token2);
        assertThat(token1.length()).isGreaterThanOrEqualTo(64);
    }
}
