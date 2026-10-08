package com.hunre.authservice.controller;

import com.hunre.authservice.dto.SessionResponse;
import com.hunre.authservice.security.JwtService;
import com.hunre.authservice.service.SessionService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessions;
    private final JwtService jwt;

    @GetMapping
    public ApiResponse<List<SessionResponse>> list(AuthenticatedUser user,
            @RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(sessions.list(user.userId(), sessionId(authorization, user)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> revoke(AuthenticatedUser user, @PathVariable Long id) {
        sessions.revoke(user.userId(), id);
        return ApiResponse.message("Đã đăng xuất phiên đăng nhập");
    }

    @PostMapping("/revoke-others")
    public ApiResponse<Void> revokeOthers(AuthenticatedUser user,
            @RequestHeader("Authorization") String authorization) {
        sessions.revokeOthers(user.userId(), sessionId(authorization, user));
        return ApiResponse.message("Đã đăng xuất mọi thiết bị khác");
    }

    // Session identity is local to auth-service; do not change the shared user contract.
    private Long sessionId(String authorization, AuthenticatedUser user) {
        try {
            if (!authorization.startsWith("Bearer ")) throw new IllegalArgumentException();
            Claims claims = jwt.parseToken(authorization.substring(7).trim());
            if (!String.valueOf(user.userId()).equals(claims.getSubject())) throw new IllegalArgumentException();
            Object sid = claims.get("sid");
            if (sid == null) return null; // Pre-deployment access tokens remain usable.
            if (!(sid instanceof Number) || !sid.toString().matches("[1-9][0-9]*")) throw new IllegalArgumentException();
            return Long.valueOf(sid.toString());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token không hợp lệ");
        }
    }
}
