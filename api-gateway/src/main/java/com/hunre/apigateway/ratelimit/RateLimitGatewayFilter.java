package com.hunre.apigateway.ratelimit;

import com.hunre.apigateway.error.ErrorResponseWriter;
import com.hunre.apigateway.security.JwtAuthenticationGatewayFilter;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Set;

/**
 * Giới hạn số request mỗi người được gửi qua gateway, đếm bằng Redis.
 *
 * <p><b>Hai chính sách.</b>
 * <ul>
 *   <li>Đăng nhập, đăng ký, làm mới token: chặt, tính theo địa chỉ IP. Đây là chỗ bị dò
 *       mật khẩu — kẻ dò chưa có token nên chỉ phân biệt được bằng IP.</li>
 *   <li>Mọi đường dẫn còn lại: rộng, đủ cho người dùng thật bấm nhanh, tính theo id người
 *       dùng nếu đã đăng nhập. Tính theo IP thì cả lớp học chung một mạng wifi sẽ dùng chung
 *       một xô và chặn lẫn nhau.</li>
 * </ul>
 *
 * <p><b>Vì sao đếm trong Redis chứ không đếm trong bộ nhớ gateway.</b> Chạy hai gateway sau
 * một load balancer thì mỗi bản đếm riêng, người dùng được gấp đôi giới hạn. Redis là chỗ
 * đếm chung, và {@code RedisRateLimiter} của Spring Cloud Gateway trừ token bằng một script
 * Lua chạy nguyên khối trong Redis, nên hai request cùng lúc không đọc trùng một số dư.
 *
 * <p><b>Redis chết thì cho qua hết.</b> {@code RedisRateLimiter} trả "được phép" khi không
 * gọi được Redis. Mất giới hạn request một lúc còn hơn cả hệ thống ngừng phục vụ chỉ vì
 * thành phần phụ này hỏng. Header {@code X-RateLimit-Remaining} lúc đó bằng -1 — nhìn vào
 * là biết Redis đang có vấn đề.
 */
public class RateLimitGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitGatewayFilter.class);

    static final String LOGIN_POLICY = "rate-limit-login";
    static final String API_POLICY = "rate-limit-api";

    private static final Set<String> LOGIN_PATHS =
            Set.of("/api/auth/login", "/api/auth/register", "/api/auth/refresh-token");

    private final RateLimiter<?> rateLimiter;
    private final RateLimitProperties properties;

    public RateLimitGatewayFilter(RateLimiter<?> rateLimiter, RateLimitProperties properties) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    @Override
    public int getOrder() {
        // Ngay sau JwtAuthenticationGatewayFilter, để đã biết request này của ai. Request
        // không có token hợp lệ thì bị chặn ở đó luôn, không tốn một lượt gọi Redis.
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().pathWithinApplication().value();

        // Healthcheck của Docker gọi /actuator mỗi 10 giây; preflight CORS do trình duyệt tự
        // gửi. Đếm hai loại này thì người dùng bị trừ token vì việc họ không làm.
        if (!properties.enabled()
                || HttpMethod.OPTIONS.equals(request.getMethod())
                || path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        boolean login = HttpMethod.POST.equals(request.getMethod()) && LOGIN_PATHS.contains(path);
        String policyId = login ? LOGIN_POLICY : API_POLICY;
        RateLimitProperties.Policy policy = login ? properties.login() : properties.api();
        String key = login ? clientIp(request) : userOrClientIp(exchange);

        return rateLimiter.isAllowed(policyId, key).flatMap(result -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            result.getHeaders().forEach(headers::set);

            if (result.isAllowed()) {
                return chain.filter(exchange);
            }

            log.info("Chặn {} {} của {}: vượt giới hạn {}", request.getMethod(), path, key, policyId);
            headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(policy.retryAfterSeconds()));
            return ErrorResponseWriter.write(exchange, ErrorCode.TOO_MANY_REQUESTS.httpStatus(),
                    ErrorCode.TOO_MANY_REQUESTS.name(),
                    "Bạn gửi quá nhiều yêu cầu. Vui lòng thử lại sau %d giây."
                            .formatted(policy.retryAfterSeconds()));
        });
    }

    private String userOrClientIp(ServerWebExchange exchange) {
        AuthenticatedUser user =
                exchange.getAttribute(JwtAuthenticationGatewayFilter.AUTHENTICATED_USER_ATTRIBUTE);
        return user != null ? "user:" + user.userId() : clientIp(exchange.getRequest());
    }

    /**
     * Lấy IP từ kết nối TCP, cố ý không đọc header {@code X-Forwarded-For}: header đó client
     * tự ghi được, kẻ dò mật khẩu chỉ cần đổi nó mỗi lần gửi là có xô mới.
     */
    private String clientIp(ServerHttpRequest request) {
        InetSocketAddress remote = request.getRemoteAddress();
        if (remote == null || remote.getAddress() == null) {
            return "ip:unknown";
        }
        return "ip:" + remote.getAddress().getHostAddress();
    }
}
