package com.hunre.enrollmentservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    public CourseLessonClient(@Value("${app.course-service-url:http://localhost:8082}") String baseUrl) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    CourseLessonClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void validateLesson(Long courseId, Long lessonId) {
        ApiResponse<LessonIdentity> response;
        try {
            response = restClient.get().uri("/api/lessons/{id}", lessonId)
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

    private ResourceNotFoundException missingLesson() {
        return new ResourceNotFoundException("Bài học không tồn tại trong khóa học này");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LessonIdentity(Long id, Long courseId) {}
}
