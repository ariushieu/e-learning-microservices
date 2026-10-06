package com.hunre.apigateway.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lỗi phát sinh ở gateway phải ra đúng mã trạng thái và không lộ cấu trúc mạng nội bộ.
 */
class GatewayErrorWebExceptionHandlerTest {

    private final GatewayErrorWebExceptionHandler handler = new GatewayErrorWebExceptionHandler();

    /**
     * Các kiểu exception Netty ném ra khi service đích không với tới được. Netty bọc exception
     * gốc bên trong lớp riêng của nó (ví dụ {@code AnnotatedNoRouteToHostException}, không
     * public), nên ở đây mô phỏng bằng cách bọc trong một RuntimeException — handler phải lần
     * theo cause chứ không chỉ nhìn lớp ngoài cùng.
     */
    static Stream<Throwable> serviceKhongVoiToiDuoc() {
        return Stream.of(
                new ConnectException("Connection refused: quiz-service/172.19.0.7:8084"),
                // Đã gặp thật khi tắt một container: bản cũ trả 500 cho kiểu này
                new RuntimeException(new NoRouteToHostException("quiz-service/172.19.0.7:8084")),
                new RuntimeException(new UnknownHostException("quiz-service")),
                new RuntimeException(new SocketException("Connection reset")));
    }

    @ParameterizedTest
    @MethodSource("serviceKhongVoiToiDuoc")
    @DisplayName("service đích không với tới được thì trả 502, không phải 500")
    void serviceChetTra502(Throwable error) {
        MockServerWebExchange exchange = exchange("/api/quizzes/course/1");

        handler.handle(exchange, error).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(body(exchange))
                .contains("\"code\":\"EXTERNAL_SERVICE_ERROR\"")
                .contains("\"path\":\"/api/quizzes/course/1\"")
                .as("tên service, IP và cổng nội bộ chỉ được nằm trong log")
                .doesNotContain("172.19.0.7")
                .doesNotContain("8084")
                .doesNotContain("quiz-service");
    }

    @Test
    @DisplayName("không có route khớp thì trả 404 với mã RESOURCE_NOT_FOUND")
    void khongCoRouteTra404() {
        MockServerWebExchange exchange = exchange("/api/khong-ton-tai");

        handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body(exchange)).contains("\"code\":\"RESOURCE_NOT_FOUND\"");
    }

    @Test
    @DisplayName("lỗi không liên quan tới mạng vẫn là 500 — không được gom hết thành 502")
    void loiKhacVanLa500() {
        MockServerWebExchange exchange = exchange("/api/courses");

        handler.handle(exchange, new IllegalStateException("bug trong filter")).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body(exchange))
                .contains("\"code\":\"INTERNAL_ERROR\"")
                .doesNotContain("bug trong filter");
    }

    private static MockServerWebExchange exchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path));
    }

    private static String body(MockServerWebExchange exchange) {
        return exchange.getResponse().getBodyAsString().block();
    }
}
