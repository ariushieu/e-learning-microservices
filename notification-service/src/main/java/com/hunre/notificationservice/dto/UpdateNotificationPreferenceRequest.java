package com.hunre.notificationservice.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body của {@code PUT /api/notifications/preferences}. Ghi đè cả bộ nên bắt buộc đủ hai cờ:
 * thiếu một cờ mà coi là "giữ nguyên" thì PUT không còn đúng nghĩa thay thế.
 */
public record UpdateNotificationPreferenceRequest(
        @NotNull(message = "Phải chọn bật hoặc tắt thông báo trong ứng dụng") Boolean inAppEnabled,
        @NotNull(message = "Phải chọn bật hoặc tắt thông báo qua email") Boolean emailEnabled) {
}
