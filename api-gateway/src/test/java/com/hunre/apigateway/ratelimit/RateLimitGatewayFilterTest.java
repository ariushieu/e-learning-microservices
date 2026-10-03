package com.hunre.apigateway.ratelimit;

import com.hunre.apigateway.security.JwtAuthenticationGatewayFilter;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Chọn đúng chính sách, đúng khóa đếm, và trả 429 đúng hình dạng khi vượt giới hạn.
 * Phần đếm thật trong Redis là của Spring Cloud Gateway, ở đây giả lập bằng mock.
 */
class RateLimitGatewayFilterTest {

    private static final RateLimitProperties PROPERTIES = new RateLimitProperties(true,
            new RateLimitProperties.Policy(1, 60, 6),
            new RateLimitProperties.Policy(20, 40, 1));

    /** Lời gọi tới limiter: chính sách nào, khóa nào. */
    private final List<String> calls = new ArrayList<>();
    private final AtomicBoolean forwarded = new AtomicBoolean();
    private final GatewayFilterChain chain = exchange -> {
        forwarded.set(true);
        return Mono.empty();
    };

    @Test
    @DisplayName("đăng nhập dùng chính sách chặt, đếm theo IP")
    void dangNhapDemTheoIp() {
        RateLimitGatewayFilter filter = filter(true);

        filter.filter(exchange(MockServerHttpRequest.post("/api/auth/login")), chain).block();

        assertThat(calls).containsExactly(RateLimitGatewayFilter.LOGIN_POLICY + " ip:10.0.0.5");
        assertThat(forwarded).isTrue();
    }

    @Test
    @DisplayName("đã đăng nhập thì đếm theo người dùng, không theo IP")
    void daDangNhapDemTheoNguoiDung() {
        RateLimitGatewayFilter filter = filter(true);
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/enrollments"));
        exchange.getAttributes().put(JwtAuthenticationGatewayFilter.AUTHENTICATED_USER_ATTRIBUTE,
                new AuthenticatedUser(42L, "sv@hunre.edu.vn", "Sinh viên", Set.of("ROLE_STUDENT")));

        filter.filter(exchange, chain).block();

        assertThat(calls).containsExactly(RateLimitGatewayFilter.API_POLICY + " user:42");
    }

    @Test
    @DisplayName("khách chưa đăng nhập xem khóa học thì đếm theo IP, chính sách thường")
    void khachDemTheoIp() {
        RateLimitGatewayFilter filter = filter(true);

        filter.filter(exchange(MockServerHttpRequest.get("/api/courses")), chain).block();

        assertThat(calls).containsExactly(RateLimitGatewayFilter.API_POLICY + " ip:10.0.0.5");
    }

    @Test
    @DisplayName("GET /api/auth/me không phải đăng nhập — không dùng chính sách chặt")
    void chiPostDangNhapMoiChat() {
        RateLimitGatewayFilter filter = filter(true);

        filter.filter(exchange(MockServerHttpRequest.get("/api/auth/me")), chain).block();

        assertThat(calls).containsExactly(RateLimitGatewayFilter.API_POLICY + " ip:10.0.0.5");
    }

    @Test
    @DisplayName("vượt giới hạn thì trả 429 kèm Retry-After, không chuyển request đi tiếp")
    void vuotGioiHanTra429() {
        RateLimitGatewayFilter filter = filter(false);
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.post("/api/auth/login"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("6");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"code\":\"TOO_MANY_REQUESTS\"")
                .contains("\"path\":\"/api/auth/login\"")
                .contains("6 giây");
        assertThat(forwarded).isFalse();
    }

    @Test
    @DisplayName("healthcheck và preflight CORS không bị đếm")
    void khongDemHealthcheckVaPreflight() {
        RateLimitGatewayFilter filter = filter(false);

        filter.filter(exchange(MockServerHttpRequest.get("/actuator/health")), chain).block();
        filter.filter(exchange(MockServerHttpRequest.options("/api/courses")), chain).block();

        assertThat(calls).isEmpty();
        assertThat(forwarded).isTrue();
    }

    @Test
    @DisplayName("tắt bằng cấu hình thì không gọi Redis")
    void tatThiKhongGoiRedis() {
        RateLimitGatewayFilter filter = new RateLimitGatewayFilter(limiter(false),
                new RateLimitProperties(false, PROPERTIES.login(), PROPERTIES.api()));

        filter.filter(exchange(MockServerHttpRequest.post("/api/auth/login")), chain).block();

        assertThat(calls).isEmpty();
        assertThat(forwarded).isTrue();
    }

    @Test
    @DisplayName("Retry-After làm tròn lên, không bao giờ bằng 0")
    void retryAfterLamTronLen() {
        assertThat(new RateLimitProperties.Policy(1, 60, 6).retryAfterSeconds()).isEqualTo(6);
        assertThat(new RateLimitProperties.Policy(20, 40, 1).retryAfterSeconds()).isEqualTo(1);
        assertThat(new RateLimitProperties.Policy(2, 10, 3).retryAfterSeconds()).isEqualTo(2);
    }

    private RateLimitGatewayFilter filter(boolean allowed) {
        return new RateLimitGatewayFilter(limiter(allowed), PROPERTIES);
    }

    @SuppressWarnings("unchecked")
    private RateLimiter<Object> limiter(boolean allowed) {
        RateLimiter<Object> limiter = mock(RateLimiter.class);
        when(limiter.isAllowed(anyString(), anyString())).thenAnswer(invocation -> {
            calls.add(invocation.getArgument(0) + " " + invocation.getArgument(1));
            return Mono.just(new RateLimiter.Response(allowed,
                    Map.of("X-RateLimit-Remaining", allowed ? "5" : "0")));
        });
        return limiter;
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        return MockServerWebExchange.from(
                request.remoteAddress(new InetSocketAddress("10.0.0.5", 51000)));
    }
}
