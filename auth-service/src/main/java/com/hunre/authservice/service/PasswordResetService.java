package com.hunre.authservice.service;

import com.hunre.authservice.domain.User;
import com.hunre.authservice.domain.UserStatus;
import com.hunre.authservice.domain.VerificationToken;
import com.hunre.authservice.domain.VerificationTokenType;
import com.hunre.authservice.outbox.AccountEventPublisher;
import com.hunre.authservice.repository.RefreshTokenRepository;
import com.hunre.authservice.repository.UserRepository;
import com.hunre.authservice.repository.VerificationTokenRepository;
import com.hunre.authservice.security.JwtService;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Quên mật khẩu: phát mã một lần qua email, rồi đổi mật khẩu bằng mã đó.
 *
 * <p>Chỉ lưu bản băm SHA-256 của mã trong {@code verification_tokens}; mã thô chỉ đi trong sự kiện
 * cho notification-service gửi thư. Mã sống {@link #TOKEN_TTL}, dùng một lần, và mã mới làm mã cũ
 * hết hiệu lực.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    /** Bấm "gửi lại" liên tục thì chỉ gửi một thư mỗi phút. */
    static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    static final String INVALID_LINK = "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Hãy yêu cầu liên kết mới.";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountEventPublisher accountEvents;

    /**
     * Luôn kết thúc bình thường, dù email có tồn tại hay không: người gọi trả cùng một câu trả lời
     * để không ai dùng chức năng này dò xem email nào đã đăng ký.
     */
    @Transactional
    public void requestReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        Optional<User> found = userRepository.findByEmailForUpdate(email);
        if (found.isEmpty() || found.get().getStatus() != UserStatus.ACTIVE) {
            log.info("Bỏ qua yêu cầu đặt lại mật khẩu: không có tài khoản đang hoạt động cho email này");
            return;
        }
        User user = found.get();
        Instant now = Instant.now();
        List<VerificationToken> open = tokenRepository.findByUserIdAndTypeAndUsedAtIsNull(
                user.getId(), VerificationTokenType.PASSWORD_RESET);
        if (open.stream().anyMatch(t -> t.getCreatedAt().isAfter(now.minus(RESEND_COOLDOWN)))) {
            log.info("Người dùng {} vừa yêu cầu đặt lại mật khẩu, chưa gửi thư mới", user.getId());
            return;
        }
        // Chỉ link mới nhất dùng được.
        open.forEach(t -> t.setUsedAt(now));

        String raw = newToken();
        Instant expiresAt = now.plus(TOKEN_TTL);
        tokenRepository.save(VerificationToken.builder()
                .user(user)
                .type(VerificationTokenType.PASSWORD_RESET)
                .tokenHash(jwtService.hashToken(raw))
                .expiresAt(expiresAt)
                .createdAt(now)
                .build());
        accountEvents.passwordResetRequested(user, raw, expiresAt);
        log.info("Đã tạo mã đặt lại mật khẩu cho người dùng {}", user.getId());
    }

    /** Đổi mật khẩu bằng mã trong email, rồi thu hồi mọi phiên đăng nhập như khi đổi mật khẩu. */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        VerificationToken token = tokenRepository.findForUpdate(jwtService.hashToken(rawToken.trim()),
                        VerificationTokenType.PASSWORD_RESET)
                .filter(VerificationToken::isValid)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, INVALID_LINK));
        User user = token.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, INVALID_LINK);
        }
        Instant now = Instant.now();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        token.setUsedAt(now);
        refreshTokenRepository.revokeAllUserTokens(user.getId(), now);
        log.info("Người dùng {} đã đặt lại mật khẩu bằng email", user.getId());
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
