package com.hunre.courseservice.client;

import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
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

/** Checks the caller's enrollments without accepting a client-supplied identity. */
@Component
public class EnrollmentAccessClient {
    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String endpoint;
    private final Duration timeout;

    public EnrollmentAccessClient(ObjectProvider<HttpServletRequest> requestProvider,
                                  ObjectMapper objectMapper,
                                  @Value("${course.enrollment.base-url}") String baseUrl,
                                  @Value("${course.enrollment.list-path}") String listPath,
                                  @Value("${course.enrollment.timeout-ms}") long timeoutMs) {
        this.requestProvider = requestProvider;
        this.objectMapper = objectMapper;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER).build();
        this.endpoint = baseUrl.replaceAll("/+$", "") + listPath;
    }

    public boolean hasEnrollment(Long courseId, Long userId) {
        HttpServletRequest incoming = requestProvider.getIfAvailable();
        String authorization = incoming == null ? null : incoming.getHeader("Authorization");
        if (userId == null || authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }
        // One total deadline covers all pages; never retry or forward tokens on redirects.
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
                if (!root.path("success").asBoolean(false) || !content.isArray()
                        || data.path("page").asInt(-1) != page || !data.path("last").isBoolean()) {
                    throw unavailable();
                }
                for (JsonNode enrollment : content) {
                    if (enrollment.path("courseId").asLong(-1) == courseId
                            && enrollment.path("userId").asLong(-1) == userId) {
                        String status = enrollment.path("status").asText();
                        return "ACTIVE".equals(status) || "COMPLETED".equals(status);
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
                throw unavailable();
            }
        }
    }

    private BusinessException unavailable() {
        return new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                "Không thể kiểm tra ghi danh lúc này, vui lòng thử lại sau");
    }
}
