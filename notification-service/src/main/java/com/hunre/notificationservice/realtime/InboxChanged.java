package com.hunre.notificationservice.realtime;

import com.hunre.notificationservice.dto.NotificationResponse;

/**
 * Hộp thư của một người vừa đổi: có thông báo mới, hoặc vừa đọc bớt. Sự kiện nội bộ của
 * Spring, không phải Kafka; {@link InboxStreamService} nghe sau khi transaction commit.
 *
 * @param userId  chủ hộp thư
 * @param created thông báo vừa tạo, {@code null} nếu chỉ là đánh dấu đã đọc
 */
public record InboxChanged(Long userId, NotificationResponse created) {

    public static InboxChanged created(Long userId, NotificationResponse notification) {
        return new InboxChanged(userId, notification);
    }

    public static InboxChanged read(Long userId) {
        return new InboxChanged(userId, null);
    }
}
