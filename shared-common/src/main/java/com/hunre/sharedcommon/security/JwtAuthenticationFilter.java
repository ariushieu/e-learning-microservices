package com.hunre.sharedcommon.security;

import com.hunre.sharedcommon.dto.ErrorResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.server.PathContainer.parsePath;

/**
 * Chặn mọi request, kiểm token và gắn danh tính vào request để controller dùng lại.
 *
 * <p>Filter chạy <b>trước</b> DispatcherServlet, nên {@code GlobalExceptionHandler} không
 * bắt được lỗi ném ra từ đây. Vì vậy lớp này tự dựng response lỗi, nhưng dùng đúng
 * {@link ErrorResponse} như phần còn lại của hệ thống — frontend không phải xử lý thêm một
 * hình dạng lỗi thứ hai chỉ vì nó đến từ filter.
 *
 * <p>Request đi qua được sẽ có {@link AuthenticatedUser} nằm trong attribute, lấy ra bằng
 * cách khai {@code AuthenticatedUser user} làm tham số controller (xem
 * {@link AuthenticatedUserArgumentResolver}).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /** Tên attribute chứa người dùng đã xác thực. */
    public static final String USER_ATTRIBUTE = AuthenticatedUser.class.getName();

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtVerifier jwtVerifier;
    private final PublicPaths publicPaths;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public JwtAuthenticationFilter(JwtVerifier jwtVerifier, List<String> publicPaths) {
        this.jwtVerifier = jwtVerifier;
        this.publicPaths = new PublicPaths(publicPaths);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Preflight của CORS không mang được header Authorization, chặn nó là chặn luôn
        // mọi lời gọi từ trình duyệt khác origin.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        return publicPaths.matches(request.getMethod(), parsePath(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            reject(request, response, "Chưa đăng nhập hoặc thiếu header Authorization");
            return;
        }

        AuthenticatedUser user;
        try {
            user = jwtVerifier.verify(header.substring(BEARER_PREFIX.length()).trim());
        } catch (BusinessException ex) {
            reject(request, response, ex.getMessage());
            return;
        }

        request.setAttribute(USER_ATTRIBUTE, user);
        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {

        log.debug("Từ chối {} {}: {}", request.getMethod(), request.getRequestURI(), message);

        response.setStatus(ErrorCode.UNAUTHORIZED.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ErrorResponse.of(ErrorCode.UNAUTHORIZED.name(), message, request.getRequestURI()));
    }
}
