package com.hunre.notificationservice.dto;

import com.hunre.notificationservice.entity.NotificationPreference;

/**
 * Tùy chọn nhận thông báo của người đang đăng nhập.
 *
 * @param inAppEnabled tạo thông báo trong ứng dụng khi có sự kiện; tắt thì sự kiện mới không
 *                     sinh thông báo, thông báo cũ vẫn còn
 * @param emailEnabled gửi email; lưu sẵn cho lúc có kênh email, hiện chưa gửi gì
 */
public record NotificationPreferenceResponse(boolean inAppEnabled, boolean emailEnabled) {

    /** Chưa có bản ghi nghĩa là chưa tắt gì. */
    public static final NotificationPreferenceResponse DEFAULTS = new NotificationPreferenceResponse(true, true);

    public static NotificationPreferenceResponse from(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                Boolean.TRUE.equals(preference.getInAppEnabled()),
                Boolean.TRUE.equals(preference.getEmailEnabled()));
    }
}
