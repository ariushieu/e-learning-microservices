package com.hunre.authservice.service;

import com.hunre.authservice.domain.*;
import com.hunre.authservice.dto.*;
import com.hunre.authservice.repository.RefreshTokenRepository;
import com.hunre.authservice.repository.RoleRepository;
import com.hunre.authservice.repository.UserRepository;
import com.hunre.authservice.security.JwtService;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.DuplicateResourceException;
import com.hunre.sharedcommon.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role studentRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        studentRole = Role.builder()
                .id(1L)
                .code(RoleCode.ROLE_STUDENT)
                .name("Học viên")
                .build();

        testUser = User.builder()
                .id(1L)
                .email("test@hunre.edu.vn")
                .passwordHash("$2a$10$encryptedPassword")
                .fullName("Nguyen Van Test")
                .status(UserStatus.ACTIVE)
                .roles(Collections.singleton(studentRole))
                .build();
    }

    @Test
    @DisplayName("Đăng ký thành công tài khoản mới với vai trò ROLE_STUDENT")
    void register_success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@hunre.edu.vn")
                .password("password123")
                .fullName("Nguyen Van Test")
                .build();

        when(userRepository.existsByEmail("test@hunre.edu.vn")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encryptedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@hunre.edu.vn");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van Test");
        assertThat(response.getRoles()).contains("ROLE_STUDENT");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng ký ném DuplicateResourceException khi email đã tồn tại")
    void register_duplicateEmail_throwsException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@hunre.edu.vn")
                .password("password123")
                .fullName("Nguyen Van Test")
                .build();

        when(userRepository.existsByEmail("test@hunre.edu.vn")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng nhập thành công trả về access token và refresh token")
    void login_success() {
        LoginRequest request = LoginRequest.builder()
                .email("test@hunre.edu.vn")
                .password("password123")
                .build();

        when(userRepository.findByEmail("test@hunre.edu.vn")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("valid.access.token");
        when(jwtService.generateRefreshToken()).thenReturn("raw-refresh-token-123");
        when(jwtService.hashToken("raw-refresh-token-123")).thenReturn("hash-123");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.login(request, "Mozilla/5.0", "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("valid.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("raw-refresh-token-123");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getEmail()).isEqualTo("test@hunre.edu.vn");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Đăng nhập sai mật khẩu ném BusinessException UNAUTHORIZED")
    void login_wrongPassword_throwsUnauthorized() {
        LoginRequest request = LoginRequest.builder()
                .email("test@hunre.edu.vn")
                .password("wrongpassword")
                .build();

        when(userRepository.findByEmail("test@hunre.edu.vn")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, null, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("Đăng nhập tài khoản bị khóa ném BusinessException FORBIDDEN")
    void login_lockedUser_throwsForbidden() {
        testUser.setStatus(UserStatus.LOCKED);
        LoginRequest request = LoginRequest.builder()
                .email("test@hunre.edu.vn")
                .password("password123")
                .build();

        when(userRepository.findByEmail("test@hunre.edu.vn")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request, null, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("Làm mới token thành công với refresh token hợp lệ")
    void refreshToken_success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-raw-refresh-token")
                .build();

        RefreshToken existingToken = RefreshToken.builder()
                .id(10L)
                .user(testUser)
                .tokenHash("hash-abc")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(jwtService.hashToken("valid-raw-refresh-token")).thenReturn("hash-abc");
        when(refreshTokenRepository.findByTokenHash("hash-abc")).thenReturn(Optional.of(existingToken));
        when(jwtService.generateAccessToken(testUser)).thenReturn("new.access.token");
        when(jwtService.generateRefreshToken()).thenReturn("new-raw-refresh-token");
        when(jwtService.hashToken("new-raw-refresh-token")).thenReturn("new-hash-xyz");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.refreshToken(request, "agent", "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("new-raw-refresh-token");
        assertThat(existingToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Đăng xuất thu hồi token thành công")
    void logout_success() {
        RefreshToken token = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .tokenHash("hash-logout")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(jwtService.hashToken("token-to-logout")).thenReturn("hash-logout");
        when(refreshTokenRepository.findByTokenHash("hash-logout")).thenReturn(Optional.of(token));

        authService.logout("token-to-logout");

        assertThat(token.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    @DisplayName("updateUserRoles - Admin cấp ROLE_INSTRUCTOR cho user thành công")
    void updateUserRoles_success() {
        Role instructorRole = Role.builder()
                .id(2L)
                .code(RoleCode.ROLE_INSTRUCTOR)
                .name("Giảng viên")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByCode(RoleCode.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByCode(RoleCode.ROLE_INSTRUCTOR)).thenReturn(Optional.of(instructorRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        Set<RoleCode> newRoles = Set.of(RoleCode.ROLE_STUDENT, RoleCode.ROLE_INSTRUCTOR);
        UserResponse response = authService.updateUserRoles(1L, newRoles);

        assertThat(response).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateUserRoles - User không tồn tại ném ResourceNotFoundException")
    void updateUserRoles_userNotFound_throwsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateUserRoles(999L, Set.of(RoleCode.ROLE_STUDENT)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
