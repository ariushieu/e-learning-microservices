package com.hunre.authservice.service;

import com.hunre.authservice.domain.RefreshToken;
import com.hunre.authservice.dto.SessionResponse;
import com.hunre.authservice.repository.RefreshTokenRepository;
import com.hunre.authservice.repository.UserRepository;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final RefreshTokenRepository tokens;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<SessionResponse> list(Long userId, Long sessionId) {
        return tokens.findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDescIdDesc(userId, Instant.now())
                .stream().map(token -> new SessionResponse(token.getId(), token.getCreatedAt(), token.getSessionStartedAt(),
                        device(token.getUserAgent()), Objects.equals(token.getId(), sessionId))).toList();
    }

    @Transactional
    public void revoke(Long userId, Long sessionId) {
        lockUser(userId);
        RefreshToken token = tokens.findByIdAndUserId(sessionId, userId)
                .filter(SessionService::active)
                .orElseThrow(() -> new ResourceNotFoundException("phiên đăng nhập", "id", sessionId));
        token.setRevokedAt(Instant.now());
    }

    @Transactional
    public void revokeOthers(Long userId, Long sessionId) {
        lockUser(userId);
        // A missing/rotated/revoked sid must never accidentally revoke every session.
        if (sessionId == null || tokens.findByIdAndUserId(sessionId, userId).filter(SessionService::active).isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Phiên hiện tại không còn hợp lệ, vui lòng đăng nhập lại");
        }
        tokens.revokeOtherSessions(userId, sessionId, Instant.now());
    }

    private void lockUser(Long userId) {
        users.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Tài khoản không còn tồn tại"));
    }

    private static boolean active(RefreshToken token) {
        return token.getRevokedAt() == null && token.getExpiresAt().isAfter(Instant.now());
    }

    static String device(String value) {
        String ua = value == null ? "" : value.toLowerCase(Locale.ROOT);
        String browser;
        if (ua.contains("edg/") || ua.contains("edga/") || ua.contains("edgios/")) browser = "Edge";
        else if (ua.contains("opr/") || ua.contains("opios/")) browser = "Opera";
        else if (ua.contains("firefox/") || ua.contains("fxios/")) browser = "Firefox";
        else if (ua.contains("chrome/") || ua.contains("crios/")) browser = "Chrome";
        else if (ua.contains("safari/") && ua.contains("version/")) browser = "Safari";
        else return "Thiết bị khác";

        String os;
        if (ua.contains("android")) os = "Android";
        else if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) os = "iOS";
        else if (ua.contains("windows")) os = "Windows";
        else if (ua.contains("macintosh") || ua.contains("mac os x")) os = "macOS";
        else if (ua.contains("linux")) os = "Linux";
        else return "Thiết bị khác";
        return browser + " trên " + os;
    }
}
