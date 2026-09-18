package com.hunre.sharedcommon.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Gắn sẵn một danh tính giả lập khi xác thực bị tắt, để controller vẫn chạy được.
 *
 * <p>Chỉ được đăng ký khi {@code elearning.security.enabled=false}. Khi xác thực bật thì
 * lớp này hoàn toàn không có mặt trong ứng dụng — đó là lý do nó tách khỏi
 * {@link JwtAuthenticationFilter} thay vì thêm một nhánh {@code if} vào trong đó: không
 * tồn tại thì không có đường nào chạy nhầm vào.
 */
public class DevIdentityFilter extends OncePerRequestFilter {

    private final AuthenticatedUser devUser;

    public DevIdentityFilter(JwtSecurityProperties.DevUser config) {
        this.devUser = new AuthenticatedUser(
                config.getId(),
                config.getEmail(),
                config.getFullName(),
                Set.copyOf(config.getRoles()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (request.getAttribute(JwtAuthenticationFilter.USER_ATTRIBUTE) == null) {
            request.setAttribute(JwtAuthenticationFilter.USER_ATTRIBUTE, devUser);
        }
        filterChain.doFilter(request, response);
    }
}
