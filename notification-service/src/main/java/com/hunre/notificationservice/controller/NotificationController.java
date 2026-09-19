package com.hunre.notificationservice.controller;

import com.hunre.notificationservice.dto.NotificationResponse;
import com.hunre.notificationservice.service.NotificationService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hộp thư thông báo của người đang đăng nhập.
 *
 * <p>Không endpoint nào nhận {@code userId} từ client: danh tính lấy từ token đã kiểm chữ
 * ký, nên không thể đọc hộp thư của người khác bằng cách đổi tham số.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getMyNotifications(
            AuthenticatedUser user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(notificationService.getMyNotifications(user.userId(), pageable));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> countUnread(AuthenticatedUser user) {
        return ApiResponse.ok(notificationService.countUnread(user.userId()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable Long id, AuthenticatedUser user) {
        return ApiResponse.ok(notificationService.markRead(id, user.userId()), "Đã đánh dấu đã đọc");
    }
}
