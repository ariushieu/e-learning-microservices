package com.hunre.courseservice.security;

import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.JwtAuthenticationFilter;
import com.hunre.sharedcommon.security.JwtVerifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Cung cấp danh tính người dùng hiện tại (nếu có).
 *
 * <p>Xử lý cả 2 tình huống:
 * <ul>
 *   <li>Endpoint được bảo vệ: {@link AuthenticatedUser} đã được filter đặt vào request attribute.</li>
 *   <li>Endpoint công khai (GET): filter bỏ qua kiểm tra bắt buộc, nhưng nếu client gửi header
 *       {@code Authorization: Bearer <token>} (ví dụ: giảng viên chủ khóa học hoặc admin xem khóa DRAFT),
 *       lớp này sẽ giải mã token để xác định danh tính.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final ObjectProvider<JwtVerifier> jwtVerifierProvider;

    public Optional<AuthenticatedUser> getCurrentUser() {
        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request == null) {
            return Optional.empty();
        }

        // 1. Kiểm tra attribute do JwtAuthenticationFilter hoặc DevIdentityFilter đặt
        Object userAttr = request.getAttribute(JwtAuthenticationFilter.USER_ATTRIBUTE);
        if (userAttr instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }

        // 2. Với các đường dẫn công khai (GET), filter không bắt buộc token nhưng request có thể vẫn kèm token
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            JwtVerifier verifier = jwtVerifierProvider.getIfAvailable();
            if (verifier != null && !token.isBlank()) {
                try {
                    return Optional.of(verifier.verify(token));
                } catch (Exception ignored) {
                    // Token không hợp lệ hoặc đã hết hạn -> coi như khách chưa đăng nhập
                }
            }
        }

        return Optional.empty();
    }
}
