package com.hunre.apigateway.error;

import com.hunre.sharedcommon.dto.ErrorResponse;
import com.hunre.sharedcommon.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;

/**
 * Dựng response lỗi của gateway theo đúng hình dạng {@link ErrorResponse} như 5 service.
 *
 * <p><b>Vì sao cần lớp này.</b> Handler mặc định của Spring Boot trả về một hình dạng JSON
 * khác hẳn ({@code timestamp, path, status, error, requestId, trace}) và khi có devtools
 * thì kèm luôn cả stack trace. Chạy thật thì ra thế này:
 *
 * <pre>{@code
 * {"status":500,"error":"Internal Server Error",
 *  "message":"Connection refused: getsockopt: localhost/127.0.0.1:8081",
 *  "trace":"io.netty.channel.AbstractChannel$AnnotatedConnectException: ..."}
 * }</pre>
 *
 * <p>Hai cái sai ở đó: frontend phải viết thêm một nhánh xử lý lỗi thứ hai chỉ vì lỗi này
 * đến từ gateway, và cấu trúc mạng nội bộ (cổng 8081) bị đẩy thẳng ra ngoài.
 *
 * <p>{@code @Order(-2)} để chạy trước {@code DefaultErrorWebExceptionHandler} của Boot,
 * lớp đó đăng ký ở thứ tự -1.
 */
@Component
@Order(-2)
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorWebExceptionHandler.class);

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        ServerHttpResponse response = exchange.getResponse();
        String path = exchange.getRequest().getPath().value();

        if (response.isCommitted()) {
            return Mono.error(error);
        }

        HttpStatusCode status;
        String code;
        String message;

        if (hasCause(error, ConnectException.class)) {
            status = ErrorCode.EXTERNAL_SERVICE_ERROR.httpStatus();
            code = ErrorCode.EXTERNAL_SERVICE_ERROR.name();
            message = "Dịch vụ tạm thời không phản hồi, vui lòng thử lại sau";
            // Chi tiết kỹ thuật chỉ nằm trong log của gateway, không gửi ra client.
            log.error("Không kết nối được service đích cho {}: {}", path, error.getMessage());
        } else if (error instanceof ResponseStatusException statusException) {
            status = statusException.getStatusCode();
            code = status.value() == 404 ? ErrorCode.RESOURCE_NOT_FOUND.name() : ErrorCode.BAD_REQUEST.name();
            message = status.value() == 404
                    ? "Không tìm thấy đường dẫn yêu cầu"
                    : "Yêu cầu không hợp lệ";
            log.warn("Lỗi định tuyến {} {}: {}", status.value(), path, statusException.getMessage());
        } else {
            status = ErrorCode.INTERNAL_ERROR.httpStatus();
            code = ErrorCode.INTERNAL_ERROR.name();
            message = "Hệ thống gặp sự cố, vui lòng thử lại sau";
            log.error("Lỗi ngoài dự kiến tại gateway, đường dẫn {}", path, error);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] body = objectMapper
                .writeValueAsString(ErrorResponse.of(code, message, path))
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);

        return response.writeWith(Mono.just(buffer));
    }

    /** Netty bọc ConnectException vào vài lớp exception khác nên phải lần theo cause. */
    private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return false;
    }
}
