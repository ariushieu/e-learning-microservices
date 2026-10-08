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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Giới hạn số request mỗi người được gửi qua gateway, đếm bằng Redis.
 *
 * <p><b>Ba chính sách.</b>
 * <ul>
 *   <li>Đăng nhập, đăng ký: chặt, tính theo <b>tài khoản</b> (email trong body). Dò mật khẩu
 *       luôn nhắm vào một tài khoản, nên đếm theo tài khoản chặn được cả khi kẻ dò đổi IP.</li>
 *   <li>Đăng nhập, đăng ký, làm mới token: nới hơn, tính theo <b>địa chỉ IP</b>. Chặn một
 *       máy thử hàng loạt tài khoản khác nhau, mỗi tài khoản vài lần.</li>
 *   <li>Mọi đường dẫn còn lại: rộng, đủ cho người dùng thật bấm nhanh, tính theo id người
 *       dùng nếu đã đăng nhập.</li>
 * </ul>
 *
 * <p><b>Vì sao không chỉ đếm theo IP.</b> Đăng nhập trên web đi qua server Next.js, nên
 * gateway thấy mọi người dùng chung một IP là container frontend. Đọc IP thật từ
 * {@code X-Forwarded-For} cũng không được: Docker Desktop đổi IP nguồn của mọi kết nối từ
 * ngoài thành IP của mạng Docker, nên ngay cả frontend cũng không biết IP thật; và Next.js
 * giữ nguyên header đó nếu trình duyệt tự gửi, tin nó là tin client. Đếm chặt theo IP thì
 * mười người đăng nhập trong một phút là người thứ mười một nhận 429.
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
    static final String LOGIN_IP_POLICY = "rate-limit-login-ip";
    static final String API_POLICY = "rate-limit-api";

    /** Body đăng nhập, đăng ký chỉ vài trăm byte; giới hạn để không ai đẩy cả MB vào bộ nhớ gateway. */
    static final int MAX_CREDENTIALS_BYTES = 16 * 1024;

    private static final Set<String> CREDENTIAL_PATHS = Set.of("/api/auth/login", "/api/auth/register");
    private static final String REFRESH_PATH = "/api/auth/refresh-token";
    private static final ObjectMapper JSON = JsonMapper.builder().build();

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

        boolean post = HttpMethod.POST.equals(request.getMethod());
        if (post && CREDENTIAL_PATHS.contains(path)) {
            return limitCredentials(exchange, chain);
        }
        if (post && REFRESH_PATH.equals(path)) {
            // Refresh token là chuỗi ngẫu nhiên, không dò được; chỉ cần chặn một máy gửi dồn.
            return limit(LOGIN_IP_POLICY, clientIp(request), properties.loginIp(), exchange,
                    () -> chain.filter(exchange));
        }
        return limit(API_POLICY, userOrClientIp(exchange), properties.api(), exchange,
                () -> chain.filter(exchange));
    }

    /**
     * Đọc body để biết email, rồi trừ token ở cả xô IP lẫn xô tài khoản. Body đã đọc thì phải
     * dựng lại cho auth-service đọc tiếp, nên request đi tiếp là bản bọc trả lại đúng các byte đó.
     */
    private Mono<Void> limitCredentials(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        return DataBufferUtils.join(request.getBody(), MAX_CREDENTIALS_BYTES)
                .map(RateLimitGatewayFilter::drain)
                .defaultIfEmpty(new byte[0])
                .flatMap(body -> {
                    ServerWebExchange replay = exchange.mutate().request(replayBody(request, body)).build();
                    String account = accountKey(body);
                    return limit(LOGIN_IP_POLICY, clientIp(request), properties.loginIp(), replay,
                            () -> account == null
                                    // Không có email thì auth-service trả 400, không dò được gì.
                                    ? chain.filter(replay)
                                    : limit(LOGIN_POLICY, account, properties.login(), replay,
                                            () -> chain.filter(replay)));
                })
                .onErrorResume(DataBufferLimitException.class, error -> ErrorResponseWriter.write(exchange,
                        ErrorCode.BAD_REQUEST.httpStatus(), ErrorCode.BAD_REQUEST.name(),
                        "Dữ liệu gửi lên quá lớn."));
    }

    private Mono<Void> limit(String policyId, String key, RateLimitProperties.Policy policy,
                             ServerWebExchange exchange, Supplier<Mono<Void>> next) {
        ServerHttpRequest request = exchange.getRequest();
        // RedisRateLimiter đặt tên xô trong Redis chỉ theo khóa, không kèm chính sách. Không thêm
        // tên chính sách thì xô "đăng nhập theo IP" và xô "API của khách" cùng là ip:<địa chỉ>:
        // khách duyệt web cũng trừ vào lượt đăng nhập, và hai cấu hình ghi đè số dư của nhau.
        String bucket = policyId + ":" + key;
        return rateLimiter.isAllowed(policyId, bucket).flatMap(result -> {
            // Đăng nhập trừ ở hai xô: header của xô sau ghi đè xô trước, tức là số lượt còn lại
            // của tài khoản — con số người dùng quan tâm.
            HttpHeaders headers = exchange.getResponse().getHeaders();
            result.getHeaders().forEach(headers::set);

            if (result.isAllowed()) {
                return next.get();
            }

            log.info("Chặn {} {}: vượt giới hạn {}", request.getMethod(),
                    request.getPath().pathWithinApplication().value(), bucket);
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
    private static String clientIp(ServerHttpRequest request) {
        InetSocketAddress remote = request.getRemoteAddress();
        if (remote == null || remote.getAddress() == null) {
            return "ip:unknown";
        }
        return "ip:" + remote.getAddress().getHostAddress();
    }

    /**
     * Khóa đếm của tài khoản, hoặc {@code null} nếu body không có email dạng chuỗi.
     *
     * <p>Chuẩn hóa giống cách MySQL so email ({@code utf8mb4_0900_ai_ci}: không phân biệt hoa
     * thường, không phân biệt dấu). Không vậy thì {@code SV@…} hay {@code sv́@…} vẫn đăng
     * nhập vào cùng tài khoản mà mỗi cách viết lại có một xô riêng. Băm SHA-256 để Redis và log
     * không chứa email.
     */
    static String accountKey(byte[] body) {
        if (body.length == 0) {
            return null;
        }
        try {
            JsonNode email = JSON.readTree(body).get("email");
            if (email == null || !email.isString() || email.stringValue().isBlank()) {
                return null;
            }
            String normalized = Normalizer.normalize(email.stringValue().strip(), Normalizer.Form.NFKD)
                    .replaceAll("\\p{M}", "")
                    .toLowerCase(Locale.ROOT);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            return "account:" + HexFormat.of().formatHex(hash);
        } catch (JacksonException notJson) {
            return null;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("JVM thiếu SHA-256", impossible);
        }
    }

    private static byte[] drain(DataBuffer buffer) {
        try {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            return bytes;
        } finally {
            DataBufferUtils.release(buffer);
        }
    }

    private static ServerHttpRequest replayBody(ServerHttpRequest request, byte[] body) {
        return new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<DataBuffer> getBody() {
                return body.length == 0 ? Flux.empty()
                        // Bọc mới mỗi lần đọc: gateway có thể đọc body nhiều lần khi thử lại.
                        : Flux.defer(() -> Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body)));
            }
        };
    }
}
