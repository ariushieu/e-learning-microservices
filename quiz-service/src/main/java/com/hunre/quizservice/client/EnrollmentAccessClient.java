package com.hunre.quizservice.client;

import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Kiểm tra ghi danh bằng token gốc; một hạn chờ chung cho tất cả các trang. */
@Component
public class EnrollmentAccessClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String endpoint;
    private final Duration timeout;

    public EnrollmentAccessClient(ObjectMapper objectMapper,
                                  @Value("${quiz.enrollment.base-url:http://localhost:8083}") String baseUrl,
                                  @Value("${quiz.enrollment.timeout-ms:2000}") long timeoutMs) {
        this.objectMapper = objectMapper;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER).build();
        this.endpoint = baseUrl.replaceAll("/+$", "") + "/api/enrollments";
    }

    public boolean hasEnrollment(Long courseId, Long userId, String authorization) {
        if (courseId == null || userId == null || authorization == null || !authorization.startsWith("Bearer ")
                || authorization.substring(7).isBlank()) {
            return false;
        }
        long deadline = System.nanoTime() + timeout.toNanos();
        for (int page = 0; ; page++) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                throw unavailable();
            }
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint
                            + "?page=" + page + "&size=100&sort=id,asc"))
                    .timeout(Duration.ofNanos(remaining))
                    .header("Authorization", authorization).GET().build();
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 401 || response.statusCode() == 403) {
                    return false;
                }
                if (response.statusCode() != 200) {
                    throw unavailable();
                }
                JsonNode root = objectMapper.readTree(response.body());
                if (root == null || !root.isObject()) {
                    throw unavailable();
                }
                JsonNode data = root.path("data");
                JsonNode content = data.path("content");
                if (!root.path("success").isBoolean() || !root.path("success").asBoolean()
                        || !content.isArray() || !data.path("page").isIntegralNumber()
                        || data.path("page").asLong(-1) != page || !data.path("last").isBoolean()) {
                    throw unavailable();
                }
                for (JsonNode enrollment : content) {
                    if (!enrollment.path("courseId").isIntegralNumber()
                            || !enrollment.path("userId").isIntegralNumber()
                            || !enrollment.path("status").isString()) {
                        throw unavailable();
                    }
                    if (enrollment.path("courseId").asLong(-1) == courseId
                            && enrollment.path("userId").asLong(-1) == userId) {
                        String status = enrollment.path("status").asString();
                        if ("ACTIVE".equals(status) || "COMPLETED".equals(status)) {
                            return true;
                        }
                    }
                }
                if (data.path("last").asBoolean()) {
                    return false;
                }
                if (content.isEmpty()) {
                    throw unavailable();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw unavailable();
            } catch (IOException | tools.jackson.core.JacksonException exception) {
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "Không thể kiểm tra ghi danh lúc này, vui lòng thử lại sau", exception);
            }
        }
    }

    private BusinessException unavailable() {
        return new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                "Không thể kiểm tra ghi danh lúc này, vui lòng thử lại sau");
    }
}
