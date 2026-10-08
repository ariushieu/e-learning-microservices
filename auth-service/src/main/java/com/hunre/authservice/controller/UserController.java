package com.hunre.authservice.controller;

import com.hunre.authservice.domain.RoleCode;
import com.hunre.authservice.domain.UserStatus;
import com.hunre.authservice.dto.UpdateUserStatusRequest;
import com.hunre.authservice.dto.UpdateUserRolesRequest;
import com.hunre.authservice.dto.UserResponse;
import com.hunre.authservice.service.AuthService;
import com.hunre.authservice.service.UserManagementService;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.exception.BusinessException;
import com.hunre.sharedcommon.exception.ErrorCode;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import com.hunre.sharedcommon.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserManagementService userManagement;

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoleCode role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            AuthenticatedUser caller) {
        requireAdmin(caller);
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Phân trang không hợp lệ: page từ 0, size từ 1 đến 100, offset không vượt quá 2147483647");
        }
        return ApiResponse.ok(userManagement.list(keyword, role, status, pageable));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> updateStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request, AuthenticatedUser caller) {
        requireAdmin(caller);
        return ApiResponse.ok(userManagement.updateStatus(id, UserStatus.valueOf(request.getStatus()), caller.userId()),
                "Cập nhật trạng thái thành công");
    }

    private void requireAdmin(AuthenticatedUser caller) {
        if (!caller.hasRole(Roles.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ quản trị viên mới được quản lý người dùng");
        }
    }

    /**
     * Gán / thay thế toàn bộ vai trò của một tài khoản.
     * Chỉ ROLE_ADMIN mới gọi được endpoint này.
     *
     * <p>Lưu ý: Quyền mới có hiệu lực khi đăng nhập lại hoặc làm mới token để sinh JWT mang các vai trò mới.
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
