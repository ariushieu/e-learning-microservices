package com.hunre.authservice.controller;

import com.hunre.authservice.dto.*;
import com.hunre.authservice.security.JwtService;
import com.hunre.authservice.service.AuthService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "Đăng ký tài khoản thành công");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIp(httpRequest);
        return ApiResponse.ok(authService.login(request, userAgent, ipAddress), "Đăng nhập thành công");
    }

    @PostMapping("/refresh-token")
    public ApiResponse<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIp(httpRequest);
        return ApiResponse.ok(authService.refreshToken(request, userAgent, ipAddress), "Làm mới token thành công");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            authService.logout(request.getRefreshToken());
        }
        return ApiResponse.message("Đăng xuất thành công");
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Chưa cung cấp token hoặc định dạng Authorization không đúng");
        }

        String token = authHeader.substring(7).trim();
        if (!jwtService.validateToken(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn");
        }

        Long userId = jwtService.extractUserId(token);
        return ApiResponse.ok(authService.getUserById(userId));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
