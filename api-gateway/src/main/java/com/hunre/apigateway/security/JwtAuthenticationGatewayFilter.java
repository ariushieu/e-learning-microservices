package com.hunre.apigateway.security;

import com.hunre.sharedcommon.dto.ErrorResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.JwtSecurityProperties;
import com.hunre.sharedcommon.security.JwtVerifier;
import com.hunre.sharedcommon.security.PublicPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * Chặn token hỏng ngay tại cổng vào, trước khi request kịp đi vào mạng nội bộ.
 *
 * <p>Đây là bản WebFlux của {@code JwtAuthenticationFilter} bên shared-common. Không dùng
 * lại được lớp kia vì nó là servlet filter, còn gateway chạy reactive. Phần thật sự quan
 * trọng — kiểm chữ ký và đọc claim — thì vẫn dùng chung {@link JwtVerifier}, nên quy tắc
 * xác thực chỉ có một bản duy nhất trong cả hệ thống.
 *
 * <p>Gateway <b>không</b> bóc token ra rồi gắn header {@code X-User-Id} cho service phía
 * sau. Header đó service không có cách nào kiểm chứng, nên ai gọi thẳng vào service kèm
 * header tự chế cũng thành người khác được. Ở đây token gốc được chuyển nguyên vẹn xuống
 * để service tự kiểm lại.
 */
@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGatewayFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtVerifier jwtVerifier;
    private final PublicPaths publicPaths;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public JwtAuthenticationGatewayFilter(JwtSecurityProperties properties) {
        this.jwtVerifier = new JwtVerifier(properties.getJwtSecret());
        this.publicPaths = new PublicPaths(properties.getPublicPaths());
    }

    @Override
    public int getOrder() {
        // Chạy trước mọi filter định tuyến: chưa xác thực thì không cần biết đi về đâu.
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (HttpMethod.OPTIONS.equals(request.getMethod()) || isPublic(request)) {
            return chain.filter(exchange);
        }

        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return reject(exchange, "Chưa đăng nhập hoặc thiếu header Authorization");
        }

        try {
            AuthenticatedUser user = jwtVerifier.verify(header.substring(BEARER_PREFIX.length()).trim());
            log.debug("Cho qua {} {} của người dùng {}",
                    request.getMethod(), request.getPath(), user.userId());
        } catch (BusinessException ex) {
            return reject(exchange, ex.getMessage());
        }

        return chain.filter(exchange);
    }

    private boolean isPublic(ServerHttpRequest request) {
        PathContainer path = request.getPath().pathWithinApplication();
        String method = request.getMethod() == null ? "" : request.getMethod().name();
        return publicPaths.matches(method, path);
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        String path = exchange.getRequest().getPath().value();

        log.debug("Từ chối {} {}: {}", exchange.getRequest().getMethod(), path, message);

        response.setStatusCode(ErrorCode.UNAUTHORIZED.httpStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] body = objectMapper
                .writeValueAsString(ErrorResponse.of(ErrorCode.UNAUTHORIZED.name(), message, path))
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);

        return response.writeWith(Mono.just(buffer));
    }
}
