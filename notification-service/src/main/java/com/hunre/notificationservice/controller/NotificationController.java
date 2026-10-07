package com.hunre.notificationservice.controller;

import com.hunre.notificationservice.dto.NotificationPreferenceResponse;
import com.hunre.notificationservice.dto.NotificationResponse;
import com.hunre.notificationservice.dto.UpdateNotificationPreferenceRequest;
import com.hunre.notificationservice.realtime.InboxStreamService;
import com.hunre.notificationservice.service.NotificationService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.dto.PageResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Hộp thư thông báo của người đang đăng nhập.
 *
 * <p>Không endpoint nào nhận {@code userId} từ client: danh tính lấy từ token đã kiểm chữ
 * ký, nên không thể đọc hộp thư của người khác bằng cách đổi tham số. Cũng vì vậy mà các
 * endpoint ghi không kiểm vai trò: ai đăng nhập cũng chỉ sửa được hộp thư của chính mình.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final InboxStreamService inboxStreamService;

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

    /**
     * Luồng Server-Sent Events: số chưa đọc và thông báo mới, đẩy ngay khi có. Không khai
     * {@code produces}: SseEmitter tự đặt {@code text/event-stream}, và client gửi Accept khác
     * vẫn nhận được luồng thay vì 406.
     */
    @GetMapping("/stream")
    public SseEmitter stream(AuthenticatedUser user) {
        return inboxStreamService.open(user.userId());
    }

    /** Đánh dấu đã đọc tất cả, cùng dạng với {@code /{id}/read} nhưng áp lên cả hộp thư. */
    @PatchMapping("/read")
    public ApiResponse<Integer> markAllRead(AuthenticatedUser user) {
        int updated = notificationService.markAllRead(user.userId());
        return ApiResponse.ok(updated, "Đã đánh dấu đã đọc " + updated + " thông báo");
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable Long id, AuthenticatedUser user) {
        return ApiResponse.ok(notificationService.markRead(id, user.userId()), "Đã đánh dấu đã đọc");
    }

    @GetMapping("/preferences")
    public ApiResponse<NotificationPreferenceResponse> getPreferences(AuthenticatedUser user) {
        return ApiResponse.ok(notificationService.getPreferences(user.userId()));
    }

    @PutMapping("/preferences")
    public ApiResponse<NotificationPreferenceResponse> updatePreferences(
            AuthenticatedUser user,
            @Valid @RequestBody UpdateNotificationPreferenceRequest request) {
        return ApiResponse.ok(notificationService.updatePreferences(user.userId(), request),
                "Đã lưu cài đặt thông báo");
    }
}
