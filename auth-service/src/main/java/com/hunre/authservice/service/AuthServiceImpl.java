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
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("người dùng", "email", email);
        }

        Role studentRole = roleRepository.findByCode(RoleCode.ROLE_STUDENT)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .code(RoleCode.ROLE_STUDENT)
                        .name("Học viên")
                        .description("Ghi danh khóa học, học bài, làm bài kiểm tra")
                        .build()));

        Set<Role> roles = new HashSet<>();
        roles.add(studentRole);

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with id: {} and email: {}", savedUser.getId(), savedUser.getEmail());
        return UserResponse.from(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String userAgent, String ipAddress) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác");
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Tài khoản của bạn đã bị khóa");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRefreshToken();
        String tokenHash = jwtService.hashToken(rawRefreshToken);

        Instant expiresAt = Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs());
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();
        refreshTokenRepository.save(refreshToken);

        log.info("User {} logged in successfully", user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .user(UserResponse.from(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String userAgent, String ipAddress) {
        String rawRefreshToken = request.getRefreshToken().trim();
        String tokenHash = jwtService.hashToken(rawRefreshToken);

        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã bị thu hồi"));

        if (!currentToken.isValid()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token đã hết hạn hoặc đã bị thu hồi");
        }

        User user = currentToken.getUser();
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Tài khoản của bạn đã bị khóa");
        }

        // Thu hồi token cũ (rotate)
        currentToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(currentToken);

        // Sinh cặp token mới
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRawRefreshToken = jwtService.generateRefreshToken();
        String newTokenHash = jwtService.hashToken(newRawRefreshToken);

        Instant expiresAt = Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs());
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .expiresAt(expiresAt)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        log.info("Refreshed token for user {}", user.getId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .user(UserResponse.from(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String tokenHash = jwtService.hashToken(refreshToken.trim());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
                log.info("Revoked refresh token for user {}", token.getUser().getId());
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("người dùng", "id", userId));
        return UserResponse.from(user);
    }
}
