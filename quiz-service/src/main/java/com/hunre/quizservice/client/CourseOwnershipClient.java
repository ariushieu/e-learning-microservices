package com.hunre.quizservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

/** Xác minh khóa học bằng chính token của người tạo bài kiểm tra. */
@Component
public class CourseOwnershipClient {
    private final RestClient restClient;

    @Autowired
    public CourseOwnershipClient(@Value("${app.course-service-url:http://localhost:8082}") String baseUrl) {
        var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    CourseOwnershipClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void requireCourseOwner(Long courseId, Long currentUserId, boolean isAdmin, String authorization) {
        if (currentUserId == null || authorization == null || authorization.isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Không thể xác minh chủ sở hữu khóa học");
        }
        ApiResponse<CourseIdentity> response;
        try {
            response = restClient.get().uri("/api/courses/{id}", courseId)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve().body(new ParameterizedTypeReference<ApiResponse<CourseIdentity>>() {});
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("khóa học", "id", courseId);
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập khóa học này", ex);
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ", ex);
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Không thể xác minh khóa học lúc này, vui lòng thử lại sau", ex);
        }
        if (response == null || !response.success() || response.data() == null
                || !courseId.equals(response.data().id()) || response.data().instructorId() == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Dữ liệu xác minh khóa học không hợp lệ, vui lòng thử lại sau");
        }
        if (!isAdmin && !currentUserId.equals(response.data().instructorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Bạn không có quyền tạo bài kiểm tra trong khóa học của giảng viên khác");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CourseIdentity(Long id, Long instructorId) {}
}
