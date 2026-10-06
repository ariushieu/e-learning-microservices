package com.hunre.authservice.controller;

import com.hunre.authservice.dto.*;
import com.hunre.authservice.service.AuthService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
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

    /**
     * Trả về thông tin người dùng hiện tại dựa trên JWT đã được JwtAuthenticationFilter xác thực.
     * Filter của shared-common đã bóc tách và kiểm tra token trước khi request đến đây,
     * nên chỉ cần nhận AuthenticatedUser là đủ — không cần tự parse header nữa.
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(AuthenticatedUser user) {
        return ApiResponse.ok(authService.getUserById(user.userId()));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
