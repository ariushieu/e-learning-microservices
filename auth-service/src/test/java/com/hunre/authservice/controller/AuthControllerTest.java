package com.hunre.authservice.controller;

import com.hunre.authservice.domain.UserStatus;
import com.hunre.authservice.dto.*;
import com.hunre.authservice.security.JwtService;
import com.hunre.authservice.service.AuthService;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.DuplicateResourceException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register thành công trả về 201 CREATED bọc trong ApiResponse")
    void register_success() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("student@hunre.edu.vn")
                .fullName("Nguyen Van A")
                .status(UserStatus.ACTIVE)
                .roles(Collections.singletonList("ROLE_STUDENT"))
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(userResponse);

        String jsonRequest = """
                {
                    "email": "student@hunre.edu.vn",
                    "password": "password123",
                    "fullName": "Nguyen Van A"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng ký tài khoản thành công"))
                .andExpect(jsonPath("$.data.email").value("student@hunre.edu.vn"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_STUDENT"));
    }

    @Test
    @DisplayName("POST /api/auth/register dữ liệu không hợp lệ trả về 400 VALIDATION_FAILED")
    void register_invalidData_returns400() throws Exception {
        String invalidJson = """
                {
                    "email": "invalid-email",
                    "password": "",
                    "fullName": ""
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("POST /api/auth/register trùng email trả về 409 DUPLICATE_RESOURCE")
    void register_duplicateEmail_returns409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("người dùng", "email", "test@hunre.edu.vn"));

        String jsonRequest = """
                {
                    "email": "test@hunre.edu.vn",
                    "password": "password123",
                    "fullName": "Nguyen Van A"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("POST /api/auth/login thành công trả về 200 OK với AuthResponse")
    void login_success() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock.jwt.token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .expiresIn(900)
                .user(UserResponse.builder().id(1L).email("student@hunre.edu.vn").build())
                .build();

        when(authService.login(any(LoginRequest.class), any(), any())).thenReturn(authResponse);

        String jsonRequest = """
                {
                    "email": "student@hunre.edu.vn",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng nhập thành công"))
                .andExpect(jsonPath("$.data.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.data.refreshToken").value("mock-refresh-token"));
    }

    @Test
    @DisplayName("POST /api/auth/login sai mật khẩu trả về 401 UNAUTHORIZED")
    void login_wrongCredentials_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác"));

        String jsonRequest = """
                {
                    "email": "student@hunre.edu.vn",
                    "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không chính xác"));
    }

    @Test
    @DisplayName("GET /api/auth/me không có Authorization header trả về 401 UNAUTHORIZED")
    void getMe_missingHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /api/auth/me với token hợp lệ trả về thông tin user")
    void getMe_validToken_returnsUser() throws Exception {
        when(jwtService.validateToken("valid.token")).thenReturn(true);
        when(jwtService.extractUserId("valid.token")).thenReturn(10L);

        UserResponse userResponse = UserResponse.builder()
                .id(10L)
                .email("student@hunre.edu.vn")
                .fullName("Nguyen Van A")
                .build();
        when(authService.getUserById(10L)).thenReturn(userResponse);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.email").value("student@hunre.edu.vn"));
    }
}
