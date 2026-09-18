package com.hunre.sharedcommon.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm cả chuỗi: filter chặn hay cho qua, và controller có nhận được đúng danh tính không.
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "elearning-microservices-auth-service-jwt-secret-key-for-local-dev-only-32bytes";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                new JwtVerifier(SECRET),
                List.of("/api/auth/login", "GET:/api/courses/**"));

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
                .addFilters(filter)
                .build();
    }

    private String token(List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("7")
                .claim("email", "sv@hunre.edu.vn")
                .claim("fullName", "Trần Thị B")
                .claim("roles", roles)
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("thiếu token trả 401 đúng hình dạng ErrorResponse của hệ thống")
    void thieuToken() throws Exception {
        mockMvc.perform(get("/api/quizzes/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/quizzes/1"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("token hợp lệ đi qua và controller nhận đúng người dùng từ token")
    void tokenHopLe() throws Exception {
        mockMvc.perform(get("/api/quizzes/1").header("Authorization", "Bearer " + token(List.of(Roles.STUDENT))))
                .andExpect(status().isOk())
                .andExpect(content().string("7:sv@hunre.edu.vn:true"));
    }

    @Test
    @DisplayName("header sai định dạng (thiếu chữ Bearer) bị từ chối")
    void saiDinhDangHeader() throws Exception {
        mockMvc.perform(get("/api/quizzes/1").header("Authorization", token(List.of(Roles.STUDENT))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("đường dẫn công khai đi qua không cần token")
    void duongDanCongKhai() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET công khai nhưng DELETE cùng đường dẫn vẫn phải có token")
    void congKhaiTheoPhuongThuc() throws Exception {
        mockMvc.perform(get("/api/courses/9"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/courses/9"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("preflight OPTIONS không bị chặn, nếu không trình duyệt không gọi được")
    void khongChanPreflight() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .options("/api/quizzes/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("thông báo 401 không lộ chi tiết kỹ thuật của thư viện JWT")
    void khongLoChiTiet() throws Exception {
        mockMvc.perform(get("/api/quizzes/1").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("io.jsonwebtoken"))));
    }

    // ---------------------------------------------------------------------

    @RestController
    static class TestController {

        @GetMapping("/api/quizzes/1")
        String cannDangNhap(AuthenticatedUser user) {
            return user.userId() + ":" + user.email() + ":" + user.hasRole(Roles.STUDENT);
        }

        @PostMapping("/api/auth/login")
        String dangNhap() {
            return "ok";
        }

        @GetMapping("/api/courses/9")
        String xemKhoaHoc() {
            return "ok";
        }

        @DeleteMapping("/api/courses/9")
        String xoaKhoaHoc() {
            return "ok";
        }
    }
}
