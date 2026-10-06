package com.hunre.enrollmentservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

/** Xác minh bài học từ service sở hữu dữ liệu trước khi ghi tiến độ. */
@Component
public class CourseLessonClient {

    private final RestClient restClient;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    @Autowired
    public CourseLessonClient(@Value("${app.course-service-url:http://localhost:8082}") String baseUrl,
                              ObjectProvider<HttpServletRequest> requestProvider) {
        this.requestProvider = requestProvider;
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    CourseLessonClient(RestClient restClient, ObjectProvider<HttpServletRequest> requestProvider) {
        this.restClient = restClient;
        this.requestProvider = requestProvider;
    }

    public void validateLesson(Long courseId, Long lessonId) {
        ApiResponse<LessonIdentity> response;
        try {
            response = restClient.get().uri("/api/lessons/{id}", lessonId)
                    .header(HttpHeaders.AUTHORIZATION, callerAuthorization())
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<LessonIdentity>>() {});
        } catch (HttpClientErrorException.NotFound ex) {
            throw missingLesson();
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Không thể xác minh bài học lúc này, vui lòng thử lại sau", ex);
        }

        if (response == null || !response.success() || response.data() == null
                || !lessonId.equals(response.data().id()) || response.data().courseId() == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Dữ liệu xác minh bài học không hợp lệ, vui lòng thử lại sau");
        }
        if (!courseId.equals(response.data().courseId())) {
            throw missingLesson();
        }
    }

    /** Snapshot có thể trễ khi Kafka lỗi: xác minh nguồn trước khi tạo/kích hoạt ghi danh. */
    public void requirePublishedCourse(Long courseId) {
        ApiResponse<CourseIdentity> response;
        try {
            response = restClient.get().uri("/api/courses/{id}", courseId)
                    .header(HttpHeaders.AUTHORIZATION, callerAuthorization())
                    .retrieve().body(new ParameterizedTypeReference<ApiResponse<CourseIdentity>>() {});
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("khóa học", "id", courseId);
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Không thể xác minh trạng thái khóa học lúc này, vui lòng thử lại sau", ex);
        }
        if (response == null || !response.success() || response.data() == null
                || !courseId.equals(response.data().id()) || response.data().status() == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "Dữ liệu khóa học không hợp lệ");
        }
        if (!"PUBLISHED".equals(response.data().status())) {
            throw new ResourceNotFoundException("khóa học", "id", courseId);
        }
    }

    private String callerAuthorization() {
        HttpServletRequest request = requestProvider.getIfAvailable();
        String authorization = request == null ? null : request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Thiếu token của người gọi");
        }
        return authorization;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CourseIdentity(Long id, String status) {}

    private ResourceNotFoundException missingLesson() {
        return new ResourceNotFoundException("Bài học không tồn tại trong khóa học này");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LessonIdentity(Long id, Long courseId) {}
}
