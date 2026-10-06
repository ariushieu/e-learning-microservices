package com.hunre.authservice.service;

import com.hunre.authservice.domain.RoleCode;
import com.hunre.authservice.dto.AuthResponse;
import com.hunre.authservice.dto.LoginRequest;
import com.hunre.authservice.dto.RefreshTokenRequest;
import com.hunre.authservice.dto.RegisterRequest;
import com.hunre.authservice.dto.UserResponse;

import java.util.Set;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request, String userAgent, String ipAddress);
    AuthResponse refreshToken(RefreshTokenRequest request, String userAgent, String ipAddress);
    void logout(String refreshToken);
    UserResponse getUserById(Long userId);
    UserResponse updateUserRoles(Long userId, Set<RoleCode> roles);
}
