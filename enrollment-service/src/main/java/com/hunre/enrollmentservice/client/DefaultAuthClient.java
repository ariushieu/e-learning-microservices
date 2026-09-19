package com.hunre.enrollmentservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class DefaultAuthClient implements AuthClient {

    @Override
    public Optional<UserDto> getUserById(Long userId) {
        if (userId == null || userId <= 0) {
            return Optional.empty();
        }

        // Mock / stub user data
        log.debug("Sử dụng stub dữ liệu cho userId={}", userId);
        return Optional.of(UserDto.builder()
                .id(userId)
                .email("student" + userId + "@hunre.edu.vn")
                .fullName("Học viên #" + userId)
                .build());
    }
}
