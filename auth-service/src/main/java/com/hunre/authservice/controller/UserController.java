package com.hunre.authservice.controller;

import com.hunre.authservice.domain.RoleCode;
import com.hunre.authservice.dto.UpdateUserRolesRequest;
import com.hunre.authservice.dto.UserResponse;
import com.hunre.authservice.service.AuthService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    /**
     * Gán / thay thế toàn bộ vai trò của một tài khoản.
     * Chỉ ROLE_ADMIN mới gọi được endpoint này.
     *
     * <p>Lưu ý: Quyền mới chỉ có hiệu lực sau khi người dùng đăng nhập lại (để sinh JWT mới mang các vai trò mới).
     * Token cũ vẫn mang vai trò cũ cho tới khi hết hạn (theo cơ chế stateless JWT).
     *
     * <p>Ví dụ: cấp ROLE_INSTRUCTOR cho học viên để họ có thể tạo bài kiểm tra.
     */
    @PatchMapping("/{id}/roles")
    public ApiResponse<UserResponse> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRolesRequest request,
            AuthenticatedUser caller
    ) {
        if (!caller.hasRole(Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ quản trị viên mới được đổi vai trò");
        }

        if (caller.userId().equals(id) && !request.getRoles().contains(RoleCode.ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATED, "Không thể tự gỡ quyền quản trị của chính mình");
        }

        return ApiResponse.ok(authService.updateUserRoles(id, request.getRoles()),
                "Cập nhật vai trò thành công");
    }
}
