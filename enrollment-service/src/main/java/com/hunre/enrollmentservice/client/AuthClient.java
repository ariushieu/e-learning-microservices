package com.hunre.enrollmentservice.client;

import java.util.Optional;

public interface AuthClient {

    /**
     * Xác thực thông tin người dùng từ Auth Service.
     *
     * @param userId ID người dùng
     * @return Thông tin người dùng nếu tồn tại
     */
    Optional<UserDto> getUserById(Long userId);
}
