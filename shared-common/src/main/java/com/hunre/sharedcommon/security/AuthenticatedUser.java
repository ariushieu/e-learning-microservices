package com.hunre.sharedcommon.security;

import java.util.Set;

/**
 * Danh tính người gọi, đọc ra từ JWT sau khi chữ ký đã được kiểm.
 *
 * <p>Đây là thứ thay thế cho {@code @RequestParam Long userId}. Tham số do client tự khai
 * thì ai cũng sửa được, còn dữ liệu ở đây đến từ token đã xác thực nên không giả mạo được
 * nếu không có khóa ký.
 *
 * <p>Chỉ chứa những gì auth-service đặt vào token. Muốn thêm thông tin khác về người dùng
 * thì gọi auth-service, đừng nhồi thêm claim: token càng to thì mọi request càng nặng, và
 * dữ liệu trong token là bản chụp lúc đăng nhập, có thể đã cũ.
 *
 * @param userId   khóa chính của người dùng bên auth_db, lấy từ claim {@code sub}
 * @param email    email đăng nhập
 * @param fullName họ tên hiển thị
 * @param roles    mã vai trò, ví dụ {@code ROLE_STUDENT}
 */
public record AuthenticatedUser(Long userId, String email, String fullName, Set<String> roles) {

    public AuthenticatedUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    /** Người dùng có đúng vai trò này không. Tên vai trò phân biệt hoa thường. */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /** Người dùng có ít nhất một trong các vai trò truyền vào không. */
    public boolean hasAnyRole(String... candidates) {
        for (String candidate : candidates) {
            if (roles.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
