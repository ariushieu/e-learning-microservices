package com.hunre.apigateway.ratelimit;

import com.hunre.apigateway.security.JwtAuthenticationGatewayFilter;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

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
            new RateLimitProperties.Policy(1, 120, 2),
            new RateLimitProperties.Policy(20, 40, 1));

    private static final String LOGIN_BODY = "{\"email\":\"sv@hunre.edu.vn\",\"password\":\"sai\"}";

    /** Tên xô trong Redis của từng lời gọi tới limiter: "<chính sách>:<khóa>". */
    private final List<String> calls = new ArrayList<>();
    /** Body mà service phía sau nhận được; null nghĩa là request không được chuyển đi. */
    private final AtomicReference<String> forwardedBody = new AtomicReference<>();
    private final GatewayFilterChain chain = exchange -> DataBufferUtils.join(exchange.getRequest().getBody())
            .map(buffer -> {
                String body = buffer.toString(StandardCharsets.UTF_8);
                DataBufferUtils.release(buffer);
                return body;
            })
            .defaultIfEmpty("")
            .doOnNext(forwardedBody::set)
            .then();

    @Test
    @DisplayName("đăng nhập trừ ở xô IP rồi xô tài khoản; body tới auth-service nguyên vẹn")
    void dangNhapDemTheoIpVaTaiKhoan() {
        filter(policy -> true).filter(login(LOGIN_BODY), chain).block();

        assertThat(calls).containsExactly(
                RateLimitGatewayFilter.LOGIN_IP_POLICY + ":ip:10.0.0.5",
                RateLimitGatewayFilter.LOGIN_POLICY + ":" + accountOf("sv@hunre.edu.vn"));
        assertThat(forwardedBody).hasValue(LOGIN_BODY);
    }

    @Test
    @DisplayName("hai người khác email từ cùng một IP không dùng chung xô tài khoản")
    void haiTaiKhoanCungIpKhacXo() {
        RateLimitGatewayFilter filter = filter(policy -> true);

        filter.filter(login("{\"email\":\"a@hunre.edu.vn\",\"password\":\"x\"}"), chain).block();
        filter.filter(login("{\"email\":\"b@hunre.edu.vn\",\"password\":\"x\"}"), chain).block();

        assertThat(calls).filteredOn(c -> c.startsWith(RateLimitGatewayFilter.LOGIN_POLICY + ":"))
                .containsExactly(
                        RateLimitGatewayFilter.LOGIN_POLICY + ":" + accountOf("a@hunre.edu.vn"),
                        RateLimitGatewayFilter.LOGIN_POLICY + ":" + accountOf("b@hunre.edu.vn"));
    }

    @Test
    @DisplayName("viết hoa, khoảng trắng, thêm dấu vẫn là cùng một tài khoản")
    void cungTaiKhoanDuVietKhac() {
        String key = accountOf("sv@hunre.edu.vn");

        assertThat(accountOf("  SV@Hunre.EDU.vn ")).isEqualTo(key);
        assertThat(accountOf("sv́@hunre.edu.vn")).isEqualTo(key);
        assertThat(accountOf("sv@hunrè.edu.vn")).isEqualTo(key);
        assertThat(accountOf("sv＠hunre.edu.vn")).isEqualTo(key);
        assertThat(key).startsWith("account:").doesNotContain("hunre");
    }

    @Test
    @DisplayName("đổi X-Forwarded-For không đổi xô IP")
    void boQuaXForwardedFor() {
        MockServerHttpRequest.BodyBuilder request = MockServerHttpRequest.post("/api/auth/login")
                .header("X-Forwarded-For", "203.0.113.9");

        filter(policy -> true).filter(exchange(request, LOGIN_BODY), chain).block();

        assertThat(calls).first().isEqualTo(RateLimitGatewayFilter.LOGIN_IP_POLICY + ":ip:10.0.0.5");
    }

    @Test
    @DisplayName("hết xô tài khoản: 429 Retry-After 6, không chuyển request đi")
    void hetXoTaiKhoanTra429() {
        MockServerWebExchange exchange = login(LOGIN_BODY);

        filter(policy -> !policy.equals(RateLimitGatewayFilter.LOGIN_POLICY)).filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("6");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"code\":\"TOO_MANY_REQUESTS\"")
                .contains("\"path\":\"/api/auth/login\"")
                .contains("6 giây");
        assertThat(forwardedBody).hasNullValue();
    }

    @Test
    @DisplayName("hết xô IP: 429 Retry-After 2, không trừ token của tài khoản")
    void hetXoIpKhongTruXoTaiKhoan() {
        MockServerWebExchange exchange = login(LOGIN_BODY);

        filter(policy -> !policy.equals(RateLimitGatewayFilter.LOGIN_IP_POLICY)).filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("2");
        assertThat(calls).containsExactly(RateLimitGatewayFilter.LOGIN_IP_POLICY + ":ip:10.0.0.5");
        assertThat(forwardedBody).hasNullValue();
    }

    @Test
    @DisplayName("đăng ký cũng đếm theo email")
    void dangKyDemTheoEmail() {
        String body = "{\"email\":\"moi@hunre.edu.vn\",\"password\":\"Abc@12345\",\"fullName\":\"Mới\"}";

        filter(policy -> true).filter(exchange(MockServerHttpRequest.post("/api/auth/register"), body), chain).block();

        assertThat(calls).containsExactly(
                RateLimitGatewayFilter.LOGIN_IP_POLICY + ":ip:10.0.0.5",
                RateLimitGatewayFilter.LOGIN_POLICY + ":" + accountOf("moi@hunre.edu.vn"));
        assertThat(forwardedBody).hasValue(body);
    }

    @Test
    @DisplayName("body không có email hoặc không phải JSON: chỉ trừ xô IP, vẫn chuyển cho auth-service báo lỗi")
    void khongCoEmailChiTruXoIp() {
        for (String body : List.of("", "{}", "{\"email\":42}", "{\"email\":\"  \"}", "không phải json")) {
            calls.clear();
            forwardedBody.set(null);

            filter(policy -> true).filter(login(body), chain).block();

            assertThat(calls).as(body).containsExactly(RateLimitGatewayFilter.LOGIN_IP_POLICY + ":ip:10.0.0.5");
            assertThat(forwardedBody).as(body).hasValue(body);
        }
    }

    @Test
    @DisplayName("body quá lớn: 400, không gọi Redis, không chuyển đi")
    void bodyQuaLon() {
        String body = "{\"email\":\"" + "a".repeat(RateLimitGatewayFilter.MAX_CREDENTIALS_BYTES) + "\"}";
        MockServerWebExchange exchange = login(body);

        filter(policy -> true).filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(calls).isEmpty();
        assertThat(forwardedBody).hasNullValue();
    }

    @Test
    @DisplayName("làm mới token chỉ trừ xô IP, không đọc body")
    void lamMoiTokenChiTruXoIp() {
        String body = "{\"refreshToken\":\"abc\"}";

        filter(policy -> true).filter(exchange(MockServerHttpRequest.post("/api/auth/refresh-token"), body), chain).block();

        assertThat(calls).containsExactly(RateLimitGatewayFilter.LOGIN_IP_POLICY + ":ip:10.0.0.5");
        assertThat(forwardedBody).hasValue(body);
    }

    @Test
    @DisplayName("đã đăng nhập thì đếm theo người dùng, không theo IP")
    void daDangNhapDemTheoNguoiDung() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/enrollments"));
        exchange.getAttributes().put(JwtAuthenticationGatewayFilter.AUTHENTICATED_USER_ATTRIBUTE,
                new AuthenticatedUser(42L, "sv@hunre.edu.vn", "Sinh viên", Set.of("ROLE_STUDENT")));

        filter(policy -> true).filter(exchange, chain).block();

        assertThat(calls).containsExactly(RateLimitGatewayFilter.API_POLICY + ":user:42");
    }

    @Test
    @DisplayName("khách chưa đăng nhập xem khóa học thì đếm theo IP, chính sách thường")
    void khachDemTheoIp() {
        filter(policy -> true).filter(exchange(MockServerHttpRequest.get("/api/courses")), chain).block();

        assertThat(calls).containsExactly(RateLimitGatewayFilter.API_POLICY + ":ip:10.0.0.5");
    }

    @Test
    @DisplayName("GET /api/auth/me không phải đăng nhập — không dùng chính sách chặt")
    void chiPostDangNhapMoiChat() {
        filter(policy -> true).filter(exchange(MockServerHttpRequest.get("/api/auth/me")), chain).block();

        assertThat(calls).containsExactly(RateLimitGatewayFilter.API_POLICY + ":ip:10.0.0.5");
    }

    @Test
    @DisplayName("healthcheck và preflight CORS không bị đếm")
    void khongDemHealthcheckVaPreflight() {
        RateLimitGatewayFilter filter = filter(policy -> false);

        filter.filter(exchange(MockServerHttpRequest.get("/actuator/health")), chain).block();
        filter.filter(exchange(MockServerHttpRequest.options("/api/courses")), chain).block();

        assertThat(calls).isEmpty();
        assertThat(forwardedBody).hasValue("");
    }

    @Test
    @DisplayName("tắt bằng cấu hình thì không gọi Redis")
    void tatThiKhongGoiRedis() {
        RateLimitGatewayFilter filter = new RateLimitGatewayFilter(limiter(policy -> false),
                new RateLimitProperties(false, PROPERTIES.login(), PROPERTIES.loginIp(), PROPERTIES.api()));

        filter.filter(login(LOGIN_BODY), chain).block();

        assertThat(calls).isEmpty();
        assertThat(forwardedBody).hasValue(LOGIN_BODY);
    }

    @Test
    @DisplayName("xô đăng nhập theo IP và xô API của khách là hai xô riêng dù cùng IP")
    void xoDangNhapVaXoApiKhongDungChung() {
        RateLimitGatewayFilter filter = filter(policy -> true);

        filter.filter(exchange(MockServerHttpRequest.get("/api/courses")), chain).block();
        filter.filter(login(LOGIN_BODY), chain).block();

        assertThat(calls).element(0).isEqualTo(RateLimitGatewayFilter.API_POLICY + ":ip:10.0.0.5");
        assertThat(calls).element(1).isEqualTo(RateLimitGatewayFilter.LOGIN_IP_POLICY + ":ip:10.0.0.5");
    }

    @Test
    @DisplayName("Retry-After làm tròn lên, không bao giờ bằng 0")
    void retryAfterLamTronLen() {
        assertThat(new RateLimitProperties.Policy(1, 60, 6).retryAfterSeconds()).isEqualTo(6);
        assertThat(new RateLimitProperties.Policy(1, 120, 2).retryAfterSeconds()).isEqualTo(2);
        assertThat(new RateLimitProperties.Policy(20, 40, 1).retryAfterSeconds()).isEqualTo(1);
        assertThat(new RateLimitProperties.Policy(2, 10, 3).retryAfterSeconds()).isEqualTo(2);
    }

    private static String accountOf(String email) {
        return RateLimitGatewayFilter.accountKey(
                ("{\"email\":\"" + email + "\"}").getBytes(StandardCharsets.UTF_8));
    }

    private RateLimitGatewayFilter filter(Predicate<String> allowedPolicy) {
        return new RateLimitGatewayFilter(limiter(allowedPolicy), PROPERTIES);
    }

    @SuppressWarnings("unchecked")
    private RateLimiter<Object> limiter(Predicate<String> allowedPolicy) {
        RateLimiter<Object> limiter = mock(RateLimiter.class);
        when(limiter.isAllowed(anyString(), anyString())).thenAnswer(invocation -> {
            String policy = invocation.getArgument(0);
            calls.add(invocation.getArgument(1));
            boolean allowed = allowedPolicy.test(policy);
            return Mono.just(new RateLimiter.Response(allowed,
                    Map.of("X-RateLimit-Remaining", allowed ? "5" : "0")));
        });
        return limiter;
    }

    private static MockServerWebExchange login(String body) {
        return exchange(MockServerHttpRequest.post("/api/auth/login"), body);
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BodyBuilder request, String body) {
        return MockServerWebExchange.from(request.remoteAddress(new InetSocketAddress("10.0.0.5", 54321))
                .header("Content-Type", "application/json")
                .body(body));
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        return MockServerWebExchange.from(request.remoteAddress(new InetSocketAddress("10.0.0.5", 54321)));
    }
}
