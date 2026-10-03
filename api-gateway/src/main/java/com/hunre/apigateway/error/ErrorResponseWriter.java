package com.hunre.apigateway.error;

import com.hunre.sharedcommon.dto.ErrorResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * Ghi lỗi ra client theo đúng hình dạng {@link ErrorResponse} mà 5 service phía sau dùng.
 *
 * <p>Gateway chạy WebFlux nên không có {@code GlobalExceptionHandler} của shared-common lo
 * hộ. Mọi chỗ trong gateway tự trả lỗi — thiếu token, quá giới hạn request, service chết —
 * đều đi qua đây, để frontend chỉ phải đọc một hình dạng lỗi duy nhất dù lỗi sinh ra ở
 * gateway hay ở service.
 */
public final class ErrorResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private ErrorResponseWriter() {
    }

    public static Mono<Void> write(ServerWebExchange exchange, HttpStatusCode status,
                                   String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        String path = exchange.getRequest().getPath().value();

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] body = OBJECT_MAPPER
                .writeValueAsString(ErrorResponse.of(code, message, path))
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);

        return response.writeWith(Mono.just(buffer));
    }
}
